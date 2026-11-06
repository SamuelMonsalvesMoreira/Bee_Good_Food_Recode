package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.RecursoNaoEncontradoException;
import br.com.beegoodfood.comum.RegraNegocioException;
import br.com.beegoodfood.estabelecimentos.EstabelecimentoResumo;
import br.com.beegoodfood.estabelecimentos.EstabelecimentoService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.beegoodfood.catalogo.StatusOferta.*;

/** Produtos e ofertas do proprietario. Toda operacao confere propriedade do estabelecimento (CA03). */
@Service
@Transactional(readOnly = true)
public class CatalogoGestaoService {
    private static final String PRODUTO_NAO_ENCONTRADO = "Produto não encontrado.";
    private static final String OFERTA_NAO_ENCONTRADA = "Oferta não encontrada.";

    public enum Acao { PUBLICAR, PAUSAR, RETOMAR, ENCERRAR }

    private final ProdutoRepository produtos;
    private final OfertaRepository ofertas;
    private final CategoriaService categorias;
    private final EstabelecimentoService estabelecimentos;
    private final Clock clock;

    public CatalogoGestaoService(ProdutoRepository produtos, OfertaRepository ofertas, CategoriaService categorias,
                                 EstabelecimentoService estabelecimentos, Clock clock) {
        this.produtos = produtos;
        this.ofertas = ofertas;
        this.categorias = categorias;
        this.estabelecimentos = estabelecimentos;
        this.clock = clock;
    }

    // ---- Produtos ----

    @Transactional
    public ProdutoResponse criarProduto(long estabelecimentoId, long usuarioId, ProdutoRequest request) {
        estabelecimentos.doProprietario(estabelecimentoId, usuarioId);
        exigirCategoriaAtiva(request.categoriaId());
        return ProdutoResponse.from(produtos.saveAndFlush(new Produto(estabelecimentoId, request, agora())));
    }

    public PaginaResponse<ProdutoResponse> listarProdutos(long estabelecimentoId, long usuarioId, int pagina, int tamanho) {
        estabelecimentos.doProprietario(estabelecimentoId, usuarioId);
        return PaginaResponse.from(produtos.findByEstabelecimentoId(estabelecimentoId, PageRequest.of(pagina, tamanho, Sort.by("id")))
                .map(ProdutoResponse::from));
    }

    public ProdutoResponse produto(long produtoId, long usuarioId) {
        return ProdutoResponse.from(produtoDoProprietario(produtoId, usuarioId));
    }

    @Transactional
    public ProdutoResponse atualizarProduto(long produtoId, long usuarioId, ProdutoRequest request) {
        var produto = produtoDoProprietario(produtoId, usuarioId);
        if (produto.categoriaId() != request.categoriaId()) { exigirCategoriaAtiva(request.categoriaId()); }
        // Itens de pedido guardam snapshot do nome: editar aqui nao altera o historico (CA11).
        produto.aplicar(request);
        return ProdutoResponse.from(produtos.saveAndFlush(produto));
    }

    // ---- Ofertas ----

    @Transactional
    public OfertaResponse criarOferta(long produtoId, long usuarioId, OfertaRequest request) {
        var produto = produtoDoProprietario(produtoId, usuarioId);
        var estabelecimento = estabelecimentos.resumo(produto.estabelecimentoId());
        if (!estabelecimento.ativo()) {
            throw RegraNegocioException.conflito("O estabelecimento precisa estar ativo para cadastrar ofertas.");
        }
        var valores = request.valores();
        validarCondicoes(valores);
        return OfertaResponse.from(ofertas.saveAndFlush(new Oferta(produto.id(), valores, agora())));
    }

