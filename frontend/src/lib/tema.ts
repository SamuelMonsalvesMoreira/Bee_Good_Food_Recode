import { useCallback, useEffect, useState } from 'react';

// Tema claro/escuro. Sem escolha salva, segue o sistema; a escolha fica so neste navegador (conveniencia).
export type Tema = 'claro' | 'escuro';
const CHAVE = 'bee.tema';

function salvo(): Tema | null {
  try {
    const valor = localStorage.getItem(CHAVE);
    return valor === 'claro' || valor === 'escuro' ? valor : null;
  } catch {
    return null;
  }
}

function doSistema(): Tema {
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'escuro' : 'claro';
}

/** Aplica a escolha salva antes da primeira pintura (chamado no main). */
export function aplicarTemaSalvo() {
  const escolha = salvo();
  if (escolha) document.documentElement.dataset.tema = escolha;
}

export function useTema() {
  const [tema, setTema] = useState<Tema>(() => salvo() ?? doSistema());

  useEffect(() => {
    if (salvo()) return;
    const consulta = window.matchMedia?.('(prefers-color-scheme: dark)');
    const mudar = () => setTema(consulta.matches ? 'escuro' : 'claro');
    consulta?.addEventListener?.('change', mudar);
    return () => consulta?.removeEventListener?.('change', mudar);
  }, []);

  const alternar = useCallback(() => {
    setTema((atual) => {
      const novo: Tema = atual === 'escuro' ? 'claro' : 'escuro';
      document.documentElement.dataset.tema = novo;
      try { localStorage.setItem(CHAVE, novo); } catch { /* sem armazenamento: vale so nesta visita */ }
      return novo;
    });
  }, []);

  return { tema, alternar };
}
