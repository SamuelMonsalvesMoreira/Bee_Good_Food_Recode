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

  return (
    <>
      <h1>Meus pedidos</h1>
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
