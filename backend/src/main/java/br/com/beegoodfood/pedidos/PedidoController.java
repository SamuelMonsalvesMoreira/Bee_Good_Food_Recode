package br.com.beegoodfood.pedidos;

import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.Usuarios;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pedidos", description = "Pedido para retirada, acompanhamento, status e cancelamento")
public class PedidoController {
    private final PedidoService service;

    public PedidoController(PedidoService service) { this.service = service; }

    @PostMapping("/pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Criar pedido (idempotente)",
            description = "Cabeçalho Idempotency-Key obrigatório (8 a 120 caracteres: letras, números, '-' e '_'). "
                    + "Repetir a mesma chave com o mesmo conteúdo devolve o pedido original (200) sem nova baixa de estoque; "
                    + "com conteúdo diferente, 409. Preços e total são sempre calculados pelo servidor.")
    public ResponseEntity<PedidoResponse> criar(
            @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9_-]{8,120}",
                    message = "Use de 8 a 120 caracteres: letras, números, '-' ou '_'.") String chave,
            @Valid @RequestBody PedidoRequest request, Authentication auth) {
        var resultado = service.criar(Usuarios.id(auth), chave, request);
        return ResponseEntity.status(resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK)
                .header("Idempotent-Replayed", Boolean.toString(!resultado.criado())).body(resultado.pedido());
    }

    @GetMapping("/pedidos")
    @Operation(summary = "Histórico de pedidos do cliente (mais recentes primeiro)")
    public PaginaResponse<PedidoResumoResponse> meus(@RequestParam(required = false) StatusPedido status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho, Authentication auth) {
        return service.doCliente(Usuarios.id(auth), status, pagina, tamanho);
    }

    @GetMapping("/pedidos/{id}")
    @Operation(summary = "Detalhe do pedido", description = "Cliente do pedido ou responsável do estabelecimento; terceiros recebem 404.")
    public PedidoResponse detalhe(@PathVariable @Min(1) long id, Authentication auth) {
        return service.detalhe(id, Usuarios.id(auth));
    }

    @GetMapping("/estabelecimentos/{estabelecimentoId}/pedidos")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    @Operation(summary = "Pedidos do estabelecimento (mais antigos primeiro)", description = "Filtro opcional por status.")
    public PaginaResponse<PedidoResumoResponse> doEstabelecimento(@PathVariable @Min(1) long estabelecimentoId,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho, Authentication auth) {
        return service.doEstabelecimento(estabelecimentoId, Usuarios.id(auth), status, pagina, tamanho);
    }

    public record StatusRequest(@NotNull StatusPedido status) { }

    @PostMapping("/pedidos/{id}/status")
    @Operation(summary = "Avançar status (responsável)",
            description = "PENDENTE → CONFIRMADO → EM_PREPARO → PRONTO_PARA_RETIRADA → CONCLUIDO, uma etapa por vez.")
    public PedidoResponse avancar(@PathVariable @Min(1) long id, @Valid @RequestBody StatusRequest request, Authentication auth) {
        return service.avancar(id, Usuarios.id(auth), request.status());
    }

    public record CancelamentoRequest(@Size(max = 500) String motivo) {
        public CancelamentoRequest { motivo = motivo == null || motivo.isBlank() ? null : motivo.strip(); }
    }

    @PostMapping("/pedidos/{id}/cancelar")
    @Operation(summary = "Cancelar pedido",
            description = "Cliente: somente PENDENTE. Responsável: qualquer estado não final, com motivo. "
                    + "Repetir o cancelamento não devolve estoque novamente.")
    public PedidoResponse cancelar(@PathVariable @Min(1) long id, @Valid @RequestBody(required = false) CancelamentoRequest request,
                                   Authentication auth) {
        return service.cancelar(id, Usuarios.id(auth), request == null ? null : request.motivo());
    }
}
