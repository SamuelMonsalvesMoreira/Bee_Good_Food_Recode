package br.com.beegoodfood.pedidos;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Snapshot imutavel do item no momento da compra (CA11).
@Entity
@Table(name = "item_pedido")
class ItemPedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "oferta_id", nullable = false, updatable = false) private Long ofertaId;
    @Column(name = "produto_nome_snapshot", nullable = false, length = 160, updatable = false) private String produtoNome;
    @Column(name = "preco_original", nullable = false, precision = 10, scale = 2, updatable = false) private BigDecimal precoOriginal;
    @Column(name = "preco_oferta", nullable = false, precision = 10, scale = 2, updatable = false) private BigDecimal precoOferta;
    @Column(nullable = false, updatable = false) private int quantidade;
    @Column(nullable = false, precision = 10, scale = 2, updatable = false) private BigDecimal subtotal;

    protected ItemPedido() { }

    ItemPedido(long ofertaId, String produtoNome, BigDecimal precoOriginal, BigDecimal precoOferta, int quantidade) {
        this.ofertaId = ofertaId;
        this.produtoNome = produtoNome;
        this.precoOriginal = precoOriginal;
        this.precoOferta = precoOferta;
        this.quantidade = quantidade;
        this.subtotal = precoOferta.multiply(BigDecimal.valueOf(quantidade));
    }

    long ofertaId() { return ofertaId; }
    String produtoNome() { return produtoNome; }
    BigDecimal precoOriginal() { return precoOriginal; }
    BigDecimal precoOferta() { return precoOferta; }
    int quantidade() { return quantidade; }
    BigDecimal subtotal() { return subtotal; }
}
