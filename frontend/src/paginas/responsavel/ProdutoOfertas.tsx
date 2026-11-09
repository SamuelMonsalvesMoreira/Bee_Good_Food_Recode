import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { catalogo, estabelecimentos, gestao, type OfertaForm } from '../../api/endpoints';
import type { Oferta, Produto } from '../../api/types';
import { BotaoRemover } from '../../componentes/BotaoRemover';
import { Campo, CampoSelecao, erroDoCampo } from '../../componentes/Campo';
import { Carregando, ErroCarga, ErroEnvio, Sucesso, Vazio } from '../../componentes/Estados';
import { FaixaPrazo } from '../../componentes/FaixaPrazo';
import { StatusOfertaEtiqueta } from '../../componentes/StatusLoja';
import { dinheiro, isoParaLocal, localParaIso, nomeFuso } from '../../lib/formato';
import { useAcao, useCarregar } from '../../lib/useCarregar';

export function ProdutoOfertas() {
  const estabelecimentoId = Number(useParams().id);
  const produtoId = Number(useParams().produtoId);
  const loja = useCarregar(() => estabelecimentos.detalhe(estabelecimentoId), [estabelecimentoId]);
  const produto = useCarregar(() => gestao.produto(produtoId), [produtoId]);
  const ofertas = useCarregar(() => gestao.ofertas(produtoId), [produtoId]);

  if ((loja.carregando && !loja.dados) || (produto.carregando && !produto.dados)) return <Carregando />;
  const erro = loja.erro ?? produto.erro;
  if (erro) return <ErroCarga erro={erro} />;
  if (!loja.dados || !produto.dados) return null;
  const fuso = loja.dados.endereco.fusoIana;
  const ativa = loja.dados.status === 'ATIVO';

  return (
    <>
      <p className="trilha">
        <Link to="/minhas-lojas">Minhas lojas</Link> / <Link to={`/minhas-lojas/${estabelecimentoId}?aba=catalogo`}>{loja.dados.nome}</Link> / {produto.dados.nome}
      </p>
      <h1>{produto.dados.nome}</h1>
      <div className="catalogo-grade">
        <section aria-labelledby="titulo-ofertas" className="pilha">
          <h2 id="titulo-ofertas" className="h-menor">Ofertas deste produto</h2>
          <p className="pequeno suave">
            Uma oferta por vez pode estar publicada ou pausada. Depois de publicar, preço e prazos não mudam: encerre e crie outra.
          </p>
          {ofertas.carregando && !ofertas.dados && <Carregando />}
          {ofertas.erro && <ErroCarga erro={ofertas.erro} tentarDeNovo={ofertas.recarregar} />}
          {ofertas.dados?.conteudo.length === 0 && <Vazio titulo="Nenhuma oferta criada.">Crie a primeira ao lado.</Vazio>}
          {ofertas.dados?.conteudo.map((oferta) => (
            <CartaoOferta key={oferta.id} oferta={oferta} fuso={fuso} lojaAtiva={ativa} aoMudar={ofertas.recarregar} />
          ))}
        </section>
        <div className="pilha">
          {ativa
            ? <NovaOferta produtoId={produtoId} fuso={fuso} aoCriar={ofertas.recarregar} />
            : <div className="aviso aviso-info"><p>A loja precisa estar ativa para criar e publicar ofertas.</p></div>}
          <EditarProduto produto={produto.dados} aoSalvar={produto.recarregar} />
        </div>
      </div>
    </>
  );
}

