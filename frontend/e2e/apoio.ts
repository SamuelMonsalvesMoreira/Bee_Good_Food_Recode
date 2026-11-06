import { expect, request, type APIRequestContext, type Page } from '@playwright/test';
import { randomBytes, randomUUID } from 'node:crypto';

// Dados de cada teste criados pela API, com nomes unicos: os testes nao dependem uns dos outros.

export const API = process.env.E2E_API ?? 'http://127.0.0.1:8081';

export interface Conta { id: number; nome: string; email: string; senha: string; token: string }

let contexto: APIRequestContext | undefined;
async function api(): Promise<APIRequestContext> {
  contexto ??= await request.newContext({ baseURL: API });
  return contexto;
}

export function unico(prefixo: string): string {
  return `${prefixo} ${randomBytes(3).toString('hex')}`;
}

async function chamar<T>(metodo: 'GET' | 'POST' | 'PUT', caminho: string, token?: string, corpo?: unknown, cabecalhos: Record<string, string> = {}): Promise<T> {
  const r = await (await api()).fetch(caminho, {
    method: metodo,
    headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...cabecalhos },
    data: corpo,
  });
  expect(r.ok(), `${metodo} ${caminho}: ${r.status()} ${await r.text()}`).toBeTruthy();
  const texto = await r.text();
  return (texto ? JSON.parse(texto) : null) as T;
}

export async function criarConta(nome = unico('Pessoa E2E')): Promise<Conta> {
  const email = `e2e-${randomUUID()}@demo.invalid`;
  const senha = `e2e-${randomBytes(9).toString('hex')}`;
  const usuario = await chamar<{ id: number }>('POST', '/api/v1/auth/cadastro', undefined, { nome, email, senha, maiorDeIdade: true });
  return { id: usuario.id, nome, email, senha, token: await token(email, senha) };
}

export async function token(email: string, senha: string): Promise<string> {
  return (await chamar<{ accessToken: string }>('POST', '/api/v1/auth/login', undefined, { email, senha })).accessToken;
}

export async function admin(): Promise<Conta> {
  const email = process.env.BEE_ADMIN_EMAIL;
  const senha = process.env.BEE_ADMIN_SENHA;
  if (!email || !senha) throw new Error('Defina BEE_ADMIN_EMAIL e BEE_ADMIN_SENHA (conta ADMINISTRADOR do banco de testes).');
  return { id: 0, nome: 'Administração', email, senha, token: await token(email, senha) };
}

export async function categoria(nome = 'Padaria'): Promise<number> {
  const adm = await admin();
  const lista = await chamar<{ conteudo: { id: number; nome: string }[] }>('GET', '/api/v1/admin/categorias?tamanho=100', adm.token);
  const achada = lista.conteudo.find((c) => c.nome === nome);
  return achada?.id ?? (await chamar<{ id: number }>('POST', '/api/v1/admin/categorias', adm.token, { nome })).id;
}

export interface Loja { dono: Conta; id: number; nome: string }

export async function solicitarLoja(dono: Conta, nome = unico('Loja E2E'), cidade = 'Santos', bairro = 'Gonzaga'): Promise<Loja> {
  const loja = await chamar<{ id: number }>('POST', '/api/v1/estabelecimentos', dono.token, {
    nome, descricao: 'Loja criada pelos testes automatizados.',
    endereco: { logradouro: 'Rua dos Testes', numero: '10', bairro, cidade, estado: 'SP', cep: '11060-000' },
  });
  return { dono, id: loja.id, nome };
}

export async function lojaAtiva(nome?: string, cidade?: string, bairro?: string): Promise<Loja> {
  const loja = await solicitarLoja(await criarConta(), nome, cidade, bairro);
  await chamar('POST', `/api/v1/admin/estabelecimentos/${loja.id}/aprovar`, (await admin()).token, { motivo: 'Aprovada pelos testes.' });
  return loja;
}

function emHoras(horas: number): string {
  return new Date(Date.now() + horas * 3_600_000).toISOString();
}

export async function situacaoDaLoja(id: number): Promise<string> {
  return (await chamar<{ status: string }>('GET', `/api/v1/estabelecimentos/${id}`, (await admin()).token)).status;
}

export interface Oferta { id: number; produto: string }

export async function ofertaPublicada(loja: Loja, produto = unico('Pão E2E'), estoque = 5, original = 10, preco = 4): Promise<Oferta> {
  const p = await chamar<{ id: number }>('POST', `/api/v1/estabelecimentos/${loja.id}/produtos`, loja.dono.token,
    { categoriaId: await categoria(), nome: produto, descricao: 'Produto dos testes.' });
  const o = await chamar<{ id: number }>('POST', `/api/v1/produtos/${p.id}/ofertas`, loja.dono.token,
    { precoOriginal: original, precoOferta: preco, estoque, inicioVenda: emHoras(-1), vendaAte: emHoras(4), retiradaAte: emHoras(6) });
  await chamar('POST', `/api/v1/ofertas/${o.id}/publicar`, loja.dono.token);
  return { id: o.id, produto };
}

export async function pedidoPelaApi(cliente: Conta, ofertaId: number, quantidade = 1): Promise<number> {
  const pedido = await chamar<{ id: number }>('POST', '/api/v1/pedidos', cliente.token,
    { formaPagamento: 'PIX', itens: [{ ofertaId, quantidade }] }, { 'Idempotency-Key': `e2e-${randomUUID()}` });
  return pedido.id;
}

/** Login pela interface. Recarregar a pagina encerra a sessao (token so em memoria). */
export async function entrar(page: Page, conta: Conta) {
  await page.goto('/entrar');
  await page.getByLabel('E-mail').fill(conta.email);
  await page.getByLabel('Senha').fill(conta.senha);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Sair' })).toBeVisible();
}

/** Navegacao interna (sem recarregar), preservando a sessao em memoria. */
export async function irPeloMenu(page: Page, link: string) {
  await page.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: link }).click();
}
