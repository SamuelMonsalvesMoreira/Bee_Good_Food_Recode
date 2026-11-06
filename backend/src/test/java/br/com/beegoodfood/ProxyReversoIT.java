package br.com.beegoodfood;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 7: atras de proxy confiavel (FORWARD_HEADERS_STRATEGY=native), o limite de tentativas vale por
 * cliente real (X-Forwarded-For). Os testes rodam no loopback, que e o proxy confiavel padrao.
 * Contexto proprio (sem IntegracaoBase) para usar um limite baixo de tentativas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProxyReversoIT {
    @Autowired Environment environment;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @DynamicPropertySource
    static void atrasDeProxy(DynamicPropertyRegistry registry) {
        String url = System.getenv("BEE_TEST_MYSQL_URL");
        if (url == null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/bee_good_food_validacao\\?.*")) {
            throw new IllegalStateException("Execute database/scripts/validar-banco.ps1 -Api para usar MySQL isolado.");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("BEE_TEST_MYSQL_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("BEE_TEST_MYSQL_PASSWORD"));
        registry.add("server.forward-headers-strategy", () -> "native");
        registry.add("bee.security.auth-requests-per-minute", () -> 3);
    }

    @Test
    void limiteDeTentativasSeparaClientesReaisAtrasDoProxy() throws Exception {
        for (int i = 0; i < 3; i++) { assertThat(login("203.0.113.10").statusCode()).isEqualTo(400); }
        var bloqueado = login("203.0.113.10");
        assertThat(bloqueado.statusCode()).isEqualTo(429);
        assertThat(bloqueado.headers().firstValue("retry-after")).hasValue("60");
        assertThat(bloqueado.body()).doesNotContain("203.0.113.10");
        // Outro cliente atras do mesmo proxy nao herda o bloqueio.
        assertThat(login("198.51.100.20").statusCode()).isEqualTo(400);
    }

    private HttpResponse<String> login(String clienteReal) throws Exception {
        var uri = URI.create("http://127.0.0.1:" + environment.getRequiredProperty("local.server.port") + "/api/v1/auth/login");
        return http.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-Forwarded-For", clienteReal)
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
    }
}
