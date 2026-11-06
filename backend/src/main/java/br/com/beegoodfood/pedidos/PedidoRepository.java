package br.com.beegoodfood.pedidos;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByClienteIdAndIdempotencyKey(Long clienteId, String idempotencyKey);

    // Leitura com bloqueio: enxerga pedido confirmado por transacao concorrente com a mesma chave.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.clienteId = :cliente and p.idempotencyKey = :chave")
    Optional<Pedido> bloquearPorChave(@Param("cliente") long clienteId, @Param("chave") String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquear(@Param("id") long id);

    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);
    Page<Pedido> findByClienteIdAndStatus(Long clienteId, String status, Pageable pageable);
    Page<Pedido> findByEstabelecimentoId(Long estabelecimentoId, Pageable pageable);
    Page<Pedido> findByEstabelecimentoIdAndStatus(Long estabelecimentoId, String status, Pageable pageable);
}
