package br.com.beegoodfood.catalogo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "produto")
class Produto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "estabelecimento_id", nullable = false, updatable = false) private Long estabelecimentoId;
    @Column(name = "categoria_id", nullable = false) private Long categoriaId;
    @Column(nullable = false, length = 160) private String nome;
    @Column(length = 500) private String descricao;
    @Column(nullable = false) private boolean ativo;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm;

    protected Produto() { }

    Produto(long estabelecimentoId, ProdutoRequest request, LocalDateTime agora) {
        this.estabelecimentoId = estabelecimentoId;
        this.ativo = true;
        this.criadoEm = agora;
        aplicar(request);
    }

    void aplicar(ProdutoRequest request) {
        this.categoriaId = request.categoriaId();
        this.nome = request.nome();
        this.descricao = request.descricao();
        if (request.ativo() != null) { this.ativo = request.ativo(); }
    }

    Long id() { return id; }
    long estabelecimentoId() { return estabelecimentoId; }
    long categoriaId() { return categoriaId; }
    String nome() { return nome; }
    String descricao() { return descricao; }
    boolean ativo() { return ativo; }
}
