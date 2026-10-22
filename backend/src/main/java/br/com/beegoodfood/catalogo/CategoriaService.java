package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.RecursoNaoEncontradoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    public PaginaResponse<CategoriaResponse> listar(int pagina, int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending().and(Sort.by("id")));
        return PaginaResponse.from(repository.findByAtivoTrue(pageable).map(CategoriaResponse::from));
    }

    public CategoriaResponse buscar(long id) {
        return repository.findByIdAndAtivoTrue(id).map(CategoriaResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }
}
