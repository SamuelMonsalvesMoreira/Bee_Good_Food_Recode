package br.com.beegoodfood;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Etapa 6: CORS somente para a origem explicita do frontend, sem credenciais de navegador. */
class CorsIT extends IntegracaoBase {
    static final String ORIGEM_FRONTEND = "http://localhost:5173";

    @Test
    void preflightDaOrigemDoFrontendLiberaCabecalhosDoPedido() throws Exception {
        var response = preflight(ORIGEM_FRONTEND, "POST", "authorization,content-type,idempotency-key");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("access-control-allow-origin")).hasValue(ORIGEM_FRONTEND);
        assertThat(response.headers().firstValue("access-control-allow-headers").orElse("").toLowerCase())
                .contains("authorization", "idempotency-key", "content-type");
        assertThat(response.headers().firstValue("access-control-allow-credentials")).isEmpty();
    }

    @Test
    void preflightLiberaRemocaoParaOFrontend() throws Exception {
        var response = preflight(ORIGEM_FRONTEND, "DELETE", "authorization");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Access-Control-Allow-Methods").orElse("")).contains("DELETE");
    }

    @Test
    void outraOrigemNaoRecebeLiberacao() throws Exception {
        var response = preflight("https://site-malicioso.example", "POST", "authorization");
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("access-control-allow-origin")).isEmpty();
        var simples = http.send(HttpRequest.newBuilder(uri("/api/v1/categorias")).header("Origin", "https://site-malicioso.example")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(simples.headers().firstValue("access-control-allow-origin")).isEmpty();
    }

    @Test
    void respostaComumExpoeCabecalhoDeIdempotencia() throws Exception {
        var response = http.send(HttpRequest.newBuilder(uri("/api/v1/ofertas")).header("Origin", ORIGEM_FRONTEND)
                .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("access-control-allow-origin")).hasValue(ORIGEM_FRONTEND);
        assertThat(response.headers().firstValue("access-control-expose-headers").orElse("")).contains("Idempotent-Replayed");
    }

    private HttpResponse<String> preflight(String origem, String metodo, String cabecalhos) throws Exception {
        return http.send(HttpRequest.newBuilder(uri("/api/v1/pedidos")).header("Origin", origem)
                .header("Access-Control-Request-Method", metodo).header("Access-Control-Request-Headers", cabecalhos)
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
