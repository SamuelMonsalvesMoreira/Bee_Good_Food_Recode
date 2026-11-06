package br.com.beegoodfood;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Etapa 5.1 e 5.2: solicitacao, propriedade e moderacao com historico (CA03, CA19, CA22, CA26). */
class EstabelecimentoIT extends IntegracaoBase {
    private Conta admin;

    @BeforeAll
    void preparar() throws Exception {
        limparBanco(jdbc);
        admin = admin();
    }

    @Test
    void solicitacaoNasceEmAnaliseVinculadaAoTokenComHistoricoInicial() throws Exception {
        var dono = conta();
        var response = send("POST", "/api/v1/estabelecimentos", dono.token(), estabelecimento("Padaria Sol", "São Paulo", "Sé"));
        assertThat(response.statusCode()).isEqualTo(201);
        var body = tree(response);
        long id = body.get("id").asLong();
        assertThat(body.get("status").asText()).isEqualTo("PENDENTE_ANALISE");
        assertThat(body.get("responsavelId").asLong()).isEqualTo(dono.id());
        assertThat(body.get("endereco").get("cep").asText()).isEqualTo("01001000");
        assertThat(body.get("endereco").get("fusoIana").asText()).isEqualTo("America/Sao_Paulo");
        assertThat(count("SELECT COUNT(*) FROM estabelecimento WHERE id=? AND responsavel_id=?", id, dono.id())).isOne();
        assertThat(count("SELECT COUNT(*) FROM endereco_estabelecimento WHERE estabelecimento_id=?", id)).isOne();
        assertThat(count("SELECT COUNT(*) FROM historico_status_estabelecimento WHERE estabelecimento_id=? "
                + "AND status_anterior IS NULL AND status_novo='PENDENTE_ANALISE' AND ator_id=?", id, dono.id())).isOne();
        // Solicitar nao concede RESPONSAVEL.
        assertThat(tree(get("/api/v1/auth/me", dono.token())).get("papeis").toString()).isEqualTo("[\"CLIENTE\"]");
    }

