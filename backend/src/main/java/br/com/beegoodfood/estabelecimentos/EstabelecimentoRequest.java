package br.com.beegoodfood.estabelecimentos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record EstabelecimentoRequest(
        @NotBlank @Size(max = 160) String nome,
        @Size(max = 500) String descricao,
        @NotNull @Valid EnderecoRequest endereco) {
    public EstabelecimentoRequest {
        nome = nome == null ? null : nome.strip();
        descricao = EnderecoRequest.vazioParaNulo(descricao);
    }
}
