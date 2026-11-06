package br.com.beegoodfood.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Relogio com precisao de microssegundos, igual a DATETIME(6): o instante respondido e o mesmo gravado.
 * Substitui Clock.tick(..., 1 microssegundo): no Java 21, TickClock.millis() divide por zero com tique
 * menor que 1 ms (corrigido em versoes posteriores do JDK), o que quebrava o limite de tentativas.
 */
public final class RelogioMicrossegundos extends Clock {
    private final Clock base;

    public RelogioMicrossegundos(Clock base) { this.base = base; }

    @Override public ZoneId getZone() { return base.getZone(); }

    @Override public Clock withZone(ZoneId zone) { return new RelogioMicrossegundos(base.withZone(zone)); }

    @Override public Instant instant() { return base.instant().truncatedTo(ChronoUnit.MICROS); }

    @Override public long millis() { return base.millis(); }
}
