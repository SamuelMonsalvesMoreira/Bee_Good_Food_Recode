package br.com.beegoodfood.auth;

import jakarta.validation.constraints.*;

public record CadastroRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 12, max = 72) @SenhaEmBytes String senha,
        @NotNull @AssertTrue(message = "É necessário declarar que você tem 18 anos ou mais.") Boolean maiorDeIdade) {
    public CadastroRequest {
        nome = nome == null ? null : nome.strip();
        email = email == null ? null : email.strip();
    }

    @Override public String toString() { return "CadastroRequest[conteudo omitido]"; }
}
