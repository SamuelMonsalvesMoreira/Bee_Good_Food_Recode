package br.com.beegoodfood.estabelecimentos;

import br.com.beegoodfood.comum.Datas;
import java.time.Instant;

public record HistoricoEstabelecimentoResponse(long id, String statusAnterior, String statusNovo, Long atorId,
                                               String motivo, Instant criadoEm) {
    static HistoricoEstabelecimentoResponse from(HistoricoStatusEstabelecimento h) {
        return new HistoricoEstabelecimentoResponse(h.id(), h.statusAnterior(), h.statusNovo(), h.atorId(),
                h.motivo(), Datas.instante(h.criadoEm()));
    }
}
