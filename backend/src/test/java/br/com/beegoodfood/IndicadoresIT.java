package br.com.beegoodfood;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Etapa 5.7: formulas de metricas-e-validacao.md (CA17). Periodo de setembro/2026 em America/Sao_Paulo:
 * [2026-09-01T03:00Z, 2026-10-01T03:00Z). Instantes de criacao/conclusao sao fixados por SQL apos o fluxo real da API.
 */
class IndicadoresIT extends IntegracaoBase {
    private static final String SETEMBRO = "inicio=2026-09-01T00:00&fim=2026-10-01T00:00";
    private Conta admin;
    private Loja loja;
    private long oferta;

    @BeforeAll
    void preparar() throws Exception {
        limparBanco(jdbc);
        admin = admin();
        long categoria = categoria(admin, "Padaria");
        loja = loja(admin, "Padaria Métricas", "Santos", "Centro");
        oferta = ofertaPublicada(loja.dono(), produto(loja.dono(), loja.estabelecimentoId(), categoria, "Pão"), oferta("10.00", "6.00", 500, 6, 8));
        var cliente = conta();

        // Coorte de setembro (10 pedidos): 6 concluidos (um deles so em outubro), 2 cancelados, 2 em andamento.
        for (int i = 1; i <= 5; i++) {
            long id = pedido(cliente, 1);
            concluir(id);
            datas(id, "2026-09-0" + i + " 12:00:00", "2026-09-1" + i + " 12:00:00");
        }
        long concluidoEmOutubro = pedido(cliente, 1);
        concluir(concluidoEmOutubro);
        datas(concluidoEmOutubro, "2026-09-20 12:00:00", "2026-10-05 12:00:00");
        for (int i = 0; i < 2; i++) {
            long id = pedido(cliente, 1);
            assertThat(send("POST", "/api/v1/pedidos/" + id + "/cancelar", cliente.token(), Map.of()).statusCode()).isEqualTo(200);
            datas(id, "2026-09-21 12:00:00", null);
        }
        for (int i = 0; i < 2; i++) { datas(pedido(cliente, 1), "2026-09-22 12:00:00", null); }

        // Criado em agosto e concluido exatamente no inicio: entra no volume, nao na coorte.
        long noInicio = pedido(cliente, 2);
        concluir(noInicio);
        datas(noInicio, "2026-08-20 12:00:00", "2026-09-01 03:00:00");
        // Concluido exatamente no fim: pertence ao periodo seguinte.
        long noFim = pedido(cliente, 3);
        concluir(noFim);
        datas(noFim, "2026-08-21 12:00:00", "2026-10-01 03:00:00");
        // Instante do pedido criado exatamente no fim tambem fica fora da coorte.
        datas(pedido(cliente, 1), "2026-10-01 03:00:00", null);

        // Outro estabelecimento com conclusao em setembro: nunca entra nos indicadores desta loja.
        var outra = loja(admin, "Concorrente", "Santos", "Centro");
        long ofertaOutra = ofertaPublicada(outra.dono(), produto(outra.dono(), outra.estabelecimentoId(), categoria, "Pão"), oferta("10.00", "1.00", 50, 6, 8));
        var r = send("POST", "/api/v1/pedidos", cliente.token(), Map.of("formaPagamento", "PIX", "itens", List.of(Map.of("ofertaId", ofertaOutra, "quantidade", 9))),
                "Idempotency-Key", "k-" + UUID.randomUUID());
        long deOutra = tree(r).get("id").asLong();
        for (var s : List.of("CONFIRMADO", "EM_PREPARO", "PRONTO_PARA_RETIRADA", "CONCLUIDO")) {
            send("POST", "/api/v1/pedidos/" + deOutra + "/status", outra.dono().token(), Map.of("status", s));
        }
        datas(deOutra, "2026-09-10 12:00:00", "2026-09-11 12:00:00");
        // A publicacao real ocorreu hoje; fixar em julho para o teste de atividade sem pedidos.
        jdbc.update("UPDATE oferta SET publicada_em='2026-07-15 12:00:00' WHERE id=?", oferta);
    }

    @Test
    void formulasDoPeriodoComCoorteELimitesSemiabertos() throws Exception {
        var response = get(url(SETEMBRO), loja.dono().token());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        var body = tree(response);
        assertThat(body.get("fusoIana").asText()).isEqualTo("America/Sao_Paulo");
        assertThat(body.get("periodo").get("inicio").asText()).isEqualTo("2026-09-01T03:00:00Z");
        assertThat(body.get("periodo").get("fim").asText()).isEqualTo("2026-10-01T03:00:00Z");
        assertThat(body.get("periodo").get("inicioLocal").asText()).startsWith("2026-09-01T00:00");
        assertThat(body.get("consultadoEm").isNull()).isFalse();
        // Volume: 5 concluidos de setembro + o concluido exatamente no inicio (quantidade 2).
        assertThat(body.get("pedidosConcluidos").asLong()).isEqualTo(6);
        assertThat(body.get("itensComercializados").asLong()).isEqualTo(7);
        assertThat(body.get("valorMovimentado").decimalValue()).isEqualByComparingTo("42.00");
        assertThat(body.get("economiaEstimada").decimalValue()).isEqualByComparingTo("28.00");
        assertThat(body.get("estabelecimentoComAtividade").asInt()).isOne();
        var coorte = body.get("coorte");
        assertThat(coorte.get("pedidos").asLong()).isEqualTo(10);
        assertThat(coorte.get("concluidos").asLong()).isEqualTo(6);
        assertThat(coorte.get("cancelados").asLong()).isEqualTo(2);
        assertThat(coorte.get("emAndamento").asLong()).isEqualTo(2);
        assertThat(coorte.get("taxaConclusao").decimalValue()).isEqualByComparingTo("60");
        assertThat(coorte.get("taxaCancelamento").decimalValue()).isEqualByComparingTo("20");
    }