    public PaginaResponse<OfertaResponse> listarOfertas(long produtoId, long usuarioId, int pagina, int tamanho) {
        produtoDoProprietario(produtoId, usuarioId);
        return PaginaResponse.from(ofertas.findByProdutoId(produtoId, PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id")))
                .map(OfertaResponse::from));
    }

    public OfertaResponse oferta(long ofertaId, long usuarioId) {
        var oferta = ofertas.findById(ofertaId).orElseThrow(() -> new RecursoNaoEncontradoException(OFERTA_NAO_ENCONTRADA));
        exigirProprietario(oferta, usuarioId);
        return OfertaResponse.from(oferta);
    }

    @Transactional
    public OfertaResponse editarRascunho(long ofertaId, long usuarioId, OfertaRequest request) {
        var oferta = ofertaBloqueada(ofertaId, usuarioId);
        if (oferta.status() != RASCUNHO) {
            throw RegraNegocioException.conflito("Preço e prazos não mudam após a publicação. Encerre esta oferta e crie outra.");
        }
        var valores = request.valores();
        validarCondicoes(valores);
        oferta.editarRascunho(valores);
        return OfertaResponse.from(ofertas.saveAndFlush(oferta));
    }

    @Transactional
    public OfertaResponse alterarEstado(long ofertaId, long usuarioId, Acao acao) {
        var oferta = ofertaBloqueada(ofertaId, usuarioId);
        var atual = oferta.status();
        switch (acao) {
            case PUBLICAR -> {
                exigirEstado(atual == RASCUNHO, atual);
                exigirPublicavel(oferta, true);
                oferta.publicar(agora());
            }
            case RETOMAR -> {
                exigirEstado(atual == PAUSADA, atual);
                exigirPublicavel(oferta, false);
                oferta.publicar(agora());
            }
            case PAUSAR -> {
                exigirEstado(atual == PUBLICADA, atual);
                oferta.pausar();
            }
            case ENCERRAR -> {
                exigirEstado(atual != ENCERRADA, atual);
                oferta.encerrar(agora());
            }
        }
        // A unicidade de oferta publicada/pausada por produto e da constraint do MySQL (resiste a concorrencia).
        return OfertaResponse.from(ofertas.saveAndFlush(oferta));
    }

    @Transactional
    public OfertaResponse ajustarEstoque(long ofertaId, long usuarioId, int ajuste) {
        var oferta = ofertas.findById(ofertaId).orElseThrow(() -> new RecursoNaoEncontradoException(OFERTA_NAO_ENCONTRADA));
        exigirProprietario(oferta, usuarioId);
        if (ofertas.ajustarEstoque(ofertaId, ajuste) == 0) {
            throw RegraNegocioException.conflito(oferta.status() == ENCERRADA
                    ? "Oferta encerrada não aceita ajuste de estoque."
                    : "O ajuste deixaria o estoque abaixo de zero ou acima de 100000.");
        }
        return OfertaResponse.from(ofertas.findById(ofertaId).orElseThrow());
    }

    // ---- Regras ----

    private void exigirPublicavel(Oferta oferta, boolean publicacao) {
        var produto = produtos.findById(oferta.produtoId()).orElseThrow();
        var estabelecimento = estabelecimentos.resumo(produto.estabelecimentoId());
        if (!estabelecimento.ativo()) {
            throw RegraNegocioException.conflito("O estabelecimento precisa estar ativo para publicar ofertas.");
        }
        if (!produto.ativo()) { throw RegraNegocioException.conflito("O produto está desativado."); }
        if (!categorias.ativa(produto.categoriaId())) { throw RegraNegocioException.conflito("A categoria do produto está desativada."); }
        if (!oferta.vendaAte().isAfter(agora())) {
            throw RegraNegocioException.invalido("vendaAte", "O fim da venda já passou. Crie uma nova oferta.");
        }
        if (publicacao && oferta.estoqueDisponivel() <= 0) {
            throw RegraNegocioException.invalido("estoque", "Informe estoque positivo para publicar.");
        }
    }

    private void validarCondicoes(OfertaRequest.Valores v) {
        var campos = new LinkedHashMap<String, String>();
        if (v.precoOferta().compareTo(v.precoOriginal()) > 0) {
            campos.put("precoOferta", "O preço da oferta não pode ser maior que o preço original.");
        }
        if (!v.inicioVenda().isBefore(v.vendaAte())) {
            campos.put("vendaAte", "O fim da venda deve ser posterior ao início.");
        } else if (!v.vendaAte().isAfter(agora())) {
            campos.put("vendaAte", "O fim da venda já passou.");
        }
        if (v.retiradaAte().isBefore(v.vendaAte())) {
            campos.put("retiradaAte", "O prazo de retirada não pode terminar antes do fim da venda.");
        }
        if (!campos.isEmpty()) { throw RegraNegocioException.invalido(campos); }
    }

    private void exigirEstado(boolean permitido, StatusOferta atual) {
        if (!permitido) { throw RegraNegocioException.conflito("Transição não permitida para oferta em " + atual.name() + "."); }
    }

    private void exigirCategoriaAtiva(long categoriaId) {
        if (!categorias.ativa(categoriaId)) {
            throw RegraNegocioException.invalido("categoriaId", "Escolha uma categoria ativa.");
        }
    }

    private Produto produtoDoProprietario(long produtoId, long usuarioId) {
        var produto = produtos.findById(produtoId).orElseThrow(() -> new RecursoNaoEncontradoException(PRODUTO_NAO_ENCONTRADO));
        try {
            estabelecimentos.doProprietario(produto.estabelecimentoId(), usuarioId);
        } catch (RecursoNaoEncontradoException error) {
            throw new RecursoNaoEncontradoException(PRODUTO_NAO_ENCONTRADO);
        }
        return produto;
    }

    private Oferta ofertaBloqueada(long ofertaId, long usuarioId) {
        var oferta = ofertas.bloquear(ofertaId).orElseThrow(() -> new RecursoNaoEncontradoException(OFERTA_NAO_ENCONTRADA));
        exigirProprietario(oferta, usuarioId);
        return oferta;
    }

    private EstabelecimentoResumo exigirProprietario(Oferta oferta, long usuarioId) {
        var produto = produtos.findById(oferta.produtoId()).orElseThrow();
        try {
            return estabelecimentos.doProprietario(produto.estabelecimentoId(), usuarioId);
        } catch (RecursoNaoEncontradoException error) {
            throw new RecursoNaoEncontradoException(OFERTA_NAO_ENCONTRADA);
        }
    }

    private LocalDateTime agora() { return LocalDateTime.now(clock); }
}
