package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.Datas;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/** Datas com fuso explicito (ex.: 2026-11-06T18:00:00-03:00); armazenadas como instante UTC. */
public record OfertaRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoOriginal,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal precoOferta,
        @NotNull @Min(0) @Max(100000) Integer estoque,
        @NotNull OffsetDateTime inicioVenda,
        @NotNull OffsetDateTime vendaAte,
        @NotNull OffsetDateTime retiradaAte) {

    record Valores(BigDecimal precoOriginal, BigDecimal precoOferta, int estoque,
                   LocalDateTime inicioVenda, LocalDateTime vendaAte, LocalDateTime retiradaAte) { }

    Valores valores() {
        return new Valores(precoOriginal.setScale(2), precoOferta.setScale(2), estoque,
                utc(inicioVenda), utc(vendaAte), utc(retiradaAte));
    }

    private static LocalDateTime utc(OffsetDateTime data) {
        // DATETIME(6): precisao de microssegundos.
        return Datas.utc(data.toInstant().truncatedTo(ChronoUnit.MICROS));
    }
}
