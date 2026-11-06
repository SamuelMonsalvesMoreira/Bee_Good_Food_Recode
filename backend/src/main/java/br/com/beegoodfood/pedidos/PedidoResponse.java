package br.com.beegoodfood.pedidos;

import br.com.beegoodfood.comum.Datas;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Datas em UTC; apresentar no fusoIana registrado no pedido. */
public record PedidoResponse(long id, long clienteId, long estabelecimentoId, StatusPedido status,
                             FormaPagamento formaPagamento, BigDecimal total, Estabelecimento estabelecimento,
                             Instant retiradaAte, Instant concluidoEm, Instant criadoEm,
                             List<Item> itens, List<Historico> historico) {
    public record Estabelecimento(String nome, String logradouro, String numero, String complemento, String bairro,
                                  String cidade, String estado, String cep, String fusoIana) { }

    public record Item(long ofertaId, String produtoNome, BigDecimal precoOriginal, BigDecimal precoOferta,
                       int quantidade, BigDecimal subtotal) { }

    public record Historico(String statusAnterior, String statusNovo, Long atorId, String motivo, Instant criadoEm) { }

    static PedidoResponse from(Pedido p) {
        return new PedidoResponse(p.id(), p.clienteId(), p.estabelecimentoId(), p.status(), p.formaPagamento(), p.total(),
                new Estabelecimento(p.estabelecimentoNome(), p.logradouro(), p.numero(), p.complemento(), p.bairro(),
                        p.cidade(), p.estado(), p.cep(), p.fusoIana()),
                Datas.instante(p.retiradaAte()), Datas.instante(p.concluidoEm()), Datas.instante(p.criadoEm()),
                p.itens().stream().map(i -> new Item(i.ofertaId(), i.produtoNome(), i.precoOriginal(), i.precoOferta(),
                        i.quantidade(), i.subtotal())).toList(),
                p.historico().stream().map(h -> new Historico(h.statusAnterior(), h.statusNovo(), h.atorId(), h.motivo(),
                        Datas.instante(h.criadoEm()))).toList());
    }
}
