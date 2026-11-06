import { useState, type FormEvent } from 'react';
import { indicadores } from '../../api/endpoints';
import { Campo } from '../../componentes/Campo';
import { Carregando, ErroCarga } from '../../componentes/Estados';
import { dataHora, dinheiro, isoParaLocal, nomeFuso } from '../../lib/formato';
import { useCarregar } from '../../lib/useCarregar';

type Atalho = '7d' | 'mes' | 'mesPassado' | 'personalizado';

/** Periodos interpretados no fuso da loja, como a API exige ([inicio, fim)). */
function periodo(atalho: Atalho, fuso: string): { inicio: string; fim?: string } {
  const hojeLocal = isoParaLocal(new Date(), fuso).slice(0, 10);
  const [ano, mes] = hojeLocal.split('-').map(Number) as [number, number];
  const doisDigitos = (n: number) => String(n).padStart(2, '0');
  if (atalho === 'mes') return { inicio: `${ano}-${doisDigitos(mes)}-01T00:00` };
  if (atalho === 'mesPassado') {
    const [a, m] = mes === 1 ? [ano - 1, 12] : [ano, mes - 1];
    return { inicio: `${a}-${doisDigitos(m)}-01T00:00`, fim: `${ano}-${doisDigitos(mes)}-01T00:00` };
  }
  const seteDias = isoParaLocal(new Date(Date.now() - 7 * 86_400_000), fuso).slice(0, 10);
  return { inicio: `${seteDias}T00:00` };
}

function taxa(valor: number | null): string {
  return valor === null ? 'N/A — sem pedidos no período' : `${valor.toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;
}

export function AbaIndicadores({ estabelecimentoId, fuso }: { estabelecimentoId: number; fuso: string }) {
  const [atalho, setAtalho] = useState<Atalho>('7d');
  const [faixa, setFaixa] = useState(() => periodo('7d', fuso));
  const [manual, setManual] = useState({ inicio: faixa.inicio, fim: isoParaLocal(new Date(), fuso) });
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => indicadores.consultar(estabelecimentoId, faixa.inicio, faixa.fim), [estabelecimentoId, faixa.inicio, faixa.fim]);

  function escolher(novo: Atalho) {
    setAtalho(novo);
    if (novo !== 'personalizado') setFaixa(periodo(novo, fuso));
  }

  function aplicar(evento: FormEvent) {
    evento.preventDefault();
    setFaixa({ inicio: manual.inicio, fim: manual.fim });
  }

  const c = dados?.coorte;
  return (
    <section aria-labelledby="titulo-indicadores">
      <h2 id="titulo-indicadores" className="h-menor">Indicadores</h2>
      <div className="linha-acoes" role="group" aria-label="Período">
        {([['7d', 'Últimos 7 dias'], ['mes', 'Este mês'], ['mesPassado', 'Mês passado'], ['personalizado', 'Outro período']] as const).map(([valor, rotulo]) => (
          <button key={valor} className={`botao botao-pequeno ${atalho === valor ? 'botao-principal' : 'botao-secundario'}`}
            aria-pressed={atalho === valor} onClick={() => escolher(valor)}>{rotulo}</button>
        ))}
      </div>
      {atalho === 'personalizado' && (
        <form className="linha-acoes periodo" onSubmit={aplicar}>
          <Campo rotulo="De" type="datetime-local" value={manual.inicio} onChange={(e) => setManual({ ...manual, inicio: e.target.value })} />
          <Campo rotulo="Até (não incluso)" type="datetime-local" value={manual.fim} onChange={(e) => setManual({ ...manual, fim: e.target.value })} />
          <button className="botao botao-secundario">Aplicar</button>
        </form>
      )}
      {carregando && !dados && <Carregando />}
      {erro && <ErroCarga erro={{ ...erro, detalhe: Object.values(erro.campos)[0] ?? erro.detalhe }} tentarDeNovo={recarregar} />}
      {dados && c && (
        <div aria-busy={carregando}>
          <p className="pequeno suave periodo-texto">
            De {dataHora(dados.periodo.inicio, fuso)} até {dataHora(dados.periodo.fim, fuso)}, no {nomeFuso(fuso)}.
          </p>
          <dl className="metricas">
            <div><dt>Pedidos retirados</dt><dd className="numero">{dados.pedidosConcluidos}</dd></div>
            <div><dt>Valor dos pedidos retirados</dt><dd className="numero">{dinheiro(dados.valorMovimentado)}</dd></div>
            <div><dt>Unidades vendidas</dt><dd className="numero">{dados.itensComercializados}</dd></div>
            <div><dt>Desconto concedido aos clientes</dt><dd className="numero">{dinheiro(dados.economiaEstimada)}</dd></div>
          </dl>
          <h3>Pedidos feitos no período</h3>
          <p className="suave pequeno">
            Situação atual dos {c.pedidos} pedidos criados neste período. Pedidos em andamento ainda podem mudar as taxas.
          </p>
          {c.pedidos > 0 && (
            <div className="composicao" aria-hidden="true">
              <span className="composicao-ok" style={{ flexGrow: c.concluidos }} />
              <span className="composicao-erro" style={{ flexGrow: c.cancelados }} />
              <span className="composicao-andamento" style={{ flexGrow: c.emAndamento }} />
            </div>
          )}
          <dl className="metricas metricas-pequenas">
            <div><dt><span className="legenda legenda-ok" aria-hidden="true" />Taxa de retirada</dt><dd>{taxa(c.taxaConclusao)}</dd></div>
            <div><dt><span className="legenda legenda-erro" aria-hidden="true" />Taxa de cancelamento</dt><dd>{taxa(c.taxaCancelamento)}</dd></div>
            <div><dt><span className="legenda legenda-andamento" aria-hidden="true" />Em andamento</dt><dd className="numero">{c.emAndamento}</dd></div>
          </dl>
          <p className="pequeno suave">
            O valor é o que foi registrado nos pedidos, não um pagamento confirmado pelo site.
          </p>
        </div>
      )}
    </section>
  );
}
