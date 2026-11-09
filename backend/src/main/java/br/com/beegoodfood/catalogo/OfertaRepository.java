package br.com.beegoodfood.catalogo;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OfertaRepository extends JpaRepository<Oferta, Long> {
    Page<Oferta> findByProdutoId(Long produtoId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oferta o where o.id = :id")
    Optional<Oferta> bloquear(@Param("id") long id);

    // Bloqueio em ordem de ID, como na venda, para nao criar deadlock com pedidos simultaneos.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Oferta o where o.produtoId = :produtoId order by o.id")
    List<Oferta> bloquearDoProduto(@Param("produtoId") long produtoId);

    // Ajuste relativo e atomico: nao sobrescreve baixas concorrentes nem deixa o saldo negativo.
    // CAST evita aritmetica UNSIGNED com ajuste negativo.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "UPDATE oferta SET estoque_disponivel = CAST(estoque_disponivel AS SIGNED) + :ajuste "
            + "WHERE id = :id AND status <> 'ENCERRADA' AND CAST(estoque_disponivel AS SIGNED) + :ajuste BETWEEN 0 AND 100000")
    int ajustarEstoque(@Param("id") long id, @Param("ajuste") int ajuste);
}
