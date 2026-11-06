import { useCallback, useEffect, useRef, useState, type DependencyList } from 'react';
import { ApiError } from '../api/client';

export interface Carga<T> {
  dados: T | undefined;
  erro: ApiError | undefined;
  carregando: boolean;
  recarregar: () => void;
}

/** Busca dados ao montar e quando as dependencias mudam; descarta respostas de buscas antigas. */
export function useCarregar<T>(buscar: (sinal: AbortSignal) => Promise<T>, deps: DependencyList): Carga<T> {
  const [dados, setDados] = useState<T>();
  const [erro, setErro] = useState<ApiError>();
  const [carregando, setCarregando] = useState(true);
  const [versao, setVersao] = useState(0);
  const buscarRef = useRef(buscar);
  buscarRef.current = buscar;

  useEffect(() => {
    const controle = new AbortController();
    setCarregando(true);
    setErro(undefined);
    buscarRef.current(controle.signal)
      .then((resultado) => { if (!controle.signal.aborted) setDados(resultado); })
      .catch((e: unknown) => {
        if (controle.signal.aborted || (e as Error).name === 'AbortError') return;
        setErro(e instanceof ApiError ? e : new ApiError(0, 'Erro', 'Não foi possível carregar.'));
      })
      .finally(() => { if (!controle.signal.aborted) setCarregando(false); });
    return () => controle.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, versao]);

  const recarregar = useCallback(() => setVersao((v) => v + 1), []);
  return { dados, erro, carregando, recarregar };
}

/** Executa uma acao (POST/PUT) guardando o estado de envio e o erro para exibir no formulario. */
export function useAcao() {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<ApiError>();
  const executar = useCallback(async <T,>(acao: () => Promise<T>): Promise<T | undefined> => {
    setEnviando(true);
    setErro(undefined);
    try {
      return await acao();
    } catch (e) {
      setErro(e instanceof ApiError ? e : new ApiError(0, 'Erro', 'Não foi possível concluir agora. Tente de novo.'));
      return undefined;
    } finally {
      setEnviando(false);
    }
  }, []);
  return { enviando, erro, executar, limparErro: () => setErro(undefined) };
}
