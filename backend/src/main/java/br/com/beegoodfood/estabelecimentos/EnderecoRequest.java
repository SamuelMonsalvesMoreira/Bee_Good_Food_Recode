package br.com.beegoodfood.estabelecimentos;

import jakarta.validation.constraints.*;
import java.util.Locale;

public record EnderecoRequest(
        @NotBlank @Size(max = 180) String logradouro,
        @NotBlank @Size(max = 30) String numero,
        @Size(max = 120) String complemento,
        @NotBlank @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "Informe a sigla da UF com duas letras.") String estado,
        @NotBlank @Pattern(regexp = "[0-9]{5}-?[0-9]{3}", message = "Informe o CEP com 8 dígitos.") String cep,
        @Size(max = 64) String fusoIana) {
    public static final String FUSO_PADRAO = "America/Sao_Paulo";

    public EnderecoRequest {
        logradouro = strip(logradouro);
        numero = strip(numero);
        complemento = vazioParaNulo(complemento);
        bairro = strip(bairro);
        cidade = strip(cidade);
        estado = estado == null ? null : estado.strip().toUpperCase(Locale.ROOT);
        cep = cep == null ? null : cep.strip().replace("-", "");
        fusoIana = vazioParaNulo(fusoIana) == null ? FUSO_PADRAO : fusoIana.strip();
    }

    private static String strip(String valor) { return valor == null ? null : valor.strip(); }
    static String vazioParaNulo(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }
}
