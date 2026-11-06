package br.com.beegoodfood.comum;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Banco e servidor em UTC: DATETIME(6) guarda o instante como horario UTC sem fuso. */
public final class Datas {
    private Datas() { }

    public static LocalDateTime utc(Instant instante) {
        return instante == null ? null : LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    public static Instant instante(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC);
    }
}
