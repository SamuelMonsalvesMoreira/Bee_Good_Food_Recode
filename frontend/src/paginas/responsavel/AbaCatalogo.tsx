import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { catalogo, gestao } from '../../api/endpoints';
import { BotaoRemover } from '../../componentes/BotaoRemover';
import { Campo, CampoSelecao, erroDoCampo } from '../../componentes/Campo';
import { Carregando, ErroCarga, ErroEnvio, Vazio } from '../../componentes/Estados';
import { useAcao, useCarregar } from '../../lib/useCarregar';

export function AbaCatalogo({ estabelecimentoId, ativa }: { estabelecimentoId: number; ativa: boolean }) {
  const produtos = useCarregar(() => gestao.produtos(estabelecimentoId), [estabelecimentoId]);
  const categorias = useCarregar(() => catalogo.categorias(), []);
  const nomesCategoria = new Map(categorias.dados?.conteudo.map((c) => [c.id, c.nome]));
  const [novo, setNovo] = useState({ categoriaId: '', nome: '', descricao: '' });
  const criar = useAcao();

  async function cadastrar(evento: FormEvent) {
    evento.preventDefault();
    const produto = await criar.executar(() => gestao.criarProduto(estabelecimentoId,
      { categoriaId: Number(novo.categoriaId), nome: novo.nome, descricao: novo.descricao }));
    if (produto) { setNovo({ categoriaId: novo.categoriaId, nome: '', descricao: '' }); produtos.recarregar(); }
  }

  return (
    <div className="catalogo-grade">
      <section aria-labelledby="titulo-produtos">
        <h2 id="titulo-produtos" className="h-menor">Produtos</h2>
        {!ativa && <div className="aviso aviso-info"><p>Com a loja fora do ar, você pode manter produtos, mas não publicar ofertas.</p></div>}
        {produtos.carregando && !produtos.dados && <Carregando />}
        {produtos.erro && <ErroCarga erro={produtos.erro} tentarDeNovo={produtos.recarregar} />}
        {produtos.dados && produtos.dados.conteudo.length === 0 && (
          <Vazio titulo="Nenhum produto ainda.">Cadastre o que costuma sobrar. Depois, crie uma oferta com preço, estoque e prazos.</Vazio>
        )}
        {produtos.dados && produtos.dados.conteudo.length > 0 && (
          <ul className="lista-lojas">
            {produtos.dados.conteudo.map((p) => (
              <li key={p.id} className="linha-pedido">
                <div>
                  <Link className="item-nome" to={`/minhas-lojas/${estabelecimentoId}/produtos/${p.id}`}>{p.nome}</Link>
                  <p className="pequeno suave">{nomesCategoria.get(p.categoriaId) ?? 'Categoria desativada'}</p>
                </div>
                {!p.ativo && <span className="etiqueta">Desativado</span>}
                <div className="linha-acoes">
                  <Link className="botao botao-secundario botao-pequeno" to={`/minhas-lojas/${estabelecimentoId}/produtos/${p.id}`}>
                    Ofertas<span className="sr-only"> de {p.nome}</span>
                  </Link>
                  <BotaoRemover nome={p.nome} explicacao="O produto e as ofertas dele que nunca venderam saem do catálogo."
                    remover={() => gestao.removerProduto(p.id)} aoRemover={produtos.recarregar} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <form className="painel painel-cinza" onSubmit={cadastrar} aria-labelledby="titulo-novo-produto">
        <h2 id="titulo-novo-produto" className="h-menor">Novo produto</h2>
        <ErroEnvio erro={criar.erro} />
        <CampoSelecao rotulo="Categoria" required value={novo.categoriaId} erro={erroDoCampo(criar.erro, 'categoriaId')}
          onChange={(e) => setNovo({ ...novo, categoriaId: e.target.value })}>
          <option value="">Escolha</option>
          {categorias.dados?.conteudo.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </CampoSelecao>
        <Campo rotulo="Nome" required maxLength={160} value={novo.nome} placeholder="Pão de fermentação natural"
          onChange={(e) => setNovo({ ...novo, nome: e.target.value })} erro={erroDoCampo(criar.erro, 'nome')} />
        <Campo rotulo="Descrição (opcional)" maxLength={500} value={novo.descricao}
          onChange={(e) => setNovo({ ...novo, descricao: e.target.value })} erro={erroDoCampo(criar.erro, 'descricao')} />
        <button className="botao botao-principal" disabled={criar.enviando}>Cadastrar produto</button>
      </form>
    </div>
  );
}
