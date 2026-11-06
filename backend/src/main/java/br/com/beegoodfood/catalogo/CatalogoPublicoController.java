package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.comum.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ofertas")
@Tag(name = "Catálogo público", description = "Ofertas compráveis agora, sem autenticação")
public class CatalogoPublicoController {
    private final CatalogoPublicoService service;

    public CatalogoPublicoController(CatalogoPublicoService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Buscar ofertas compráveis",
            description = "Filtros combinados (E): texto (produto, descrição ou estabelecimento), categoria, cidade, bairro e estabelecimento. "
                    + "Cidade e bairro ignoram maiúsculas e acentos. Ordenação: fim da venda e ID. Tamanho padrão 20 (1 a 100).")
    public PaginaResponse<OfertaPublicaResponse> buscar(
            @RequestParam(required = false) @Size(max = 100) String texto,
            @RequestParam(required = false) @Min(1) Long categoriaId,
            @RequestParam(required = false) @Size(max = 100) String cidade,
            @RequestParam(required = false) @Size(max = 100) String bairro,
            @RequestParam(required = false) @Min(1) Long estabelecimentoId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        var filtros = new CatalogoPublicoService.Filtros(limpar(texto), categoriaId, limpar(cidade), limpar(bairro), estabelecimentoId);
        return service.buscar(filtros, pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe de oferta comprável", description = "Oferta indisponível, expirada ou inexistente retorna 404.")
    public OfertaPublicaResponse detalhe(@PathVariable @Min(1) long id) {
        return service.detalhe(id);
    }

    private static String limpar(String valor) { return valor == null || valor.isBlank() ? null : valor.strip(); }
}
