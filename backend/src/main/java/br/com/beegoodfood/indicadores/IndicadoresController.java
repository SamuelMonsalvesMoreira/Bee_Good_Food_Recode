package br.com.beegoodfood.indicadores;

import br.com.beegoodfood.comum.Usuarios;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estabelecimentos/{estabelecimentoId}/indicadores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('RESPONSAVEL')")
@Tag(name = "Indicadores", description = "Vendas e aproveitamento de um estabelecimento do responsável")
public class IndicadoresController {
    private final IndicadoresService service;

    public IndicadoresController(IndicadoresService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Indicadores do período [inicio, fim)",
            description = "inicio e fim em data e hora local do estabelecimento (AAAA-MM-DDTHH:MM). fim omitido = agora; "
                    + "fim não pode estar no futuro. Volume usa concluidoEm; taxas usam a coorte de pedidos criados no período. "
                    + "Taxa null = N/A (sem pedidos no período).")
    public IndicadoresResponse calcular(@PathVariable @Min(1) long estabelecimentoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Authentication auth) {
        return service.calcular(estabelecimentoId, Usuarios.id(auth), inicio, fim);
    }
}
