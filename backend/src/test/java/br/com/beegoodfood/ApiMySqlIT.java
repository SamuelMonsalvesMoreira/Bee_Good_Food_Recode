package br.com.beegoodfood;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiMySqlIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired Environment environment;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        String url = System.getenv("BEE_TEST_MYSQL_URL");
        if (url == null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bee_good_food_validacao\\?.*")) {
            throw new IllegalStateException("Execute database/scripts/validar-banco.ps1 -Api para usar MySQL isolado.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("BEE_TEST_MYSQL_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("BEE_TEST_MYSQL_PASSWORD"));
    }

    @BeforeAll
    void fixtures() {
        // Banco exclusivo do runner. Nenhum seed de producao ou banco do usuario e utilizado.
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("bee_good_food_validacao");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Integer.class)).isZero();
        jdbc.update("INSERT INTO categoria (id,nome,nome_normalizado,slug,ativo) VALUES "
                + "(101,'Doces','doces','doces',true),"
                + "(102,'Padaria','padaria','padaria',true),"
                + "(103,'Oculta','oculta','oculta',false)");
    }

    @Test
    void flywayCreatesSchemaAndReferenceRoles() {
        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success=1 ORDER BY installed_rank", String.class))
                .containsExactly("1", "2");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()", Integer.class)).isEqualTo(13);
        assertThat(jdbc.queryForList("SELECT codigo FROM papel ORDER BY codigo", String.class))
                .containsExactly("ADMINISTRADOR", "CLIENTE", "RESPONSAVEL");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class)).isZero();
    }

    @Test
    void healthChecksDatabaseWithoutExposingCredentials() throws Exception {
        var response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(tree(response).get("status").asText()).isEqualTo("UP");
        assertThat(response.body()).doesNotContain("jdbc", "password", "components");
    }

    @Test
    void activeCategoriesUseStablePaginationAndPublicDto() throws Exception {
        var first = get("/api/v1/categorias?tamanho=1");
        var second = get("/api/v1/categorias?tamanho=1&pagina=1");
        assertThat(first.statusCode()).isEqualTo(200);
        var body = tree(first);
        assertThat(body.get("totalElementos").asInt()).isEqualTo(2);
        assertThat(body.get("totalPaginas").asInt()).isEqualTo(2);
        assertThat(body.get("ultima").asBoolean()).isFalse();
        assertThat(body.get("conteudo").get(0).get("id").asLong()).isEqualTo(101);
        assertThat(tree(second).get("conteudo").get(0).get("id").asLong()).isEqualTo(102);
        assertThat(tree(second).get("ultima").asBoolean()).isTrue();
        assertThat(first.body()).doesNotContain("ativo", "nome_normalizado", "criado_em");
    }

    @Test
    void missingPageReturnsEmptyCollection() throws Exception {
        var response = get("/api/v1/categorias?pagina=10");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(tree(response).get("conteudo").size()).isZero();
    }

    @Test
    void categoryDetailReadsPersistedData() throws Exception {
        var response = get("/api/v1/categorias/102");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(tree(response).get("nome").asText()).isEqualTo("Padaria");
    }

    @ParameterizedTest
    @ValueSource(strings = {"103", "99999"})
    void inactiveAndUnknownCategoriesReturnSameNotFound(String id) throws Exception {
        var response = get("/api/v1/categorias/" + id);
        assertProblem(response, 404);
        assertThat(tree(response).get("detail").asText()).isEqualTo("Categoria não encontrada.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"?pagina=-1", "?tamanho=0", "?tamanho=101", "?pagina=abc", "/0", "/abc"})
    void invalidParametersReturnFieldErrors(String query) throws Exception {
        var response = get("/api/v1/categorias" + query);
        assertProblem(response, 400);
        assertThat(tree(response).get("campos").isEmpty()).isFalse();
    }

    @Test
    void stageThreeDoesNotAllowCategoryWrites() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/categorias"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(10)).build();
        assertProblem(http.send(request, HttpResponse.BodyHandlers.ofString()), 405);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Integer.class)).isEqualTo(3);
    }

    @Test
    void unknownRouteReturnsSafeJson() throws Exception {
        assertProblem(get("/api/v1/inexistente"), 404);
    }

    @Test
    void openApiDocumentsImplementedEndpoints() throws Exception {
        var response = get("/v3/api-docs");
        assertThat(response.statusCode()).isEqualTo(200);
        var paths = tree(response).get("paths");
        assertThat(paths.has("/api/v1/categorias")).isTrue();
        assertThat(paths.has("/api/v1/categorias/{id}")).isTrue();
        assertThat(paths.get("/api/v1/categorias").has("post")).isFalse();
    }

    @Test
    void swaggerInterfaceIsServed() throws Exception {
        var response = get("/swagger-ui/index.html");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Swagger UI");
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + environment.getRequiredProperty("local.server.port") + path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode tree(HttpResponse<String> response) { return json.readTree(response.body()); }

    private void assertProblem(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
        assertThat(tree(response).get("status").asInt()).isEqualTo(status);
        assertThat(response.body()).doesNotContain("stackTrace", "jdbc:", "org.hibernate", "senha_hash");
    }
}
