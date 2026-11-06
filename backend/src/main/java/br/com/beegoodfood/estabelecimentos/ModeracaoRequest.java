package br.com.beegoodfood.estabelecimentos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Toda decisao administrativa exige motivo. */
public record ModeracaoRequest(@NotBlank @Size(max = 500) String motivo) {
    public ModeracaoRequest { motivo = motivo == null ? null : motivo.strip(); }
}
