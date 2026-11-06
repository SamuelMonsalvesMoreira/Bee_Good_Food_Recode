package br.com.beegoodfood.catalogo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Datas em UTC. O saldo de estoque so muda por UPDATE atomico (OfertaRepository), nunca por sobrescrita.
@Entity
@Table(name = "oferta")
class Oferta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "produto_id", nullable = false, updatable = false) private Long produtoId;
    @Column(name = "preco_original", nullable = false, precision = 10, scale = 2) private BigDecimal precoOriginal;
    @Column(name = "preco_oferta", nullable = false, precision = 10, scale = 2) private BigDecimal precoOferta;
    @Column(name = "estoque_disponivel", nullable = false) private int estoqueDisponivel;
    @Column(name = "inicio_venda", nullable = false) private LocalDateTime inicioVenda;
    @Column(name = "venda_ate", nullable = false) private LocalDateTime vendaAte;
    @Column(name = "retirada_ate", nullable = false) private LocalDateTime retiradaAte;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "publicada_em") private LocalDateTime publicadaEm;
    @Column(name = "encerrada_em") private LocalDateTime encerradaEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm;

    protected Oferta() { }

    Oferta(long produtoId, OfertaRequest.Valores valores, LocalDateTime agora) {
        this.produtoId = produtoId;
        this.status = StatusOferta.RASCUNHO.name();
        this.criadoEm = agora;
        this.estoqueDisponivel = valores.estoque();
        aplicarCondicoes(valores);
    }

    /** Somente em RASCUNHO: preco e prazos ficam imutaveis apos a publicacao. */
    void editarRascunho(OfertaRequest.Valores valores) {
        aplicarCondicoes(valores);
        this.estoqueDisponivel = valores.estoque();
    }

    private void aplicarCondicoes(OfertaRequest.Valores v) {
        this.precoOriginal = v.precoOriginal();
        this.precoOferta = v.precoOferta();
        this.inicioVenda = v.inicioVenda();
        this.vendaAte = v.vendaAte();
        this.retiradaAte = v.retiradaAte();
    }

    void publicar(LocalDateTime agora) {
        this.status = StatusOferta.PUBLICADA.name();
        // publicadaEm registra somente a primeira publicacao (indicadores).
        if (publicadaEm == null) { publicadaEm = agora; }
    }

    void pausar() { this.status = StatusOferta.PAUSADA.name(); }

    void encerrar(LocalDateTime agora) {
        this.status = StatusOferta.ENCERRADA.name();
        this.encerradaEm = agora;
    }

    Long id() { return id; }
    long produtoId() { return produtoId; }
    BigDecimal precoOriginal() { return precoOriginal; }
    BigDecimal precoOferta() { return precoOferta; }
    int estoqueDisponivel() { return estoqueDisponivel; }
    LocalDateTime inicioVenda() { return inicioVenda; }
    LocalDateTime vendaAte() { return vendaAte; }
    LocalDateTime retiradaAte() { return retiradaAte; }
    StatusOferta status() { return StatusOferta.valueOf(status); }
    LocalDateTime publicadaEm() { return publicadaEm; }
    LocalDateTime encerradaEm() { return encerradaEm; }
}
