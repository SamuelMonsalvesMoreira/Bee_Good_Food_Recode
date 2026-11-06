package br.com.beegoodfood;

import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Etapas 5.5 e 5.6: pedido transacional, idempotencia, status e cancelamento (CA05, CA08-CA16, CA23, CA24, CA26). */
class PedidoIT extends IntegracaoBase {
    private Conta admin;
    private long categoria;
    private Loja loja;

    @BeforeAll
    void preparar() throws Exception {
        limparBanco(jdbc);
        admin = admin();
        categoria = categoria(admin, "Padaria");
        loja = loja(admin, "Padaria Central", "Santos", "Centro");
    }

    // ---- Criacao ----

    @Test
    void pedidoUsaPrecosDoServidorSnapshotsEMenorPrazoDeRetirada() throws Exception {
        var cliente = conta();
        long a = ofertaNova("Pão de forma", "10.00", "5.00", 10, 3, 6);
        long b = ofertaNova("Rosquinha", "4.00", "2.50", 10, 3, 4);
        var body = pedido("PIX", item(b, 2), item(a, 3));
        // Valores adulterados enviados pelo frontend sao ignorados (CA08).
        body.put("total", 0.01);
        ((Map<String, Object>) ((List<?>) body.get("itens")).getFirst()).put("precoOferta", 0.01);
        var response = criar(cliente, chave(), body);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        assertThat(response.headers().firstValue("Idempotent-Replayed")).hasValue("false");
        var pedido = tree(response);
        long id = pedido.get("id").asLong();
        assertThat(pedido.get("status").asText()).isEqualTo("PENDENTE");
        assertThat(pedido.get("total").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(pedido.get("clienteId").asLong()).isEqualTo(cliente.id());
        assertThat(pedido.get("formaPagamento").asText()).isEqualTo("PIX");
        assertThat(pedido.get("itens").size()).isEqualTo(2);
        // Itens em ordem deterministica (ID da oferta), independente da ordem enviada.
        assertThat(pedido.get("itens").get(0).get("produtoNome").asText()).isEqualTo("Pão de forma");
        assertThat(pedido.get("itens").get(0).get("subtotal").decimalValue()).isEqualByComparingTo("15.00");
        assertThat(pedido.get("itens").get(1).get("produtoNome").asText()).isEqualTo("Rosquinha");
        assertThat(pedido.get("itens").get(1).get("subtotal").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(pedido.get("estabelecimento").get("nome").asText()).isEqualTo("Padaria Central");
        assertThat(pedido.get("estabelecimento").get("fusoIana").asText()).isEqualTo("America/Sao_Paulo");
        var retiradaB = jdbc.queryForObject("SELECT retirada_ate FROM oferta WHERE id=?", java.time.LocalDateTime.class, b);
        assertThat(Instant.parse(pedido.get("retiradaAte").asText())).isEqualTo(retiradaB.toInstant(java.time.ZoneOffset.UTC));
        assertThat(pedido.get("historico").size()).isOne();
        assertThat(pedido.get("historico").get(0).get("statusNovo").asText()).isEqualTo("PENDENTE");
        assertThat(estoque(a)).isEqualTo(7);
        assertThat(estoque(b)).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM item_pedido WHERE pedido_id=?", id)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM historico_status_pedido WHERE pedido_id=? AND status_anterior IS NULL AND ator_id=?", id, cliente.id())).isOne();
        assertThat(jdbc.queryForObject("SELECT total FROM pedido WHERE id=?", BigDecimal.class, id)).isEqualByComparingTo("20.00");
    }

