package br.com.beegoodfood;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Base dos testes HTTP/MySQL da Etapa 5. Usa somente o banco isolado do runner. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegracaoBase {
    static final String SENHA = "Senha-ficticia-teste-123";
    private static final List<String> TABELAS = List.of("historico_status_pedido", "item_pedido", "pedido", "oferta",
            "produto", "categoria", "historico_status_estabelecimento", "endereco_estabelecimento", "estabelecimento",
            "usuario_papel", "usuario");

    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected Environment environment;
    protected final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    protected final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        String url = System.getenv("BEE_TEST_MYSQL_URL");
        if (url == null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bee_good_food_validacao\\?.*")) {
            throw new IllegalStateException("Execute database/scripts/validar-banco.ps1 -Api para usar MySQL isolado.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("BEE_TEST_MYSQL_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("BEE_TEST_MYSQL_PASSWORD"));
        registry.add("bee.security.auth-requests-per-minute", () -> 300);
    }

    /** Cada classe comeca do zero. Recusa qualquer banco que nao seja o de validacao. */
    static void limparBanco(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("bee_good_food_validacao");
        TABELAS.forEach(tabela -> jdbc.update("DELETE FROM " + tabela));
    }

    // ---- Contas ----

    record Conta(long id, String email, String token) { }

    Conta conta() throws Exception {
        String email = "teste-" + UUID.randomUUID() + "@example.invalid";
        var created = send("POST", "/api/v1/auth/cadastro", null,
                Map.of("nome", "Pessoa de Teste", "email", email, "senha", SENHA, "maiorDeIdade", true));
        assertThat(created.statusCode()).isEqualTo(201);
        var login = send("POST", "/api/v1/auth/login", null, Map.of("email", email, "senha", SENHA));
        assertThat(login.statusCode()).isEqualTo(200);
        return new Conta(tree(created).get("id").asLong(), email, tree(login).get("accessToken").asText());
    }

    /** Administrador criado por procedimento controlado (SQL direto), como documentado. */
    Conta admin() throws Exception {
        var conta = conta();
        jdbc.update("INSERT INTO usuario_papel(usuario_id,papel_codigo) VALUES (?, 'ADMINISTRADOR')", conta.id());
        return conta;
    }

    // ---- Fixtures de dominio via API ----

    Map<String, Object> endereco(String cidade, String bairro) {
        var e = new LinkedHashMap<String, Object>();
        e.put("logradouro", "Rua das Flores");
        e.put("numero", "100");
        e.put("complemento", "Loja 2");
        e.put("bairro", bairro);
        e.put("cidade", cidade);
        e.put("estado", "SP");
        e.put("cep", "01001-000");
        e.put("fusoIana", "America/Sao_Paulo");
        return e;
    }

    Map<String, Object> estabelecimento(String nome, String cidade, String bairro) {
        var body = new LinkedHashMap<String, Object>();
        body.put("nome", nome);
        body.put("descricao", "Padaria de bairro");
        body.put("endereco", endereco(cidade, bairro));
        return body;
    }

    long solicitarEstabelecimento(Conta dono, String nome, String cidade, String bairro) throws Exception {
        var response = send("POST", "/api/v1/estabelecimentos", dono.token(), estabelecimento(nome, cidade, bairro));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return tree(response).get("id").asLong();
    }

    long estabelecimentoAtivo(Conta dono, Conta admin, String nome, String cidade, String bairro) throws Exception {
        long id = solicitarEstabelecimento(dono, nome, cidade, bairro);
        var aprovado = send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", admin.token(), Map.of("motivo", "Documentação conferida"));
        assertThat(aprovado.statusCode()).as(aprovado.body()).isEqualTo(200);
        return id;
    }

    // ---- HTTP ----

    HttpRequest request(String method, String path, String token, Object body, String... headers) {
        var builder = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30));
        if (token != null) { builder.header("Authorization", "Bearer " + token); }
        for (int i = 0; i + 1 < headers.length; i += 2) { builder.header(headers[i], headers[i + 1]); }
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body instanceof String s ? s : json.writeValueAsString(body)));
        }
        return builder.build();
    }

    HttpResponse<String> send(String method, String path, String token, Object body, String... headers) throws Exception {
        return http.send(request(method, path, token, body, headers), HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> get(String path, String token) throws Exception { return send("GET", path, token, null); }

    /** Dispara as requisicoes ao mesmo tempo e devolve as respostas na mesma ordem. */
    List<HttpResponse<String>> simultaneas(List<HttpRequest> requests) throws Exception {
        List<CompletableFuture<HttpResponse<String>>> futures = requests.stream()
                .map(r -> http.sendAsync(r, HttpResponse.BodyHandlers.ofString())).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(60, TimeUnit.SECONDS);
        return futures.stream().map(CompletableFuture::join).toList();
    }

    URI uri(String path) {
        return URI.create("http://127.0.0.1:" + environment.getRequiredProperty("local.server.port") + path);
    }

    JsonNode tree(HttpResponse<String> response) { return json.readTree(response.body()); }

    int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }

    void assertProblem(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(response.headers().firstValue("content-type").orElse("")).contains("application/problem+json");
        assertThat(tree(response).get("status").asInt()).isEqualTo(status);
        assertThat(response.body()).doesNotContain("stackTrace", "jdbc:", "org.hibernate", "senha_hash", "Duplicate entry");
    }
}
