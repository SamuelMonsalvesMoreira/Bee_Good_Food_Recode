import { TicketCheck } from 'lucide-react';
import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { Link, useLocation, useParams } from 'react-router-dom';
import { pedidos } from '../api/endpoints';
import type { Pedido, StatusPedido as Status } from '../api/types';
import { useSessao } from '../auth/sessao';
import { CampoTexto } from '../componentes/Campo';
import { Dialogo } from '../componentes/Dialogo';
import { Carregando, ErroCarga, Vazio } from '../componentes/Estados';
import { EtapasPedido, StatusPedido } from '../componentes/StatusPedido';
import { dataHora, dinheiro, nomeFuso, prazo, ROTULO_PAGAMENTO, ROTULO_STATUS_PEDIDO } from '../lib/formato';
import { useAcao, useCarregar } from '../lib/useCarregar';

const PROXIMO: Partial<Record<Status, { status: Status; acao: string }>> = {
  PENDENTE: { status: 'CONFIRMADO', acao: 'Confirmar pedido' },
  CONFIRMADO: { status: 'EM_PREPARO', acao: 'Iniciar preparo' },
  EM_PREPARO: { status: 'PRONTO_PARA_RETIRADA', acao: 'Marcar como pronto' },
  PRONTO_PARA_RETIRADA: { status: 'CONCLUIDO', acao: 'Registrar retirada' },
};

/** Codigo curto para conferencia no balcao (inspirado no comprovante de retirada dos apps do setor). */
export function codigoRetirada(id: number): string {
  return `BG-${String(id).padStart(4, '0')}`;
}

