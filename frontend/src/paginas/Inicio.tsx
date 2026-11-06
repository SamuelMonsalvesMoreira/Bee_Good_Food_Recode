import { BarChart3, Clock, Percent, Search, Store, Timer } from 'lucide-react';
import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { catalogo, type OrdemCatalogo } from '../api/endpoints';
import type { OfertaPublica } from '../api/types';
import { Bee } from '../componentes/Bee';
import { Campo, CampoSelecao } from '../componentes/Campo';
import { ErroCarga, Vazio } from '../componentes/Estados';
import { IconeCategoria } from '../componentes/IconeCategoria';
import { OfertaEsboco, OfertaItem } from '../componentes/OfertaItem';
import { Paginacao } from '../componentes/Paginacao';
import { dinheiro, prazo } from '../lib/formato';
import { useCarregar } from '../lib/useCarregar';

const ORDENS: { valor: OrdemCatalogo; rotulo: string }[] = [
  { valor: 'PRAZO', rotulo: 'Termina antes' },
  { valor: 'DESCONTO', rotulo: 'Maior desconto' },
  { valor: 'PRECO', rotulo: 'Menor preço' },
  { valor: 'RECENTES', rotulo: 'Mais recentes' },
];

export function Inicio() {
  const [params, setParams] = useSearchParams();
  const filtros = {
    texto: params.get('texto') ?? '',
    cidade: params.get('cidade') ?? '',
    bairro: params.get('bairro') ?? '',
    categoriaId: params.get('categoria') ?? '',
    ordem: (ORDENS.find((o) => o.valor === params.get('ordem'))?.valor ?? 'PRAZO') as OrdemCatalogo,
    pagina: Number(params.get('pagina') ?? 0) || 0,
  };
  const [rascunho, setRascunho] = useState(filtros);
  const filtrando = Boolean(filtros.texto || filtros.cidade || filtros.bairro || filtros.categoriaId);
  const vitrine = !filtrando && filtros.pagina === 0;

  const categorias = useCarregar(() => catalogo.categorias(), []);
  const busca = useCarregar(
    (sinal) => catalogo.buscar({
      texto: filtros.texto, cidade: filtros.cidade, bairro: filtros.bairro, ordem: filtros.ordem,
      categoriaId: filtros.categoriaId ? Number(filtros.categoriaId) : undefined, pagina: filtros.pagina, tamanho: 12,
    }, sinal),
    [params.toString()],
  );
  // Vitrine no estilo dos lideres do setor: secoes curtas antes da lista completa.
  const terminando = useCarregar((sinal) => (vitrine ? catalogo.buscar({ ordem: 'PRAZO', tamanho: 6 }, sinal) : Promise.resolve(undefined)), [vitrine]);
  const descontos = useCarregar((sinal) => (vitrine ? catalogo.buscar({ ordem: 'DESCONTO', tamanho: 6 }, sinal) : Promise.resolve(undefined)), [vitrine]);

  function aplicar(novos: URLSearchParams) {
    novos.delete('pagina');
    setParams(novos);
  }

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    const novos = new URLSearchParams();
    if (rascunho.texto.trim()) novos.set('texto', rascunho.texto.trim());
    if (rascunho.cidade.trim()) novos.set('cidade', rascunho.cidade.trim());
    if (rascunho.bairro.trim()) novos.set('bairro', rascunho.bairro.trim());
    if (rascunho.categoriaId) novos.set('categoria', rascunho.categoriaId);
    if (filtros.ordem !== 'PRAZO') novos.set('ordem', filtros.ordem);
    aplicar(novos);
  }

  function filtrarCategoria(id: string) {
    // Filtro rapido: aplica na hora, mantendo os demais filtros ja buscados.
    setRascunho({ ...rascunho, categoriaId: id });
    const novos = new URLSearchParams(params);
    if (id) novos.set('categoria', id); else novos.delete('categoria');
    aplicar(novos);
  }

  function ordenar(ordem: OrdemCatalogo) {
    const novos = new URLSearchParams(params);
    if (ordem === 'PRAZO') novos.delete('ordem'); else novos.set('ordem', ordem);
    aplicar(novos);
  }

  function irParaPagina(pagina: number) {
    const novos = new URLSearchParams(params);
    novos.set('pagina', String(pagina));
    setParams(novos);
  }

  const destaque = vitrine ? descontos.dados?.conteudo[0] ?? busca.dados?.conteudo[0] : undefined;
  const total = busca.dados?.totalElementos;

  return (
    <>
      <section className="abertura" aria-labelledby="titulo-abertura">
        <div className="abertura-texto">
          <h1 id="titulo-abertura">O que sobrou hoje na padaria do bairro, mais barato e pronto para retirar.</h1>
          <p className="abertura-lead">
            Pequenos negócios de comida publicam aqui o excedente do dia com desconto.
            Você reserva pelo site e retira no balcão até o horário indicado. O pagamento é feito na retirada.
          </p>
          <ol className="passos">
            <li><strong>Reserve</strong> as ofertas de uma loja.</li>
            <li><strong>Acompanhe</strong> a loja confirmar e preparar.</li>
            <li><strong>Retire e pague</strong> no balcão, dentro do prazo.</li>
          </ol>
        </div>
        <div className="abertura-vitrine">
          <div className="abertura-bee" aria-hidden="true"><Bee tamanho={150} /></div>
          {destaque && <Destaque oferta={destaque} />}
        </div>
      </section>

      <form className="busca" role="search" aria-label="Buscar ofertas" onSubmit={buscar}>
        <Campo rotulo="O que você procura" placeholder="pão, bolo, salada…" value={rascunho.texto} maxLength={100}
          onChange={(e) => setRascunho({ ...rascunho, texto: e.target.value })} />
        <Campo rotulo="Cidade" placeholder="Santos" value={rascunho.cidade} maxLength={100} autoComplete="address-level2"
          onChange={(e) => setRascunho({ ...rascunho, cidade: e.target.value })} />
        <Campo rotulo="Bairro" placeholder="Gonzaga" value={rascunho.bairro} maxLength={100}
          onChange={(e) => setRascunho({ ...rascunho, bairro: e.target.value })} />
        <CampoSelecao rotulo="Categoria" value={rascunho.categoriaId}
          onChange={(e) => setRascunho({ ...rascunho, categoriaId: e.target.value })}>
          <option value="">Todas</option>
          {categorias.dados?.conteudo.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </CampoSelecao>
        <button className="botao botao-principal busca-botao" type="submit"><Search size={18} aria-hidden="true" />Buscar ofertas</button>
      </form>

      {categorias.dados && categorias.dados.conteudo.length > 0 && (
        <div className="atalhos" role="group" aria-label="Filtrar por categoria">
          <button className="atalho" aria-pressed={!filtros.categoriaId} onClick={() => filtrarCategoria('')}>Todas</button>
          {categorias.dados.conteudo.map((c) => (
            <button key={c.id} className="atalho" aria-pressed={filtros.categoriaId === String(c.id)} onClick={() => filtrarCategoria(String(c.id))}>
              <IconeCategoria slug={c.slug} />{c.nome}
            </button>
          ))}
        </div>
      )}

      {vitrine && (terminando.dados?.conteudo.length ?? 0) > 0 && (
        <Trilho titulo="Terminam logo" icone={<Timer aria-hidden="true" />} descricao="Últimas horas para comprar." ofertas={terminando.dados!.conteudo} />
      )}
      {vitrine && (descontos.dados?.conteudo.length ?? 0) > 0 && (
        <Trilho titulo="Maiores descontos" icone={<Percent aria-hidden="true" />} descricao="Onde a economia é maior agora." ofertas={descontos.dados!.conteudo} />
      )}

      <section aria-labelledby="titulo-resultados" aria-busy={busca.carregando}>
        <div className="resultados-topo">
          <div>
            <h2 id="titulo-resultados">{filtrando ? 'Resultado da busca' : 'Ofertas disponíveis agora'}</h2>
            {total !== undefined && (
              <p className="suave" aria-live="polite">{total === 1 ? '1 oferta' : `${total} ofertas`}</p>
            )}
          </div>
          <CampoSelecao rotulo="Ordenar por" value={filtros.ordem} onChange={(e) => ordenar(e.target.value as OrdemCatalogo)}>
            {ORDENS.map((o) => <option key={o.valor} value={o.valor}>{o.rotulo}</option>)}
          </CampoSelecao>
        </div>
        {busca.carregando && !busca.dados && (
          <div className="ofertas" role="status" aria-label="Procurando ofertas">
            {[0, 1, 2, 3].map((i) => <OfertaEsboco key={i} />)}
          </div>
        )}
        {busca.erro && <ErroCarga erro={busca.erro} tentarDeNovo={busca.recarregar} />}
        {busca.dados && busca.dados.conteudo.length === 0 && (
          <Vazio titulo={filtrando ? 'Nenhuma oferta com esses filtros.' : 'Nenhuma oferta disponível agora.'}>
            {filtrando
              ? 'Tente outra cidade ou bairro, ou remova a categoria. Cidade e bairro aceitam escrita sem acento.'
              : 'As lojas costumam publicar perto do fim do dia. Volte mais tarde.'}
          </Vazio>
        )}
        {busca.dados && busca.dados.conteudo.length > 0 && (
          <>
            <div className="ofertas">
              {busca.dados.conteudo.map((oferta) => <OfertaItem key={oferta.id} oferta={oferta} />)}
            </div>
            <Paginacao pagina={busca.dados} aoMudar={irParaPagina} />
          </>
        )}
      </section>

      <section className="negocios" aria-labelledby="titulo-negocios">
        <div className="negocios-texto">
          <h2 id="titulo-negocios">Tem um negócio de comida? Transforme o excedente do dia em venda.</h2>
          <p>Padarias, hortifrutis, confeitarias e restaurantes publicam ofertas em poucos minutos e recebem os pedidos com o prazo de retirada já combinado.</p>
          <Link className="botao botao-principal" to="/minhas-lojas/nova">Cadastrar minha loja</Link>
        </div>
        <ul className="negocios-lista">
          <li><Store aria-hidden="true" /><span><strong>Sem taxa de entrega</strong> O cliente retira no balcão e paga na hora.</span></li>
          <li><Clock aria-hidden="true" /><span><strong>Prazos claros</strong> Você define até quando vende e até quando o cliente pode retirar.</span></li>
          <li><BarChart3 aria-hidden="true" /><span><strong>Indicadores</strong> Acompanhe pedidos retirados, valor e cancelamentos por período.</span></li>
        </ul>
      </section>
    </>
  );
}

