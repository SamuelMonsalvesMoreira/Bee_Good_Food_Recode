package br.com.beegoodfood.catalogo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProdutoRepository extends JpaRepository<Produto, Long> {
    Page<Produto> findByEstabelecimentoId(Long estabelecimentoId, Pageable pageable);
}