    @Test
    void periodoSeguinteRecebeConclusaoNoFimESemCoorteTemTaxaNa() throws Exception {
        var outubro = tree(get(url("inicio=2026-10-01T00:00&fim=2026-11-01T00:00"), loja.dono().token()));
        // Concluido em 05/10 (coorte de setembro) + concluido exatamente no fim de setembro.
        assertThat(outubro.get("pedidosConcluidos").asLong()).isEqualTo(2);
        assertThat(outubro.get("itensComercializados").asLong()).isEqualTo(4);
        assertThat(outubro.get("coorte").get("pedidos").asLong()).isOne();
        assertThat(outubro.get("coorte").get("taxaConclusao").decimalValue()).isEqualByComparingTo("0");
        var julho = tree(get(url("inicio=2026-07-01T00:00&fim=2026-08-01T00:00"), loja.dono().token()));
        assertThat(julho.get("pedidosConcluidos").asLong()).isZero();
        assertThat(julho.get("valorMovimentado").decimalValue()).isEqualByComparingTo("0");
        assertThat(julho.get("coorte").get("pedidos").asLong()).isZero();
        assertThat(julho.get("coorte").get("taxaConclusao").isNull()).isTrue();
        assertThat(julho.get("coorte").get("taxaCancelamento").isNull()).isTrue();
        // Publicacao em julho sem pedidos ainda caracteriza atividade.
        assertThat(julho.get("estabelecimentoComAtividade").asInt()).isOne();
        var janeiro = tree(get(url("inicio=2026-01-01T00:00&fim=2026-02-01T00:00"), loja.dono().token()));
        assertThat(janeiro.get("estabelecimentoComAtividade").asInt()).isZero();
    }

    @Test
    void fimOmitidoUsaOInstanteDaConsulta() throws Exception {
        var body = tree(get(url("inicio=2026-01-01T00:00"), loja.dono().token()));
        var fim = Instant.parse(body.get("periodo").get("fim").asText());
        assertThat(java.time.Duration.between(fim, Instant.now()).abs().toSeconds()).isLessThan(60);
        // Todos os concluidos desta loja (setembro, outubro e o do limite).
        assertThat(body.get("pedidosConcluidos").asLong()).isEqualTo(8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"inicio=2026-09-01T00:00&fim=2026-09-01T00:00", "inicio=2026-10-01T00:00&fim=2026-09-01T00:00",
            "inicio=2026-09-01T00:00&fim=2099-01-01T00:00", "fim=2026-09-01T00:00", "inicio=01/09/2026", "inicio=2026-13-01T00:00"})
    void periodoInvalidoRetorna400(String query) throws Exception {
        assertProblem(get(url(query), loja.dono().token()), 400);
    }

    @Test
    void indicadoresSoParaOProprioEstabelecimento() throws Exception {
        var outra = loja(admin, "Curiosa", "Santos", "Centro");
        var response = get(url(SETEMBRO), outra.dono().token());
        assertProblem(response, 404);
        assertThat(response.body()).doesNotContain("pedidosConcluidos", "42.00");
        assertProblem(get(url(SETEMBRO), conta().token()), 403);
        assertProblem(get(url(SETEMBRO), null), 401);
    }

    // ---- Auxiliares ----

    private String url(String query) { return "/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/indicadores?" + query; }

    private long pedido(Conta cliente, int quantidade) throws Exception {
        var response = send("POST", "/api/v1/pedidos", cliente.token(),
                Map.of("formaPagamento", "PIX", "itens", List.of(Map.of("ofertaId", oferta, "quantidade", quantidade))),
                "Idempotency-Key", "k-" + UUID.randomUUID());
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return tree(response).get("id").asLong();
    }

    private void concluir(long id) throws Exception {
        for (var s : List.of("CONFIRMADO", "EM_PREPARO", "PRONTO_PARA_RETIRADA", "CONCLUIDO")) {
            assertThat(send("POST", "/api/v1/pedidos/" + id + "/status", loja.dono().token(), Map.of("status", s)).statusCode()).isEqualTo(200);
        }
    }

    /** Instantes em UTC. */
    private void datas(long pedido, String criadoEm, String concluidoEm) {
        jdbc.update("UPDATE pedido SET criado_em=?, concluido_em=? WHERE id=?", criadoEm, concluidoEm, pedido);
    }
}
