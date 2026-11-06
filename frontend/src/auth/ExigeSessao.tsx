import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Papel } from '../api/types';
import { Vazio } from '../componentes/Estados';
import { useSessao } from './sessao';

/**
 * Protege rotas privadas na interface. A autorizacao real e sempre do servidor;
 * aqui so evitamos mostrar telas que a API vai recusar.
 */
export function ExigeSessao({ papel }: { papel?: Papel }) {
  const { usuario, tem } = useSessao();
  const local = useLocation();
  if (!usuario) return <Navigate to="/entrar" replace state={{ voltar: local.pathname }} />;
  if (papel && !tem(papel)) {
    return <Vazio titulo="Esta área não está disponível para sua conta.">Se você acabou de ter uma loja aprovada, saia e entre de novo.</Vazio>;
  }
  return <Outlet />;
}
