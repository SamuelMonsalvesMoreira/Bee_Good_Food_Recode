package br.com.beegoodfood.auth;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 72) @SenhaEmBytes String senha) {
    public LoginRequest { email = email == null ? null : email.strip(); }
    @Override public String toString() { return "LoginRequest[conteudo omitido]"; }
}
