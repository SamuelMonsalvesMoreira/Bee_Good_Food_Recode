import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import type { ApiError } from '../api/client';

interface Base {
  rotulo: string;
  ajuda?: ReactNode;
  erro?: string;
}

function Envoltorio({ id, rotulo, ajuda, erro, children }: Base & { id: string; children: ReactNode }) {
  return (
    <div className="campo" data-erro={erro ? 'true' : undefined}>
      <label htmlFor={id}>{rotulo}</label>
      {children}
      {ajuda && <span className="ajuda" id={`${id}-ajuda`}>{ajuda}</span>}
      {erro && <span className="erro" id={`${id}-erro`}>{erro}</span>}
    </div>
  );
}

function descritoPor(id: string, ajuda?: ReactNode, erro?: string) {
  return [ajuda ? `${id}-ajuda` : '', erro ? `${id}-erro` : ''].filter(Boolean).join(' ') || undefined;
}

export function Campo({ rotulo, ajuda, erro, ...input }: Base & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId();
  return (
    <Envoltorio id={id} rotulo={rotulo} ajuda={ajuda} erro={erro}>
      <input id={id} aria-invalid={erro ? true : undefined} aria-describedby={descritoPor(id, ajuda, erro)} {...input} />
    </Envoltorio>
  );
}

export function CampoTexto({ rotulo, ajuda, erro, ...area }: Base & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId();
  return (
    <Envoltorio id={id} rotulo={rotulo} ajuda={ajuda} erro={erro}>
      <textarea id={id} aria-invalid={erro ? true : undefined} aria-describedby={descritoPor(id, ajuda, erro)} {...area} />
    </Envoltorio>
  );
}

export function CampoSelecao({ rotulo, ajuda, erro, children, ...select }: Base & SelectHTMLAttributes<HTMLSelectElement>) {
  const id = useId();
  return (
    <Envoltorio id={id} rotulo={rotulo} ajuda={ajuda} erro={erro}>
      <select id={id} aria-invalid={erro ? true : undefined} aria-describedby={descritoPor(id, ajuda, erro)} {...select}>
        {children}
      </select>
    </Envoltorio>
  );
}

/** Mensagem do campo vinda da API (`campos`), aceitando nomes aninhados como endereco.cidade. */
export function erroDoCampo(erro: ApiError | undefined, ...nomes: string[]): string | undefined {
  if (!erro) return undefined;
  for (const nome of nomes) {
    const mensagem = erro.campos[nome];
    if (mensagem) return mensagem;
  }
  return undefined;
}