function CartaoOferta({ oferta, fuso, lojaAtiva, aoMudar }: { oferta: Oferta; fuso: string; lojaAtiva: boolean; aoMudar: () => void }) {
  const acao = useAcao();
  const [ajuste, setAjuste] = useState('');
  const executar = async (fazer: () => Promise<unknown>) => { if (await acao.executar(fazer)) { setAjuste(''); aoMudar(); } };
  const s = oferta.status;

  return (
    <article className="painel oferta-gestao">
      <div className="titulo-com-acao">
        <div>
          <strong className="numero">{dinheiro(oferta.precoOferta)}</strong>{' '}
          <s className="suave numero">{dinheiro(oferta.precoOriginal)}</s>
          <p className="pequeno">Estoque: <strong className="numero">{oferta.estoqueDisponivel}</strong></p>
        </div>
        <StatusOfertaEtiqueta status={s} />
      </div>
      <FaixaPrazo vendaAte={oferta.vendaAte} retiradaAte={oferta.retiradaAte} fuso={fuso} compacta />
      {acao.erro && <div className="aviso aviso-erro" role="alert"><p>{Object.values(acao.erro.campos)[0] ?? acao.erro.detalhe}</p></div>}
      <div className="linha-acoes">
        {s === 'RASCUNHO' && lojaAtiva && <button className="botao botao-principal botao-pequeno" disabled={acao.enviando} onClick={() => executar(() => gestao.acaoOferta(oferta.id, 'publicar'))}>Publicar</button>}
        {s === 'PUBLICADA' && <button className="botao botao-secundario botao-pequeno" disabled={acao.enviando} onClick={() => executar(() => gestao.acaoOferta(oferta.id, 'pausar'))}>Pausar</button>}
        {s === 'PAUSADA' && lojaAtiva && <button className="botao botao-principal botao-pequeno" disabled={acao.enviando} onClick={() => executar(() => gestao.acaoOferta(oferta.id, 'retomar'))}>Retomar</button>}
        {s !== 'ENCERRADA' && <button className="botao botao-perigo botao-pequeno" disabled={acao.enviando} onClick={() => executar(() => gestao.acaoOferta(oferta.id, 'encerrar'))}>Encerrar</button>}
        {(s === 'RASCUNHO' || s === 'ENCERRADA') && (
          <BotaoRemover nome={`oferta de ${dinheiro(oferta.precoOferta)}`} explicacao="A oferta sai da sua lista. Se já teve pedidos, ela fica no histórico e não pode ser removida."
            remover={() => gestao.removerOferta(oferta.id)} aoRemover={aoMudar} />
        )}
      </div>
      {s !== 'ENCERRADA' && (
        <form className="ajuste" onSubmit={(e) => { e.preventDefault(); void executar(() => gestao.ajustarEstoque(oferta.id, Number(ajuste))); }}>
          <Campo rotulo="Ajustar estoque" type="number" inputMode="numeric" step={1} min={-oferta.estoqueDisponivel} max={100000}
            value={ajuste} onChange={(e) => setAjuste(e.target.value)} ajuda="Use números negativos para retirar unidades." />
          <button className="botao botao-secundario botao-pequeno" disabled={acao.enviando || !ajuste || Number(ajuste) === 0}>Aplicar</button>
        </form>
      )}
    </article>
  );
}

