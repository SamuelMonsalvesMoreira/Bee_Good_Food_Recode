import { createContext, useContext } from 'react';
import type { Papel, Usuario } from '../api/types';

// Contexto e hook em modulo proprio: o recarregamento do Vite (Fast Refresh) nao recria o contexto.

export interface Sessao {
  usuario: Usuario | null;
  expirou: boolean;
  entrar: (email: string, senha: string) => Promise<Usuario>;
  sair: () => void;
  atualizar: () => Promise<void>;
  tem: (papel: Papel) => boolean;
}

export const SessaoContext = createContext<Sessao | null>(null);

export function useSessao(): Sessao {
  const contexto = useContext(SessaoContext);
  if (!contexto) throw new Error('useSessao fora do SessaoProvider');
  return contexto;
}
