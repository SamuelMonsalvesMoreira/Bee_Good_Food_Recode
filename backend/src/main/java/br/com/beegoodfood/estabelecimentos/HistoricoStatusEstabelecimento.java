package br.com.beegoodfood.estabelecimentos;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// Registro imutavel: somente insercao.
@Entity
@Table(name = "historico_status_estabelecimento")
class HistoricoStatusEstabelecimento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "estabelecimento_id", nullable = false, updatable = false) private Long estabelecimentoId;
    @Column(name = "ator_id", updatable = false) private Long atorId;
    @Column(name = "status_anterior", length = 24, updatable = false) private String statusAnterior;
    @Column(name = "status_novo", nullable = false, length = 24, updatable = false) private String statusNovo;
    @Column(length = 500, updatable = false) private String motivo;
    @Column(name = "criado_em", nullable = false, updatable = false) private LocalDateTime criadoEm;

    protected HistoricoStatusEstabelecimento() { }

    HistoricoStatusEstabelecimento(long estabelecimentoId, long atorId, StatusEstabelecimento anterior,
                                   StatusEstabelecimento novo, String motivo, LocalDateTime agora) {
        this.estabelecimentoId = estabelecimentoId;
        this.atorId = atorId;
        this.statusAnterior = anterior == null ? null : anterior.name();
        this.statusNovo = novo.name();
        this.motivo = motivo;
        this.criadoEm = agora;
    }

    Long id() { return id; }
    Long atorId() { return atorId; }
    String statusAnterior() { return statusAnterior; }
    String statusNovo() { return statusNovo; }
    String motivo() { return motivo; }
    LocalDateTime criadoEm() { return criadoEm; }
}
