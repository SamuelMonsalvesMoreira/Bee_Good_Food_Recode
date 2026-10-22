package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias", description = "Consulta de categorias ativas do catálogo")
public class CategoriaController {
    private final CategoriaService service;

    public CategoriaController(CategoriaService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar categorias ativas", description = "Página começa em zero; tamanho entre 1 e 100. Ordenação por nome e ID.")
    public PaginaResponse<CategoriaResponse> listar(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.listar(pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar categoria ativa pelo ID")
    public CategoriaResponse buscar(@PathVariable @Min(1) long id) {
        return service.buscar(id);
    }
}
