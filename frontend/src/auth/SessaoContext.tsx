import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { aoPerderSessao, definirToken } from '../api/client';
import { auth } from '../api/endpoints';
import type { Usuario } from '../api/types';
import { MODO_DEMO } from '../lib/demo';
import { SessaoContext, type Sessao } from './sessao';

// Sessao somente em memoria (CA18): recarregar a pagina exige novo login; logout limpa token e dados privados.
// Excecao do modo demonstracao: o "token" e ficticio e so vale na API simulada do navegador, entao a sessao
// de demonstracao sobrevive ao recarregar (sessionStorage da aba) para quem avalia o TCC nao perder o login.
const CHAVE_DEMO = 'bee.demo.sessao';

function sessaoDemoSalva(): Usuario | null {
  if (!MODO_DEMO) return null;
  try {
    const salva = JSON.parse(sessionStorage.getItem(CHAVE_DEMO) ?? 'null') as { token: string; usuario: Usuario } | null;
    if (!salva) return null;
    definirToken(salva.token);
    return salva.usuario;
  } catch {
    return null;
  }
}

export function SessaoProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(sessaoDemoSalva);
  const [expirou, setExpirou] = useState(false);
  const temporizador = useRef<number | undefined>(undefined);

  const limpar = useCallback((porExpiracao: boolean) => {
    definirToken(null);
    if (MODO_DEMO) try { sessionStorage.removeItem(CHAVE_DEMO); } catch { /* nada a limpar */ }
    window.clearTimeout(temporizador.current);
    setUsuario(null);
    setExpirou(porExpiracao);
  }, []);

  useEffect(() => {
    aoPerderSessao(() => limpar(true));
    return () => aoPerderSessao(null);
  }, [limpar]);

  const entrar = useCallback(async (email: string, senha: string) => {
    const resposta = await auth.entrar(email, senha);
    definirToken(resposta.accessToken);
    setUsuario(resposta.usuario);
    setExpirou(false);
    window.clearTimeout(temporizador.current);
    if (MODO_DEMO) {
      try { sessionStorage.setItem(CHAVE_DEMO, JSON.stringify({ token: resposta.accessToken, usuario: resposta.usuario })); } catch { /* vale so ate recarregar */ }
      return resposta.usuario;
    }
    // O JWT dura 15 minutos e nao ha refresh: encerrar localmente pouco antes de expirar.
    temporizador.current = window.setTimeout(() => limpar(true), (resposta.expiraEmSegundos - 5) * 1000);
    return resposta.usuario;
  }, [limpar]);

  // Papeis podem mudar no servidor (ex.: aprovacao concede RESPONSAVEL); reconsultar sem novo login.
  const atualizar = useCallback(async () => {
    setUsuario(await auth.eu());
  }, []);

  const valor = useMemo<Sessao>(() => ({
    usuario,
    expirou,
    entrar,
    sair: () => limpar(false),
    atualizar,
    tem: (papel) => usuario?.papeis.includes(papel) ?? false,
  }), [usuario, expirou, entrar, limpar, atualizar]);

  return <SessaoContext.Provider value={valor}>{children}</SessaoContext.Provider>;
}
