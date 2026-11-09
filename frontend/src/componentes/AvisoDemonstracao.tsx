import { FlaskConical } from 'lucide-react';
import { MODO_DEMO } from '../lib/demo';

/** Faixa fixa no modo demonstracao: deixa claro que os dados sao ficticios e nada e cobrado ou entregue. */
export function AvisoDemonstracao() {
  if (!MODO_DEMO) return null;
  return (
    <div className="faixa-demo" role="note">
      <FlaskConical size={16} aria-hidden="true" />
      <span>
        <strong>Demonstração de TCC.</strong> Dados fictícios<span className="faixa-demo-extra">, salvos só neste navegador</span>. Nada é vendido nem cobrado.
      </span>
    </div>
  );
}
