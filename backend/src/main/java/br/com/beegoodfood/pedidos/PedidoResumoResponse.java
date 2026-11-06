package br.com.beegoodfood.pedidos;

import br.com.beegoodfood.comum.Datas;
import java.math.BigDecimal;
import java.time.Instant;

/** Linha de listagem (historico do cliente ou fila do estabelecimento), com a economia dos itens. */
public record PedidoResumoResponse(long id, long clienteId, long estabelecimentoId, String estabelecimentoNome,
                                   StatusPedido status, FormaPagamento formaPagamento, BigDecimal total,
                                   BigDecimal economia, Instant retiradaAte, String fusoIana, Instant criadoEm) {
    static PedidoResumoResponse from(Pedido p) {
        // Economia pelos snapshots dos itens: diferenca entre preco original e preco da oferta registrados.
        var economia = p.itens().stream()
                .map(i -> i.precoOriginal().subtract(i.precoOferta()).multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        return new PedidoResumoResponse(p.id(), p.clienteId(), p.estabelecimentoId(), p.estabelecimentoNome(), p.status(),
                p.formaPagamento(), p.total(), economia, Datas.instante(p.retiradaAte()), p.fusoIana(), Datas.instante(p.criadoEm()));
    }
}