function Destaque({ oferta }: { oferta: OfertaPublica }) {
  return (
    <Link to={`/ofertas/${oferta.id}`} className="destaque">
      <span className="sr-only">Oferta em destaque: </span>
      <IconeCategoria slug={oferta.categoria.slug} />
      <span className="destaque-texto">
        <span className="destaque-nome">{oferta.produtoNome}</span>
        <span className="destaque-loja">{oferta.estabelecimento.nome}</span>
        <span className="destaque-preco">
          <strong>{dinheiro(oferta.precoOferta)}</strong> <s>{dinheiro(oferta.precoOriginal)}</s>
        </span>
        <span className="destaque-prazo"><Clock size={14} aria-hidden="true" /> Retire até {prazo(oferta.retiradaAte, oferta.estabelecimento.fusoIana)}</span>
      </span>
    </Link>
  );
}

/** Secao horizontal da vitrine (rola no celular). */
function Trilho({ titulo, descricao, icone, ofertas }: { titulo: string; descricao: string; icone: ReactNode; ofertas: OfertaPublica[] }) {
  const id = `trilho-${titulo.toLowerCase().replace(/\s+/g, '-')}`;
  return (
    <section className="trilho" aria-labelledby={id}>
      <div className="trilho-topo">
        <span className="trilho-icone">{icone}</span>
        <div>
          <h2 id={id} className="h-menor">{titulo}</h2>
          <p className="suave pequeno">{descricao}</p>
        </div>
      </div>
      <div className="trilho-lista">
        {ofertas.map((oferta) => <OfertaItem key={oferta.id} oferta={oferta} />)}
      </div>
    </section>
  );
}
