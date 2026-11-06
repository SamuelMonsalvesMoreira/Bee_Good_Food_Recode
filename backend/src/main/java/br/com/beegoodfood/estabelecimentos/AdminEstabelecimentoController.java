package br.com.beegoodfood.estabelecimentos;

import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.Usuarios;
import br.com.beegoodfood.estabelecimentos.EstabelecimentoService.Decisao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/estabelecimentos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Moderação", description = "Aprovação, rejeição, suspensão e reativação de estabelecimentos")
public class AdminEstabelecimentoController {
    private final EstabelecimentoService service;

    public AdminEstabelecimentoController(EstabelecimentoService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar estabelecimentos para moderação", description = "Filtro opcional por status.")
    public PaginaResponse<EstabelecimentoResponse> listar(
            @RequestParam(required = false) StatusEstabelecimento status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        return service.listarAdmin(status, pagina, tamanho);
    }

    @PostMapping("/{id}/aprovar")
    @Operation(summary = "Aprovar (PENDENTE_ANALISE → ATIVO)", description = "Concede o papel RESPONSAVEL, preservando CLIENTE.")
    public EstabelecimentoResponse aprovar(@PathVariable @Min(1) long id, @Valid @RequestBody ModeracaoRequest request, Authentication auth) {
        return service.moderar(id, Usuarios.id(auth), Decisao.APROVAR, request.motivo());
    }

    @PostMapping("/{id}/rejeitar")
    @Operation(summary = "Rejeitar (PENDENTE_ANALISE → REJEITADO)")
    public EstabelecimentoResponse rejeitar(@PathVariable @Min(1) long id, @Valid @RequestBody ModeracaoRequest request, Authentication auth) {
        return service.moderar(id, Usuarios.id(auth), Decisao.REJEITAR, request.motivo());
    }

    @PostMapping("/{id}/suspender")
    @Operation(summary = "Suspender (ATIVO → SUSPENSO)", description = "Bloqueia novas ofertas e pedidos; pedidos existentes continuam tratáveis.")
    public EstabelecimentoResponse suspender(@PathVariable @Min(1) long id, @Valid @RequestBody ModeracaoRequest request, Authentication auth) {
        return service.moderar(id, Usuarios.id(auth), Decisao.SUSPENDER, request.motivo());
    }

    @PostMapping("/{id}/reativar")
    @Operation(summary = "Reativar (SUSPENSO → ATIVO)")
    public EstabelecimentoResponse reativar(@PathVariable @Min(1) long id, @Valid @RequestBody ModeracaoRequest request, Authentication auth) {
        return service.moderar(id, Usuarios.id(auth), Decisao.REATIVAR, request.motivo());
    }
}
