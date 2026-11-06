package br.com.beegoodfood.estabelecimentos;

import br.com.beegoodfood.auth.UsuarioService;
import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.RecursoNaoEncontradoException;
import br.com.beegoodfood.comum.RegraNegocioException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.beegoodfood.estabelecimentos.StatusEstabelecimento.*;

@Service
@Transactional(readOnly = true)
public class EstabelecimentoService {
    private static final String NAO_ENCONTRADO = "Estabelecimento não encontrado.";
    private static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT",
            "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    public enum Decisao { APROVAR, REJEITAR, SUSPENDER, REATIVAR }

    private final EstabelecimentoRepository repository;
    private final HistoricoStatusEstabelecimentoRepository historico;
    private final UsuarioService usuarios;
    private final Clock clock;

    public EstabelecimentoService(EstabelecimentoRepository repository, HistoricoStatusEstabelecimentoRepository historico,
                                  UsuarioService usuarios, Clock clock) {
        this.repository = repository;
        this.historico = historico;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    @Transactional
    public EstabelecimentoResponse solicitar(long usuarioId, EstabelecimentoRequest request) {
        validarEndereco(request.endereco());
        // responsavel_id vem sempre da identidade autenticada, nunca do corpo.
        var estabelecimento = repository.saveAndFlush(new Estabelecimento(usuarioId, request, agora()));
        registrar(estabelecimento, usuarioId, null, null);
        return EstabelecimentoResponse.from(estabelecimento);
    }

    public PaginaResponse<EstabelecimentoResponse> meus(long usuarioId, int pagina, int tamanho) {
        return PaginaResponse.from(repository.findByResponsavelId(usuarioId, PageRequest.of(pagina, tamanho, Sort.by("id")))
                .map(EstabelecimentoResponse::from));
    }

    public EstabelecimentoResponse detalhe(long id, long usuarioId, boolean administrador) {
        return EstabelecimentoResponse.from(visivel(id, usuarioId, administrador));
    }

    public List<HistoricoEstabelecimentoResponse> historico(long id, long usuarioId, boolean administrador) {
        visivel(id, usuarioId, administrador);
        return historico.findByEstabelecimentoIdOrderByCriadoEmAscIdAsc(id).stream()
                .map(HistoricoEstabelecimentoResponse::from).toList();
    }

    @Transactional
    public EstabelecimentoResponse atualizar(long id, long usuarioId, EstabelecimentoRequest request) {
        validarEndereco(request.endereco());
        var estabelecimento = repository.bloquear(id).filter(e -> e.responsavelId() == usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
        var anterior = estabelecimento.status();
        if (anterior == SUSPENSO) {
            throw RegraNegocioException.conflito("Estabelecimento suspenso não pode ser alterado. Aguarde a reativação pela moderação.");
        }
        boolean enderecoMudou = estabelecimento.aplicar(request);
        if (anterior == REJEITADO) {
            transicionar(estabelecimento, usuarioId, PENDENTE_ANALISE, "Cadastro corrigido e reenviado para análise.");
        } else if (anterior == ATIVO && enderecoMudou) {
            transicionar(estabelecimento, usuarioId, PENDENTE_ANALISE, "Endereço ou fuso alterado; nova análise necessária.");
        }
        return EstabelecimentoResponse.from(repository.saveAndFlush(estabelecimento));
    }

    public PaginaResponse<EstabelecimentoResponse> listarAdmin(StatusEstabelecimento status, int pagina, int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by("id"));
        var page = status == null ? repository.findAll(pageable) : repository.findByStatus(status.name(), pageable);
        return PaginaResponse.from(page.map(EstabelecimentoResponse::from));
    }

    @Transactional
    public EstabelecimentoResponse moderar(long id, long administradorId, Decisao decisao, String motivo) {
        var estabelecimento = repository.bloquear(id).orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
        if (estabelecimento.responsavelId() == administradorId) {
            throw new AccessDeniedException("Moderacao do proprio estabelecimento");
        }
        var atual = estabelecimento.status();
        var novo = switch (decisao) {
            case APROVAR -> atual == PENDENTE_ANALISE ? ATIVO : null;
            case REJEITAR -> atual == PENDENTE_ANALISE ? REJEITADO : null;
            case SUSPENDER -> atual == ATIVO ? SUSPENSO : null;
            case REATIVAR -> atual == SUSPENSO ? ATIVO : null;
        };
        if (novo == null) {
            throw RegraNegocioException.conflito("Esta decisão não é permitida para um estabelecimento em " + atual.name() + ".");
        }
        if (novo == ATIVO && !enderecoCompleto(estabelecimento)) {
            throw RegraNegocioException.conflito("Endereço completo e fuso horário válido são obrigatórios para aprovar.");
        }
        transicionar(estabelecimento, administradorId, novo, motivo);
        if (novo == ATIVO) {
            // Papel cumulativo: CLIENTE e preservado. Operacao idempotente.
            usuarios.concederResponsavel(estabelecimento.responsavelId());
        }
        return EstabelecimentoResponse.from(repository.saveAndFlush(estabelecimento));
    }

    // ---- Contrato com outros modulos ----

    /** Proprietario do estabelecimento; terceiros recebem 404 para nao revelar a existencia. */
    public EstabelecimentoResumo doProprietario(long id, long usuarioId) {
        return repository.findById(id).filter(e -> e.responsavelId() == usuarioId).map(EstabelecimentoResumo::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }

    public EstabelecimentoResumo resumo(long id) {
        return repository.findById(id).map(EstabelecimentoResumo::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }

    /** Usar dentro da transacao do pedido: bloqueio compartilhado contra moderacao simultanea. */
    @Transactional
    public EstabelecimentoResumo bloquearParaVenda(long id) {
        return repository.bloquearLeitura(id).map(EstabelecimentoResumo::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }

    // ---- Regras internas ----

    private Estabelecimento visivel(long id, long usuarioId, boolean administrador) {
        return repository.findById(id).filter(e -> administrador || e.responsavelId() == usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
    }

    private void transicionar(Estabelecimento estabelecimento, long atorId, StatusEstabelecimento novo, String motivo) {
        var anterior = estabelecimento.status();
        estabelecimento.alterarStatus(novo);
        registrar(estabelecimento, atorId, anterior, motivo);
    }

    private void registrar(Estabelecimento e, long atorId, StatusEstabelecimento anterior, String motivo) {
        historico.save(new HistoricoStatusEstabelecimento(e.id(), atorId, anterior, e.status(), motivo, agora()));
    }

    private void validarEndereco(EnderecoRequest endereco) {
        if (!UFS.contains(endereco.estado())) {
            throw RegraNegocioException.invalido("endereco.estado", "Informe uma UF brasileira válida.");
        }
        if (!fusoValido(endereco.fusoIana())) {
            throw RegraNegocioException.invalido("endereco.fusoIana", "Informe um fuso IANA válido, como America/Sao_Paulo.");
        }
    }

    private boolean enderecoCompleto(Estabelecimento e) {
        return naoVazio(e.logradouro()) && naoVazio(e.numero()) && naoVazio(e.bairro()) && naoVazio(e.cidade())
                && e.estado() != null && UFS.contains(e.estado()) && e.cep() != null && e.cep().matches("[0-9]{8}")
                && fusoValido(e.fusoIana());
    }

    private static boolean naoVazio(String valor) { return valor != null && !valor.isBlank(); }

    // Somente identificadores de regiao (ex.: America/Sao_Paulo); offsets fixos nao seguem horario local.
    static boolean fusoValido(String fuso) {
        return fuso != null && fuso.contains("/") && ZoneId.getAvailableZoneIds().contains(fuso);
    }

    private LocalDateTime agora() { return LocalDateTime.now(clock); }
}
