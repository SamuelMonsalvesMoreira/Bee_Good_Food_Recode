package br.com.beegoodfood.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelogioMicrossegundosTest {
    private final Clock base = Clock.fixed(Instant.parse("2026-11-06T12:00:00.123456789Z"), ZoneOffset.UTC);
    private final Clock relogio = new RelogioMicrossegundos(base);

    @Test
    void instanteTemPrecisaoDeMicrossegundosComoDatetime6() {
        assertThat(relogio.instant()).isEqualTo(Instant.parse("2026-11-06T12:00:00.123456Z"));
    }

    @Test
    void millisNaoDivideporZeroNoJava21() {
        // Regressao: Clock.tick(..., 1 microssegundo).millis() lancava ArithmeticException no Java 21.
        assertThat(relogio.millis()).isEqualTo(base.millis());
    }

    @Test
    void preservaPrecisaoAoTrocarDeFuso() {
        var emSaoPaulo = relogio.withZone(ZoneId.of("America/Sao_Paulo"));
        assertThat(emSaoPaulo.getZone()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
        assertThat(emSaoPaulo.instant().getNano() % 1000).isZero();
    }
}
