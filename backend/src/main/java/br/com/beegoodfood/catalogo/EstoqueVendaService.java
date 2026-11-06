package br.com.beegoodfood.catalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contrato do catalogo para o modulo de pedidos. So funciona dentro da transacao do pedido (MANDATORY):
 * bloqueio, baixa e devolucao sao confirmados ou desfeitos junto com o pedido (CA09).
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class EstoqueVendaService {
    public record OfertaParaVenda(long ofertaId, long produtoId, String produtoNome, long estabelecimentoId,
                                  StatusOferta status, int estoque, BigDecimal precoOriginal, BigDecimal precoOferta,
                                  LocalDateTime inicioVenda, LocalDateTime vendaAte, LocalDateTime retiradaAte,
                                  boolean produtoAtivo, boolean categoriaAtiva) {
        /** Mesmas regras do catalogo publico, exceto estoque e estabelecimento (verificados a parte). */
        public boolean compravel(LocalDateTime agora) {
            return status == StatusOferta.PUBLICADA && !inicioVenda.isAfter(agora) && vendaAte.isAfter(agora)
                    && produtoAtivo && categoriaAtiva;
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public EstoqueVendaService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    /**
     * Bloqueia somente as linhas de oferta (FOR UPDATE OF o), em ordem crescente de ID para evitar deadlock
     * entre pedidos concorrentes. Produto e categoria nao sao bloqueados para nao serializar a loja inteira.
     */
    public List<OfertaParaVenda> bloquear(Collection<Long> ofertaIds) {
        if (ofertaIds.isEmpty()) { return List.of(); }
        return jdbc.query("SELECT o.id, o.produto_id, p.nome, p.estabelecimento_id, o.status, o.estoque_disponivel,"
                        + " o.preco_original, o.preco_oferta, o.inicio_venda, o.venda_ate, o.retirada_ate, p.ativo AS produto_ativo,"
                        + " c.ativo AS categoria_ativa FROM oferta o JOIN produto p ON p.id = o.produto_id"
                        + " JOIN categoria c ON c.id = p.categoria_id WHERE o.id IN (:ids) ORDER BY o.id FOR UPDATE OF o",
                new MapSqlParameterSource("ids", ofertaIds),
                (rs, n) -> new OfertaParaVenda(rs.getLong("id"), rs.getLong("produto_id"), rs.getString("nome"),
                        rs.getLong("estabelecimento_id"), StatusOferta.valueOf(rs.getString("status")),
                        rs.getInt("estoque_disponivel"), rs.getBigDecimal("preco_original"), rs.getBigDecimal("preco_oferta"),
                        rs.getObject("inicio_venda", LocalDateTime.class), rs.getObject("venda_ate", LocalDateTime.class),
                        rs.getObject("retirada_ate", LocalDateTime.class), rs.getBoolean("produto_ativo"),
                        rs.getBoolean("categoria_ativa")));
    }

    /** Baixa condicional: nunca deixa o saldo negativo. Retorna false se o estoque nao bastar. */
    public boolean baixar(long ofertaId, int quantidade) {
        return jdbc.update("UPDATE oferta SET estoque_disponivel = estoque_disponivel - :q WHERE id = :id AND estoque_disponivel >= :q",
                new MapSqlParameterSource("id", ofertaId).addValue("q", quantidade)) == 1;
    }

    /** Devolucao ao cancelar. Nao altera o status: oferta encerrada ou expirada continua indisponivel. */
    public void devolver(long ofertaId, int quantidade) {
        int linhas = jdbc.update("UPDATE oferta SET estoque_disponivel = estoque_disponivel + :q WHERE id = :id",
                new MapSqlParameterSource("id", ofertaId).addValue("q", quantidade));
        if (linhas != 1) { throw new IllegalStateException("Oferta do item nao encontrada"); }
    }
}
