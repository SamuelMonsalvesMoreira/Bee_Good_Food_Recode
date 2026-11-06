package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categorias")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Categorias (administração)", description = "Categorias globais; somente administrador altera")
public class AdminCategoriaController {
    private final CategoriaService service;

    public AdminCategoriaController(CategoriaService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar todas as categorias, inclusive inativas")
    public PaginaResponse<CategoriaAdminResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.listarTodas(pagina, tamanho);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar categoria", description = "Slug gerado a partir do nome. Nome normalizado e slug são únicos (409).")
    public CategoriaAdminResponse criar(@Valid @RequestBody CategoriaAdminRequest request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar ou desativar categoria", description = "Não existe exclusão física; desativar preserva o histórico.")
    public CategoriaAdminResponse atualizar(@PathVariable @Min(1) long id, @Valid @RequestBody CategoriaAdminRequest request) {
        return service.atualizar(id, request);
    }
}
