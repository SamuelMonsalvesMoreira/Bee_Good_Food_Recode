import { Link, useParams } from 'react-router-dom';
import { catalogo } from '../api/endpoints';
import { AdicionarAoCarrinho } from '../componentes/AdicionarAoCarrinho';
import { Carregando, ErroCarga, Vazio } from '../componentes/Estados';
import { FaixaPrazo } from '../componentes/FaixaPrazo';
import { desconto, dinheiro } from '../lib/formato';
import { useCarregar } from '../lib/useCarregar';

export function OfertaDetalhe() {
  const id = Number(useParams().id);
  const { dados: oferta, erro, carregando, recarregar } = useCarregar(() => catalogo.oferta(id), [id]);

  if (carregando && !oferta) return <Carregando />;
  if (erro?.status === 404) {
    return (
      <Vazio titulo="Esta oferta não está mais disponível." acao={<Link className="botao botao-principal" to="/">Ver outras ofertas</Link>}>
        Ela pode ter esgotado, saído do prazo de venda ou sido encerrada pela loja.
      </Vazio>
    );
  }
  if (erro) return <ErroCarga erro={erro} tentarDeNovo={recarregar} />;
  if (!oferta) return null;

  const loja = oferta.estabelecimento;
  const pct = desconto(oferta.precoOriginal, oferta.precoOferta);
  return (
    <article className="detalhe">
      <p className="trilha"><Link to="/">Ofertas</Link> / <Link to={`/lojas/${loja.id}`}>{loja.nome}</Link></p>
      <div className="detalhe-grade">
        <div>
          <p className="etiqueta">{oferta.categoria.nome}</p>
          <h1>{oferta.produtoNome}</h1>
          {oferta.produtoDescricao && <p className="detalhe-descricao">{oferta.produtoDescricao}</p>}
          <div className="detalhe-preco">
            <strong className="numero">{dinheiro(oferta.precoOferta)}</strong>
            <span className="suave">
              de <s className="numero">{dinheiro(oferta.precoOriginal)}</s>{pct > 0 && <> ({pct}% de desconto)</>}
            </span>
          </div>
          <FaixaPrazo vendaAte={oferta.vendaAte} retiradaAte={oferta.retiradaAte} fuso={loja.fusoIana} />
        </div>
        <aside className="painel painel-cinza detalhe-lateral" aria-label="Reservar">
          <p className="pequeno suave">
            {oferta.estoqueDisponivel === 1 ? 'Última unidade disponível' : `${oferta.estoqueDisponivel} unidades disponíveis`}
          </p>
          <AdicionarAoCarrinho oferta={oferta} />
          <hr className="divisor" />
          <h2 className="h-menor">Onde retirar</h2>
          <address>
            <strong>{loja.nome}</strong><br />
            {loja.logradouro}, {loja.numero}{loja.complemento ? `, ${loja.complemento}` : ''}<br />
            {loja.bairro}, {loja.cidade} ({loja.estado})
          </address>
          <p className="pequeno suave">Pagamento na retirada: Pix, dinheiro ou cartão, como você escolher no pedido.</p>
        </aside>
      </div>
    </article>
  );
}
