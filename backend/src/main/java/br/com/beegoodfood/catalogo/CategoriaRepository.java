package br.com.beegoodfood.catalogo;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

// Nao expoe operacoes de escrita nesta etapa.
interface CategoriaRepository extends Repository<Categoria, Long> {
    Page<Categoria> findByAtivoTrue(Pageable pageable);
    Optional<Categoria> findByIdAndAtivoTrue(Long id);
}
