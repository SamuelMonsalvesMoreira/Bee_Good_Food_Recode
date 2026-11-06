package br.com.beegoodfood.pedidos;

import br.com.beegoodfood.comum.Datas;
import java.math.BigDecimal;
import java.time.Instant;

/** Linha de listagem (historico do cliente ou fila do estabelecimento), sem carregar itens. */
public record PedidoResumoResponse(long id, long clienteId, long estabelecimentoId, String estabelecimentoNome,
                                   StatusPedido status, FormaPagamento formaPagamento, BigDecimal total,
                                   Instant retiradaAte, String fusoIana, Instant criadoEm) {
    static PedidoResumoResponse from(Pedido p) {
        return new PedidoResumoResponse(p.id(), p.clienteId(), p.estabelecimentoId(), p.estabelecimentoNome(), p.status(),
                p.formaPagamento(), p.total(), Datas.instante(p.retiradaAte()), p.fusoIana(), Datas.instante(p.criadoEm()));
    }
}
