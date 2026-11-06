package br.com.beegoodfood.pedidos;

import br.com.beegoodfood.catalogo.EstoqueVendaService;
import br.com.beegoodfood.catalogo.EstoqueVendaService.OfertaParaVenda;
import br.com.beegoodfood.comum.PaginaResponse;
import br.com.beegoodfood.comum.RecursoNaoEncontradoException;
import br.com.beegoodfood.comum.RegraNegocioException;
import br.com.beegoodfood.estabelecimentos.EstabelecimentoService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Fluxos transacionais de criacao, avanco e cancelamento de pedidos (decisoes-do-mvp.md). */
@Service
public class PedidoService {
    private static final String NAO_ENCONTRADO = "Pedido não encontrado.";
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("99999999.99");

    public record Resultado(PedidoResponse pedido, boolean criado) { }

    private final PedidoRepository pedidos;
    private final EstoqueVendaService estoque;
    private final EstabelecimentoService estabelecimentos;
    private final TransactionTemplate tx;
    private final TransactionTemplate leitura;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidos, EstoqueVendaService estoque, EstabelecimentoService estabelecimentos,
                         PlatformTransactionManager transactions, Clock clock) {
        this.pedidos = pedidos;
        this.estoque = estoque;
        this.estabelecimentos = estabelecimentos;
        this.tx = new TransactionTemplate(transactions);
        this.leitura = new TransactionTemplate(transactions);
        this.leitura.setReadOnly(true);
        this.clock = clock;
    }

    // ---- Criacao (5.5) ----

    public Resultado criar(long clienteId, String chave, PedidoRequest request) {
        Map<Long, Integer> itens = normalizar(request);
        String hash = impressaoDigital(request.formaPagamento(), itens);
        var existente = leitura.execute(s -> pedidos.findByClienteIdAndIdempotencyKey(clienteId, chave)
                .map(p -> repeticao(p, hash)).orElse(null));
        if (existente != null) { return existente; }
        try {
            return tx.execute(s -> criarNaTransacao(clienteId, chave, hash, request.formaPagamento(), itens));
        } catch (DataIntegrityViolationException | PessimisticLockingFailureException error) {
            // Disputa pela mesma chave: a transacao perdedora foi desfeita (inclusive a baixa de estoque).
            var vencedor = leitura.execute(s -> pedidos.findByClienteIdAndIdempotencyKey(clienteId, chave)
                    .map(p -> repeticao(p, hash)).orElse(null));
            if (vencedor != null) { return vencedor; }
            throw error;
        }
    }

    private Resultado criarNaTransacao(long clienteId, String chave, String hash, FormaPagamento forma, Map<Long, Integer> itens) {
        var agora = agora();
        // 1. Bloqueia as ofertas em ordem crescente de ID (evita deadlock entre pedidos).
        var ofertas = estoque.bloquear(itens.keySet());
        // 2. Releitura com bloqueio: pedido concorrente com a mesma chave ja confirmado e devolvido sem nova baixa.
        var concorrente = pedidos.bloquearPorChave(clienteId, chave);
        if (concorrente.isPresent()) { return repeticao(concorrente.get(), hash); }
        if (ofertas.size() != itens.size()) {
            throw RegraNegocioException.conflito("Uma ou mais ofertas não estão disponíveis para compra.");
        }
        var estabelecimentoIds = ofertas.stream().map(OfertaParaVenda::estabelecimentoId).distinct().toList();
        if (estabelecimentoIds.size() != 1) {
            throw RegraNegocioException.conflito("Um pedido só pode conter ofertas de um mesmo estabelecimento.");
        }
        // 3. Bloqueio compartilhado do estabelecimento: moderacao simultanea aguarda.
        var estabelecimento = estabelecimentos.bloquearParaVenda(estabelecimentoIds.getFirst());
        if (!estabelecimento.ativo()) {
            throw RegraNegocioException.conflito("O estabelecimento não está recebendo pedidos no momento.");
        }
        if (estabelecimento.responsavelId() == clienteId) {
            throw RegraNegocioException.conflito("Não é possível fazer pedido no próprio estabelecimento.");
        }
        var itensPedido = new ArrayList<ItemPedido>();
        LocalDateTime retiradaAte = null;
        for (var oferta : ofertas) {
            int quantidade = itens.get(oferta.ofertaId());
            // 4. Disponibilidade e estoque relidos sob bloqueio; precos sempre do banco (CA05, CA08).
            if (!oferta.compravel(agora)) {
                throw RegraNegocioException.conflito("A oferta " + oferta.ofertaId() + " não está disponível para compra.");
            }
            if (oferta.estoque() < quantidade || !estoque.baixar(oferta.ofertaId(), quantidade)) {
                throw RegraNegocioException.conflito("Estoque insuficiente para a oferta " + oferta.ofertaId() + ".");
            }
            var item = new ItemPedido(oferta.ofertaId(), oferta.produtoNome(), oferta.precoOriginal(), oferta.precoOferta(), quantidade);
            if (item.subtotal().compareTo(LIMITE_VALOR) > 0) {
                throw RegraNegocioException.invalido("itens", "O valor do pedido excede o limite permitido.");
            }
            itensPedido.add(item);
            // O menor prazo de retirada entre as ofertas vale para o pedido (DEC-018).
            if (retiradaAte == null || oferta.retiradaAte().isBefore(retiradaAte)) { retiradaAte = oferta.retiradaAte(); }
        }
        var pedido = new Pedido(clienteId, estabelecimento, forma, chave, hash, itensPedido, retiradaAte, agora);
        if (pedido.total().compareTo(LIMITE_VALOR) > 0) {
            throw RegraNegocioException.invalido("itens", "O valor do pedido excede o limite permitido.");
        }
        // 5. Pedido, itens e historico inicial gravados juntos; a chave unica protege contra corrida.
        return new Resultado(PedidoResponse.from(pedidos.saveAndFlush(pedido)), true);
    }

    private Resultado repeticao(Pedido existente, String hash) {
        if (!existente.payloadHash().equals(hash)) {
            throw RegraNegocioException.conflito("Esta chave de idempotência já foi usada com outro conteúdo de pedido.");
        }
        return new Resultado(PedidoResponse.from(existente), false);
    }

    /** Itens repetidos sao rejeitados; ordem dos itens nao altera a impressao digital. */
    private Map<Long, Integer> normalizar(PedidoRequest request) {
        var itens = new TreeMap<Long, Integer>();
        for (var item : request.itens()) {
            if (itens.put(item.ofertaId(), item.quantidade()) != null) {
                throw RegraNegocioException.invalido("itens", "Cada oferta deve aparecer uma única vez no pedido.");
            }
        }
        return itens;
    }

    static String impressaoDigital(FormaPagamento forma, Map<Long, Integer> itens) {
        String canonico = "v1|" + forma.name() + "|" + itens.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(","));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonico.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    // ---- Consultas ----

    public PaginaResponse<PedidoResumoResponse> doCliente(long clienteId, StatusPedido status, int pagina, int tamanho) {
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "criadoEm", "id"));
        return leitura.execute(s -> PaginaResponse.from((status == null ? pedidos.findByClienteId(clienteId, pageable)
                : pedidos.findByClienteIdAndStatus(clienteId, status.name(), pageable)).map(PedidoResumoResponse::from)));
    }

    public PaginaResponse<PedidoResumoResponse> doEstabelecimento(long estabelecimentoId, long usuarioId, StatusPedido status,
                                                                  int pagina, int tamanho) {
        // Disponivel tambem para estabelecimento suspenso: pedidos existentes continuam trataveis (CA26).
        estabelecimentos.doProprietario(estabelecimentoId, usuarioId);
        var pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "criadoEm", "id"));
        return leitura.execute(s -> PaginaResponse.from((status == null ? pedidos.findByEstabelecimentoId(estabelecimentoId, pageable)
                : pedidos.findByEstabelecimentoIdAndStatus(estabelecimentoId, status.name(), pageable)).map(PedidoResumoResponse::from)));
    }

    public PedidoResponse detalhe(long pedidoId, long usuarioId) {
        return leitura.execute(s -> {
            var pedido = pedidos.findById(pedidoId).orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
            papel(pedido, usuarioId);
            return PedidoResponse.from(pedido);
        });
    }

    // ---- Status e cancelamento (5.6) ----

    public PedidoResponse avancar(long pedidoId, long usuarioId, StatusPedido destino) {
        return tx.execute(s -> {
            var pedido = pedidos.bloquear(pedidoId).orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
            if (papel(pedido, usuarioId) != Papel.ESTABELECIMENTO) {
                throw new AccessDeniedException("Somente o estabelecimento altera o status");
            }
            var atual = pedido.status();
            if (destino == StatusPedido.CANCELADO) {
                throw RegraNegocioException.invalido("status", "Use a operação de cancelamento para cancelar o pedido.");
            }
            if (atual.proximo() != destino) {
                throw RegraNegocioException.conflito("Transição de " + atual.name() + " para " + destino.name() + " não é permitida.");
            }
            pedido.transicionar(destino, usuarioId, null, agora());
            return PedidoResponse.from(pedidos.saveAndFlush(pedido));
        });
    }

    public PedidoResponse cancelar(long pedidoId, long usuarioId, String motivo) {
        return tx.execute(s -> {
            // Bloqueio do pedido: cancelamentos simultaneos sao serializados e so um devolve estoque (CA16).
            var pedido = pedidos.bloquear(pedidoId).orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADO));
            var papel = papel(pedido, usuarioId);
            var atual = pedido.status();
            if (atual == StatusPedido.CANCELADO) { return PedidoResponse.from(pedido); }
            if (atual == StatusPedido.CONCLUIDO) { throw RegraNegocioException.conflito("Pedido concluído não pode ser cancelado."); }
            if (papel == Papel.CLIENTE && atual != StatusPedido.PENDENTE) {
                throw RegraNegocioException.conflito("O cliente só pode cancelar pedidos pendentes. Fale com o estabelecimento.");
            }
            if (papel == Papel.ESTABELECIMENTO && motivo == null) {
                throw RegraNegocioException.invalido("motivo", "Informe o motivo do cancelamento.");
            }
            for (var item : pedido.itens()) { estoque.devolver(item.ofertaId(), item.quantidade()); }
            pedido.transicionar(StatusPedido.CANCELADO, usuarioId, motivo, agora());
            return PedidoResponse.from(pedidos.saveAndFlush(pedido));
        });
    }

    private enum Papel { CLIENTE, ESTABELECIMENTO }

    /** Cliente do pedido ou responsavel do estabelecimento; terceiros recebem 404 (sem revelar existencia). */
    private Papel papel(Pedido pedido, long usuarioId) {
        if (pedido.clienteId() == usuarioId) { return Papel.CLIENTE; }
        if (estabelecimentos.resumo(pedido.estabelecimentoId()).responsavelId() == usuarioId) { return Papel.ESTABELECIMENTO; }
        throw new RecursoNaoEncontradoException(NAO_ENCONTRADO);
    }

    private LocalDateTime agora() { return LocalDateTime.now(clock); }
}
