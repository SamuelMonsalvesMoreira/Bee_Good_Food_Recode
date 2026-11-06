// Popula um banco de DEMONSTRACAO pela propria API (Node 20+, sem dependencias).
//
// Pre-requisitos: API em execucao e uma conta ADMINISTRADOR criada pelo procedimento controlado
// de docs/planejamento/autenticacao.md (o projeto nao tem administrador nem senha padrao).
//
//   $env:BEE_ADMIN_EMAIL = 'admin@exemplo.invalid'
//   $env:BEE_ADMIN_SENHA = Read-Host 'Senha do administrador' -MaskInput
//   npm run demo:popular
//
// As contas ficticias criadas recebem senhas aleatorias, gravadas somente em frontend/.demo/contas.json
// (fora do Git). Nunca usar em producao: cria lojas, ofertas e pedidos ficticios.

import { mkdirSync, writeFileSync } from 'node:fs';
import { randomBytes, randomUUID } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const API = (process.env.BEE_API ?? 'http://127.0.0.1:8080').replace(/\/$/, '');
const ADMIN_EMAIL = process.env.BEE_ADMIN_EMAIL;
const ADMIN_SENHA = process.env.BEE_ADMIN_SENHA;
const SAIDA = join(dirname(fileURLToPath(import.meta.url)), '..', '.demo', 'contas.json');

if (!ADMIN_EMAIL || !ADMIN_SENHA) {
  console.error('Defina BEE_ADMIN_EMAIL e BEE_ADMIN_SENHA de uma conta ADMINISTRADOR (veja docs/planejamento/autenticacao.md).');
  process.exit(1);
}

