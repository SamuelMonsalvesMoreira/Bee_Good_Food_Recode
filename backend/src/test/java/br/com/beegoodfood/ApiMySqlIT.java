package br.com.beegoodfood;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import br.com.beegoodfood.auth.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
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
        // Muitas chamadas de teste no mesmo loopback; o filtro tem testes proprios com limite real.
        registry.add("bee.security.auth-requests-per-minute", () -> 300);
    }

    @BeforeAll
    void fixtures() {
        // Banco exclusivo do runner. Nenhum seed de producao ou banco do usuario e utilizado.
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("bee_good_food_validacao");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class)).isZero();
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
    void anonymousCategoryWritesRequireAuthentication() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/categorias"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(10)).build();
        assertProblem(http.send(request, HttpResponse.BodyHandlers.ofString()), 401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categoria", Integer.class)).isEqualTo(3);
    }

    @Test
    void unknownRouteReturnsSafeJson() throws Exception {
        assertProblem(get("/api/v1/inexistente"), 401);
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

    @Test
    void signupPersistsHashAdultDeclarationAndOnlyClientRole() throws Exception {
        String email = newEmail();
        var response = post("/api/v1/auth/cadastro", registration(email));
        assertThat(response.statusCode()).isEqualTo(201);
        long id = tree(response).get("id").asLong();
        assertThat(tree(response).get("papeis").toString()).isEqualTo("[\"CLIENTE\"]");
        assertThat(response.body()).doesNotContain("senha", "hash", "accessToken", TEST_PASSWORD);
        String stored = jdbc.queryForObject("SELECT senha_hash FROM usuario WHERE id=?", String.class, id);
        assertThat(stored).startsWith("$2a$12$");
        assertThat(passwords.matches(TEST_PASSWORD, stored)).isTrue();
        assertThat(jdbc.queryForObject("SELECT maioridade_confirmada_em IS NOT NULL FROM usuario WHERE id=?", Boolean.class, id)).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario_papel WHERE usuario_id=?", Integer.class, id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nome", "email", "senhaCurta", "senhaUnicode", "papel", "ativo", "id", "senhaHash", "menor", "semDeclaracao", "declaracaoNula"})
    void invalidRegistrationNeverCreatesUser(String scenario) throws Exception {
        var body = registration(newEmail());
        switch (scenario) {
            case "nome" -> body.put("nome", " ");
            case "email" -> body.put("email", "nao-e-email");
            case "senhaCurta" -> body.put("senha", "curta");
            case "senhaUnicode" -> body.put("senha", "á".repeat(37));
            case "papel" -> body.put("papeis", List.of("ADMINISTRADOR"));
            case "ativo" -> body.put("ativo", true);
            case "id" -> body.put("id", 1);
            case "senhaHash" -> body.put("senhaHash", "qualquer");
            case "menor" -> body.put("maiorDeIdade", false);
            case "semDeclaracao" -> body.remove("maiorDeIdade");
            case "declaracaoNula" -> body.put("maiorDeIdade", null);
        }
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class);
        var response = post("/api/v1/auth/cadastro", body);
        assertProblem(response, 400);
        assertThat(response.body()).doesNotContain(TEST_PASSWORD, "qualquer", "senha_hash");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class)).isEqualTo(before);
    }

    @Test
    void concurrentCaseInsensitiveDuplicateCreatesExactlyOneClient() throws Exception {
        String email = newEmail();
        var first = http.sendAsync(postRequest("/api/v1/auth/cadastro", registration(email)), HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(postRequest("/api/v1/auth/cadastro", registration(" " + email.toUpperCase(java.util.Locale.ROOT) + " ")), HttpResponse.BodyHandlers.ofString());
        CompletableFuture.allOf(first, second).get(30, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(List.of(first.get().statusCode(), second.get().statusCode())).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email_normalizado=?", Integer.class, email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario_papel up JOIN usuario u ON u.id=up.usuario_id WHERE u.email_normalizado=?", Integer.class, email)).isEqualTo(1);
    }

    @Test
    void loginReturnsFifteenMinuteTokenAndCurrentProfileWithoutCookie() throws Exception {
        var account = account();
        var response = post("/api/v1/auth/login", Map.of("email", " " + account.email().toUpperCase(java.util.Locale.ROOT) + " ", "senha", TEST_PASSWORD));
        assertThat(response.statusCode()).isEqualTo(200);
        var jwt = decoder.decode(tree(response).get("accessToken").asText());
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.getSubject()).isEqualTo(Long.toString(account.id()));
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "senha", "papeis");
        assertThat(tree(response).get("expiraEmSegundos").asInt()).isEqualTo(900);
        assertThat(response.headers().firstValue("cache-control").orElse("")).contains("no-store");
        assertThat(response.headers().allValues("set-cookie")).isEmpty();
        var me = authenticated("/api/v1/auth/me", jwt.getTokenValue());
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(tree(me).get("id").asLong()).isEqualTo(account.id());
        assertThat(me.body()).doesNotContain(TEST_PASSWORD, "hash", "accessToken");
    }

    @Test
    void invalidUnknownAndInactiveLoginsHaveSameErrorAndDisabledTokenStopsWorking() throws Exception {
        var account = account();
        var wrong = post("/api/v1/auth/login", Map.of("email", account.email(), "senha", "senha-errada-123"));
        var unknown = post("/api/v1/auth/login", Map.of("email", newEmail(), "senha", "senha-errada-123"));
        jdbc.update("UPDATE usuario SET ativo=false WHERE id=?", account.id());
        var inactive = post("/api/v1/auth/login", Map.of("email", account.email(), "senha", TEST_PASSWORD));
        for (var response : List.of(wrong, unknown, inactive)) {
            assertProblem(response, 401);
            assertThat(tree(response).get("detail").asText()).isEqualTo("E-mail ou senha inválidos.");
        }
        assertProblem(authenticated("/api/v1/auth/me", account.token()), 401);
    }

    @Test
    void authorizationUsesCurrentDatabaseRolesNotClientClaims() throws Exception {
        var account = account();
        assertProblem(get("/api/v1/admin/usuarios"), 401);
        assertProblem(authenticated("/api/v1/admin/usuarios", account.token()), 403);
        var now = Instant.now();
        var fakeRoles = JwtClaimsSet.builder().issuer(TokenService.ISSUER).audience(List.of(TokenService.AUDIENCE))
                .subject(Long.toString(account.id())).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(900))
                .claim("papeis", List.of("ADMINISTRADOR")).claim("scope", "ROLE_ADMINISTRADOR").build();
        var signedWithFakeRoles = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), fakeRoles));
        assertProblem(authenticated("/api/v1/admin/usuarios", signedWithFakeRoles.getTokenValue()), 403);
        jdbc.update("INSERT INTO usuario_papel(usuario_id,papel_codigo) VALUES (?, 'RESPONSAVEL')", account.id());
        assertProblem(authenticated("/api/v1/admin/usuarios", account.token()), 403);
        jdbc.update("INSERT INTO usuario_papel(usuario_id,papel_codigo) VALUES (?, 'ADMINISTRADOR')", account.id());
        var admin = authenticated("/api/v1/admin/usuarios?tamanho=1", account.token());
        assertThat(admin.statusCode()).isEqualTo(200);
        assertThat(tree(admin).get("conteudo").size()).isEqualTo(1);
        assertThat(admin.body()).doesNotContain("senha", "hash", "accessToken");
        assertProblem(authenticated("/api/v1/admin/usuarios?tamanho=101", account.token()), 400);
        jdbc.update("DELETE FROM usuario_papel WHERE usuario_id=? AND papel_codigo='ADMINISTRADOR'", account.id());
        assertProblem(authenticated("/api/v1/admin/usuarios", account.token()), 403);
    }

    @ParameterizedTest
    @ValueSource(strings = {"expirado", "futuro", "emissor", "audiencia", "semAudiencia", "semExpiracao", "semEmissao", "semInicio", "semAssunto", "duracaoLonga", "usuarioInexistente", "idInvalido", "idOverflow", "assinatura", "malformado"})
    void invalidTokensAreRejected(String scenario) throws Exception {
        var account = account();
        Instant now = Instant.now();
        var builder = JwtClaimsSet.builder().issuer(TokenService.ISSUER).audience(List.of(TokenService.AUDIENCE))
                .subject(Long.toString(account.id())).issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(900));
        switch (scenario) {
            case "expirado" -> builder.issuedAt(now.minusSeconds(1000)).notBefore(now.minusSeconds(1000)).expiresAt(now.minusSeconds(1));
            case "futuro" -> builder.issuedAt(now.plusSeconds(120)).notBefore(now.plusSeconds(120)).expiresAt(now.plusSeconds(900));
            case "emissor" -> builder.issuer("outro-emissor");
            case "audiencia" -> builder.audience(List.of("outra-api"));
            case "semExpiracao" -> builder.claims(claims -> claims.remove("exp"));
            case "semAudiencia" -> builder.claims(claims -> claims.remove("aud"));
            case "semEmissao" -> builder.claims(claims -> claims.remove("iat"));
            case "semInicio" -> builder.claims(claims -> claims.remove("nbf"));
            case "semAssunto" -> builder.claims(claims -> claims.remove("sub"));
            case "duracaoLonga" -> builder.expiresAt(now.plusSeconds(3600));
            case "usuarioInexistente" -> builder.subject("999999999");
            case "idInvalido" -> builder.subject("abc");
            case "idOverflow" -> builder.subject("9999999999999999999");
        }
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), builder.build())).getTokenValue();
        if (scenario.equals("assinatura")) {
            int start = token.lastIndexOf('.') + 1;
            token = token.substring(0, start) + (token.charAt(start) == 'A' ? 'B' : 'A') + token.substring(start + 1);
        }
        if (scenario.equals("malformado")) { token = "nao-e-jwt"; }
        assertProblem(authenticated("/api/v1/auth/me", token), 401);
    }

    @Test
    void missingTokenAndBasicAuthenticationAreNotAccepted() throws Exception {
        assertProblem(get("/api/v1/auth/me"), 401);
        assertProblem(get("/api/v1/auth/me?access_token=qualquer"), 401);
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/me")).header("Authorization", "Basic dGVzdGU6dGVzdGU=").GET().build();
        assertProblem(http.send(request, HttpResponse.BodyHandlers.ofString()), 401);
    }

    @Test
    void malformedJsonReturnsSafeError() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/cadastro")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"senha\":")).build();
        assertProblem(http.send(request, HttpResponse.BodyHandlers.ofString()), 400);
    }

    @Test
    void unsupportedContentTypeIsNotAnInternalError() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/cadastro")).header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("texto")).build();
        assertProblem(http.send(request, HttpResponse.BodyHandlers.ofString()), 415);
    }

    @Test
    void authenticatedCategoryWritesRemainUnimplemented() throws Exception {
        var account = account();
        var request = HttpRequest.newBuilder(uri("/api/v1/categorias")).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + account.token()).POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertProblem(response, 405);
        assertThat(response.headers().firstValue("allow").orElse("")).contains("GET");
    }

    private static final String TEST_PASSWORD = "Senha-ficticia-teste-123";
    private record Account(long id, String email, String token) { }
    private String newEmail() { return "teste-" + UUID.randomUUID() + "@example.invalid"; }
    private Map<String, Object> registration(String email) {
        return new LinkedHashMap<>(Map.of("nome", "Cliente de Teste", "email", email, "senha", TEST_PASSWORD, "maiorDeIdade", true));
    }
    private Account account() throws Exception {
        String email = newEmail();
        var created = post("/api/v1/auth/cadastro", registration(email));
        assertThat(created.statusCode()).isEqualTo(201);
        var login = post("/api/v1/auth/login", Map.of("email", email, "senha", TEST_PASSWORD));
        assertThat(login.statusCode()).isEqualTo(200);
        return new Account(tree(created).get("id").asLong(), email, tree(login).get("accessToken").asText());
    }
    private HttpRequest postRequest(String path, Object body) {
        return HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
    }
    private HttpResponse<String> post(String path, Object body) throws Exception {
        return http.send(postRequest(path, body), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> authenticated(String path, String token) throws Exception {
        return http.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
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
