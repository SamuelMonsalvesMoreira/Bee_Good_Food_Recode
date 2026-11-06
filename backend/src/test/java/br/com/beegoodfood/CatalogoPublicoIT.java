package br.com.beegoodfood;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/** Etapa 5.4: catalogo publico com filtros, paginacao e somente ofertas compraveis (CA05, CA21, CA24). */
class CatalogoPublicoIT extends IntegracaoBase {
    private long padaria, doces;
    private long paoFrances, boloCenoura, paoDeMel, sonhoEspecial;
    private final List<Long> naoCompraveis = new ArrayList<>();

    @BeforeAll
    void preparar() throws Exception {
        limparBanco(jdbc);
        var admin = admin();
        padaria = categoria(admin, "Padaria");
        doces = categoria(admin, "Doces");
        var santos = loja(admin, "Padaria Gonzaga", "Santos", "Gonzaga");
        var paulo = loja(admin, "Empório Pinheiros", "São Paulo", "Pinheiros");
        var suspensa = loja(admin, "Loja Suspensa", "Santos", "Gonzaga");

        paoFrances = ofertaPublicada(santos.dono(), produto(santos.dono(), santos.estabelecimentoId(), padaria, "Pão francês"), oferta("1.00", "0.50", 30, 2, 3));
        boloCenoura = ofertaPublicada(santos.dono(), produto(santos.dono(), santos.estabelecimentoId(), doces, "Bolo de cenoura"), oferta("30.00", "15.00", 2, 3, 4));
        paoDeMel = ofertaPublicada(paulo.dono(), produto(paulo.dono(), paulo.estabelecimentoId(), doces, "Pão de mel"), oferta("8.00", "4.00", 5, 4, 6));
        sonhoEspecial = ofertaPublicada(paulo.dono(), produto(paulo.dono(), paulo.estabelecimentoId(), padaria, "Sonho 100%_especial"), oferta("9.00", "6.00", 5, 5, 6));

        // Nao compraveis: rascunho, pausada, sem estoque, futura, expirada, produto/categoria inativos e loja suspensa.
        var dono = santos.dono();
        long est = santos.estabelecimentoId();
        naoCompraveis.add(rascunho(dono, produto(dono, est, padaria, "Rascunho de pão"), oferta("5.00", "2.00", 5, 3, 4)));
        long pausada = ofertaPublicada(dono, produto(dono, est, padaria, "Pão pausado"), oferta("5.00", "2.00", 5, 3, 4));
        send("POST", "/api/v1/ofertas/" + pausada + "/pausar", dono.token(), null);
        naoCompraveis.add(pausada);
        long semEstoque = ofertaPublicada(dono, produto(dono, est, padaria, "Pão esgotado"), oferta("5.00", "2.00", 1, 3, 4));
        send("POST", "/api/v1/ofertas/" + semEstoque + "/estoque", dono.token(), Map.of("ajuste", -1));
        naoCompraveis.add(semEstoque);
        var futura = oferta("5.00", "2.00", 5, 3, 4);
        futura.put("inicioVenda", java.time.OffsetDateTime.now().plusHours(1).toString());
        naoCompraveis.add(ofertaPublicada(dono, produto(dono, est, padaria, "Pão futuro"), futura));
        long expirada = ofertaPublicada(dono, produto(dono, est, padaria, "Pão expirado"), oferta("5.00", "2.00", 5, 3, 4));
        jdbc.update("UPDATE oferta SET inicio_venda = UTC_TIMESTAMP(6) - INTERVAL 3 HOUR, venda_ate = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE id=?", expirada);
        naoCompraveis.add(expirada);
        long produtoInativo = produto(dono, est, padaria, "Pão inativo");
        naoCompraveis.add(ofertaPublicada(dono, produtoInativo, oferta("5.00", "2.00", 5, 3, 4)));
        send("PUT", "/api/v1/produtos/" + produtoInativo, dono.token(), Map.of("categoriaId", padaria, "nome", "Pão inativo", "ativo", false));
        long categoriaInativa = categoria(admin, "Sazonais");
        naoCompraveis.add(ofertaPublicada(dono, produto(dono, est, categoriaInativa, "Panetone"), oferta("50.00", "20.00", 5, 3, 4)));
        send("PUT", "/api/v1/admin/categorias/" + categoriaInativa, admin.token(), Map.of("nome", "Sazonais", "ativo", false));
        naoCompraveis.add(ofertaPublicada(suspensa.dono(), produto(suspensa.dono(), suspensa.estabelecimentoId(), padaria, "Pão suspenso"), oferta("5.00", "2.00", 5, 3, 4)));
        send("POST", "/api/v1/admin/estabelecimentos/" + suspensa.estabelecimentoId() + "/suspender", admin.token(), Map.of("motivo", "Teste"));
    }

    @Test
    void visitanteVeSomenteOfertasCompraveisEmOrdemDeterministica() throws Exception {
        var response = get("/api/v1/ofertas", null);
        assertThat(response.statusCode()).isEqualTo(200);
        var body = tree(response);
        assertThat(ids(body)).containsExactly(paoFrances, boloCenoura, paoDeMel, sonhoEspecial);
        assertThat(body.get("tamanho").asInt()).isEqualTo(20);
        assertThat(body.get("totalElementos").asInt()).isEqualTo(4);
        assertThat(body.get("ultima").asBoolean()).isTrue();
        var primeira = body.get("conteudo").get(0);
        assertThat(primeira.get("estabelecimento").get("fusoIana").asText()).isEqualTo("America/Sao_Paulo");
        assertThat(primeira.get("vendaAte").asText()).endsWith("Z");
        assertThat(primeira.get("retiradaAte").asText()).isNotEqualTo(primeira.get("vendaAte").asText());
        assertThat(response.body()).doesNotContain("responsavel", "cep", "email");
    }

