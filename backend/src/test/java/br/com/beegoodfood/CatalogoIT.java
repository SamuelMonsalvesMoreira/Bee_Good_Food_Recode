package br.com.beegoodfood;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Etapa 5.3: categorias (CA20), produtos e ciclo de vida das ofertas (CA03, CA04, CA06, CA25). */
class CatalogoIT extends IntegracaoBase {
    private Conta admin;
    private long categoria;

    @BeforeAll
    void preparar() throws Exception {
        limparBanco(jdbc);
        admin = admin();
        categoria = categoria(admin, "Padaria");
    }

    // ---- Categorias ----

    @Test
    void administradorCriaCategoriaComSlugEDuplicidadeNormalizadaRetornaConflito() throws Exception {
        var criada = send("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", "  Pães   e Bolos "));
        assertThat(criada.statusCode()).isEqualTo(201);
        assertThat(tree(criada).get("nome").asText()).isEqualTo("Pães e Bolos");
        assertThat(tree(criada).get("slug").asText()).isEqualTo("paes-e-bolos");
        for (var duplicada : List.of("pães e bolos", "PAES E BOLOS", "Paes-e-Bolos")) {
            var response = send("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", duplicada));
            assertProblem(response, 409);
            assertThat(tree(response).get("detail").asText()).contains("categoria");
        }
        assertProblem(send("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", "!!!")), 400);
        assertProblem(send("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", " ")), 400);
    }

    @Test
    void categoriasDuplicadasSimultaneasCriamApenasUma() throws Exception {
        var respostas = simultaneas(List.of(
                request("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", "Laticínios")),
                request("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", "laticinios")),
                request("POST", "/api/v1/admin/categorias", admin.token(), Map.of("nome", "LATICÍNIOS"))));
        assertThat(respostas.stream().map(r -> r.statusCode()).toList()).containsExactlyInAnyOrder(201, 409, 409);
        assertThat(count("SELECT COUNT(*) FROM categoria WHERE slug='laticinios'")).isOne();
    }

    @Test
    void somenteAdministradorAlteraCategorias() throws Exception {
        var cliente = conta();
        assertProblem(send("POST", "/api/v1/admin/categorias", cliente.token(), Map.of("nome", "Hackeada")), 403);
        assertProblem(send("POST", "/api/v1/admin/categorias", null, Map.of("nome", "Hackeada")), 401);
        assertProblem(send("PUT", "/api/v1/admin/categorias/" + categoria, cliente.token(), Map.of("nome", "X", "ativo", false)), 403);
        assertThat(count("SELECT COUNT(*) FROM categoria WHERE nome='Hackeada'")).isZero();
    }

    @Test
    void desativarCategoriaPreservaProdutosESomeDaConsultaPublica() throws Exception {
        long id = categoria(admin, "Congelados");
        var loja = loja(admin, "Loja Fria", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), id, "Lasanha");
        long oferta = ofertaPublicada(loja.dono(), produto, oferta("30.00", "18.00", 5, 4, 6));
        var response = send("PUT", "/api/v1/admin/categorias/" + id, admin.token(), Map.of("nome", "Congelados", "ativo", false));
        assertThat(tree(response).get("ativo").asBoolean()).isFalse();
        assertProblem(get("/api/v1/categorias/" + id, null), 404);
        assertThat(count("SELECT COUNT(*) FROM produto WHERE id=? AND categoria_id=?", produto, id)).isOne();
        var todas = get("/api/v1/admin/categorias?tamanho=100", admin.token());
        assertThat(todas.body()).contains("\"slug\":\"congelados\"");
        // Oferta de categoria desativada nao pode ser retomada e novos produtos nao podem usa-la.
        assertThat(send("POST", "/api/v1/ofertas/" + oferta + "/pausar", loja.dono().token(), null).statusCode()).isEqualTo(200);
        assertProblem(send("POST", "/api/v1/ofertas/" + oferta + "/retomar", loja.dono().token(), null), 409);
        assertProblem(send("POST", "/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/produtos", loja.dono().token(),
                Map.of("categoriaId", id, "nome", "Outro")), 400);
    }

    // ---- Produtos e propriedade ----

    @Test
    void produtosExigemPapelResponsavelEPropriedade() throws Exception {
        var loja = loja(admin, "Loja A", "Santos", "Centro");
        var outraLoja = loja(admin, "Loja B", "Santos", "Centro");
        var cliente = conta();
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Pão de queijo");
        var body = Map.<String, Object>of("categoriaId", categoria, "nome", "Invasão");
        assertProblem(send("POST", "/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/produtos", cliente.token(), body), 403);
        assertProblem(send("POST", "/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/produtos", outraLoja.dono().token(), body), 404);
        assertProblem(get("/api/v1/produtos/" + produto, outraLoja.dono().token()), 404);
        assertProblem(send("PUT", "/api/v1/produtos/" + produto, outraLoja.dono().token(), body), 404);
        assertProblem(get("/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/produtos", outraLoja.dono().token()), 404);
        assertThat(jdbc.queryForObject("SELECT nome FROM produto WHERE id=?", String.class, produto)).isEqualTo("Pão de queijo");
        var editado = send("PUT", "/api/v1/produtos/" + produto, loja.dono().token(),
                Map.of("categoriaId", categoria, "nome", "Pão de queijo mineiro", "ativo", false));
        assertThat(tree(editado).get("nome").asText()).isEqualTo("Pão de queijo mineiro");
        assertThat(tree(editado).get("ativo").asBoolean()).isFalse();
    }

    @Test
    void solicitanteSemAprovacaoNaoGerenciaCatalogo() throws Exception {
        var dono = conta();
        long pendente = solicitarEstabelecimento(dono, "Pendente", "Santos", "Centro");
        assertProblem(send("POST", "/api/v1/estabelecimentos/" + pendente + "/produtos", dono.token(),
                Map.of("categoriaId", categoria, "nome", "Bolo")), 403);
    }

    // ---- Ofertas ----

    @ParameterizedTest
    @ValueSource(strings = {"precoMaior", "precoZero", "precoNegativo", "casasDecimais", "estoqueNegativo", "inicioAposFim",
            "retiradaAntesDaVenda", "vendaEncerrada", "semData", "dataSemFuso", "estoqueAcimaDoLimite"})
    void ofertaInvalidaERejeitadaComCampoExplicito(String cenario) throws Exception {
        var loja = loja(admin, "Validação", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Torta");
        var body = oferta("20.00", "10.00", 5, 3, 5);
        var agora = java.time.OffsetDateTime.now();
        switch (cenario) {
            case "precoMaior" -> body.put("precoOferta", new java.math.BigDecimal("20.01"));
            case "precoZero" -> body.put("precoOferta", 0);
            case "precoNegativo" -> body.put("precoOriginal", -1);
            case "casasDecimais" -> body.put("precoOferta", new java.math.BigDecimal("9.999"));
            case "estoqueNegativo" -> body.put("estoque", -1);
            case "estoqueAcimaDoLimite" -> body.put("estoque", 100001);
            case "inicioAposFim" -> body.put("inicioVenda", agora.plusHours(4).toString());
            case "retiradaAntesDaVenda" -> body.put("retiradaAte", agora.plusHours(2).toString());
            case "vendaEncerrada" -> {
                body.put("inicioVenda", agora.minusHours(5).toString());
                body.put("vendaAte", agora.minusMinutes(1).toString());
            }
            case "semData" -> body.remove("vendaAte");
            case "dataSemFuso" -> body.put("vendaAte", "2030-01-01T10:00:00");
        }
        var response = send("POST", "/api/v1/produtos/" + produto + "/ofertas", loja.dono().token(), body);
        assertProblem(response, 400);
        if (!cenario.equals("dataSemFuso")) { assertThat(tree(response).get("campos").isEmpty()).isFalse(); }
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE produto_id=?", produto)).isZero();
    }

    @Test
    void cicloDeVidaDaOfertaEImutabilidadeAposPublicacao() throws Exception {
        var loja = loja(admin, "Ciclo", "Santos", "Centro");
        String token = loja.dono().token();
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Croissant");
        long id = rascunho(loja.dono(), produto, oferta("12.00", "8.00", 10, 3, 5));
        var editado = send("PUT", "/api/v1/ofertas/" + id, token, oferta("12.00", "7.50", 8, 3, 5));
        assertThat(tree(editado).get("precoOferta").decimalValue()).isEqualByComparingTo("7.50");
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/pausar", token, null), 409);
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/retomar", token, null), 409);
        var publicada = tree(send("POST", "/api/v1/ofertas/" + id + "/publicar", token, null));
        assertThat(publicada.get("status").asText()).isEqualTo("PUBLICADA");
        String publicadaEm = publicada.get("publicadaEm").asText();
        assertThat(publicadaEm).endsWith("Z");
        assertProblem(send("PUT", "/api/v1/ofertas/" + id, token, oferta("12.00", "1.00", 8, 3, 5)), 409);
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/publicar", token, null), 409);
        assertThat(tree(send("POST", "/api/v1/ofertas/" + id + "/pausar", token, null)).get("status").asText()).isEqualTo("PAUSADA");
        var retomada = tree(send("POST", "/api/v1/ofertas/" + id + "/retomar", token, null));
        assertThat(retomada.get("status").asText()).isEqualTo("PUBLICADA");
        assertThat(retomada.get("publicadaEm").asText()).isEqualTo(publicadaEm);
        // Segunda oferta do mesmo produto nao pode ficar publicada ou pausada ao mesmo tempo.
        long segunda = rascunho(loja.dono(), produto, oferta("12.00", "6.00", 3, 3, 5));
        var conflito = send("POST", "/api/v1/ofertas/" + segunda + "/publicar", token, null);
        assertProblem(conflito, 409);
        assertThat(tree(conflito).get("detail").asText()).contains("já possui uma oferta");
        var encerrada = tree(send("POST", "/api/v1/ofertas/" + id + "/encerrar", token, null));
        assertThat(encerrada.get("status").asText()).isEqualTo("ENCERRADA");
        assertThat(encerrada.get("encerradaEm").isNull()).isFalse();
        for (var acao : List.of("publicar", "retomar", "pausar", "encerrar")) {
            assertProblem(send("POST", "/api/v1/ofertas/" + id + "/" + acao, token, null), 409);
        }
        assertThat(send("POST", "/api/v1/ofertas/" + segunda + "/publicar", token, null).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT preco_oferta FROM oferta WHERE id=?", java.math.BigDecimal.class, id)).isEqualByComparingTo("7.50");
    }

    @Test
    void publicacaoExigeEstoqueProdutoAtivoEPrazoFuturo() throws Exception {
        var loja = loja(admin, "Regras", "Santos", "Centro");
        String token = loja.dono().token();
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Sonho");
        long semEstoque = rascunho(loja.dono(), produto, oferta("10.00", "5.00", 0, 3, 5));
        assertProblem(send("POST", "/api/v1/ofertas/" + semEstoque + "/publicar", token, null), 400);
        long vencida = rascunho(loja.dono(), produto, oferta("10.00", "5.00", 3, 3, 5));
        jdbc.update("UPDATE oferta SET inicio_venda = UTC_TIMESTAMP(6) - INTERVAL 2 HOUR, "
                + "venda_ate = UTC_TIMESTAMP(6) - INTERVAL 1 MINUTE WHERE id=?", vencida);
        var response = send("POST", "/api/v1/ofertas/" + vencida + "/publicar", token, null);
        assertProblem(response, 400);
        assertThat(tree(response).get("campos").has("vendaAte")).isTrue();
        long valida = rascunho(loja.dono(), produto, oferta("10.00", "5.00", 3, 3, 5));
        send("PUT", "/api/v1/produtos/" + produto, token, Map.of("categoriaId", categoria, "nome", "Sonho", "ativo", false));
        assertProblem(send("POST", "/api/v1/ofertas/" + valida + "/publicar", token, null), 409);
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE produto_id=? AND status='PUBLICADA'", produto)).isZero();
    }

    @Test
    void estabelecimentoNaoAtivoNaoCriaNemPublicaOfertas() throws Exception {
        var loja = loja(admin, "Suspensa", "Santos", "Centro");
        String token = loja.dono().token();
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Biscoito");
        long id = rascunho(loja.dono(), produto, oferta("10.00", "5.00", 3, 3, 5));
        send("POST", "/api/v1/admin/estabelecimentos/" + loja.estabelecimentoId() + "/suspender", admin.token(), Map.of("motivo", "Teste"));
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/publicar", token, null), 409);
        assertProblem(send("POST", "/api/v1/produtos/" + produto + "/ofertas", token, oferta("10.00", "5.00", 3, 3, 5)), 409);
        // Encerrar continua permitido para o proprietario.
        assertThat(send("POST", "/api/v1/ofertas/" + id + "/encerrar", token, null).statusCode()).isEqualTo(200);
    }

    @Test
    void publicacoesSimultaneasDoMesmoProdutoDeixamApenasUmaAtiva() throws Exception {
        var loja = loja(admin, "Corrida", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Baguete");
        var requests = new ArrayList<java.net.http.HttpRequest>();
        for (int i = 0; i < 4; i++) {
            long id = rascunho(loja.dono(), produto, oferta("10.00", "5.00", 3, 3, 5));
            requests.add(request("POST", "/api/v1/ofertas/" + id + "/publicar", loja.dono().token(), null));
        }
        var respostas = simultaneas(requests);
        assertThat(respostas.stream().filter(r -> r.statusCode() == 200).count()).isOne();
        assertThat(respostas.stream().filter(r -> r.statusCode() == 409).count()).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE produto_id=? AND status IN ('PUBLICADA','PAUSADA')", produto)).isOne();
    }

    @Test
    void ajusteDeEstoqueRelativoNuncaFicaNegativoMesmoComConcorrencia() throws Exception {
        var loja = loja(admin, "Estoque", "Santos", "Centro");
        String token = loja.dono().token();
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Cuca");
        long id = ofertaPublicada(loja.dono(), produto, oferta("10.00", "5.00", 5, 3, 5));
        assertThat(tree(send("POST", "/api/v1/ofertas/" + id + "/estoque", token, Map.of("ajuste", 3))).get("estoqueDisponivel").asInt()).isEqualTo(8);
        assertThat(tree(send("POST", "/api/v1/ofertas/" + id + "/estoque", token, Map.of("ajuste", -3))).get("estoqueDisponivel").asInt()).isEqualTo(5);
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/estoque", token, Map.of("ajuste", -6)), 409);
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/estoque", token, Map.of("ajuste", 0)), 400);
        var requests = new ArrayList<java.net.http.HttpRequest>();
        for (int i = 0; i < 8; i++) { requests.add(request("POST", "/api/v1/ofertas/" + id + "/estoque", token, Map.of("ajuste", -1))); }
        var respostas = simultaneas(requests);
        assertThat(respostas.stream().filter(r -> r.statusCode() == 200).count()).isEqualTo(5);
        assertThat(respostas.stream().filter(r -> r.statusCode() == 409).count()).isEqualTo(3);
        assertThat(count("SELECT estoque_disponivel FROM oferta WHERE id=?", id)).isZero();
    }

    // ---- Remocao (CRUD completo, DEC-038) ----

    @Test
    void administradorRemoveCategoriaSemProdutosEBloqueiaComProdutos() throws Exception {
        long livre = categoria(admin, "Para Remover");
        assertThat(send("DELETE", "/api/v1/admin/categorias/" + livre, admin.token(), null).statusCode()).isEqualTo(204);
        assertThat(count("SELECT COUNT(*) FROM categoria WHERE id=?", livre)).isZero();
        assertProblem(send("DELETE", "/api/v1/admin/categorias/" + livre, admin.token(), null), 404);

        long usada = categoria(admin, "Em Uso");
        var loja = loja(admin, "Loja Categoria", "Santos", "Centro");
        produto(loja.dono(), loja.estabelecimentoId(), usada, "Item");
        var recusa = send("DELETE", "/api/v1/admin/categorias/" + usada, admin.token(), null);
        assertProblem(recusa, 409);
        assertThat(tree(recusa).get("detail").asText()).contains("Desative a categoria");
        assertThat(count("SELECT COUNT(*) FROM categoria WHERE id=?", usada)).isOne();
    }

    @Test
    void somenteAdministradorRemoveCategoria() throws Exception {
        long id = categoria(admin, "Protegida");
        assertProblem(send("DELETE", "/api/v1/admin/categorias/" + id, null, null), 401);
        assertProblem(send("DELETE", "/api/v1/admin/categorias/" + id, conta().token(), null), 403);
        assertThat(count("SELECT COUNT(*) FROM categoria WHERE id=?", id)).isOne();
    }

    @Test
    void responsavelRemoveProdutoESuasOfertasSemPedidos() throws Exception {
        var loja = loja(admin, "Loja Remove", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Descartável");
        long encerrada = ofertaPublicada(loja.dono(), produto, oferta("10.00", "5.00", 2, 4, 6));
        assertThat(send("POST", "/api/v1/ofertas/" + encerrada + "/encerrar", loja.dono().token(), null).statusCode()).isEqualTo(200);
        var rascunho = send("POST", "/api/v1/produtos/" + produto + "/ofertas", loja.dono().token(), oferta("10.00", "6.00", 2, 4, 6));
        assertThat(rascunho.statusCode()).isEqualTo(201);

        assertThat(send("DELETE", "/api/v1/produtos/" + produto, loja.dono().token(), null).statusCode()).isEqualTo(204);
        assertThat(count("SELECT COUNT(*) FROM produto WHERE id=?", produto)).isZero();
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE produto_id=?", produto)).isZero();
        assertProblem(get("/api/v1/produtos/" + produto, loja.dono().token()), 404);
    }

    @Test
    void produtoEOfertaEmVendaNaoSaoRemovidos() throws Exception {
        var loja = loja(admin, "Loja Em Venda", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Em venda");
        long oferta = ofertaPublicada(loja.dono(), produto, oferta("10.00", "5.00", 2, 4, 6));
        assertProblem(send("DELETE", "/api/v1/produtos/" + produto, loja.dono().token(), null), 409);
        assertProblem(send("DELETE", "/api/v1/ofertas/" + oferta, loja.dono().token(), null), 409);
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE id=?", oferta)).isOne();
    }

    @Test
    void ofertaEProdutoComPedidoFicamNoHistorico() throws Exception {
        var loja = loja(admin, "Loja Historico", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Vendido");
        long oferta = ofertaPublicada(loja.dono(), produto, oferta("10.00", "5.00", 3, 4, 6));
        var pedido = send("POST", "/api/v1/pedidos", conta().token(), Map.of("formaPagamento", "PIX",
                "itens", List.of(Map.of("ofertaId", oferta, "quantidade", 1))), "Idempotency-Key", "remocao-historico-1");
        assertThat(pedido.statusCode()).isEqualTo(201);
        assertThat(send("POST", "/api/v1/ofertas/" + oferta + "/encerrar", loja.dono().token(), null).statusCode()).isEqualTo(200);

        var recusaOferta = send("DELETE", "/api/v1/ofertas/" + oferta, loja.dono().token(), null);
        assertProblem(recusaOferta, 409);
        assertThat(tree(recusaOferta).get("detail").asText()).contains("histórico");
        assertProblem(send("DELETE", "/api/v1/produtos/" + produto, loja.dono().token(), null), 409);
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE id=?", oferta)).isOne();
        assertThat(count("SELECT COUNT(*) FROM produto WHERE id=?", produto)).isOne();
        assertThat(count("SELECT COUNT(*) FROM item_pedido WHERE oferta_id=?", oferta)).isOne();
    }

    @Test
    void remocaoDeProdutoComUmaOfertaVendidaDesfazTudo() throws Exception {
        var loja = loja(admin, "Loja Transacao", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Parcial");
        long vendida = ofertaPublicada(loja.dono(), produto, oferta("10.00", "5.00", 3, 4, 6));
        var pedido = send("POST", "/api/v1/pedidos", conta().token(), Map.of("formaPagamento", "PIX",
                "itens", List.of(Map.of("ofertaId", vendida, "quantidade", 1))), "Idempotency-Key", "remocao-transacao-1");
        assertThat(pedido.statusCode()).isEqualTo(201);
        send("POST", "/api/v1/ofertas/" + vendida + "/encerrar", loja.dono().token(), null);
        long rascunho = tree(send("POST", "/api/v1/produtos/" + produto + "/ofertas", loja.dono().token(), oferta("10.00", "6.00", 2, 4, 6))).get("id").asLong();

        assertProblem(send("DELETE", "/api/v1/produtos/" + produto, loja.dono().token(), null), 409);
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE id=?", rascunho)).isOne();
    }

    @Test
    void responsavelRemoveRascunhoEProdutoSegueNoCatalogo() throws Exception {
        var loja = loja(admin, "Loja Rascunho", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Com rascunho");
        var rascunho = send("POST", "/api/v1/produtos/" + produto + "/ofertas", loja.dono().token(), oferta("10.00", "6.00", 2, 4, 6));
        long oferta = tree(rascunho).get("id").asLong();
        assertThat(send("DELETE", "/api/v1/ofertas/" + oferta, loja.dono().token(), null).statusCode()).isEqualTo(204);
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE id=?", oferta)).isZero();
        assertThat(count("SELECT COUNT(*) FROM produto WHERE id=?", produto)).isOne();
    }

    @Test
    void outroResponsavelNaoRemoveProdutoNemOferta() throws Exception {
        var dona = loja(admin, "Loja Dona", "Santos", "Centro");
        var outra = loja(admin, "Loja Outra", "Santos", "Centro");
        long produto = produto(dona.dono(), dona.estabelecimentoId(), categoria, "Alheio");
        var rascunho = send("POST", "/api/v1/produtos/" + produto + "/ofertas", dona.dono().token(), oferta("10.00", "6.00", 2, 4, 6));
        long oferta = tree(rascunho).get("id").asLong();
        assertProblem(send("DELETE", "/api/v1/produtos/" + produto, outra.dono().token(), null), 404);
        assertProblem(send("DELETE", "/api/v1/ofertas/" + oferta, outra.dono().token(), null), 404);
        assertProblem(send("DELETE", "/api/v1/produtos/" + produto, null, null), 401);
        assertProblem(send("DELETE", "/api/v1/ofertas/" + oferta, conta().token(), null), 403);
        assertThat(count("SELECT COUNT(*) FROM produto WHERE id=?", produto)).isOne();
        assertThat(count("SELECT COUNT(*) FROM oferta WHERE id=?", oferta)).isOne();
    }

    @Test
    void outroResponsavelNaoAlteraOfertas() throws Exception {
        var loja = loja(admin, "Dona", "Santos", "Centro");
        var intruso = loja(admin, "Intrusa", "Santos", "Centro");
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Rosca");
        long id = ofertaPublicada(loja.dono(), produto, oferta("10.00", "5.00", 5, 3, 5));
        String token = intruso.dono().token();
        for (var acao : List.of("pausar", "encerrar", "publicar", "retomar")) {
            assertProblem(send("POST", "/api/v1/ofertas/" + id + "/" + acao, token, null), 404);
        }
        assertProblem(send("POST", "/api/v1/ofertas/" + id + "/estoque", token, Map.of("ajuste", 100)), 404);
        assertProblem(send("PUT", "/api/v1/ofertas/" + id, token, oferta("10.00", "1.00", 5, 3, 5)), 404);
        assertProblem(get("/api/v1/gestao/ofertas/" + id, token), 404);
        assertProblem(get("/api/v1/produtos/" + produto + "/ofertas", token), 404);
        assertProblem(send("POST", "/api/v1/produtos/" + produto + "/ofertas", token, oferta("10.00", "5.00", 5, 3, 5)), 404);
        assertThat(jdbc.queryForObject("SELECT CONCAT(status, ':', estoque_disponivel) FROM oferta WHERE id=?", String.class, id)).isEqualTo("PUBLICADA:5");
    }
}
