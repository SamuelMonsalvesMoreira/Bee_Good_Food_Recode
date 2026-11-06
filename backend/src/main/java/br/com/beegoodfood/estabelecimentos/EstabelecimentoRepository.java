package br.com.beegoodfood.estabelecimentos;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {
    Page<Estabelecimento> findByResponsavelId(Long responsavelId, Pageable pageable);
    Page<Estabelecimento> findByStatus(String status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estabelecimento e where e.id = :id")
    Optional<Estabelecimento> bloquear(@Param("id") long id);

    // Leitura compartilhada: uma moderacao concorrente aguarda a criacao do pedido terminar.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select e from Estabelecimento e where e.id = :id")
    Optional<Estabelecimento> bloquearLeitura(@Param("id") long id);
}