    @Test
    void historicoDoPedidoNaoMudaComEdicoesPosteriores() throws Exception {
        var cliente = conta();
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Broa");
        long oferta = ofertaPublicada(loja.dono(), produto, oferta("6.00", "3.00", 10, 3, 5));
        long id = tree(criar(cliente, chave(), pedido("DINHEIRO", item(oferta, 2)))).get("id").asLong();
        var antes = tree(get("/api/v1/pedidos/" + id, cliente.token()));
        var token = loja.dono().token();
        send("PUT", "/api/v1/produtos/" + produto, token, Map.of("categoriaId", categoria, "nome", "Broa renomeada"));
        send("POST", "/api/v1/ofertas/" + oferta + "/encerrar", token, null);
        ofertaPublicada(loja.dono(), produto, oferta("6.00", "1.00", 10, 3, 5));
        var depois = tree(get("/api/v1/pedidos/" + id, cliente.token()));
        assertThat(depois.get("itens")).isEqualTo(antes.get("itens"));
        assertThat(depois.get("estabelecimento")).isEqualTo(antes.get("estabelecimento"));
        assertThat(depois.get("retiradaAte")).isEqualTo(antes.get("retiradaAte"));
        assertThat(depois.get("itens").get(0).get("produtoNome").asText()).isEqualTo("Broa");
    }

    @Test
    void mesmaChaveEMesmoConteudoDevolvemPedidoOriginalSemNovaBaixa() throws Exception {
        var cliente = conta();
        long a = ofertaNova("Bolo", "20.00", "10.00", 10, 3, 5);
        long b = ofertaNova("Torta", "30.00", "12.00", 10, 3, 5);
        String chave = chave();
        var primeiro = criar(cliente, chave, pedido("PIX", item(a, 1), item(b, 2)));
        assertThat(primeiro.statusCode()).isEqualTo(201);
        // Ordem diferente dos itens: mesmo conteudo.
        var repetido = criar(cliente, chave, pedido("PIX", item(b, 2), item(a, 1)));
        assertThat(repetido.statusCode()).isEqualTo(200);
        assertThat(repetido.headers().firstValue("Idempotent-Replayed")).hasValue("true");
        assertThat(tree(repetido).get("id").asLong()).isEqualTo(tree(primeiro).get("id").asLong());
        assertThat(estoque(a)).isEqualTo(9);
        assertThat(estoque(b)).isEqualTo(8);
        // Mesma chave com conteudo diferente: 409 sem alteracao.
        for (var diferente : List.of(pedido("PIX", item(a, 2), item(b, 2)), pedido("DINHEIRO", item(a, 1), item(b, 2)), pedido("PIX", item(a, 1)))) {
            var conflito = criar(cliente, chave, diferente);
            assertProblem(conflito, 409);
            assertThat(tree(conflito).get("detail").asText()).contains("chave de idempotência");
        }
        assertThat(estoque(a)).isEqualTo(9);
        assertThat(count("SELECT COUNT(*) FROM pedido WHERE cliente_id=?", cliente.id())).isOne();
        // A mesma chave usada por outro cliente cria outro pedido e nao revela o primeiro.
        var outro = conta();
        var deOutro = criar(outro, chave, pedido("PIX", item(a, 1), item(b, 2)));
        assertThat(deOutro.statusCode()).isEqualTo(201);
        assertThat(tree(deOutro).get("id").asLong()).isNotEqualTo(tree(primeiro).get("id").asLong());
        assertThat(tree(deOutro).get("clienteId").asLong()).isEqualTo(outro.id());
    }

    @Test
    void repeticoesSimultaneasDaMesmaChaveCriamUmUnicoPedido() throws Exception {
        var cliente = conta();
        long a = ofertaNova("Pão doce", "8.00", "4.00", 20, 3, 5);
        String chave = chave();
        var requests = new ArrayList<HttpRequest>();
        for (int i = 0; i < 6; i++) { requests.add(criarRequest(cliente, chave, pedido("PIX", item(a, 2)))); }
        var respostas = simultaneas(requests);
        assertThat(respostas.stream().filter(r -> r.statusCode() == 201).count()).as(corpos(respostas)).isOne();
        assertThat(respostas.stream().filter(r -> r.statusCode() == 200).count()).as(corpos(respostas)).isEqualTo(5);
        assertThat(respostas.stream().map(r -> tree(r).get("id").asLong()).distinct().count()).isOne();
        assertThat(count("SELECT COUNT(*) FROM pedido WHERE cliente_id=?", cliente.id())).isOne();
        assertThat(estoque(a)).isEqualTo(18);
    }

