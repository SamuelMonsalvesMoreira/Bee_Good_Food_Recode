import { useState, type FormEvent } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { admin } from '../../api/endpoints';
import type { CategoriaAdmin, Estabelecimento, StatusEstabelecimento } from '../../api/types';
import { useSessao } from '../../auth/sessao';
import { BotaoRemover } from '../../componentes/BotaoRemover';
import { Campo, CampoSelecao, CampoTexto, erroDoCampo } from '../../componentes/Campo';
import { Dialogo } from '../../componentes/Dialogo';
import { Carregando, ErroCarga, ErroEnvio, Vazio } from '../../componentes/Estados';
import { Paginacao } from '../../componentes/Paginacao';
import { StatusLoja } from '../../componentes/StatusLoja';
import { dataHora, ROTULO_STATUS_LOJA } from '../../lib/formato';
import { useAcao, useCarregar } from '../../lib/useCarregar';

export function Admin() {
  return (
    <>
      <h1>Moderação</h1>
      <nav className="abas" aria-label="Moderação">
        <NavLink className="aba" to="/admin" end>Lojas</NavLink>
        <NavLink className="aba" to="/admin/categorias">Categorias</NavLink>
      </nav>
      <div className="aba-conteudo"><Outlet /></div>
    </>
  );
}

type Decisao = 'aprovar' | 'rejeitar' | 'suspender' | 'reativar';
const DECISOES: Record<StatusEstabelecimento, { decisao: Decisao; rotulo: string; perigo?: boolean }[]> = {
  PENDENTE_ANALISE: [{ decisao: 'aprovar', rotulo: 'Aprovar' }, { decisao: 'rejeitar', rotulo: 'Pedir ajustes', perigo: true }],
  ATIVO: [{ decisao: 'suspender', rotulo: 'Suspender', perigo: true }],
  SUSPENSO: [{ decisao: 'reativar', rotulo: 'Reativar' }],
  REJEITADO: [],
};

export function AdminLojas() {
  const { usuario } = useSessao();
  const [status, setStatus] = useState<StatusEstabelecimento | ''>('PENDENTE_ANALISE');
  const [pagina, setPagina] = useState(0);
  const lista = useCarregar(() => admin.estabelecimentos(status || undefined, pagina), [status, pagina]);
  const [alvo, setAlvo] = useState<{ loja: Estabelecimento; decisao: Decisao; rotulo: string }>();
  const [motivo, setMotivo] = useState('');
  const acao = useAcao();

  async function decidir() {
    if (!alvo) return;
    const ok = await acao.executar(() => admin.moderar(alvo.loja.id, alvo.decisao, motivo));
    if (ok) { setAlvo(undefined); setMotivo(''); lista.recarregar(); }
  }

  return (
    <section aria-labelledby="titulo-lojas">
      <div className="titulo-com-acao">
        <h2 id="titulo-lojas" className="h-menor">Lojas</h2>
        <CampoSelecao rotulo="Situação" value={status} onChange={(e) => { setStatus(e.target.value as StatusEstabelecimento | ''); setPagina(0); }}>
          {(['PENDENTE_ANALISE', 'ATIVO', 'SUSPENSO', 'REJEITADO'] as const).map((s) => <option key={s} value={s}>{ROTULO_STATUS_LOJA[s]}</option>)}
          <option value="">Todas</option>
        </CampoSelecao>
      </div>
      {lista.carregando && !lista.dados && <Carregando />}
      {lista.erro && <ErroCarga erro={lista.erro} tentarDeNovo={lista.recarregar} />}
      {lista.dados?.conteudo.length === 0 && <Vazio titulo={status === 'PENDENTE_ANALISE' ? 'Nenhuma loja aguardando análise.' : 'Nenhuma loja nesta situação.'} />}
      <ul className="pilha lista-moderacao">
        {lista.dados?.conteudo.map((loja) => (
          <li key={loja.id} className="painel">
            <div className="titulo-com-acao">
              <div>
                <h3>{loja.nome}</h3>
                <p className="pequeno suave">Solicitada em {dataHora(loja.criadoEm, loja.endereco.fusoIana)}</p>
              </div>
              <StatusLoja status={loja.status} />
            </div>
            {loja.descricao && <p>{loja.descricao}</p>}
            <address className="pequeno">
              {loja.endereco.logradouro}, {loja.endereco.numero}{loja.endereco.complemento ? `, ${loja.endereco.complemento}` : ''},{' '}
              {loja.endereco.bairro}, {loja.endereco.cidade} ({loja.endereco.estado}), CEP {loja.endereco.cep.replace(/(\d{5})(\d{3})/, '$1-$2')}.
              Fuso: {loja.endereco.fusoIana}.
            </address>
            {loja.responsavelId === usuario?.id ? (
              <p className="pequeno suave">Esta loja é sua: outra pessoa da moderação precisa decidir.</p>
            ) : (
              <div className="linha-acoes">
                {DECISOES[loja.status].map((d) => (
                  <button key={d.decisao} className={`botao botao-pequeno ${d.perigo ? 'botao-perigo' : 'botao-principal'}`}
                    onClick={() => { acao.limparErro(); setAlvo({ loja, ...d }); }}>
                    {d.rotulo}
                  </button>
                ))}
              </div>
            )}
          </li>
        ))}
      </ul>
      {lista.dados && <Paginacao pagina={lista.dados} aoMudar={setPagina} />}

      <Dialogo
        aberto={alvo !== undefined}
        titulo={alvo ? `${alvo.rotulo}: ${alvo.loja.nome}` : ''}
        aoFechar={() => setAlvo(undefined)}
        acoes={(
          <>
            <button className="botao botao-principal" disabled={acao.enviando || !motivo.trim()} onClick={decidir}>{alvo?.rotulo ?? 'Confirmar'}</button>
            <button className="botao botao-secundario" onClick={() => setAlvo(undefined)}>Voltar</button>
          </>
        )}
      >
        <ErroEnvio erro={acao.erro} />
        <CampoTexto rotulo="Motivo" required maxLength={500} value={motivo} onChange={(e) => setMotivo(e.target.value)}
          ajuda="Fica registrado no histórico e é mostrado ao responsável." erro={erroDoCampo(acao.erro, 'motivo')} />
      </Dialogo>
    </section>
  );
}

