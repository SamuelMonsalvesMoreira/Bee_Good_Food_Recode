package br.com.beegoodfood.catalogo;

public record CategoriaAdminResponse(long id, String nome, String slug, boolean ativo) {
    static CategoriaAdminResponse from(Categoria c) {
        return new CategoriaAdminResponse(c.getId(), c.getNome(), c.getSlug(), c.isAtivo());
    }
}
