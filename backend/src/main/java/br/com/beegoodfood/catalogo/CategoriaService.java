package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.RecursoNaoEncontradoException;
import br.com.beegoodfood.comum.RegraNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {
    private static final String NAO_ENCONTRADA = "Categoria não encontrada.";
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
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADA));
    }

    // ---- Administracao (CA20) ----

    public PaginaResponse<CategoriaAdminResponse> listarTodas(int pagina, int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by("nome").ascending().and(Sort.by("id")));
        return PaginaResponse.from(repository.findAll(pageable).map(CategoriaAdminResponse::from));
    }

    @Transactional
    public CategoriaAdminResponse criar(CategoriaAdminRequest request) {
        var categoria = new Categoria(request.nome());
        validarSlug(categoria);
        if (Boolean.FALSE.equals(request.ativo())) { categoria.definirAtivo(false); }
        // Duplicidade (inclusive concorrente) vira 409 pela constraint unica do MySQL.
        return CategoriaAdminResponse.from(repository.saveAndFlush(categoria));
    }

    @Transactional
    public CategoriaAdminResponse atualizar(long id, CategoriaAdminRequest request) {
        var categoria = repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADA));
        categoria.renomear(request.nome());
        validarSlug(categoria);
        // Desativar preserva produtos e pedidos; as ofertas deixam de ser compraveis (filtro do catalogo).
        if (request.ativo() != null) { categoria.definirAtivo(request.ativo()); }
        return CategoriaAdminResponse.from(repository.saveAndFlush(categoria));
    }

    /** Remove de vez a categoria sem produtos. A FK do MySQL recusa se houver produto, inclusive concorrente (DEC-038). */
    @Transactional
    public void remover(long id) {
        repository.delete(repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADA)));
        try {
            repository.flush();
        } catch (DataIntegrityViolationException error) {
            throw RegraNegocioException.conflito("Esta categoria tem produtos. Desative a categoria em vez de remover.");
        }
    }

    /** Contrato interno: categoria ativa para vincular produtos. */
    boolean ativa(long id) { return repository.findByIdAndAtivoTrue(id).isPresent(); }

    private void validarSlug(Categoria categoria) {
        if (categoria.getSlug().isEmpty()) {
            throw RegraNegocioException.invalido("nome", "Use ao menos uma letra ou número no nome.");
        }
    }
}