export function AdminCategorias() {
  const lista = useCarregar(() => admin.categorias(), []);
  const [nome, setNome] = useState('');
  const criar = useAcao();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    if (await criar.executar(() => admin.criarCategoria(nome))) { setNome(''); lista.recarregar(); }
  }

  return (
    <div className="catalogo-grade">
      <section aria-labelledby="titulo-categorias">
        <h2 id="titulo-categorias" className="h-menor">Categorias</h2>
        {lista.carregando && !lista.dados && <Carregando />}
        {lista.erro && <ErroCarga erro={lista.erro} tentarDeNovo={lista.recarregar} />}
        {lista.dados?.conteudo.length === 0 && <Vazio titulo="Nenhuma categoria cadastrada.">Crie a primeira para as lojas classificarem seus produtos.</Vazio>}
        <ul className="pilha">
          {lista.dados?.conteudo.map((c) => <LinhaCategoria key={c.id} categoria={c} aoSalvar={lista.recarregar} />)}
        </ul>
      </section>
      <form className="painel painel-cinza" onSubmit={enviar} aria-labelledby="titulo-nova-categoria">
        <h2 id="titulo-nova-categoria" className="h-menor">Nova categoria</h2>
        <ErroEnvio erro={criar.erro} />
        <Campo rotulo="Nome" required maxLength={100} value={nome} onChange={(e) => setNome(e.target.value)}
          erro={erroDoCampo(criar.erro, 'nome') ?? (criar.erro?.status === 409 ? criar.erro.detalhe : undefined)} />
        <button className="botao botao-principal" disabled={criar.enviando || !nome.trim()}>Criar categoria</button>
      </form>
    </div>
  );
}

function LinhaCategoria({ categoria, aoSalvar }: { categoria: CategoriaAdmin; aoSalvar: () => void }) {
  const [nome, setNome] = useState(categoria.nome);
  const acao = useAcao();
  const salvar = async (ativo: boolean) => { if (await acao.executar(() => admin.atualizarCategoria(categoria.id, nome, ativo))) aoSalvar(); };

  return (
    <li className="linha-categoria">
      <form className="linha-acoes" onSubmit={(e) => { e.preventDefault(); void salvar(categoria.ativo); }}>
        <Campo rotulo="Nome" value={nome} maxLength={100} onChange={(e) => setNome(e.target.value)}
          erro={acao.erro ? (Object.values(acao.erro.campos)[0] ?? acao.erro.detalhe) : undefined} />
        <button className="botao botao-secundario botao-pequeno" disabled={acao.enviando || nome === categoria.nome}>Renomear</button>
        <button type="button" className={`botao botao-pequeno ${categoria.ativo ? 'botao-perigo' : 'botao-principal'}`}
          disabled={acao.enviando} onClick={() => salvar(!categoria.ativo)}>
          {categoria.ativo ? 'Desativar' : 'Reativar'}
        </button>
        <BotaoRemover nome={categoria.nome} explicacao="A categoria sai da plataforma. Só é possível remover quando nenhum produto usa."
          remover={() => admin.removerCategoria(categoria.id)} aoRemover={aoSalvar} />
        {!categoria.ativo && <span className="etiqueta">Desativada</span>}
      </form>
    </li>
  );
}
