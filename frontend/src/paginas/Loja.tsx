import { Link, useParams } from 'react-router-dom';
import { catalogo } from '../api/endpoints';
import { Carregando, ErroCarga, Vazio } from '../componentes/Estados';
import { OfertaItem } from '../componentes/OfertaItem';
import { useCarregar } from '../lib/useCarregar';

/** Ofertas de uma loja. Sem rota publica de estabelecimento: os dados vem das proprias ofertas. */
export function Loja() {
  const id = Number(useParams().id);
  const { dados, erro, carregando, recarregar } = useCarregar(
    (sinal) => catalogo.buscar({ estabelecimentoId: id, tamanho: 100 }, sinal), [id]);

  if (carregando && !dados) return <Carregando />;
  if (erro) return <ErroCarga erro={erro} tentarDeNovo={recarregar} />;
  const loja = dados?.conteudo[0]?.estabelecimento;
  if (!loja) {
    return (
      <Vazio titulo="Esta loja não tem ofertas disponíveis agora." acao={<Link className="botao botao-principal" to="/">Ver todas as ofertas</Link>}>
        Volte mais tarde: as ofertas costumam ser publicadas perto do fim do dia.
      </Vazio>
    );
  }
  return (
    <>
      <p className="trilha"><Link to="/">Ofertas</Link> / {loja.nome}</p>
      <header className="loja-topo">
        <h1>{loja.nome}</h1>
        <address className="suave">
          {loja.logradouro}, {loja.numero}{loja.complemento ? `, ${loja.complemento}` : ''}, {loja.bairro}, {loja.cidade} ({loja.estado})
        </address>
      </header>
      <h2 className="h-menor">{dados.totalElementos === 1 ? '1 oferta disponível' : `${dados.totalElementos} ofertas disponíveis`}</h2>
      <div className="ofertas">
        {dados.conteudo.map((oferta) => <OfertaItem key={oferta.id} oferta={oferta} mostrarLoja={false} />)}
      </div>
    </>
  );
}
