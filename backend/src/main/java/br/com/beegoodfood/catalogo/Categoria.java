package br.com.beegoodfood.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Mapeamento de leitura. A estrutura e criada pelo Flyway, nao pelo Hibernate.
@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false)
    private boolean ativo;

    protected Categoria() { }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getSlug() { return slug; }
}
