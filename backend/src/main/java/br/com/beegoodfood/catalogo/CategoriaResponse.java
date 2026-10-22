package br.com.beegoodfood.catalogo;

public record CategoriaResponse(Long id, String nome, String slug) {
    static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getSlug());
    }
}
