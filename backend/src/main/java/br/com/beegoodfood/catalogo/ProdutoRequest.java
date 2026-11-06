package br.com.beegoodfood.catalogo;

import jakarta.validation.constraints.*;

/** ativo omitido: true na criacao, valor atual na edicao. Desativar e logico (DEC-012). */
public record ProdutoRequest(
        @NotNull @Min(1) Long categoriaId,
        @NotBlank @Size(max = 160) String nome,
        @Size(max = 500) String descricao,
        Boolean ativo) {
    public ProdutoRequest {
        nome = nome == null ? null : nome.strip();
        descricao = descricao == null || descricao.isBlank() ? null : descricao.strip();
    }
}
