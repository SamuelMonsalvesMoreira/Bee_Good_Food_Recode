package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.Datas;
import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.RecursoNaoEncontradoException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalogo publico (CA05, CA21). Uma oferta so aparece quando e compravel agora: PUBLICADA, dentro de
 * [inicioVenda, vendaAte), com estoque, produto e categoria ativos e estabelecimento ATIVO.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoPublicoService {
    static final String COMPRAVEL = " o.status = 'PUBLICADA' AND o.inicio_venda <= :agora AND o.venda_ate > :agora"
            + " AND o.estoque_disponivel > 0 AND p.ativo AND c.ativo AND e.status = 'ATIVO' AND e.ativo ";
    private static final String FROM = " FROM oferta o JOIN produto p ON p.id = o.produto_id"
            + " JOIN categoria c ON c.id = p.categoria_id JOIN estabelecimento e ON e.id = p.estabelecimento_id"
            + " JOIN endereco_estabelecimento en ON en.estabelecimento_id = e.id WHERE " + COMPRAVEL;
    private static final String COLUNAS = "SELECT o.id, o.produto_id, p.nome AS produto_nome, p.descricao AS produto_descricao,"
            + " c.id AS categoria_id, c.nome AS categoria_nome, c.slug, o.preco_original, o.preco_oferta, o.estoque_disponivel,"
            + " o.inicio_venda, o.venda_ate, o.retirada_ate, e.id AS estabelecimento_id, e.nome AS estabelecimento_nome,"
            + " en.logradouro, en.numero, en.complemento, en.bairro, en.cidade, en.estado, en.fuso_iana";

    public record Filtros(String texto, Long categoriaId, String cidade, String bairro, Long estabelecimentoId, Ordem ordem) {
        public Filtros(String texto, Long categoriaId, String cidade, String bairro, Long estabelecimentoId) {
            this(texto, categoriaId, cidade, bairro, estabelecimentoId, Ordem.PRAZO);
        }
    }

    /** Ordenacoes da vitrine; o ID sempre desempata para paginas estaveis. */
    public enum Ordem {
        PRAZO("o.venda_ate ASC, o.id ASC"),
        DESCONTO("(o.preco_original - o.preco_oferta) / o.preco_original DESC, o.id ASC"),
        PRECO("o.preco_oferta ASC, o.id ASC"),
        RECENTES("o.publicada_em DESC, o.id DESC");

        private final String sql;
        Ordem(String sql) { this.sql = sql; }
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public CatalogoPublicoService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public PaginaResponse<OfertaPublicaResponse> buscar(Filtros filtros, int pagina, int tamanho) {
        var params = new MapSqlParameterSource("agora", LocalDateTime.now(clock));
        var where = new StringBuilder();
        if (filtros.texto() != null) {
            // Texto tratado como literal: curingas do LIKE sao escapados.
            where.append(" AND (p.nome LIKE :texto OR p.descricao LIKE :texto OR e.nome LIKE :texto)");
            params.addValue("texto", "%" + escaparLike(filtros.texto()) + "%");
        }
        if (filtros.categoriaId() != null) { where.append(" AND c.id = :categoria"); params.addValue("categoria", filtros.categoriaId()); }
        // Colacao utf8mb4_0900_ai_ci: comparacao sem diferenciar maiusculas e acentos.
        if (filtros.cidade() != null) { where.append(" AND en.cidade = :cidade"); params.addValue("cidade", filtros.cidade()); }
        if (filtros.bairro() != null) { where.append(" AND en.bairro = :bairro"); params.addValue("bairro", filtros.bairro()); }
        if (filtros.estabelecimentoId() != null) {
            where.append(" AND e.id = :estabelecimento");
            params.addValue("estabelecimento", filtros.estabelecimentoId());
        }
        long total = jdbc.queryForObject("SELECT COUNT(*)" + FROM + where, params, Long.class);
        params.addValue("limite", tamanho).addValue("deslocamento", (long) pagina * tamanho);
        // Ordenacao deterministica escolhida de uma lista fechada (nunca texto do cliente no SQL).
        var ordem = filtros.ordem() == null ? Ordem.PRAZO : filtros.ordem();
        List<OfertaPublicaResponse> conteudo = total == 0 ? List.of()
                : jdbc.query(COLUNAS + FROM + where + " ORDER BY " + ordem.sql + " LIMIT :limite OFFSET :deslocamento",
                params, (rs, n) -> mapear(rs));
        int totalPaginas = (int) Math.ceil((double) total / tamanho);
        return new PaginaResponse<>(conteudo, pagina, tamanho, total, totalPaginas, pagina >= totalPaginas - 1);
    }

    public OfertaPublicaResponse detalhe(long id) {
        var params = new MapSqlParameterSource("agora", LocalDateTime.now(clock)).addValue("id", id);
        return jdbc.query(COLUNAS + FROM + " AND o.id = :id", params, (rs, n) -> mapear(rs)).stream().findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Oferta não encontrada ou indisponível."));
    }

    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static OfertaPublicaResponse mapear(ResultSet rs) throws SQLException {
        return new OfertaPublicaResponse(rs.getLong("id"), rs.getLong("produto_id"), rs.getString("produto_nome"),
                rs.getString("produto_descricao"),
                new OfertaPublicaResponse.Categoria(rs.getLong("categoria_id"), rs.getString("categoria_nome"), rs.getString("slug")),
                rs.getBigDecimal("preco_original"), rs.getBigDecimal("preco_oferta"), rs.getInt("estoque_disponivel"),
                Datas.instante(rs.getObject("inicio_venda", LocalDateTime.class)),
                Datas.instante(rs.getObject("venda_ate", LocalDateTime.class)),
                Datas.instante(rs.getObject("retirada_ate", LocalDateTime.class)),
                new OfertaPublicaResponse.Estabelecimento(rs.getLong("estabelecimento_id"), rs.getString("estabelecimento_nome"),
                        rs.getString("logradouro"), rs.getString("numero"), rs.getString("complemento"), rs.getString("bairro"),
                        rs.getString("cidade"), rs.getString("estado"), rs.getString("fuso_iana")));
    }
}