    @Test
    void ultimaUnidadeDisputadaPorVariosClientesGeraUmUnicoPedido() throws Exception {
        long a = ofertaNova("Última fatia", "10.00", "3.00", 1, 3, 5);
        var requests = new ArrayList<HttpRequest>();
        for (int i = 0; i < 5; i++) { requests.add(criarRequest(conta(), chave(), pedido("PIX", item(a, 1)))); }
        var respostas = simultaneas(requests);
        assertThat(respostas.stream().filter(r -> r.statusCode() == 201).count()).as(corpos(respostas)).isOne();
        assertThat(respostas.stream().filter(r -> r.statusCode() == 409).count()).as(corpos(respostas)).isEqualTo(4);
        assertThat(estoque(a)).isZero();
        assertThat(count("SELECT COUNT(*) FROM item_pedido WHERE oferta_id=?", a)).isOne();
        // Sem estoque, a oferta sai do catalogo publico.
        assertProblem(get("/api/v1/ofertas/" + a, null), 404);
    }

    @ParameterizedTest
    @ValueSource(strings = {"semChave", "chaveCurta", "chaveInvalida", "semItens", "quantidadeZero", "quantidadeAlta", "itemRepetido",
            "formaInvalida", "semForma", "campoDesconhecido", "clienteNoCorpo", "itensDemais"})
    void pedidoInvalidoRetorna400SemEfeitos(String cenario) throws Exception {
        var cliente = conta();
        long a = ofertaNova("Validação " + cenario, "10.00", "5.00", 10, 3, 5);
        var body = pedido("PIX", item(a, 1));
        String chave = chave();
        switch (cenario) {
            case "semChave" -> chave = null;
            case "chaveCurta" -> chave = "abc";
            case "chaveInvalida" -> chave = "chave com espaço!!";
            case "semItens" -> body.put("itens", List.of());
            case "quantidadeZero" -> body.put("itens", List.of(item(a, 0)));
            case "quantidadeAlta" -> body.put("itens", List.of(item(a, 101)));
            case "itemRepetido" -> body.put("itens", List.of(item(a, 1), item(a, 2)));
            case "formaInvalida" -> body.put("formaPagamento", "BOLETO");
            case "semForma" -> body.remove("formaPagamento");
            case "campoDesconhecido" -> body.put("desconto", 50);
            case "clienteNoCorpo" -> body.put("clienteId", admin.id());
            case "itensDemais" -> {
                var itens = new ArrayList<Map<String, Object>>();
                for (int i = 1; i <= 21; i++) { itens.add(item(1000 + i, 1)); }
                body.put("itens", itens);
            }
        }
        var request = chave == null
                ? request("POST", "/api/v1/pedidos", cliente.token(), body)
                : criarRequest(cliente, chave, body);
        assertProblem(http.send(request, HttpResponse.BodyHandlers.ofString()), 400);
        assertThat(count("SELECT COUNT(*) FROM pedido WHERE cliente_id=?", cliente.id())).isZero();
        assertThat(estoque(a)).isEqualTo(10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rascunho", "pausada", "encerrada", "expirada", "futura", "estoqueInsuficiente", "inexistente",
            "doisEstabelecimentos", "produtoInativo", "proprioEstabelecimento"})
    void ofertaNaoCompravelRetorna409SemDescontarNada(String cenario) throws Exception {
        var cliente = conta();
        var token = loja.dono().token();
        long valida = ofertaNova("Válida " + cenario, "10.00", "5.00", 10, 3, 5);
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, "Alvo " + cenario);
        long alvo = rascunho(loja.dono(), produto, oferta("10.00", "5.00", 3, 3, 5));
        var comprador = cliente;
        switch (cenario) {
            case "rascunho" -> { }
            case "pausada" -> { publicar(alvo); send("POST", "/api/v1/ofertas/" + alvo + "/pausar", token, null); }
            case "encerrada" -> { publicar(alvo); send("POST", "/api/v1/ofertas/" + alvo + "/encerrar", token, null); }
            case "expirada" -> {
                publicar(alvo);
                jdbc.update("UPDATE oferta SET inicio_venda = UTC_TIMESTAMP(6) - INTERVAL 3 HOUR, venda_ate = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE id=?", alvo);
            }
            case "futura" -> {
                publicar(alvo);
                jdbc.update("UPDATE oferta SET inicio_venda = UTC_TIMESTAMP(6) + INTERVAL 1 HOUR WHERE id=?", alvo);
            }
            case "estoqueInsuficiente" -> publicar(alvo);
            case "inexistente" -> alvo = 999999;
            case "doisEstabelecimentos" -> {
                var outra = loja(admin, "Outra loja", "Santos", "Centro");
                alvo = ofertaPublicada(outra.dono(), produto(outra.dono(), outra.estabelecimentoId(), categoria, "De outra loja"), oferta("10.00", "5.00", 3, 3, 5));
            }
            case "produtoInativo" -> {
                publicar(alvo);
                send("PUT", "/api/v1/produtos/" + produto, token, Map.of("categoriaId", categoria, "nome", "Alvo", "ativo", false));
            }
            case "proprioEstabelecimento" -> { publicar(alvo); comprador = loja.dono(); }
        }
        int quantidade = cenario.equals("estoqueInsuficiente") ? 4 : 1;
        var response = criar(comprador, chave(), pedido("PIX", item(valida, 2), item(alvo, quantidade)));
        assertProblem(response, 409);
        // Atomicidade (CA09): o item valido tambem nao foi descontado.
        assertThat(estoque(valida)).isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM pedido WHERE cliente_id=?", comprador.id())).isZero();
        assertThat(count("SELECT COUNT(*) FROM item_pedido WHERE oferta_id=?", valida)).isZero();
    }

    @Test
    void somenteClienteCriaPedidoAutenticado() throws Exception {
        long a = ofertaNova("Anônimo", "10.00", "5.00", 10, 3, 5);
        assertProblem(send("POST", "/api/v1/pedidos", null, pedido("PIX", item(a, 1)), "Idempotency-Key", chave()), 401);
        assertThat(estoque(a)).isEqualTo(10);
    }

    // ---- Status (5.6) ----

    @Test
    void responsavelAvancaUmaEtapaPorVezComHistoricoEConclusaoUnica() throws Exception {
        var cliente = conta();
        long id = pedidoSimples(cliente, 1);
        var token = loja.dono().token();
        // CA14: PENDENTE direto para CONCLUIDO e outros saltos sao proibidos.
        for (var salto : List.of("CONCLUIDO", "EM_PREPARO", "PRONTO_PARA_RETIRADA", "PENDENTE")) {
            assertProblem(status(id, token, salto), 409);
        }
        assertThat(jdbc.queryForObject("SELECT status FROM pedido WHERE id=?", String.class, id)).isEqualTo("PENDENTE");
        assertProblem(status(id, token, "CANCELADO"), 400);
        assertProblem(status(id, cliente.token(), "CONFIRMADO"), 403);
        assertProblem(status(id, conta().token(), "CONFIRMADO"), 404);
        for (var proximo : List.of("CONFIRMADO", "EM_PREPARO", "PRONTO_PARA_RETIRADA")) {
            var response = status(id, token, proximo);
            assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
            assertThat(tree(response).get("status").asText()).isEqualTo(proximo);
            assertThat(tree(response).get("concluidoEm").isNull()).isTrue();
        }
        var concluido = tree(status(id, token, "CONCLUIDO"));
        assertThat(concluido.get("concluidoEm").asText()).endsWith("Z");
        assertProblem(status(id, token, "CONCLUIDO"), 409);
        assertProblem(cancelar(id, token, "tarde demais"), 409);
        var estados = jdbc.queryForList("SELECT CONCAT(IFNULL(status_anterior,'-'),'>',status_novo) FROM historico_status_pedido WHERE pedido_id=? ORDER BY id", String.class, id);
        assertThat(estados).containsExactly("->PENDENTE", "PENDENTE>CONFIRMADO", "CONFIRMADO>EM_PREPARO", "EM_PREPARO>PRONTO_PARA_RETIRADA", "PRONTO_PARA_RETIRADA>CONCLUIDO");
        assertThat(count("SELECT COUNT(*) FROM historico_status_pedido WHERE pedido_id=? AND status_novo<>'PENDENTE' AND ator_id=?", id, loja.dono().id())).isEqualTo(4);
    }

    @Test
    void avancosSimultaneosDoMesmoStatusRegistramUmaUnicaTransicao() throws Exception {
        long id = pedidoSimples(conta(), 1);
        var requests = new ArrayList<HttpRequest>();
        for (int i = 0; i < 4; i++) { requests.add(request("POST", "/api/v1/pedidos/" + id + "/status", loja.dono().token(), Map.of("status", "CONFIRMADO"))); }
        var respostas = simultaneas(requests);
        assertThat(respostas.stream().filter(r -> r.statusCode() == 200).count()).isOne();
        assertThat(respostas.stream().filter(r -> r.statusCode() == 409).count()).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM historico_status_pedido WHERE pedido_id=? AND status_novo='CONFIRMADO'", id)).isOne();
    }

    // ---- Cancelamento ----

    @Test
    void clienteCancelaPendenteUmaUnicaVezEEstoqueVoltaExatamenteUmaVez() throws Exception {
        var cliente = conta();
        long oferta = ofertaNova("Cancelável", "10.00", "5.00", 10, 3, 5);
        long id = tree(criar(cliente, chave(), pedido("PIX", item(oferta, 4)))).get("id").asLong();
        assertThat(estoque(oferta)).isEqualTo(6);
        var cancelado = cancelar(id, cliente.token(), "Mudei de ideia");
        assertThat(cancelado.statusCode()).isEqualTo(200);
        assertThat(tree(cancelado).get("status").asText()).isEqualTo("CANCELADO");
        assertThat(estoque(oferta)).isEqualTo(10);
        // Repeticao (CA16): resposta igual e sem nova devolucao.
        assertThat(cancelar(id, cliente.token(), null).statusCode()).isEqualTo(200);
        assertThat(estoque(oferta)).isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM historico_status_pedido WHERE pedido_id=? AND status_novo='CANCELADO' AND motivo='Mudei de ideia'", id)).isOne();
        assertProblem(status(id, loja.dono().token(), "CONFIRMADO"), 409);
    }

    @Test
    void cancelamentosSimultaneosDevolvemEstoqueUmaUnicaVez() throws Exception {
        var cliente = conta();
        long oferta = ofertaNova("Corrida de cancelamento", "10.00", "5.00", 10, 3, 5);
        long id = tree(criar(cliente, chave(), pedido("PIX", item(oferta, 3)))).get("id").asLong();
        var requests = new ArrayList<HttpRequest>();
        for (int i = 0; i < 3; i++) { requests.add(request("POST", "/api/v1/pedidos/" + id + "/cancelar", cliente.token(), Map.of())); }
        for (int i = 0; i < 3; i++) { requests.add(request("POST", "/api/v1/pedidos/" + id + "/cancelar", loja.dono().token(), Map.of("motivo", "Sem forno"))); }
        var respostas = simultaneas(requests);
        assertThat(respostas).allSatisfy(r -> assertThat(r.statusCode()).as(r.body()).isEqualTo(200));
        assertThat(estoque(oferta)).isEqualTo(10);
        assertThat(count("SELECT COUNT(*) FROM historico_status_pedido WHERE pedido_id=? AND status_novo='CANCELADO'", id)).isOne();
    }

    @Test
    void clienteSoCancelaPendenteEResponsavelPrecisaDeMotivo() throws Exception {
        var cliente = conta();
        long oferta = ofertaNova("Confirmado", "10.00", "5.00", 10, 3, 5);
        long id = tree(criar(cliente, chave(), pedido("PIX", item(oferta, 2)))).get("id").asLong();
        status(id, loja.dono().token(), "CONFIRMADO");
        status(id, loja.dono().token(), "EM_PREPARO");
        assertProblem(cancelar(id, cliente.token(), "quero cancelar"), 409);
        assertProblem(cancelar(id, loja.dono().token(), null), 400);
        assertProblem(cancelar(id, loja.dono().token(), " "), 400);
        assertProblem(cancelar(id, conta().token(), "intruso"), 404);
        assertThat(estoque(oferta)).isEqualTo(8);
        var cancelado = cancelar(id, loja.dono().token(), "Faltou ingrediente");
        assertThat(tree(cancelado).get("status").asText()).isEqualTo("CANCELADO");
        assertThat(estoque(oferta)).isEqualTo(10);
    }

    @Test
    void cancelamentoDevolveEstoqueSemReabrirOfertaEncerrada() throws Exception {
        var cliente = conta();
        long oferta = ofertaNova("Encerrável", "10.00", "5.00", 5, 3, 5);
        long id = tree(criar(cliente, chave(), pedido("PIX", item(oferta, 2)))).get("id").asLong();
        send("POST", "/api/v1/ofertas/" + oferta + "/encerrar", loja.dono().token(), null);
        assertThat(cancelar(id, cliente.token(), null).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT CONCAT(status,':',estoque_disponivel) FROM oferta WHERE id=?", String.class, oferta)).isEqualTo("ENCERRADA:5");
        assertProblem(get("/api/v1/ofertas/" + oferta, null), 404);
    }

    // ---- Suspensao e consultas ----

    @Test
    void suspensaoBloqueiaNovosPedidosMasPreservaOsExistentes() throws Exception {
        var suspensa = loja(admin, "Loja a suspender", "Santos", "Centro");
        long produto = produto(suspensa.dono(), suspensa.estabelecimentoId(), categoria, "Pão suspenso");
        long oferta = ofertaPublicada(suspensa.dono(), produto, oferta("10.00", "5.00", 10, 3, 5));
        var cliente = conta();
        long emAndamento = tree(criar(cliente, chave(), pedido("PIX", item(oferta, 1)))).get("id").asLong();
        long aCancelar = tree(criar(cliente, chave(), pedido("PIX", item(oferta, 2)))).get("id").asLong();
        send("POST", "/api/v1/admin/estabelecimentos/" + suspensa.estabelecimentoId() + "/suspender", admin.token(), Map.of("motivo", "Vistoria"));
        assertProblem(criar(cliente, chave(), pedido("PIX", item(oferta, 1))), 409);
        assertThat(get("/api/v1/pedidos/" + emAndamento, cliente.token()).statusCode()).isEqualTo(200);
        var fila = tree(get("/api/v1/estabelecimentos/" + suspensa.estabelecimentoId() + "/pedidos", suspensa.dono().token()));
        assertThat(fila.get("totalElementos").asInt()).isEqualTo(2);
        assertThat(status(emAndamento, suspensa.dono().token(), "CONFIRMADO").statusCode()).isEqualTo(200);
        assertThat(cancelar(aCancelar, cliente.token(), null).statusCode()).isEqualTo(200);
        assertThat(estoque(oferta)).isEqualTo(9);
    }

    @Test
    void listagensRespeitamClienteEEstabelecimento() throws Exception {
        var cliente = conta();
        var outro = conta();
        long primeiro = pedidoSimples(cliente, 1);
        long segundo = pedidoSimples(cliente, 1);
        long deOutro = pedidoSimples(outro, 1);
        var meus = tree(get("/api/v1/pedidos", cliente.token()));
        assertThat(meus.get("totalElementos").asInt()).isEqualTo(2);
        assertThat(meus.get("conteudo").get(0).get("id").asLong()).isEqualTo(segundo);
        assertThat(meus.get("conteudo").get(1).get("id").asLong()).isEqualTo(primeiro);
        // Economia pelos snapshots: (10,00 - 5,00) x 1 em cada pedido.
        assertThat(meus.get("conteudo").get(0).get("economia").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(get("/api/v1/pedidos?status=CANCELADO", cliente.token()).body()).contains("\"totalElementos\":0");
        assertProblem(get("/api/v1/pedidos/" + deOutro, cliente.token()), 404);
        assertProblem(get("/api/v1/pedidos?status=QUALQUER", cliente.token()), 400);
        var fila = tree(get("/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/pedidos?status=PENDENTE&tamanho=100", loja.dono().token()));
        var idsFila = new ArrayList<Long>();
        fila.get("conteudo").forEach(p -> idsFila.add(p.get("id").asLong()));
        assertThat(idsFila).contains(primeiro, segundo, deOutro);
        assertThat(get("/api/v1/pedidos/" + deOutro, loja.dono().token()).statusCode()).isEqualTo(200);
        var intruso = loja(admin, "Intrusa", "Santos", "Centro");
        assertProblem(get("/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/pedidos", intruso.dono().token()), 404);
        assertProblem(get("/api/v1/pedidos/" + primeiro, intruso.dono().token()), 404);
        assertProblem(get("/api/v1/estabelecimentos/" + loja.estabelecimentoId() + "/pedidos", cliente.token()), 403);
    }

    // ---- Auxiliares ----

    private long ofertaNova(String nome, String original, String preco, int estoque, int vendaHoras, int retiradaHoras) throws Exception {
        long produto = produto(loja.dono(), loja.estabelecimentoId(), categoria, nome);
        return ofertaPublicada(loja.dono(), produto, oferta(original, preco, estoque, vendaHoras, retiradaHoras));
    }

    private long pedidoSimples(Conta cliente, int quantidade) throws Exception {
        long oferta = ofertaNova("Simples " + UUID.randomUUID(), "10.00", "5.00", 10, 3, 5);
        var response = criar(cliente, chave(), pedido("PIX", item(oferta, quantidade)));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return tree(response).get("id").asLong();
    }

    private void publicar(long oferta) throws Exception {
        assertThat(send("POST", "/api/v1/ofertas/" + oferta + "/publicar", loja.dono().token(), null).statusCode()).isEqualTo(200);
    }

    private Map<String, Object> pedido(String forma, Map<String, Object>... itens) {
        var body = new LinkedHashMap<String, Object>();
        body.put("formaPagamento", forma);
        body.put("itens", new ArrayList<>(List.of(itens)));
        return body;
    }

    private Map<String, Object> item(long ofertaId, int quantidade) {
        return new LinkedHashMap<>(Map.of("ofertaId", ofertaId, "quantidade", quantidade));
    }

    private String chave() { return "teste-" + UUID.randomUUID(); }

    private HttpRequest criarRequest(Conta cliente, String chave, Object body) {
        return request("POST", "/api/v1/pedidos", cliente.token(), body, "Idempotency-Key", chave);
    }

    private HttpResponse<String> criar(Conta cliente, String chave, Object body) throws Exception {
        return http.send(criarRequest(cliente, chave, body), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> status(long id, String token, String status) throws Exception {
        return send("POST", "/api/v1/pedidos/" + id + "/status", token, Map.of("status", status));
    }

    private HttpResponse<String> cancelar(long id, String token, String motivo) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        if (motivo != null) { body.put("motivo", motivo); }
        return send("POST", "/api/v1/pedidos/" + id + "/cancelar", token, body);
    }

    private int estoque(long oferta) { return count("SELECT estoque_disponivel FROM oferta WHERE id=?", oferta); }

    private String corpos(List<HttpResponse<String>> respostas) {
        return respostas.stream().map(r -> r.statusCode() + " " + r.body()).toList().toString();
    }
}
