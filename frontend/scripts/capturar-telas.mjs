// Gera as imagens do README a partir do site em execucao com dados de demonstracao.
//
// 1. Popule um banco SO de demonstracao com `npm run demo:popular` (cria .demo/contas.json).
// 2. Rode o site apontando para essa API (ex.: $env:BEE_API_PROXY='http://127.0.0.1:8082'; npx vite --port 5175).
// 3. $env:BEE_SITE='http://localhost:5175'; $env:BEE_ADMIN_EMAIL=...; $env:BEE_ADMIN_SENHA=...; npm run telas
//
// As imagens vao para docs/imagens. Somente dados ficticios aparecem nelas.

import { chromium, devices } from '@playwright/test';
import { mkdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
const SITE = process.env.BEE_SITE ?? 'http://localhost:5175';
const SAIDA = join(raiz, '..', 'docs', 'imagens');
const contas = JSON.parse(readFileSync(join(raiz, '.demo', 'contas.json'), 'utf8'));
const admin = { email: process.env.BEE_ADMIN_EMAIL, senha: process.env.BEE_ADMIN_SENHA };
mkdirSync(SAIDA, { recursive: true });

const navegador = await chromium.launch();
const contexto = await navegador.newContext({ viewport: { width: 1280, height: 860 }, deviceScaleFactor: 1.5, locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' });
const pagina = await contexto.newPage();

async function foto(nome, opcoes = {}) {
  await pagina.waitForLoadState('networkidle');
  await pagina.evaluate(() => document.fonts.ready);
  await pagina.screenshot({ path: join(SAIDA, `${nome}.png`), ...opcoes });
  console.log(`docs/imagens/${nome}.png`);
}

async function entrar(conta) {
  await pagina.goto(`${SITE}/entrar`);
  await pagina.getByLabel('E-mail').fill(conta.email);
  await pagina.getByLabel('Senha').fill(conta.senha);
  await pagina.getByRole('button', { name: 'Entrar', exact: true }).click();
  await pagina.getByRole('button', { name: 'Sair' }).waitFor();
}

async function sair() {
  await pagina.getByRole('button', { name: 'Sair' }).click();
}

// Visitante
await pagina.goto(SITE);
await foto('inicio');
await pagina.getByRole('heading', { name: 'Ofertas disponíveis agora' }).scrollIntoViewIfNeeded();
await pagina.mouse.wheel(0, -120);
await foto('ofertas');
await pagina.getByRole('link', { name: 'Pão de fermentação natural', exact: true }).first().click();
await pagina.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
await pagina.getByText('Adicionado ao carrinho.').waitFor();
await foto('detalhe-oferta');

// Cliente: carrinho e pedido
await entrar(contas.cliente);
await pagina.getByRole('link', { name: /^Carrinho/ }).click();
await foto('carrinho');
await pagina.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Meus pedidos' }).click();
await pagina.getByRole('link', { name: /^Pedido \d+/ }).last().click();
await pagina.getByRole('heading', { level: 1, name: /^Pedido \d+/ }).waitFor();
await foto('pedido');
await sair();

// Responsavel: fila, indicadores e ofertas
await entrar(contas.padaria);
await pagina.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Minhas lojas' }).click();
await pagina.getByRole('link', { name: 'Padaria da Dona Lúcia' }).click();
await pagina.getByLabel('Mostrar').selectOption({ label: 'Todos' });
await pagina.getByRole('table').waitFor();
await foto('painel-pedidos');
await pagina.getByRole('button', { name: 'Indicadores' }).click();
await pagina.getByText('Taxa de retirada').waitFor();
await foto('indicadores');
await pagina.getByRole('button', { name: 'Produtos e ofertas' }).click();
await pagina.getByRole('link', { name: 'Pão francês (saco com 10)', exact: true }).click();
await pagina.getByRole('heading', { name: 'Ofertas deste produto' }).waitFor();
await foto('ofertas-gestao');
await sair();

// Moderacao
await entrar(admin);
await pagina.getByRole('navigation', { name: 'Principal' }).getByRole('link', { name: 'Moderação' }).click();
await pagina.getByRole('button', { name: 'Aprovar' }).first().click();
await pagina.getByRole('dialog').getByLabel('Motivo').fill('Endereço e dados conferidos.');
await foto('moderacao');
await pagina.keyboard.press('Escape');

// Celular
const celular = await navegador.newContext({ ...devices['Pixel 7'], locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' });
const pc = await celular.newPage();
await pc.goto(SITE);
await pc.waitForLoadState('networkidle');
await pc.getByRole('heading', { name: 'Ofertas disponíveis agora' }).scrollIntoViewIfNeeded();
await pc.evaluate(() => document.fonts.ready);
await pc.screenshot({ path: join(SAIDA, 'celular.png') });
console.log('docs/imagens/celular.png');

await navegador.close();
