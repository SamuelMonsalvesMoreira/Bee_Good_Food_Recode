import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { catalogo } from '../api/endpoints';
import { Bee } from '../componentes/Bee';
import { Campo, CampoSelecao } from '../componentes/Campo';
import { Carregando, ErroCarga, Vazio } from '../componentes/Estados';
import { OfertaItem } from '../componentes/OfertaItem';
import { Paginacao } from '../componentes/Paginacao';
import { useCarregar } from '../lib/useCarregar';

export function Inicio() {
  const [params, setParams] = useSearchParams();
  const filtros = {
    texto: params.get('texto') ?? '',
    cidade: params.get('cidade') ?? '',
    bairro: params.get('bairro') ?? '',
    categoriaId: params.get('categoria') ?? '',
    pagina: Number(params.get('pagina') ?? 0) || 0,
  };
  const [rascunho, setRascunho] = useState(filtros);

  const categorias = useCarregar(() => catalogo.categorias(), []);
  const busca = useCarregar(
    (sinal) => catalogo.buscar({
      texto: filtros.texto, cidade: filtros.cidade, bairro: filtros.bairro,
      categoriaId: filtros.categoriaId ? Number(filtros.categoriaId) : undefined, pagina: filtros.pagina, tamanho: 12,
    }, sinal),
    [params.toString()],
  );

  function buscar(evento: FormEvent) {
    evento.preventDefault();
    const novos = new URLSearchParams();
    if (rascunho.texto.trim()) novos.set('texto', rascunho.texto.trim());
    if (rascunho.cidade.trim()) novos.set('cidade', rascunho.cidade.trim());
    if (rascunho.bairro.trim()) novos.set('bairro', rascunho.bairro.trim());
    if (rascunho.categoriaId) novos.set('categoria', rascunho.categoriaId);
    setParams(novos);
  }

  function irParaPagina(pagina: number) {
    const novos = new URLSearchParams(params);
    novos.set('pagina', String(pagina));
    setParams(novos);
  }

  const filtrando = Boolean(filtros.texto || filtros.cidade || filtros.bairro || filtros.categoriaId);

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
        <div className="abertura-bee" aria-hidden="true"><Bee tamanho={170} /></div>
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
        <button className="botao botao-principal busca-botao" type="submit">Buscar ofertas</button>
      </form>

      <section aria-labelledby="titulo-resultados" aria-busy={busca.carregando}>
        <div className="resultados-topo">
          <h2 id="titulo-resultados">{filtrando ? 'Resultado da busca' : 'Ofertas disponíveis agora'}</h2>
          {busca.dados && (
            <p className="suave" aria-live="polite">
              {busca.dados.totalElementos === 1 ? '1 oferta' : `${busca.dados.totalElementos} ofertas`}
            </p>
          )}
        </div>
        {busca.carregando && !busca.dados && <Carregando texto="Procurando ofertas…" />}
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
    </>
  );
}
