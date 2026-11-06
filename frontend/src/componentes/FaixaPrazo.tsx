import { minutosAte, nomeFuso, prazo } from '../lib/formato';

interface Props {
  vendaAte: string;
  retiradaAte: string;
  fuso: string;
  compacta?: boolean;
}

/**
 * Elemento-assinatura: dois prazos lado a lado, sempre separados (DEC-018, criterio de interface).
 * O trecho mel vai de agora ate o fim da venda; o trecho folha, do fim da venda ao fim da retirada.
 */
export function FaixaPrazo({ vendaAte, retiradaAte, fuso, compacta = false }: Props) {
  const agora = Date.now();
  const venda = Math.max(new Date(vendaAte).getTime() - agora, 0);
  const retirada = Math.max(new Date(retiradaAte).getTime() - new Date(vendaAte).getTime(), 0);
  const total = venda + retirada || 1;
  // Larguras proporcionais, com minimo para os dois trechos continuarem legiveis.
  const larguraVenda = Math.min(Math.max((venda / total) * 100, 28), 72);
  const restante = minutosAte(vendaAte);

  return (
    <div className={`faixa${compacta ? ' faixa-compacta' : ''}`}>
      <div className="faixa-barra" aria-hidden="true">
        <span className="faixa-venda" style={{ width: `${larguraVenda}%` }} />
        <span className="faixa-retirada" />
      </div>
      <dl className="faixa-rotulos">
        <div>
          <dt>Compre até</dt>
          <dd>
            {prazo(vendaAte, fuso)}
            {restante > 0 && restante <= 90 && <span className="faixa-urgente">faltam {restante} min</span>}
          </dd>
        </div>
        <div className="faixa-direita">
          <dt>Retire até</dt>
          <dd>{prazo(retiradaAte, fuso)}</dd>
        </div>
      </dl>
      {!compacta && <p className="faixa-fuso">Prazos no {nomeFuso(fuso)}, onde fica a loja.</p>}
    </div>
  );
}