    @Test
    void fusoPadraoQuandoOmitido() throws Exception {
        var dono = conta();
        var body = estabelecimento("Sem fuso", "Campinas", "Centro");
        @SuppressWarnings("unchecked") var endereco = (Map<String, Object>) body.get("endereco");
        endereco.remove("fusoIana");
        var response = send("POST", "/api/v1/estabelecimentos", dono.token(), body);
        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(tree(response).get("endereco").get("fusoIana").asText()).isEqualTo("America/Sao_Paulo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"semNome", "semEndereco", "uf", "ufInexistente", "cep", "fuso", "fusoOffset", "responsavel", "status", "semBairro"})
    void solicitacaoInvalidaNaoCriaRegistro(String cenario) throws Exception {
        var dono = conta();
        var body = estabelecimento("Teste", "Santos", "Gonzaga");
        @SuppressWarnings("unchecked") var endereco = (Map<String, Object>) body.get("endereco");
        switch (cenario) {
            case "semNome" -> body.put("nome", " ");
            case "semEndereco" -> body.remove("endereco");
            case "uf" -> endereco.put("estado", "São");
            case "ufInexistente" -> endereco.put("estado", "XX");
            case "cep" -> endereco.put("cep", "123");
            case "fuso" -> endereco.put("fusoIana", "Marte/Base");
            case "fusoOffset" -> endereco.put("fusoIana", "+03:00");
            case "responsavel" -> body.put("responsavelId", admin.id());
            case "status" -> body.put("status", "ATIVO");
            case "semBairro" -> endereco.put("bairro", "");
        }
        int antes = count("SELECT COUNT(*) FROM estabelecimento");
        assertProblem(send("POST", "/api/v1/estabelecimentos", dono.token(), body), 400);
        assertThat(count("SELECT COUNT(*) FROM estabelecimento")).isEqualTo(antes);
    }

    @Test
    void anonimoNaoSolicitaNemConsulta() throws Exception {
        assertProblem(send("POST", "/api/v1/estabelecimentos", null, estabelecimento("X", "Santos", "Centro")), 401);
        assertProblem(get("/api/v1/estabelecimentos/meus", null), 401);
    }

    @Test
    void somenteProprietarioOuAdministradorConsultaEditaEVeHistorico() throws Exception {
        var dono = conta();
        var outro = conta();
        long id = solicitarEstabelecimento(dono, "Mercado Bom", "Santos", "Centro");
        assertThat(get("/api/v1/estabelecimentos/" + id, dono.token()).statusCode()).isEqualTo(200);
        assertThat(get("/api/v1/estabelecimentos/" + id, admin.token()).statusCode()).isEqualTo(200);
        assertThat(get("/api/v1/estabelecimentos/" + id + "/historico", admin.token()).statusCode()).isEqualTo(200);
        // Terceiros recebem a mesma resposta de um ID inexistente (sem revelar dados).
        for (var path : List.of("/api/v1/estabelecimentos/" + id, "/api/v1/estabelecimentos/" + id + "/historico", "/api/v1/estabelecimentos/999999")) {
            var response = get(path, outro.token());
            assertProblem(response, 404);
            assertThat(response.body()).doesNotContain("Mercado Bom");
        }
        assertProblem(send("PUT", "/api/v1/estabelecimentos/" + id, outro.token(), estabelecimento("Invadido", "Santos", "Centro")), 404);
        assertThat(jdbc.queryForObject("SELECT nome FROM estabelecimento WHERE id=?", String.class, id)).isEqualTo("Mercado Bom");
        var meus = tree(get("/api/v1/estabelecimentos/meus", outro.token()));
        assertThat(meus.get("totalElementos").asInt()).isZero();
    }

    @Test
    void aprovacaoAtivaRegistraHistoricoEConcedePapelCumulativo() throws Exception {
        var dono = conta();
        long id = solicitarEstabelecimento(dono, "Hortifruti", "Santos", "Boqueirão");
        var response = send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", admin.token(), Map.of("motivo", "  Dados conferidos  "));
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(tree(response).get("status").asText()).isEqualTo("ATIVO");
        assertThat(tree(get("/api/v1/auth/me", dono.token())).get("papeis").toString()).isEqualTo("[\"CLIENTE\",\"RESPONSAVEL\"]");
        var historico = tree(get("/api/v1/estabelecimentos/" + id + "/historico", dono.token()));
        assertThat(historico.size()).isEqualTo(2);
        assertThat(historico.get(1).get("statusAnterior").asText()).isEqualTo("PENDENTE_ANALISE");
        assertThat(historico.get(1).get("statusNovo").asText()).isEqualTo("ATIVO");
        assertThat(historico.get(1).get("atorId").asLong()).isEqualTo(admin.id());
        assertThat(historico.get(1).get("motivo").asText()).isEqualTo("Dados conferidos");
        assertThat(historico.get(1).get("criadoEm").asText()).endsWith("Z");
    }

    @Test
    void moderacaoExigeAdministradorEMotivo() throws Exception {
        var dono = conta();
        long id = solicitarEstabelecimento(dono, "Doceria", "Santos", "Centro");
        assertProblem(send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", dono.token(), Map.of("motivo", "eu mesmo")), 403);
        assertProblem(send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", null, Map.of("motivo", "x")), 401);
        assertProblem(send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", admin.token(), Map.of("motivo", " ")), 400);
        assertProblem(send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", admin.token(), Map.of()), 400);
        assertProblem(send("POST", "/api/v1/admin/estabelecimentos/999999/aprovar", admin.token(), Map.of("motivo", "x")), 404);
        assertThat(jdbc.queryForObject("SELECT status FROM estabelecimento WHERE id=?", String.class, id)).isEqualTo("PENDENTE_ANALISE");
        assertThat(count("SELECT COUNT(*) FROM historico_status_estabelecimento WHERE estabelecimento_id=?", id)).isOne();
    }

    @Test
    void administradorNaoModeraOProprioEstabelecimento() throws Exception {
        long id = solicitarEstabelecimento(admin, "Do administrador", "Santos", "Centro");
        assertProblem(send("POST", "/api/v1/admin/estabelecimentos/" + id + "/aprovar", admin.token(), Map.of("motivo", "auto")), 403);
        assertThat(jdbc.queryForObject("SELECT status FROM estabelecimento WHERE id=?", String.class, id)).isEqualTo("PENDENTE_ANALISE");
    }

    @Test
    void cicloCompletoDeModeracaoEEdicaoPeloProprietario() throws Exception {
        var dono = conta();
        long id = solicitarEstabelecimento(dono, "Empório", "Santos", "Centro");
        String base = "/api/v1/admin/estabelecimentos/" + id;
        var motivo = Map.of("motivo", "Revisão");
        // Transicoes proibidas a partir de PENDENTE_ANALISE.
        assertProblem(send("POST", base + "/suspender", admin.token(), motivo), 409);
        assertProblem(send("POST", base + "/reativar", admin.token(), motivo), 409);
        assertThat(tree(send("POST", base + "/rejeitar", admin.token(), Map.of("motivo", "CEP divergente"))).get("status").asText()).isEqualTo("REJEITADO");
        assertProblem(send("POST", base + "/aprovar", admin.token(), motivo), 409);
        // Proprietario corrige: volta para analise.
        var corrigido = send("PUT", "/api/v1/estabelecimentos/" + id, dono.token(), estabelecimento("Empório", "Santos", "Centro"));
        assertThat(tree(corrigido).get("status").asText()).isEqualTo("PENDENTE_ANALISE");
        assertThat(tree(send("POST", base + "/aprovar", admin.token(), motivo)).get("status").asText()).isEqualTo("ATIVO");
        // Ativo: mudar apenas nome mantem ATIVO; mudar cidade exige nova analise.
        var renomeado = send("PUT", "/api/v1/estabelecimentos/" + id, dono.token(), estabelecimento("Empório Novo", "Santos", "Centro"));
        assertThat(tree(renomeado).get("status").asText()).isEqualTo("ATIVO");
        var mudou = send("PUT", "/api/v1/estabelecimentos/" + id, dono.token(), estabelecimento("Empório Novo", "Guarujá", "Centro"));
        assertThat(tree(mudou).get("status").asText()).isEqualTo("PENDENTE_ANALISE");
        assertThat(tree(send("POST", base + "/aprovar", admin.token(), motivo)).get("status").asText()).isEqualTo("ATIVO");
        assertThat(tree(send("POST", base + "/suspender", admin.token(), Map.of("motivo", "Denúncia"))).get("status").asText()).isEqualTo("SUSPENSO");
        assertProblem(send("PUT", "/api/v1/estabelecimentos/" + id, dono.token(), estabelecimento("Tentativa", "Santos", "Centro")), 409);
        assertProblem(send("POST", base + "/aprovar", admin.token(), motivo), 409);
        assertThat(tree(send("POST", base + "/reativar", admin.token(), motivo)).get("status").asText()).isEqualTo("ATIVO");
        var estados = jdbc.queryForList("SELECT status_novo FROM historico_status_estabelecimento WHERE estabelecimento_id=? ORDER BY id", String.class, id);
        assertThat(estados).containsExactly("PENDENTE_ANALISE", "REJEITADO", "PENDENTE_ANALISE", "ATIVO", "PENDENTE_ANALISE", "ATIVO", "SUSPENSO", "ATIVO");
        assertThat(count("SELECT COUNT(*) FROM usuario_papel WHERE usuario_id=? AND papel_codigo='RESPONSAVEL'", dono.id())).isOne();
    }

    @Test
    void decisoesSimultaneasProduzemUmaUnicaTransicao() throws Exception {
        var dono = conta();
        long id = solicitarEstabelecimento(dono, "Concorrência", "Santos", "Centro");
        String base = "/api/v1/admin/estabelecimentos/" + id;
        var respostas = simultaneas(List.of(
                request("POST", base + "/aprovar", admin.token(), Map.of("motivo", "ok")),
                request("POST", base + "/rejeitar", admin.token(), Map.of("motivo", "não")),
                request("POST", base + "/aprovar", admin.token(), Map.of("motivo", "ok"))));
        assertThat(respostas.stream().filter(r -> r.statusCode() == 200).count()).isOne();
        assertThat(respostas.stream().filter(r -> r.statusCode() == 409).count()).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM historico_status_estabelecimento WHERE estabelecimento_id=?", id)).isEqualTo(2);
    }

    @Test
    void listagemAdministrativaFiltraPorStatusSemDadosSensiveis() throws Exception {
        var dono = conta();
        long id = solicitarEstabelecimento(dono, "Listado", "Santos", "Centro");
        var pendentes = get("/api/v1/admin/estabelecimentos?status=PENDENTE_ANALISE&tamanho=100", admin.token());
        assertThat(pendentes.statusCode()).isEqualTo(200);
        assertThat(pendentes.body()).contains("\"id\":" + id).doesNotContain("senha", "hash", "email");
        var ativos = tree(get("/api/v1/admin/estabelecimentos?status=ATIVO&tamanho=100", admin.token()));
        ativos.get("conteudo").forEach(e -> assertThat(e.get("status").asText()).isEqualTo("ATIVO"));
        assertProblem(get("/api/v1/admin/estabelecimentos?status=INVALIDO", admin.token()), 400);
        assertProblem(get("/api/v1/admin/estabelecimentos", dono.token()), 403);
    }
}