function NovaOferta({ produtoId, fuso, aoCriar }: { produtoId: number; fuso: string; aoCriar: () => void }) {
  const agora = new Date();
  const horas = (h: number) => isoParaLocal(new Date(agora.getTime() + h * 3_600_000), fuso);
  const [dados, setDados] = useState({ precoOriginal: '', precoOferta: '', estoque: '', inicioVenda: horas(0), vendaAte: horas(3), retiradaAte: horas(5) });
  const [criada, setCriada] = useState(false);
  const acao = useAcao();
  const muda = (campo: keyof typeof dados) => (e: { target: { value: string } }) => { setCriada(false); setDados({ ...dados, [campo]: e.target.value }); };

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const corpo: OfertaForm = {
      precoOriginal: Number(dados.precoOriginal.replace(',', '.')),
      precoOferta: Number(dados.precoOferta.replace(',', '.')),
      estoque: Number(dados.estoque),
      // Horarios digitados no fuso da loja, enviados com deslocamento explicito.
      inicioVenda: localParaIso(dados.inicioVenda, fuso),
      vendaAte: localParaIso(dados.vendaAte, fuso),
      retiradaAte: localParaIso(dados.retiradaAte, fuso),
    };
    if (await acao.executar(() => gestao.criarOferta(produtoId, corpo))) { setCriada(true); aoCriar(); }
  }

  return (
    <form className="painel painel-cinza" onSubmit={enviar} noValidate aria-labelledby="titulo-nova-oferta">
      <h2 id="titulo-nova-oferta" className="h-menor">Nova oferta</h2>
      {criada && <Sucesso><p>Oferta criada como rascunho. Confira e publique.</p></Sucesso>}
      <ErroEnvio erro={acao.erro} />
      <div className="grade-campos">
        <Campo rotulo="Preço original (R$)" inputMode="decimal" required value={dados.precoOriginal} onChange={muda('precoOriginal')} erro={erroDoCampo(acao.erro, 'precoOriginal')} />
        <Campo rotulo="Preço da oferta (R$)" inputMode="decimal" required value={dados.precoOferta} onChange={muda('precoOferta')} erro={erroDoCampo(acao.erro, 'precoOferta')} />
        <Campo rotulo="Estoque" type="number" inputMode="numeric" min={0} max={100000} required value={dados.estoque} onChange={muda('estoque')} erro={erroDoCampo(acao.erro, 'estoque')} />
      </div>
      <p className="pequeno suave">Prazos no {nomeFuso(fuso)}.</p>
      <Campo rotulo="Início da venda" type="datetime-local" required value={dados.inicioVenda} onChange={muda('inicioVenda')} erro={erroDoCampo(acao.erro, 'inicioVenda')} />
      <Campo rotulo="Compre até (fim da venda)" type="datetime-local" required value={dados.vendaAte} onChange={muda('vendaAte')} erro={erroDoCampo(acao.erro, 'vendaAte')} />
      <Campo rotulo="Retire até" type="datetime-local" required value={dados.retiradaAte} onChange={muda('retiradaAte')}
        ajuda="Pode ser igual ou depois do fim da venda." erro={erroDoCampo(acao.erro, 'retiradaAte')} />
      <button className="botao botao-principal" disabled={acao.enviando}>Criar rascunho</button>
    </form>
  );
}

function EditarProduto({ produto, aoSalvar }: { produto: Produto; aoSalvar: () => void }) {
  const categorias = useCarregar(() => catalogo.categorias(), []);
  const [dados, setDados] = useState({ categoriaId: String(produto.categoriaId), nome: produto.nome, descricao: produto.descricao ?? '', ativo: produto.ativo });
  const [salvo, setSalvo] = useState(false);
  const acao = useAcao();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const ok = await acao.executar(() => gestao.atualizarProduto(produto.id, { ...dados, categoriaId: Number(dados.categoriaId) }));
    if (ok) { setSalvo(true); aoSalvar(); }
  }

  return (
    <form className="painel" onSubmit={enviar} aria-labelledby="titulo-editar-produto">
      <h2 id="titulo-editar-produto" className="h-menor">Dados do produto</h2>
      {salvo && <Sucesso><p>Produto salvo. Pedidos já feitos mantêm o nome antigo.</p></Sucesso>}
      <ErroEnvio erro={acao.erro} />
      <Campo rotulo="Nome" maxLength={160} value={dados.nome} onChange={(e) => setDados({ ...dados, nome: e.target.value })} erro={erroDoCampo(acao.erro, 'nome')} />
      <CampoSelecao rotulo="Categoria" value={dados.categoriaId} onChange={(e) => setDados({ ...dados, categoriaId: e.target.value })} erro={erroDoCampo(acao.erro, 'categoriaId')}>
        {!categorias.dados?.conteudo.some((c) => String(c.id) === dados.categoriaId) && <option value={dados.categoriaId}>Categoria atual (desativada)</option>}
        {categorias.dados?.conteudo.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
      </CampoSelecao>
      <Campo rotulo="Descrição" maxLength={500} value={dados.descricao} onChange={(e) => setDados({ ...dados, descricao: e.target.value })} />
      <label className="marcar campo">
        <input type="checkbox" checked={dados.ativo} onChange={(e) => setDados({ ...dados, ativo: e.target.checked })} />
        <span>Produto ativo (desativado, suas ofertas saem da busca)</span>
      </label>
      <button className="botao botao-secundario" disabled={acao.enviando}>Salvar produto</button>
    </form>
  );
}
