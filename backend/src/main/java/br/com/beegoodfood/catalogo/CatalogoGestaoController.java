package br.com.beegoodfood.catalogo;

import br.com.beegoodfood.catalogo.CatalogoGestaoService.Acao;
import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.Usuarios;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('RESPONSAVEL')")
@Tag(name = "Gestão do catálogo", description = "Produtos e ofertas do estabelecimento do responsável autenticado")
public class CatalogoGestaoController {
    private final CatalogoGestaoService service;

    public CatalogoGestaoController(CatalogoGestaoService service) { this.service = service; }

    @PostMapping("/estabelecimentos/{estabelecimentoId}/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar produto", description = "Categoria precisa estar ativa.")
    public ProdutoResponse criarProduto(@PathVariable @Min(1) long estabelecimentoId, @Valid @RequestBody ProdutoRequest request,
                                        Authentication auth) {
        return service.criarProduto(estabelecimentoId, Usuarios.id(auth), request);
    }

    @GetMapping("/estabelecimentos/{estabelecimentoId}/produtos")
    @Operation(summary = "Listar produtos do estabelecimento, inclusive desativados")
    public PaginaResponse<ProdutoResponse> listarProdutos(@PathVariable @Min(1) long estabelecimentoId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho, Authentication auth) {
        return service.listarProdutos(estabelecimentoId, Usuarios.id(auth), pagina, tamanho);
    }

    @GetMapping("/produtos/{id}")
    @Operation(summary = "Consultar produto próprio")
    public ProdutoResponse produto(@PathVariable @Min(1) long id, Authentication auth) {
        return service.produto(id, Usuarios.id(auth));
    }

    @PutMapping("/produtos/{id}")
    @Operation(summary = "Editar ou desativar produto", description = "Pedidos existentes preservam o nome registrado.")
    public ProdutoResponse atualizarProduto(@PathVariable @Min(1) long id, @Valid @RequestBody ProdutoRequest request, Authentication auth) {
        return service.atualizarProduto(id, Usuarios.id(auth), request);
    }

    @PostMapping("/produtos/{produtoId}/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar oferta em rascunho", description = "Exige estabelecimento ativo, inicioVenda < vendaAte <= retiradaAte e fim da venda futuro.")
    public OfertaResponse criarOferta(@PathVariable @Min(1) long produtoId, @Valid @RequestBody OfertaRequest request, Authentication auth) {
        return service.criarOferta(produtoId, Usuarios.id(auth), request);
    }

    @GetMapping("/produtos/{produtoId}/ofertas")
    @Operation(summary = "Listar ofertas do produto (mais recentes primeiro)")
    public PaginaResponse<OfertaResponse> listarOfertas(@PathVariable @Min(1) long produtoId,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho, Authentication auth) {
        return service.listarOfertas(produtoId, Usuarios.id(auth), pagina, tamanho);
    }

    @GetMapping("/gestao/ofertas/{id}")
    @Operation(summary = "Consultar oferta própria em qualquer estado")
    public OfertaResponse oferta(@PathVariable @Min(1) long id, Authentication auth) {
        return service.oferta(id, Usuarios.id(auth));
    }

    @PutMapping("/ofertas/{id}")
    @Operation(summary = "Editar oferta em rascunho", description = "Após a publicação, preço e prazos são imutáveis.")
    public OfertaResponse editar(@PathVariable @Min(1) long id, @Valid @RequestBody OfertaRequest request, Authentication auth) {
        return service.editarRascunho(id, Usuarios.id(auth), request);
    }

    @PostMapping("/ofertas/{id}/publicar")
    @Operation(summary = "Publicar (RASCUNHO → PUBLICADA)", description = "No máximo uma oferta publicada ou pausada por produto (409).")
    public OfertaResponse publicar(@PathVariable @Min(1) long id, Authentication auth) {
        return service.alterarEstado(id, Usuarios.id(auth), Acao.PUBLICAR);
    }

    @PostMapping("/ofertas/{id}/pausar")
    @Operation(summary = "Pausar (PUBLICADA → PAUSADA)")
    public OfertaResponse pausar(@PathVariable @Min(1) long id, Authentication auth) {
        return service.alterarEstado(id, Usuarios.id(auth), Acao.PAUSAR);
    }

    @PostMapping("/ofertas/{id}/retomar")
    @Operation(summary = "Retomar (PAUSADA → PUBLICADA)")
    public OfertaResponse retomar(@PathVariable @Min(1) long id, Authentication auth) {
        return service.alterarEstado(id, Usuarios.id(auth), Acao.RETOMAR);
    }

    @PostMapping("/ofertas/{id}/encerrar")
    @Operation(summary = "Encerrar (estado final)")
    public OfertaResponse encerrar(@PathVariable @Min(1) long id, Authentication auth) {
        return service.alterarEstado(id, Usuarios.id(auth), Acao.ENCERRAR);
    }

    public record AjusteEstoqueRequest(@NotNull @Min(-100000) @Max(100000) Integer ajuste) { }

    @PostMapping("/ofertas/{id}/estoque")
    @Operation(summary = "Ajustar estoque (relativo)", description = "Soma o ajuste ao saldo atual sem sobrescrever vendas concorrentes. Nunca fica negativo.")
    public OfertaResponse ajustarEstoque(@PathVariable @Min(1) long id, @Valid @RequestBody AjusteEstoqueRequest request, Authentication auth) {
        if (request.ajuste() == 0) {
            throw br.com.beegoodfood.comum.RegraNegocioException.invalido("ajuste", "Informe um ajuste diferente de zero.");
        }
        return service.ajustarEstoque(id, Usuarios.id(auth), request.ajuste());
    }
}
