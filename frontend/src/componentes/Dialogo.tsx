import { useEffect, useRef, type ReactNode } from 'react';

interface Props {
  aberto: boolean;
  titulo: string;
  children: ReactNode;
  aoFechar: () => void;
  acoes: ReactNode;
}

/** Dialogo modal nativo: foco preso, Esc fecha e o foco volta para quem abriu. */
export function Dialogo({ aberto, titulo, children, aoFechar, acoes }: Props) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialogo = ref.current;
    if (!dialogo) return;
    if (aberto && !dialogo.open) dialogo.showModal?.();
    if (!aberto && dialogo.open) dialogo.close?.();
  }, [aberto]);

  return (
    <dialog ref={ref} className="dialogo" aria-labelledby="dialogo-titulo" onClose={aoFechar} onCancel={aoFechar}>
      <h2 id="dialogo-titulo">{titulo}</h2>
      <div className="dialogo-corpo">{children}</div>
      <div className="linha-acoes dialogo-acoes">{acoes}</div>
    </dialog>
  );
}
