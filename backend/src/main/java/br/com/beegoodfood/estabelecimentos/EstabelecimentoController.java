package br.com.beegoodfood.estabelecimentos;

import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.Usuarios;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estabelecimentos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Estabelecimentos", description = "Solicitação e manutenção do estabelecimento pelo proprietário")
public class EstabelecimentoController {
    private final EstabelecimentoService service;

    public EstabelecimentoController(EstabelecimentoService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Solicitar cadastro de estabelecimento",
            description = "Nasce em PENDENTE_ANALISE, vinculado ao usuário autenticado. Fuso IANA padrão: America/Sao_Paulo.")
    public EstabelecimentoResponse solicitar(@Valid @RequestBody EstabelecimentoRequest request, Authentication auth) {
        return service.solicitar(Usuarios.id(auth), request);
    }

    @GetMapping("/meus")
    @Operation(summary = "Listar meus estabelecimentos")
    public PaginaResponse<EstabelecimentoResponse> meus(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho, Authentication auth) {
        return service.meus(Usuarios.id(auth), pagina, tamanho);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar estabelecimento (proprietário ou administrador)")
    public EstabelecimentoResponse detalhe(@PathVariable @Min(1) long id, Authentication auth) {
        return service.detalhe(id, Usuarios.id(auth), Usuarios.temPapel(auth, "ADMINISTRADOR"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar estabelecimento",
            description = "Rejeitado volta para análise ao ser corrigido. Ativo com novo endereço ou fuso volta para análise. Suspenso não pode ser editado.")
    public EstabelecimentoResponse atualizar(@PathVariable @Min(1) long id, @Valid @RequestBody EstabelecimentoRequest request,
                                             Authentication auth) {
        return service.atualizar(id, Usuarios.id(auth), request);
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico de moderação (proprietário ou administrador)")
    public List<HistoricoEstabelecimentoResponse> historico(@PathVariable @Min(1) long id, Authentication auth) {
        return service.historico(id, Usuarios.id(auth), Usuarios.temPapel(auth, "ADMINISTRADOR"));
    }
}
