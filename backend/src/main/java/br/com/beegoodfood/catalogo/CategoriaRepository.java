package br.com.beegoodfood.catalogo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

// Escrita somente pelo servico administrativo.
interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Page<Categoria> findByAtivoTrue(Pageable pageable);
    Optional<Categoria> findByIdAndAtivoTrue(Long id);
}
