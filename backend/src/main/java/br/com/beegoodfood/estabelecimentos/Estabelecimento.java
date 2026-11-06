package br.com.beegoodfood.estabelecimentos;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

// Endereco 1:1 obrigatorio, mapeado como tabela secundaria ligada por estabelecimento_id.
@Entity
@Table(name = "estabelecimento")
@SecondaryTable(name = "endereco_estabelecimento", pkJoinColumns = @PrimaryKeyJoinColumn(name = "estabelecimento_id"))
class Estabelecimento {
    static final String ENDERECO = "endereco_estabelecimento";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "responsavel_id", nullable = false, updatable = false) private Long responsavelId;
    @Column(nullable = false, length = 160) private String nome;
    @Column(length = 500) private String descricao;
    @Column(nullable = false, length = 24) private String status;
    @Column(nullable = false) private boolean ativo;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm;

    @Column(table = ENDERECO, nullable = false, length = 180) private String logradouro;
    @Column(table = ENDERECO, nullable = false, length = 30) private String numero;
    @Column(table = ENDERECO, length = 120) private String complemento;
    @Column(table = ENDERECO, nullable = false, length = 100) private String bairro;
    @Column(table = ENDERECO, nullable = false, length = 100) private String cidade;
    @Column(table = ENDERECO, nullable = false, columnDefinition = "char(2)") private String estado;
    @Column(table = ENDERECO, nullable = false, length = 12) private String cep;
    @Column(table = ENDERECO, name = "fuso_iana", nullable = false, length = 64) private String fusoIana;

    protected Estabelecimento() { }

    Estabelecimento(long responsavelId, EstabelecimentoRequest request, LocalDateTime agora) {
        this.responsavelId = responsavelId;
        this.status = StatusEstabelecimento.PENDENTE_ANALISE.name();
        this.ativo = true;
        this.criadoEm = agora;
        aplicar(request);
    }

    /** Retorna true quando endereco ou fuso mudaram (exige nova analise se ja aprovado). */
    boolean aplicar(EstabelecimentoRequest request) {
        var e = request.endereco();
        boolean enderecoMudou = !Objects.equals(e.logradouro(), logradouro) || !Objects.equals(e.numero(), numero)
                || !Objects.equals(e.complemento(), complemento) || !Objects.equals(e.bairro(), bairro)
                || !Objects.equals(e.cidade(), cidade) || !Objects.equals(e.estado(), estado)
                || !Objects.equals(e.cep(), cep) || !Objects.equals(e.fusoIana(), fusoIana);
        this.nome = request.nome();
        this.descricao = request.descricao();
        this.logradouro = e.logradouro();
        this.numero = e.numero();
        this.complemento = e.complemento();
        this.bairro = e.bairro();
        this.cidade = e.cidade();
        this.estado = e.estado();
        this.cep = e.cep();
        this.fusoIana = e.fusoIana();
        return enderecoMudou;
    }

    void alterarStatus(StatusEstabelecimento novo) { this.status = novo.name(); }

    Long id() { return id; }
    long responsavelId() { return responsavelId; }
    String nome() { return nome; }
    String descricao() { return descricao; }
    StatusEstabelecimento status() { return StatusEstabelecimento.valueOf(status); }
    LocalDateTime criadoEm() { return criadoEm; }
    String logradouro() { return logradouro; }
    String numero() { return numero; }
    String complemento() { return complemento; }
    String bairro() { return bairro; }
    String cidade() { return cidade; }
    String estado() { return estado; }
    String cep() { return cep; }
    String fusoIana() { return fusoIana; }
}
