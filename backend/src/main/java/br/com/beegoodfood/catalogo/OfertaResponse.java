package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.Datas;
import java.math.BigDecimal;
import java.time.Instant;

/** Visao de gestao do proprietario. */
public record OfertaResponse(long id, long produtoId, BigDecimal precoOriginal, BigDecimal precoOferta,
                             int estoqueDisponivel, Instant inicioVenda, Instant vendaAte, Instant retiradaAte,
                             StatusOferta status, Instant publicadaEm, Instant encerradaEm) {
    static OfertaResponse from(Oferta o) {
        return new OfertaResponse(o.id(), o.produtoId(), o.precoOriginal(), o.precoOferta(), o.estoqueDisponivel(),
                Datas.instante(o.inicioVenda()), Datas.instante(o.vendaAte()), Datas.instante(o.retiradaAte()),
                o.status(), Datas.instante(o.publicadaEm()), Datas.instante(o.encerradaEm()));
    }
}
