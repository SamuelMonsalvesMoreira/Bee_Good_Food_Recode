import { Link } from 'react-router-dom';
import type { OfertaPublica } from '../api/types';
import { desconto, dinheiro } from '../lib/formato';
import { FaixaPrazo } from './FaixaPrazo';

export function OfertaItem({ oferta, mostrarLoja = true }: { oferta: OfertaPublica; mostrarLoja?: boolean }) {
  const pct = desconto(oferta.precoOriginal, oferta.precoOferta);
  const loja = oferta.estabelecimento;
  return (
    <article className="oferta">
      <div className="oferta-topo">
        <div>
          <h3 className="oferta-nome">
            <Link to={`/ofertas/${oferta.id}`} className="oferta-link">{oferta.produtoNome}</Link>
          </h3>
          {mostrarLoja && (
            <p className="oferta-loja">
              <Link to={`/lojas/${loja.id}`}>{loja.nome}</Link>
              <span className="suave">{loja.bairro}, {loja.cidade}</span>
            </p>
          )}
        </div>
        <div className="oferta-preco">
          {pct > 0 && <span className="oferta-desconto">−{pct}%</span>}
          <strong className="numero">{dinheiro(oferta.precoOferta)}</strong>
          <s className="suave numero" aria-label={`Preço original ${dinheiro(oferta.precoOriginal)}`}>{dinheiro(oferta.precoOriginal)}</s>
        </div>
      </div>
      <FaixaPrazo vendaAte={oferta.vendaAte} retiradaAte={oferta.retiradaAte} fuso={loja.fusoIana} compacta />
      <p className="oferta-rodape pequeno">
        <span className="suave">{oferta.categoria.nome}</span>
        <span className={oferta.estoqueDisponivel <= 2 ? 'oferta-pouco' : 'suave'}>
          {oferta.estoqueDisponivel === 1 ? 'Última unidade' : `${oferta.estoqueDisponivel} disponíveis`}
        </span>
      </p>
    </article>
  );
}
