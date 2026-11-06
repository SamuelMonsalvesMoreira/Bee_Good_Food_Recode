import { useEffect } from 'react';
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom';
import { estabelecimentos } from '../../api/endpoints';
import { useSessao } from '../../auth/sessao';
import { Carregando, ErroCarga, Sucesso, Vazio } from '../../componentes/Estados';
import { FormularioLoja } from '../../componentes/FormularioLoja';
import { StatusLoja } from '../../componentes/StatusLoja';
import { dataHora, ROTULO_STATUS_LOJA } from '../../lib/formato';
import { useAcao, useCarregar } from '../../lib/useCarregar';
import { AbaCatalogo } from './AbaCatalogo';
import { AbaIndicadores } from './AbaIndicadores';
import { AbaPedidos } from './AbaPedidos';

const ABAS = [
  { id: 'pedidos', rotulo: 'Pedidos' },
  { id: 'catalogo', rotulo: 'Produtos e ofertas' },
  { id: 'indicadores', rotulo: 'Indicadores' },
  { id: 'dados', rotulo: 'Dados da loja' },
] as const;
type Aba = (typeof ABAS)[number]['id'];

export function PainelLoja() {
  const id = Number(useParams().id);
  const enviada = (useLocation().state as { enviada?: boolean } | null)?.enviada;
  const [params, setParams] = useSearchParams();
  const { tem, atualizar } = useSessao();
  const loja = useCarregar(() => estabelecimentos.detalhe(id), [id]);
  const historico = useCarregar(() => estabelecimentos.historico(id), [id]);
  const salvar = useAcao();

  // A aprovacao concede RESPONSAVEL no servidor; atualizar os papeis da sessao ao abrir o painel.
  useEffect(() => { void atualizar().catch(() => undefined); }, [atualizar, loja.dados?.status]);

  if (loja.carregando && !loja.dados) return <Carregando />;
  if (loja.erro?.status === 404) return <Vazio titulo="Loja não encontrada." acao={<Link className="botao botao-principal" to="/minhas-lojas">Minhas lojas</Link>} />;
  if (loja.erro) return <ErroCarga erro={loja.erro} tentarDeNovo={loja.recarregar} />;
  const dados = loja.dados;
  if (!dados) return null;

  const responsavel = tem('RESPONSAVEL');
  const operaCatalogo = responsavel && dados.status !== 'PENDENTE_ANALISE' && dados.status !== 'REJEITADO';
  const pedida = (params.get('aba') as Aba | null) ?? (operaCatalogo ? 'pedidos' : 'dados');
  const aba: Aba = operaCatalogo || pedida === 'dados' ? pedida : 'dados';
  const ultimaDecisao = historico.dados?.filter((h) => h.motivo).at(-1);

  return (
    <>
      <p className="trilha"><Link to="/minhas-lojas">Minhas lojas</Link> / {dados.nome}</p>
      <header className="titulo-com-acao">
        <div>
          <h1>{dados.nome}</h1>
          <p className="suave">{dados.endereco.logradouro}, {dados.endereco.numero}, {dados.endereco.bairro}, {dados.endereco.cidade}</p>
        </div>
        <StatusLoja status={dados.status} />
      </header>

      {enviada && <Sucesso><p>Cadastro enviado para análise. Você será avisado aqui quando a moderação decidir.</p></Sucesso>}
      <AvisoStatus status={dados.status} motivo={ultimaDecisao?.motivo ?? undefined} />

      {operaCatalogo && (
        <nav className="abas" aria-label="Seções da loja">
          {ABAS.map((a) => (
            <button key={a.id} className="aba" aria-current={aba === a.id ? 'page' : undefined}
              onClick={() => setParams({ aba: a.id }, { replace: true })}>
              {a.rotulo}
            </button>
          ))}
        </nav>
      )}

      <div className="aba-conteudo">
        {aba === 'pedidos' && <AbaPedidos estabelecimentoId={id} />}
        {aba === 'catalogo' && <AbaCatalogo estabelecimentoId={id} ativa={dados.status === 'ATIVO'} />}
        {aba === 'indicadores' && <AbaIndicadores estabelecimentoId={id} fuso={dados.endereco.fusoIana} />}
        {aba === 'dados' && (
          <div className="pilha">
            {dados.status === 'SUSPENSO' ? (
              <p className="suave">A loja está suspensa e os dados não podem ser alterados até a reativação.</p>
            ) : (
              <>
                {dados.status === 'ATIVO' && (
                  <div className="aviso aviso-info"><p>Mudar endereço ou fuso envia a loja para nova análise e pausa as vendas até a aprovação.</p></div>
                )}
                {salvar.erro === undefined && salvar.enviando === false && params.get('salvo') && <Sucesso><p>Dados salvos.</p></Sucesso>}
                <FormularioLoja key={dados.id} inicial={dados} enviando={salvar.enviando} erro={salvar.erro}
                  rotuloEnviar={dados.status === 'REJEITADO' ? 'Corrigir e reenviar' : 'Salvar dados'}
                  aoEnviar={async (form) => {
                    const atualizado = await salvar.executar(() => estabelecimentos.atualizar(id, form));
                    if (atualizado) { loja.recarregar(); historico.recarregar(); setParams({ aba: 'dados', salvo: '1' }, { replace: true }); }
                  }} />
              </>
            )}
            {historico.dados && historico.dados.length > 0 && (
              <section>
                <h2 className="h-menor">Histórico da moderação</h2>
                <ol className="historico">
                  {historico.dados.map((h) => (
                    <li key={h.id}>
                      <strong>{ROTULO_STATUS_LOJA[h.statusNovo]}</strong>
                      <span className="suave pequeno"> em {dataHora(h.criadoEm, dados.endereco.fusoIana)}</span>
                      {h.motivo && <p className="pequeno">{h.motivo}</p>}
                    </li>
                  ))}
                </ol>
              </section>
            )}
          </div>
        )}
      </div>
    </>
  );
}

function AvisoStatus({ status, motivo }: { status: string; motivo?: string }) {
  if (status === 'PENDENTE_ANALISE') {
    return <div className="aviso aviso-info"><p>Em análise pela moderação. Assim que for aprovada, você poderá publicar ofertas.</p></div>;
  }
  if (status === 'REJEITADO') {
    return (
      <div className="aviso aviso-erro">
        <p><strong>O cadastro precisa de ajustes.</strong> {motivo && <>Motivo informado: {motivo}</>}</p>
        <p>Corrija os dados abaixo e reenvie para análise.</p>
      </div>
    );
  }
  if (status === 'SUSPENSO') {
    return (
      <div className="aviso aviso-erro">
        <p><strong>Loja suspensa.</strong> {motivo && <>Motivo: {motivo}</>}</p>
        <p>Novas ofertas e pedidos estão bloqueados. Você ainda pode tratar e cancelar os pedidos já feitos.</p>
      </div>
    );
  }
  return null;
}
