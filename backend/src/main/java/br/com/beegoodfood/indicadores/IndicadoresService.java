package br.com.beegoodfood.indicadores;

import br.com.beegoodfood.comum.Datas;
import br.com.beegoodfood.comum.RegraNegocioException;
import br.com.beegoodfood.estabelecimentos.EstabelecimentoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Formulas de docs/planejamento/metricas-e-validacao.md. Valores vem dos snapshots, nunca do catalogo atual. */
@Service
public class IndicadoresService {
    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private final NamedParameterJdbcTemplate jdbc;
    private final EstabelecimentoService estabelecimentos;
    private final Clock clock;

    public IndicadoresService(NamedParameterJdbcTemplate jdbc, EstabelecimentoService estabelecimentos, Clock clock) {
        this.jdbc = jdbc;
        this.estabelecimentos = estabelecimentos;
        this.clock = clock;
    }

    // Uma unica transacao de leitura: todas as consultas enxergam o mesmo instantaneo (REPEATABLE READ).
    @Transactional(readOnly = true)
    public IndicadoresResponse calcular(long estabelecimentoId, long usuarioId, LocalDateTime inicioLocal, LocalDateTime fimLocal) {
        // Propriedade verificada antes de qualquer calculo; terceiros recebem 404 sem dados.
        var estabelecimento = estabelecimentos.doProprietario(estabelecimentoId, usuarioId);
        var zona = ZoneId.of(estabelecimento.fusoIana());
        Instant consultadoEm = clock.instant();
        if (fimLocal == null) { fimLocal = LocalDateTime.ofInstant(consultadoEm, zona).truncatedTo(ChronoUnit.MICROS); }
        // Horario inexistente (inicio do horario de verao) avanca; horario repetido usa o primeiro deslocamento.
        Instant inicio = inicioLocal.atZone(zona).toInstant();
        Instant fim = fimLocal.atZone(zona).toInstant();
        if (!inicio.isBefore(fim)) { throw RegraNegocioException.invalido("fim", "O fim do período deve ser posterior ao início."); }
        if (fim.isAfter(consultadoEm)) { throw RegraNegocioException.invalido("fim", "O fim do período não pode estar no futuro."); }

        var p = new MapSqlParameterSource("estabelecimento", estabelecimentoId)
                .addValue("inicio", Datas.utc(inicio)).addValue("fim", Datas.utc(fim));
        String concluidoNoPeriodo = " p.estabelecimento_id = :estabelecimento AND p.concluido_em >= :inicio AND p.concluido_em < :fim";

        var volume = jdbc.queryForMap("SELECT COUNT(*) AS pedidos, COALESCE(SUM(p.total), 0) AS valor FROM pedido p WHERE"
                + concluidoNoPeriodo, p);
        var itens = jdbc.queryForMap("SELECT COALESCE(SUM(i.quantidade), 0) AS quantidade,"
                + " COALESCE(SUM((i.preco_original - i.preco_oferta) * i.quantidade), 0) AS economia"
                + " FROM item_pedido i JOIN pedido p ON p.id = i.pedido_id WHERE" + concluidoNoPeriodo, p);
        long concluidos = numero(volume.get("pedidos"));
        boolean publicou = Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM oferta o"
                + " JOIN produto pr ON pr.id = o.produto_id WHERE pr.estabelecimento_id = :estabelecimento"
                + " AND o.publicada_em >= :inicio AND o.publicada_em < :fim)", p, Boolean.class));
        var coorte = jdbc.queryForMap("SELECT COUNT(*) AS pedidos, COALESCE(SUM(p.status = 'CONCLUIDO'), 0) AS concluidos,"
                + " COALESCE(SUM(p.status = 'CANCELADO'), 0) AS cancelados FROM pedido p"
                + " WHERE p.estabelecimento_id = :estabelecimento AND p.criado_em >= :inicio AND p.criado_em < :fim", p);
        long total = numero(coorte.get("pedidos"));
        long coorteConcluidos = numero(coorte.get("concluidos"));
        long coorteCancelados = numero(coorte.get("cancelados"));

        return new IndicadoresResponse(estabelecimentoId, estabelecimento.fusoIana(),
                new IndicadoresResponse.Periodo(inicio, fim, inicioLocal, fimLocal), consultadoEm,
                concluidos, decimal(volume.get("valor")), numero(itens.get("quantidade")), decimal(itens.get("economia")),
                publicou || concluidos > 0 ? 1 : 0,
                new IndicadoresResponse.Coorte(total, coorteConcluidos, coorteCancelados, total - coorteConcluidos - coorteCancelados,
                        taxa(coorteConcluidos, total), taxa(coorteCancelados, total)));
    }

    /** Denominador zero: N/A (null). Numerador zero com pedidos: 0%. */
    static BigDecimal taxa(long parte, long total) {
        if (total == 0) { return null; }
        return BigDecimal.valueOf(parte).multiply(CEM).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_EVEN);
    }

    private static long numero(Object valor) { return ((Number) valor).longValue(); }

    private static BigDecimal decimal(Object valor) {
        return (valor instanceof BigDecimal b ? b : new BigDecimal(valor.toString())).setScale(2, RoundingMode.UNNECESSARY);
    }
}
