// Cliente HTTP unico. O token vive somente em memoria (DEC: sem refresh token, sem localStorage).

const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly titulo: string,
    readonly detalhe: string,
    readonly campos: Record<string, string> = {},
  ) {
    super(detalhe);
  }
}

let tokenAtual: string | null = null;
let aoExpirar: (() => void) | null = null;

export function definirToken(token: string | null) {
  tokenAtual = token;
}

/** Chamado quando a API responde 401 com sessao ativa: a interface limpa a sessao e pede novo login. */
export function aoPerderSessao(callback: (() => void) | null) {
  aoExpirar = callback;
}

interface Opcoes {
  metodo?: 'GET' | 'POST' | 'PUT';
  corpo?: unknown;
  cabecalhos?: Record<string, string>;
  sinal?: AbortSignal;
}

export interface Resposta<T> {
  dados: T;
  status: number;
  cabecalhos: Headers;
}

export async function requisitarCompleto<T>(caminho: string, opcoes: Opcoes = {}): Promise<Resposta<T>> {
  const cabecalhos: Record<string, string> = { Accept: 'application/json', ...opcoes.cabecalhos };
  if (opcoes.corpo !== undefined) cabecalhos['Content-Type'] = 'application/json';
  const tinhaSessao = tokenAtual !== null;
  if (tokenAtual) cabecalhos.Authorization = `Bearer ${tokenAtual}`;

  let resposta: Response;
  try {
    resposta = await fetch(BASE + caminho, {
      method: opcoes.metodo ?? 'GET',
      headers: cabecalhos,
      body: opcoes.corpo === undefined ? undefined : JSON.stringify(opcoes.corpo),
      signal: opcoes.sinal,
    });
  } catch (erro) {
    if ((erro as Error).name === 'AbortError') throw erro;
    throw new ApiError(0, 'Sem conexão', 'Não foi possível falar com o servidor. Confira sua conexão e tente de novo.');
  }

  const texto = await resposta.text();
  const corpo = texto ? safeJson(texto) : null;
  if (!resposta.ok) {
    if (resposta.status === 401 && tinhaSessao && aoExpirar) aoExpirar();
    const problema = (corpo ?? {}) as { title?: string; detail?: string; campos?: Record<string, string> };
    throw new ApiError(
      resposta.status,
      problema.title ?? 'Erro',
      problema.detail ?? mensagemPadrao(resposta.status),
      problema.campos ?? {},
    );
  }
  return { dados: corpo as T, status: resposta.status, cabecalhos: resposta.headers };
}

export async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  return (await requisitarCompleto<T>(caminho, opcoes)).dados;
}

function safeJson(texto: string): unknown {
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

function mensagemPadrao(status: number): string {
  if (status === 401) return 'Sua sessão terminou. Entre novamente.';
  if (status === 403) return 'Você não tem permissão para esta ação.';
  if (status === 404) return 'Não encontramos o que você procurava.';
  if (status === 429) return 'Muitas tentativas. Aguarde um minuto.';
  return 'Não foi possível concluir agora. Tente de novo.';
}

/** Monta query string ignorando valores vazios. */
export function query(params: Record<string, string | number | undefined | null>): string {
  const busca = new URLSearchParams();
  for (const [chave, valor] of Object.entries(params)) {
    if (valor !== undefined && valor !== null && valor !== '') busca.set(chave, String(valor));
  }
  const texto = busca.toString();
  return texto ? `?${texto}` : '';
}
