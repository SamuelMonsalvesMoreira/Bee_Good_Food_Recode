import type { ReactNode } from 'react';
import type { ApiError } from '../api/client';
import { Bee } from './Bee';

export function Carregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <div className="estado" role="status" aria-live="polite">
      <span className="estado-pulso" aria-hidden="true" />
      <p>{texto}</p>
    </div>
  );
}

export function Vazio({ titulo, children, acao }: { titulo: string; children?: ReactNode; acao?: ReactNode }) {
  return (
    <div className="estado estado-vazio">
      <Bee tamanho={56} humor="pensativa" />
      <h3>{titulo}</h3>
      {children && <div className="suave">{children}</div>}
      {acao}
    </div>
  );
}

export function ErroCarga({ erro, tentarDeNovo }: { erro: ApiError; tentarDeNovo?: () => void }) {
  return (
    <div className="aviso aviso-erro" role="alert">
      <p><strong>{erro.titulo === 'Erro' ? 'Não foi possível carregar.' : erro.titulo}</strong> {erro.detalhe}</p>
      {tentarDeNovo && <button className="botao botao-secundario botao-pequeno" onClick={tentarDeNovo}>Tentar de novo</button>}
    </div>
  );
}

/** Erro de envio de formulario: mensagem geral; os campos mostram seus proprios erros. */
export function ErroEnvio({ erro }: { erro?: ApiError }) {
  if (!erro) return null;
  const temCampos = Object.keys(erro.campos).length > 0;
  return (
    <div className="aviso aviso-erro" role="alert">
      <p>{temCampos ? 'Confira os campos destacados.' : erro.detalhe}</p>
    </div>
  );
}

export function Sucesso({ children }: { children: ReactNode }) {
  return <div className="aviso aviso-sucesso" role="status">{children}</div>;
}