export function PedidoDetalhe() {
  const id = Number(useParams().id);
  const novo = (useLocation().state as { novo?: boolean } | null)?.novo;
  const { usuario } = useSessao();
  const carga = useCarregar(() => pedidos.detalhe(id), [id]);
  const [pedido, setPedido] = useState<Pedido>();
  const [cancelando, setCancelando] = useState(false);
  const [motivo, setMotivo] = useState('');
  const acao = useAcao();

  useEffect(() => { if (carga.dados) setPedido(carga.dados); }, [carga.dados]);

  // Acompanhamento: enquanto o pedido esta em andamento, atualiza a cada 30 s (substitui a notificacao do TCC).
  const emAndamento = pedido && !['CONCLUIDO', 'CANCELADO'].includes(pedido.status);
  useEffect(() => {
    if (!emAndamento) return;
    const intervalo = window.setInterval(() => {
      pedidos.detalhe(id).then(setPedido).catch(() => undefined);
    }, 30_000);
    return () => window.clearInterval(intervalo);
  }, [emAndamento, id]);

  if (carga.carregando && !pedido) return <Carregando />;
  if (carga.erro?.status === 404) return <Vazio titulo="Pedido não encontrado." acao={<Link className="botao botao-principal" to="/pedidos">Meus pedidos</Link>} />;
  if (carga.erro) return <ErroCarga erro={carga.erro} tentarDeNovo={carga.recarregar} />;
  if (!pedido) return null;

  const souCliente = usuario?.id === pedido.clienteId;
  const souLoja = !souCliente;
  const fuso = pedido.estabelecimento.fusoIana;
  const proximo = souLoja ? PROXIMO[pedido.status] : undefined;
  const podeCancelar = souCliente ? pedido.status === 'PENDENTE' : !['CONCLUIDO', 'CANCELADO'].includes(pedido.status);
  const economia = pedido.itens.reduce((s, i) => s + (i.precoOriginal - i.precoOferta) * i.quantidade, 0);

  async function avancar() {
    if (!proximo) return;
    const atualizado = await acao.executar(() => pedidos.avancar(pedido!.id, proximo.status));
    if (atualizado) { setPedido(atualizado); toast.success(`Pedido ${atualizado.id}: ${ROTULO_STATUS_PEDIDO[atualizado.status]}.`); }
  }

  async function cancelar() {
    const atualizado = await acao.executar(() => pedidos.cancelar(pedido!.id, motivo.trim() || undefined));
    if (atualizado) { setPedido(atualizado); setCancelando(false); setMotivo(''); toast.success('Pedido cancelado. Os itens voltaram a ficar disponíveis.'); }
  }

  return (
    <article className="pedido">
      <p className="trilha">
        {souCliente ? <Link to="/pedidos">Meus pedidos</Link> : <Link to={`/minhas-lojas/${pedido.estabelecimentoId}`}>Painel da loja</Link>} / Pedido {pedido.id}
      </p>
      {novo && (
        <div className="aviso aviso-sucesso" role="status">
          <p><strong>Reserva feita.</strong> A loja vai confirmar seu pedido. Esta página se atualiza sozinha.</p>
        </div>
      )}
      <header className="pedido-topo">
        <div>
          <h1>Pedido {pedido.id}</h1>
          <p className="suave">Feito em {dataHora(pedido.criadoEm, fuso)}</p>
        </div>
        <StatusPedido status={pedido.status} />
      </header>

      <EtapasPedido status={pedido.status} />
      <p className="sr-only" aria-live="polite">Situação: {ROTULO_STATUS_PEDIDO[pedido.status]}</p>

      <div className="pedido-grade">
        <section className="pilha">
          {pedido.status !== 'CANCELADO' && pedido.status !== 'CONCLUIDO' && (
            <div className={`painel painel-retirada${pedido.status === 'PRONTO_PARA_RETIRADA' ? ' pronto' : ''}`}>
              <div className="retirada-codigo">
                <TicketCheck aria-hidden="true" />
                <div>
                  <p className="pequeno">Código de retirada</p>
                  <p className="codigo numero" aria-label={`Código de retirada ${codigoRetirada(pedido.id)}`}>{codigoRetirada(pedido.id)}</p>
                </div>
              </div>
              <div className="retirada-quando">
                <p className="pequeno">{pedido.status === 'PRONTO_PARA_RETIRADA' ? 'Pronto! Retire até' : 'Retire até'}</p>
                <p className="retirada-prazo">{prazo(pedido.retiradaAte, fuso)}</p>
                <p className="pequeno suave">No {nomeFuso(fuso)}. {souCliente ? 'Mostre o código no balcão.' : 'Confira o código com o cliente.'}</p>
              </div>
            </div>
          )}
          <div>
            <h2 className="h-menor">Itens</h2>
            <div className="rolagem">
              <table className="tabela">
                <thead><tr><th>Produto</th><th className="num">Qtd.</th><th className="num">Preço</th><th className="num">Subtotal</th></tr></thead>
                <tbody>
                  {pedido.itens.map((i) => (
                    <tr key={i.ofertaId}>
                      <td>{i.produtoNome}</td>
                      <td className="num">{i.quantidade}</td>
                      <td className="num">{dinheiro(i.precoOferta)} <s className="suave pequeno">{dinheiro(i.precoOriginal)}</s></td>
                      <td className="num">{dinheiro(i.subtotal)}</td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr><th colSpan={3}>Total</th><td className="num"><strong>{dinheiro(pedido.total)}</strong></td></tr>
                </tfoot>
              </table>
            </div>
            <p className="pequeno suave">Economia em relação ao preço original: {dinheiro(economia)}.</p>
          </div>
          <div>
            <h2 className="h-menor">Histórico</h2>
            <ol className="historico">
              {pedido.historico.map((h, i) => (
                <li key={i}>
                  <strong>{ROTULO_STATUS_PEDIDO[h.statusNovo]}</strong>
                  <span className="suave pequeno"> em {dataHora(h.criadoEm, fuso)}</span>
                  {h.motivo && <p className="pequeno">Motivo: {h.motivo}</p>}
                </li>
              ))}
            </ol>
          </div>
        </section>

        <aside className="pilha">
          <div className="painel painel-cinza">
            <h2 className="h-menor">Onde retirar</h2>
            <address>
              <strong>{pedido.estabelecimento.nome}</strong><br />
              {pedido.estabelecimento.logradouro}, {pedido.estabelecimento.numero}
              {pedido.estabelecimento.complemento ? `, ${pedido.estabelecimento.complemento}` : ''}<br />
              {pedido.estabelecimento.bairro}, {pedido.estabelecimento.cidade} ({pedido.estabelecimento.estado})
            </address>
            <p className="pequeno">Pagamento: {ROTULO_PAGAMENTO[pedido.formaPagamento]}.</p>
          </div>
          {acao.erro && <div className="aviso aviso-erro" role="alert"><p>{acao.erro.detalhe}</p></div>}
          {(proximo || podeCancelar) && (
            <div className="linha-acoes">
              {proximo && (
                <button className="botao botao-principal" disabled={acao.enviando} onClick={avancar}>{proximo.acao}</button>
              )}
              {podeCancelar && (
                <button className="botao botao-perigo" disabled={acao.enviando} onClick={() => setCancelando(true)}>Cancelar pedido</button>
              )}
            </div>
          )}
          {souCliente && pedido.status !== 'PENDENTE' && !['CONCLUIDO', 'CANCELADO'].includes(pedido.status) && (
            <p className="pequeno suave">A loja já confirmou o pedido. Para cancelar agora, fale diretamente com ela.</p>
          )}
        </aside>
      </div>

      <Dialogo
        aberto={cancelando}
        titulo="Cancelar este pedido?"
        aoFechar={() => setCancelando(false)}
        acoes={(
          <>
            <button className="botao botao-perigo" disabled={acao.enviando || (souLoja && !motivo.trim())} onClick={cancelar}>
              Cancelar pedido
            </button>
            <button className="botao botao-secundario" onClick={() => setCancelando(false)}>Voltar</button>
          </>
        )}
      >
        <p>Os itens voltam a ficar disponíveis para outras pessoas.</p>
        <CampoTexto rotulo={souLoja ? 'Motivo (o cliente verá)' : 'Motivo (opcional)'} maxLength={500} value={motivo}
          onChange={(e) => setMotivo(e.target.value)} required={souLoja} />
      </Dialogo>
    </article>
  );
}
