package br.com.beegoodfood.pedidos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Somente oferta, quantidade e forma de pagamento. Valores monetarios enviados pelo frontend (carrinho)
 * sao ignorados e recalculados no servidor (CA08); qualquer outro campo desconhecido e rejeitado.
 */
@JsonIgnoreProperties({"total", "subtotal"})
public record PedidoRequest(
        @NotNull FormaPagamento formaPagamento,
        @NotNull @Size(min = 1, max = 20) List<@NotNull @Valid Item> itens) {

    @JsonIgnoreProperties({"precoOriginal", "precoOferta", "preco", "subtotal", "produtoNome"})
    public record Item(@NotNull @Min(1) Long ofertaId, @NotNull @Min(1) @Max(100) Integer quantidade) { }
}