async function chamar(metodo, caminho, { token, corpo, cabecalhos } = {}) {
  const resposta = await fetch(API + caminho, {
    method: metodo,
    headers: {
      ...(corpo !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...cabecalhos,
    },
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });
  const texto = await resposta.text();
  const dados = texto ? JSON.parse(texto) : null;
  if (!resposta.ok) throw new Error(`${metodo} ${caminho} respondeu ${resposta.status}: ${dados?.detail ?? texto}`);
  return dados;
}

const sufixo = randomBytes(3).toString('hex');
const contas = {};

async function criarConta(chave, nome) {
  const email = `${chave}-${sufixo}@demo.invalid`;
  const senha = `demo-${randomBytes(9).toString('base64url')}`;
  await chamar('POST', '/api/v1/auth/cadastro', { corpo: { nome, email, senha, maiorDeIdade: true } });
  const { accessToken } = await chamar('POST', '/api/v1/auth/login', { corpo: { email, senha } });
  contas[chave] = { nome, email, senha };
  return accessToken;
}

/** Horario ISO com deslocamento de Sao Paulo, a partir de agora. */
function emHoras(horas) {
  const instante = new Date(Date.now() + horas * 3_600_000);
  const local = new Date(instante.getTime() - 3 * 3_600_000).toISOString().slice(0, 16);
  return `${local}:00-03:00`;
}

const LOJAS = [
  {
    chave: 'padaria', pessoa: 'Lúcia (demonstração)', nome: 'Padaria da Dona Lúcia', descricao: 'Pães de fermentação natural e bolos caseiros.',
    endereco: { logradouro: 'Av. Ana Costa', numero: '412', bairro: 'Gonzaga', cidade: 'Santos', estado: 'SP', cep: '11060-002' },
    produtos: [
      ['Padaria', 'Pão de fermentação natural', 'Pão de 600 g assado hoje cedo.', 24, 12, 6, 6, 8],
      ['Padaria', 'Pão francês (saco com 10)', null, 9.5, 4.5, 12, 4, 6],
      ['Confeitaria', 'Bolo de cenoura com chocolate', 'Bolo inteiro, 1,2 kg.', 48, 26, 2, 7, 9],
    ],
  },
  {
    chave: 'hortifruti', pessoa: 'Antônio (demonstração)', nome: 'Hortifruti Boqueirão', descricao: 'Frutas e verduras da feira do dia.',
    endereco: { logradouro: 'Rua Oswaldo Cruz', numero: '88', bairro: 'Boqueirão', cidade: 'Santos', estado: 'SP', cep: '11045-100' },
    produtos: [
      ['Hortifruti', 'Cesta de frutas maduras', 'Banana, mamão e manga prontos para consumo.', 30, 14, 5, 8, 10],
      ['Hortifruti', 'Kit salada', 'Alface, tomate e pepino.', 15, 7, 8, 5, 6],
    ],
  },
  {
    chave: 'emporio', pessoa: 'Marina (demonstração)', nome: 'Empório Pinheiros', descricao: 'Marmitas e refeições do almoço.',
    endereco: { logradouro: 'Rua dos Pinheiros', numero: '1020', bairro: 'Pinheiros', cidade: 'São Paulo', estado: 'SP', cep: '05422-001' },
    produtos: [
      ['Refeições', 'Marmita de frango com legumes', 'Porção de 450 g, preparada hoje.', 32, 16, 7, 3, 5],
      ['Confeitaria', 'Torta de limão (fatia)', null, 14, 6, 1, 9, 10],
    ],
  },
];

const { accessToken: admin } = await chamar('POST', '/api/v1/auth/login', { corpo: { email: ADMIN_EMAIL, senha: ADMIN_SENHA } });
const eu = await chamar('GET', '/api/v1/auth/me', { token: admin });
if (!eu.papeis.includes('ADMINISTRADOR')) throw new Error('A conta informada nao tem o papel ADMINISTRADOR.');

// Categorias: reaproveita as existentes (nome unico), cria as que faltam.
const existentes = (await chamar('GET', '/api/v1/admin/categorias?tamanho=100', { token: admin })).conteudo;
const categorias = {};
for (const nome of ['Padaria', 'Confeitaria', 'Hortifruti', 'Refeições']) {
  const achada = existentes.find((c) => c.nome.localeCompare(nome, 'pt-BR', { sensitivity: 'base' }) === 0);
  categorias[nome] = achada?.id ?? (await chamar('POST', '/api/v1/admin/categorias', { token: admin, corpo: { nome } })).id;
}

let ofertas = 0;
for (const loja of LOJAS) {
  const token = await criarConta(loja.chave, loja.pessoa);
  const est = await chamar('POST', '/api/v1/estabelecimentos', { token, corpo: { nome: loja.nome, descricao: loja.descricao, endereco: loja.endereco } });
  await chamar('POST', `/api/v1/admin/estabelecimentos/${est.id}/aprovar`, { token: admin, corpo: { motivo: 'Loja de demonstração aprovada pelo script.' } });
  for (const [categoria, nome, descricao, original, preco, estoque, venda, retirada] of loja.produtos) {
    const produto = await chamar('POST', `/api/v1/estabelecimentos/${est.id}/produtos`, { token, corpo: { categoriaId: categorias[categoria], nome, descricao } });
    const oferta = await chamar('POST', `/api/v1/produtos/${produto.id}/ofertas`, {
      token, corpo: { precoOriginal: original, precoOferta: preco, estoque, inicioVenda: emHoras(-1), vendaAte: emHoras(venda), retiradaAte: emHoras(retirada) },
    });
    await chamar('POST', `/api/v1/ofertas/${oferta.id}/publicar`, { token });
    ofertas++;
  }
}

// Loja aguardando moderacao, para demonstrar a tela do administrador.
const pendente = await criarConta('pendente', 'Rafael (demonstração)');
await chamar('POST', '/api/v1/estabelecimentos', {
  token: pendente,
  corpo: { nome: 'Doceria Vila Mathias', descricao: 'Doces finos e brigadeiros.',
    endereco: { logradouro: 'Rua Campos Sales', numero: '77', bairro: 'Vila Mathias', cidade: 'Santos', estado: 'SP', cep: '11015-100' } },
});

// Cliente com um pedido aguardando a loja.
const cliente = await criarConta('cliente', 'Camila (demonstração)');
const [primeira] = (await chamar('GET', '/api/v1/ofertas?texto=fermenta')).conteudo;
if (primeira) {
  await chamar('POST', '/api/v1/pedidos', {
    token: cliente, corpo: { formaPagamento: 'PIX', itens: [{ ofertaId: primeira.id, quantidade: 1 }] },
    cabecalhos: { 'Idempotency-Key': `demo-${randomUUID()}` },
  });
}

mkdirSync(dirname(SAIDA), { recursive: true });
writeFileSync(SAIDA, JSON.stringify(contas, null, 2) + '\n', { mode: 0o600 });
console.log(`Pronto: ${LOJAS.length} lojas ativas, 1 em análise, ${ofertas} ofertas publicadas e 1 pedido.`);
console.log(`Contas fictícias (com senhas) gravadas em ${SAIDA}. Esse arquivo não vai para o Git.`);
