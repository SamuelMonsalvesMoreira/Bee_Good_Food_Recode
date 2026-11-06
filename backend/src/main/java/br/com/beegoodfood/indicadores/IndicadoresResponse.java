package br.com.beegoodfood.indicadores;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Indicadores de um unico estabelecimento (CA17). Taxas sao percentuais com 4 casas, sem arredondamento de
 * apresentacao; null significa "N/A — sem pedidos no periodo" (denominador zero), nunca zero.
 */
public record IndicadoresResponse(long estabelecimentoId, String fusoIana, Periodo periodo, Instant consultadoEm,
                                  long pedidosConcluidos, BigDecimal valorMovimentado, long itensComercializados,
                                  BigDecimal economiaEstimada, int estabelecimentoComAtividade, Coorte coorte) {
    /** inicio/fim em UTC; inicioLocal/fimLocal como informados, no fuso do estabelecimento. Intervalo [inicio, fim). */
    public record Periodo(Instant inicio, Instant fim, LocalDateTime inicioLocal, LocalDateTime fimLocal) { }

    /** Pedidos criados no periodo, classificados pelo estado no instante da consulta. */
    public record Coorte(long pedidos, long concluidos, long cancelados, long emAndamento,
                         BigDecimal taxaConclusao, BigDecimal taxaCancelamento) { }
}
