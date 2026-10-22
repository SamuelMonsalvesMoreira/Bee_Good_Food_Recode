package br.com.beegoodfood.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String nome;
    @Column(nullable = false, length = 320) private String email;
    @Column(name = "email_normalizado", nullable = false, length = 320) private String emailNormalizado;
    @Column(name = "senha_hash", nullable = false, length = 255) private String senhaHash;
    @Column(name = "maioridade_confirmada_em", nullable = false) private LocalDateTime maioridadeConfirmadaEm;
    @Column(nullable = false) private boolean ativo;

    @ElementCollection
    @CollectionTable(name = "usuario_papel", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "papel_codigo", length = 30, nullable = false)
    private Set<String> papeis = new HashSet<>();

    protected Usuario() { }

    Usuario(CadastroRequest request, String emailNormalizado, String hash, LocalDateTime agora) {
        this.nome = request.nome();
        this.email = request.email();
        this.emailNormalizado = emailNormalizado;
        this.senhaHash = hash;
        this.maioridadeConfirmadaEm = agora;
        this.ativo = true;
        this.papeis.add("CLIENTE");
    }

    Long id() { return id; }
    String nome() { return nome; }
    String email() { return email; }
    String senhaHash() { return senhaHash; }
    boolean ativo() { return ativo; }
    Set<String> papeis() { return papeis; }
}
