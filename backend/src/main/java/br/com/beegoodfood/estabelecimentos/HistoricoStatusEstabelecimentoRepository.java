package br.com.beegoodfood.estabelecimentos;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface HistoricoStatusEstabelecimentoRepository extends JpaRepository<HistoricoStatusEstabelecimento, Long> {
    List<HistoricoStatusEstabelecimento> findByEstabelecimentoIdOrderByCriadoEmAscIdAsc(Long estabelecimentoId);
}
