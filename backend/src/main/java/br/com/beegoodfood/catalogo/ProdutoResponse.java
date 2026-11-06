package br.com.beegoodfood.catalogo;

public record ProdutoResponse(long id, long estabelecimentoId, long categoriaId, String nome, String descricao, boolean ativo) {
    static ProdutoResponse from(Produto p) {
        return new ProdutoResponse(p.id(), p.estabelecimentoId(), p.categoriaId(), p.nome(), p.descricao(), p.ativo());
    }
}
