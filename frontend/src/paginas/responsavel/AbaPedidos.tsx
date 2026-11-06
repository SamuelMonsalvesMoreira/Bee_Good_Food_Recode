import { useState } from 'react';
import { Link } from 'react-router-dom';
import { pedidos } from '../../api/endpoints';
import type { StatusPedido as Status } from '../../api/types';
import { CampoSelecao } from '../../componentes/Campo';
import { Carregando, ErroCarga, Vazio } from '../../componentes/Estados';
import { Paginacao } from '../../componentes/Paginacao';
import { StatusPedido } from '../../componentes/StatusPedido';
import { dataHora, dinheiro, prazo, ROTULO_STATUS_PEDIDO } from '../../lib/formato';
import { useCarregar } from '../../lib/useCarregar';

const FILTROS: (Status | '')[] = ['PENDENTE', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO', 'CANCELADO', ''];

export function AbaPedidos({ estabelecimentoId }: { estabelecimentoId: number }) {
  const [status, setStatus] = useState<Status | ''>('PENDENTE');
  const [pagina, setPagina] = useState(0);
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => pedidos.doEstabelecimento(estabelecimentoId, pagina, status || undefined), [estabelecimentoId, status, pagina]);

  return (
    <section aria-labelledby="titulo-fila">
      <div className="titulo-com-acao">
        <h2 id="titulo-fila" className="h-menor">Fila de pedidos</h2>
        <div className="linha-acoes">
          <CampoSelecao rotulo="Mostrar" value={status} onChange={(e) => { setStatus(e.target.value as Status | ''); setPagina(0); }}>
            {FILTROS.map((f) => <option key={f} value={f}>{f ? ROTULO_STATUS_PEDIDO[f] : 'Todos'}</option>)}
          </CampoSelecao>
          <button className="botao botao-secundario botao-pequeno" onClick={recarregar}>Atualizar</button>
        </div>
      </div>
      {carregando && !dados && <Carregando />}
      {erro && <ErroCarga erro={erro} tentarDeNovo={recarregar} />}
      {dados && dados.conteudo.length === 0 && (
        <Vazio titulo={status === 'PENDENTE' ? 'Nenhum pedido aguardando confirmação.' : 'Nenhum pedido nesta situação.'} />
      )}
      {dados && dados.conteudo.length > 0 && (
        <>
          <div className="rolagem">
            <table className="tabela">
              <thead>
                <tr><th>Pedido</th><th>Situação</th><th>Feito em</th><th>Retirar até</th><th className="num">Total</th></tr>
              </thead>
              <tbody>
                {dados.conteudo.map((p) => (
                  <tr key={p.id}>
                    <td><Link to={`/pedidos/${p.id}`}>Pedido {p.id}</Link></td>
                    <td><StatusPedido status={p.status} /></td>
                    <td>{dataHora(p.criadoEm, p.fusoIana)}</td>
                    <td>{prazo(p.retiradaAte, p.fusoIana)}</td>
                    <td className="num">{dinheiro(p.total)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Paginacao pagina={dados} aoMudar={setPagina} />
        </>
      )}
    </section>
  );
}
