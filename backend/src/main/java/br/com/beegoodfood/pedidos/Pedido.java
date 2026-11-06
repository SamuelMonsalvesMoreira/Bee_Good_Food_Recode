package br.com.beegoodfood.pedidos;

import br.com.beegoodfood.estabelecimentos.EstabelecimentoResumo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Nome, endereco, fuso e prazo de retirada copiados do estabelecimento na criacao (DEC-010, CA24).
@Entity
@Table(name = "pedido")
class Pedido {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "cliente_id", nullable = false, updatable = false) private Long clienteId;
    @Column(name = "estabelecimento_id", nullable = false, updatable = false) private Long estabelecimentoId;
    @Column(nullable = false, length = 24) private String status;
    @Column(name = "forma_pagamento_declarada", nullable = false, length = 24, updatable = false) private String formaPagamento;
    @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false) private String idempotencyKey;
    @Column(name = "payload_hash", nullable = false, columnDefinition = "char(64)", updatable = false) private String payloadHash;
    @Column(nullable = false, precision = 10, scale = 2, updatable = false) private BigDecimal total;
    @Column(name = "estabelecimento_nome_snapshot", nullable = false, length = 160, updatable = false) private String estabelecimentoNome;
    @Column(name = "logradouro_snapshot", nullable = false, length = 180, updatable = false) private String logradouro;
    @Column(name = "numero_snapshot", nullable = false, length = 30, updatable = false) private String numero;
    @Column(name = "complemento_snapshot", length = 120, updatable = false) private String complemento;
    @Column(name = "bairro_snapshot", nullable = false, length = 100, updatable = false) private String bairro;
    @Column(name = "cidade_snapshot", nullable = false, length = 100, updatable = false) private String cidade;
    @Column(name = "estado_snapshot", nullable = false, columnDefinition = "char(2)", updatable = false) private String estado;
    @Column(name = "cep_snapshot", nullable = false, length = 12, updatable = false) private String cep;
    @Column(name = "fuso_iana_snapshot", nullable = false, length = 64, updatable = false) private String fusoIana;
    @Column(name = "retirada_ate", nullable = false, updatable = false) private LocalDateTime retiradaAte;
    @Column(name = "concluido_em") private LocalDateTime concluidoEm;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    @OrderBy("id")
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, updatable = false)
    @OrderBy("criadoEm, id")
    private List<HistoricoStatusPedido> historico = new ArrayList<>();

    protected Pedido() { }

    Pedido(long clienteId, EstabelecimentoResumo e, FormaPagamento forma, String chave, String hash,
           List<ItemPedido> itens, LocalDateTime retiradaAte, LocalDateTime agora) {
        this.clienteId = clienteId;
        this.estabelecimentoId = e.id();
        this.status = StatusPedido.PENDENTE.name();
        this.formaPagamento = forma.name();
        this.idempotencyKey = chave;
        this.payloadHash = hash;
        this.estabelecimentoNome = e.nome();
        this.logradouro = e.logradouro();
        this.numero = e.numero();
        this.complemento = e.complemento();
        this.bairro = e.bairro();
        this.cidade = e.cidade();
        this.estado = e.estado();
        this.cep = e.cep();
        this.fusoIana = e.fusoIana();
        this.retiradaAte = retiradaAte;
        this.criadoEm = agora;
        this.itens.addAll(itens);
        // Total sempre calculado no servidor a partir dos itens (DEC-009).
        this.total = itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        this.historico.add(new HistoricoStatusPedido(clienteId, null, StatusPedido.PENDENTE, null, agora));
    }

    void transicionar(StatusPedido novo, long atorId, String motivo, LocalDateTime agora) {
        var anterior = status();
        this.status = novo.name();
        // concluido_em e registrado uma unica vez (CONCLUIDO e final).
        if (novo == StatusPedido.CONCLUIDO && concluidoEm == null) { this.concluidoEm = agora; }
        this.historico.add(new HistoricoStatusPedido(atorId, anterior, novo, motivo, agora));
    }

    Long id() { return id; }
    long clienteId() { return clienteId; }
    long estabelecimentoId() { return estabelecimentoId; }
    StatusPedido status() { return StatusPedido.valueOf(status); }
    FormaPagamento formaPagamento() { return FormaPagamento.valueOf(formaPagamento); }
    String payloadHash() { return payloadHash; }
    BigDecimal total() { return total; }
    String estabelecimentoNome() { return estabelecimentoNome; }
    String logradouro() { return logradouro; }
    String numero() { return numero; }
    String complemento() { return complemento; }
    String bairro() { return bairro; }
    String cidade() { return cidade; }
    String estado() { return estado; }
    String cep() { return cep; }
    String fusoIana() { return fusoIana; }
    LocalDateTime retiradaAte() { return retiradaAte; }
    LocalDateTime concluidoEm() { return concluidoEm; }
    LocalDateTime criadoEm() { return criadoEm; }
    List<ItemPedido> itens() { return itens; }
    List<HistoricoStatusPedido> historico() { return historico; }
}