    @Test
    void filtrosSaoCombinadosEIgnoramMaiusculasEAcentos() throws Exception {
        assertThat(ids(tree(get("/api/v1/ofertas?texto=pao", null)))).containsExactly(paoFrances, paoDeMel);
        assertThat(ids(tree(get("/api/v1/ofertas?categoriaId=" + doces, null)))).containsExactly(boloCenoura, paoDeMel);
        assertThat(ids(tree(get("/api/v1/ofertas?cidade=sao%20paulo", null)))).containsExactly(paoDeMel, sonhoEspecial);
        assertThat(ids(tree(get("/api/v1/ofertas?cidade=Santos&bairro=GONZAGA", null)))).containsExactly(paoFrances, boloCenoura);
        assertThat(ids(tree(get("/api/v1/ofertas?texto=P%C3%83O&cidade=S%C3%A3o%20Paulo&categoriaId=" + doces, null)))).containsExactly(paoDeMel);
        assertThat(ids(tree(get("/api/v1/ofertas?texto=gonzaga", null)))).containsExactly(paoFrances, boloCenoura);
        var vazio = tree(get("/api/v1/ofertas?texto=pao&cidade=Recife", null));
        assertThat(vazio.get("conteudo").size()).isZero();
        assertThat(vazio.get("totalElementos").asInt()).isZero();
        assertThat(vazio.get("ultima").asBoolean()).isTrue();
    }

    @Test
    void curingasDoTextoSaoLiterais() throws Exception {
        assertThat(ids(tree(get("/api/v1/ofertas?texto=%25", null)))).containsExactly(sonhoEspecial);
        assertThat(ids(tree(get("/api/v1/ofertas?texto=_", null)))).containsExactly(sonhoEspecial);
        assertThat(ids(tree(get("/api/v1/ofertas?texto=%25_e", null)))).containsExactly(sonhoEspecial);
    }

    @Test
    void paginasSucessivasNaoRepetemRegistros() throws Exception {
        var vistos = new HashSet<Long>();
        for (int pagina = 0; pagina < 4; pagina++) {
            var body = tree(get("/api/v1/ofertas?tamanho=1&pagina=" + pagina, null));
            assertThat(body.get("totalPaginas").asInt()).isEqualTo(4);
            assertThat(body.get("ultima").asBoolean()).isEqualTo(pagina == 3);
            assertThat(vistos.add(ids(body).getFirst())).isTrue();
        }
        assertThat(vistos).containsExactlyInAnyOrder(paoFrances, boloCenoura, paoDeMel, sonhoEspecial);
        assertThat(tree(get("/api/v1/ofertas?tamanho=1&pagina=9", null)).get("conteudo").size()).isZero();
    }

    @Test
    void ordenacoesDaVitrineSaoDeterministicas() throws Exception {
        assertThat(ids(tree(get("/api/v1/ofertas?ordem=PRAZO", null)))).containsExactly(paoFrances, boloCenoura, paoDeMel, sonhoEspecial);
        assertThat(ids(tree(get("/api/v1/ofertas?ordem=PRECO", null)))).containsExactly(paoFrances, paoDeMel, sonhoEspecial, boloCenoura);
        // Mesmo desconto (50%): desempate pelo ID.
        assertThat(ids(tree(get("/api/v1/ofertas?ordem=DESCONTO", null)))).containsExactly(paoFrances, boloCenoura, paoDeMel, sonhoEspecial);
        assertThat(ids(tree(get("/api/v1/ofertas?ordem=RECENTES", null)))).containsExactly(sonhoEspecial, paoDeMel, boloCenoura, paoFrances);
        assertThat(ids(tree(get("/api/v1/ofertas?ordem=PRECO&cidade=santos", null)))).containsExactly(paoFrances, boloCenoura);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ordem=QUALQUER", "ordem=preco;DROP", "tamanho=0", "tamanho=101", "pagina=-1", "categoriaId=0", "categoriaId=abc", "estabelecimentoId=-5"})
    void parametrosInvalidosRetornam400(String query) throws Exception {
        assertProblem(get("/api/v1/ofertas?" + query, null), 400);
    }

    @Test
    void textoLongoDemaisERejeitado() throws Exception {
        assertProblem(get("/api/v1/ofertas?texto=" + "a".repeat(101), null), 400);
    }

    @Test
    void detalheSoExisteParaOfertaCompravel() throws Exception {
        var detalhe = get("/api/v1/ofertas/" + paoDeMel, null);
        assertThat(detalhe.statusCode()).isEqualTo(200);
        assertThat(tree(detalhe).get("estabelecimento").get("nome").asText()).isEqualTo("Empório Pinheiros");
        assertThat(tree(detalhe).get("precoOferta").decimalValue()).isEqualByComparingTo("4.00");
        for (long id : naoCompraveis) {
            assertProblem(get("/api/v1/ofertas/" + id, null), 404);
        }
        assertProblem(get("/api/v1/ofertas/999999", null), 404);
        var todos = ids(tree(get("/api/v1/ofertas?tamanho=100", null)));
        assertThat(todos).doesNotContainAnyElementsOf(naoCompraveis);
    }

    @Test
    void escritaNoCatalogoPublicoExigeAutenticacao() throws Exception {
        assertProblem(send("POST", "/api/v1/ofertas/" + paoDeMel + "/encerrar", null, null), 401);
        assertProblem(send("PUT", "/api/v1/ofertas/" + paoDeMel, null, Map.of()), 401);
    }

    private List<Long> ids(JsonNode body) {
        var ids = new ArrayList<Long>();
        body.get("conteudo").forEach(o -> ids.add(o.get("id").asLong()));
        return ids;
    }
}
