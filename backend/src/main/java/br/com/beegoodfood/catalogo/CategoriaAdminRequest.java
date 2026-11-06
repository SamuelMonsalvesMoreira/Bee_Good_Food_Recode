package br.com.beegoodfood.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Criacao: ativo omitido vale true. Edicao: ativo omitido preserva o valor atual. */
public record CategoriaAdminRequest(@NotBlank @Size(max = 100) String nome, Boolean ativo) {
    public CategoriaAdminRequest { nome = nome == null ? null : nome.strip().replaceAll("\\s+", " "); }
}
