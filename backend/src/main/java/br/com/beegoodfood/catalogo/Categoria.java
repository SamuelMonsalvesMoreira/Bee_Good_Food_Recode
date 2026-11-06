package br.com.beegoodfood.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.text.Normalizer;
import java.util.Locale;

// A estrutura e criada pelo Flyway, nao pelo Hibernate. Unicidade garantida pelas constraints do MySQL.
@Entity
@Table(name = "categoria")
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "nome_normalizado", nullable = false, length = 100)
    private String nomeNormalizado;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false)
    private boolean ativo;

    protected Categoria() { }

    Categoria(String nome) {
        renomear(nome);
        this.ativo = true;
    }

    void renomear(String nome) {
        this.nome = nome;
        this.nomeNormalizado = normalizar(nome);
        this.slug = slug(nome);
    }

    void definirAtivo(boolean ativo) { this.ativo = ativo; }

    static String normalizar(String nome) { return nome.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT); }

    /** Ex.: "Pães e Bolos" -> "paes-e-bolos". */
    static String slug(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.length() > 120 ? slug.substring(0, 120).replaceAll("-+$", "") : slug;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getSlug() { return slug; }
    public boolean isAtivo() { return ativo; }
}
