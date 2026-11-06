import { PiggyBank } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { pedidos } from '../api/endpoints';
import { Carregando, ErroCarga, Vazio } from '../componentes/Estados';
import { Paginacao } from '../componentes/Paginacao';
import { StatusPedido } from '../componentes/StatusPedido';
import { dataHora, dinheiro, prazo } from '../lib/formato';
import { useCarregar } from '../lib/useCarregar';

export function Pedidos() {
  const [params, setParams] = useSearchParams();
  const pagina = Number(params.get('pagina') ?? 0) || 0;
  const { dados, erro, carregando, recarregar } = useCarregar(() => pedidos.meus(pagina), [pagina]);
  // Impacto para o cliente (como Phenix e Too Good To Go): quanto ja economizou nas retiradas.
  const retirados = useCarregar(() => pedidos.meus(0, 'CONCLUIDO', 100), []);
  const economia = retirados.dados?.conteudo.reduce((soma, p) => soma + p.economia, 0) ?? 0;
  const quantidade = retirados.dados?.totalElementos ?? 0;

  return (
    <>
      <h1>Meus pedidos</h1>
      {quantidade > 0 && (
        <div className="economia-cliente">
          <PiggyBank aria-hidden="true" />
          <p>
            Você já economizou <strong className="numero">{dinheiro(economia)}</strong> em{' '}
            {quantidade === 1 ? '1 pedido retirado' : `${quantidade} pedidos retirados`}
            {quantidade > 100 ? ' (últimos 100)' : ''}.
          </p>
        </div>
      )}
      {carregando && !dados && <Carregando />}
      {erro && <ErroCarga erro={erro} tentarDeNovo={recarregar} />}
      {dados && dados.conteudo.length === 0 && (
        <Vazio titulo="Você ainda não fez pedidos." acao={<Link className="botao botao-principal" to="/">Ver ofertas</Link>} />
      )}
      {dados && dados.conteudo.length > 0 && (
        <>
          <ul className="lista-pedidos">
            {dados.conteudo.map((p) => (
              <li key={p.id} className="linha-pedido">
                <div>
                  <Link to={`/pedidos/${p.id}`} className="item-nome">Pedido {p.id} em {p.estabelecimentoNome}</Link>
                  <p className="pequeno suave">Feito em {dataHora(p.criadoEm, p.fusoIana)}</p>
                </div>
                <StatusPedido status={p.status} />
                <div className="linha-pedido-fim">
                  <strong className="numero">{dinheiro(p.total)}</strong>
                  {p.economia > 0 && p.status !== 'CANCELADO' && <span className="pequeno economia-linha">economia de {dinheiro(p.economia)}</span>}
                  {!['CONCLUIDO', 'CANCELADO'].includes(p.status) && (
                    <span className="pequeno">Retire até {prazo(p.retiradaAte, p.fusoIana)}</span>
                  )}
                </div>
              </li>
            ))}
          </ul>
          <Paginacao pagina={dados} aoMudar={(n) => setParams({ pagina: String(n) })} />
        </>
      )}
    </>
  );
}
