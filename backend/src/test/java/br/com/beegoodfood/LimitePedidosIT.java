package br.com.beegoodfood;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** DEC-041: limite de pedidos em andamento por cliente. Contexto proprio, com limite baixo. */
@TestPropertySource(properties = "bee.pedidos.max-em-andamento-por-cliente=2")
class LimitePedidosIT extends IntegracaoBase {
    private Loja loja;
    private long oferta;

    @BeforeAll
    void preparar() throws Exception {
        limparBanco(jdbc);
        var admin = admin();
        long categoria = categoria(admin, "Padaria");
        loja = loja(admin, "Padaria do Limite", "Santos", "Centro");
        oferta = ofertaPublicada(loja.dono(), produto(loja.dono(), loja.estabelecimentoId(), categoria, "Pão"),
                oferta("10.00", "5.00", 50, 6, 8));
    }

    @Test
    void terceiraReservaEmAndamentoERecusadaAteUmaSairDaFila() throws Exception {
        var cliente = conta();
        String primeiraChave = chave();
        long primeiro = id(criar(cliente, primeiraChave), 201);
        long segundo = id(criar(cliente, chave()), 201);
        int estoqueAntes = estoque();

        var recusado = criar(cliente, chave());
        assertProblem(recusado, 409);
        assertThat(tree(recusado).get("detail").asText()).contains("2 reservas em andamento");
        assertThat(estoque()).isEqualTo(estoqueAntes);

        // Repetir uma reserva ja feita (mesma chave) nao conta como nova.
        var repetido = criar(cliente, primeiraChave);
        assertThat(repetido.statusCode()).isEqualTo(200);
        assertThat(tree(repetido).get("id").asLong()).isEqualTo(primeiro);

        // Outro cliente nao e afetado.
        id(criar(conta(), chave()), 201);

        // Cancelar libera uma vaga.
        assertThat(send("POST", "/api/v1/pedidos/" + segundo + "/cancelar", cliente.token(), null).statusCode()).isEqualTo(200);
        long terceiro = id(criar(cliente, chave()), 201);
        assertProblem(criar(cliente, chave()), 409);

        // Retirar tambem libera: pedido concluido nao conta.
        for (var s : List.of("CONFIRMADO", "EM_PREPARO", "PRONTO_PARA_RETIRADA", "CONCLUIDO")) {
            assertThat(send("POST", "/api/v1/pedidos/" + terceiro + "/status", loja.dono().token(),
                    Map.of("status", s, "codigoRetirada", codigoRetirada(terceiro))).statusCode()).isEqualTo(200);
        }
        id(criar(cliente, chave()), 201);
    }

    @Test
    void reservasSimultaneasDoMesmoClienteRespeitamOLimite() throws Exception {
        var cliente = conta();
        int estoqueAntes = estoque();
        var requisicoes = new ArrayList<HttpRequest>();
        for (int i = 0; i < 6; i++) { requisicoes.add(requisicao(cliente, chave())); }
        var codigos = simultaneas(requisicoes).stream().map(HttpResponse::statusCode).toList();
        assertThat(codigos).containsExactlyInAnyOrder(201, 201, 409, 409, 409, 409);
        assertThat(count("SELECT COUNT(*) FROM pedido WHERE cliente_id=?", cliente.id())).isEqualTo(2);
        assertThat(estoque()).isEqualTo(estoqueAntes - 2);
    }

    private HttpRequest requisicao(Conta cliente, String chave) {
        return request("POST", "/api/v1/pedidos", cliente.token(),
                Map.of("formaPagamento", "PIX", "itens", List.of(Map.of("ofertaId", oferta, "quantidade", 1))),
                "Idempotency-Key", chave);
    }

    private HttpResponse<String> criar(Conta cliente, String chave) throws Exception {
        return http.send(requisicao(cliente, chave), HttpResponse.BodyHandlers.ofString());
    }

    private long id(HttpResponse<String> response, int esperado) throws Exception {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(esperado);
        return tree(response).get("id").asLong();
    }

    private int estoque() {
        return jdbc.queryForObject("SELECT estoque_disponivel FROM oferta WHERE id=?", Integer.class, oferta);
    }

    private String chave() { return "limite-" + UUID.randomUUID(); }
}
