// API simulada no navegador para o MODO DEMONSTRACAO (build publicada sem servidor, ex.: Vercel).
// Reproduz as rotas e as regras principais da API Java com dados ficticios. Nao substitui a API real:
// a seguranca de verdade (JWT, BCrypt, transacoes, constraints) existe so no backend.

import { CONTAS_DEMO } from '../lib/demo';
import { isoParaLocal, localParaIso } from '../lib/formato';

type Papel = 'CLIENTE' | 'RESPONSAVEL' | 'ADMINISTRADOR';
type StatusLoja = 'PENDENTE_ANALISE' | 'ATIVO' | 'REJEITADO' | 'SUSPENSO';
type StatusOferta = 'RASCUNHO' | 'PUBLICADA' | 'PAUSADA' | 'ENCERRADA';
type StatusPedido = 'PENDENTE' | 'CONFIRMADO' | 'EM_PREPARO' | 'PRONTO_PARA_RETIRADA' | 'CONCLUIDO' | 'CANCELADO';

interface Usuario { id: number; nome: string; email: string; senha: string; papeis: Papel[]; ativo: boolean }
interface Categoria { id: number; nome: string; slug: string; ativo: boolean }
interface Endereco { logradouro: string; numero: string; complemento: string | null; bairro: string; cidade: string; estado: string; cep: string; fusoIana: string }
interface Historico { id: number; statusAnterior: string | null; statusNovo: string; atorId: number | null; motivo: string | null; criadoEm: string }
interface Loja { id: number; responsavelId: number; nome: string; descricao: string | null; status: StatusLoja; endereco: Endereco; criadoEm: string; historico: Historico[] }
interface Produto { id: number; estabelecimentoId: number; categoriaId: number; nome: string; descricao: string | null; ativo: boolean }
interface Oferta { id: number; produtoId: number; precoOriginal: number; precoOferta: number; estoqueDisponivel: number; inicioVenda: string; vendaAte: string; retiradaAte: string; status: StatusOferta; publicadaEm: string | null; encerradaEm: string | null }
interface Item { ofertaId: number; produtoNome: string; precoOriginal: number; precoOferta: number; quantidade: number; subtotal: number }
interface Pedido { id: number; clienteId: number; estabelecimentoId: number; status: StatusPedido; formaPagamento: string; chave: string; impressao: string; total: number; estabelecimento: Omit<Endereco, never> & { nome: string }; retiradaAte: string; concluidoEm: string | null; criadoEm: string; itens: Item[]; historico: Omit<Historico, 'id'>[] }

interface Estado { versao: number; geradoEm: number; seq: number; usuarios: Usuario[]; categorias: Categoria[]; lojas: Loja[]; produtos: Produto[]; ofertas: Oferta[]; pedidos: Pedido[] }

export interface RespostaDemo { status: number; dados: unknown; cabecalhos?: Record<string, string> }

class Erro extends Error {
  constructor(readonly status: number, readonly titulo: string, readonly detalhe: string, readonly campos: Record<string, string> = {}) { super(detalhe); }
}
const conflito = (detalhe: string) => new Erro(409, 'Operação não permitida', detalhe);
const invalido = (campos: Record<string, string>) => new Erro(400, 'Dados inválidos', 'Confira os campos informados.', campos);
const naoEncontrado = (detalhe: string) => new Erro(404, 'Recurso não encontrado', detalhe);

const [CONTA_CLIENTE, CONTA_LOJA, CONTA_ADMIN] = CONTAS_DEMO;

const CHAVE = 'bee.demo.v1';
const VERSAO = 1;
const FUSO = 'America/Sao_Paulo';
const HORA = 3_600_000;

// Declarado antes de carregar(): semear() preenche o estado enquanto gera os dados.
let estado = {} as Estado;
estado = carregar();

function agora(): string { return new Date().toISOString(); }
function emHoras(h: number): string { return new Date(Date.now() + h * HORA).toISOString(); }
/** Horario de vitrine: arredondado para a meia hora seguinte, como uma loja publicaria. */
function meiaHora(h: number): string { const passo = 30 * 60_000; return new Date(Math.ceil((Date.now() + h * HORA) / passo) * passo).toISOString(); }
function proximoId(): number { estado.seq += 1; return estado.seq; }
function salvar() { try { sessionStorage.setItem(CHAVE, JSON.stringify(estado)); } catch { /* demonstracao segue em memoria */ } }

function carregar(): Estado {
  try {
    const salvo = sessionStorage.getItem(CHAVE);
    if (salvo) {
      const e = JSON.parse(salvo) as Estado;
      // Dados gerados ha mais de 6 horas teriam ofertas vencidas: gera de novo.
      if (e.versao === VERSAO && Date.now() - e.geradoEm < 6 * HORA) return e;
    }
  } catch { /* gera do zero */ }
  return semear();
}

/** Reinicia a demonstracao (dados novos, ofertas com prazos validos). */
export function reiniciarDemo() { estado = semear(); salvar(); }

function semear(): Estado {
  const e: Estado = { versao: VERSAO, geradoEm: Date.now(), seq: 100, usuarios: [], categorias: [], lojas: [], produtos: [], ofertas: [], pedidos: [] };
  estado = e;
  const usuario = (nome: string, email: string, papeis: Papel[]) => {
    const u = { id: proximoId(), nome, email, senha: 'bee-demonstracao', papeis, ativo: true };
    e.usuarios.push(u);
    return u;
  };
  const camila = usuario('Camila (demonstração)', CONTA_CLIENTE.email, ['CLIENTE']);
  const lucia = usuario('Lúcia (demonstração)', CONTA_LOJA.email, ['CLIENTE', 'RESPONSAVEL']);
  const moderacao = usuario('Equipe de moderação', CONTA_ADMIN.email, ['CLIENTE', 'ADMINISTRADOR']);
  const antonio = usuario('Antônio (demonstração)', 'antonio@demo.bee', ['CLIENTE', 'RESPONSAVEL']);
  const marina = usuario('Marina (demonstração)', 'marina@demo.bee', ['CLIENTE', 'RESPONSAVEL']);
  const rafael = usuario('Rafael (demonstração)', 'rafael@demo.bee', ['CLIENTE']);

  const cat = (nome: string, slug: string) => { const c = { id: proximoId(), nome, slug, ativo: true }; e.categorias.push(c); return c.id; };
  const padaria = cat('Padaria', 'padaria');
  const confeitaria = cat('Confeitaria', 'confeitaria');
  const hortifruti = cat('Hortifruti', 'hortifruti');
  const refeicoes = cat('Refeições', 'refeicoes');

  const loja = (dono: Usuario, nome: string, descricao: string, end: Omit<Endereco, 'fusoIana' | 'complemento' | 'estado'>, status: StatusLoja = 'ATIVO') => {
    const l: Loja = { id: proximoId(), responsavelId: dono.id, nome, descricao, status, criadoEm: emHoras(-72),
      endereco: { ...end, complemento: null, estado: 'SP', fusoIana: FUSO }, historico: [] };
    l.historico.push({ id: proximoId(), statusAnterior: null, statusNovo: 'PENDENTE_ANALISE', atorId: dono.id, motivo: null, criadoEm: emHoras(-72) });
    if (status === 'ATIVO') l.historico.push({ id: proximoId(), statusAnterior: 'PENDENTE_ANALISE', statusNovo: 'ATIVO', atorId: moderacao.id, motivo: 'Documentação e endereço conferidos.', criadoEm: emHoras(-70) });
    e.lojas.push(l);
    return l;
  };
  const dona = loja(lucia, 'Padaria da Dona Lúcia', 'Pães de fermentação natural e bolos caseiros.', { logradouro: 'Av. Ana Costa', numero: '412', bairro: 'Gonzaga', cidade: 'Santos', cep: '11060002' });
  const horti = loja(antonio, 'Hortifruti Boqueirão', 'Frutas e verduras da feira do dia.', { logradouro: 'Rua Oswaldo Cruz', numero: '88', bairro: 'Boqueirão', cidade: 'Santos', cep: '11045100' });
  const emporio = loja(marina, 'Empório Pinheiros', 'Marmitas e refeições do almoço.', { logradouro: 'Rua dos Pinheiros', numero: '1020', bairro: 'Pinheiros', cidade: 'São Paulo', cep: '05422001' });
  loja(rafael, 'Doceria Vila Mathias', 'Doces finos e brigadeiros.', { logradouro: 'Rua Campos Sales', numero: '77', bairro: 'Vila Mathias', cidade: 'Santos', cep: '11015100' }, 'PENDENTE_ANALISE');

  const oferta = (l: Loja, categoriaId: number, nome: string, descricao: string | null, original: number, preco: number, estoque: number, venda: number, retirada: number) => {
    const p: Produto = { id: proximoId(), estabelecimentoId: l.id, categoriaId, nome, descricao, ativo: true };
    e.produtos.push(p);
    const o: Oferta = { id: proximoId(), produtoId: p.id, precoOriginal: original, precoOferta: preco, estoqueDisponivel: estoque,
      inicioVenda: emHoras(-2), vendaAte: meiaHora(venda), retiradaAte: meiaHora(retirada), status: 'PUBLICADA', publicadaEm: emHoras(-2 + venda / 10), encerradaEm: null };
    e.ofertas.push(o);
    return o;
  };
  const paoNatural = oferta(dona, padaria, 'Pão de fermentação natural', 'Pão de 600 g assado hoje cedo.', 24, 12, 6, 5, 7);
  const sacolaPadaria = oferta(dona, padaria, 'Sacola surpresa da padaria', 'Pães e salgados variados do dia. O conteúdo muda conforme o que sobrou.', 35, 14, 5, 3, 5);
  oferta(dona, padaria, 'Pão francês (saco com 10)', null, 9.5, 4.5, 12, 1.5, 3);
  oferta(dona, confeitaria, 'Bolo de cenoura com chocolate', 'Bolo inteiro, 1,2 kg.', 48, 26, 2, 6, 8);
  oferta(horti, hortifruti, 'Cesta de frutas maduras', 'Banana, mamão e manga prontos para consumo.', 30, 14, 5, 7, 9);
  oferta(horti, hortifruti, 'Kit salada', 'Alface, tomate e pepino.', 15, 7, 8, 4, 5);
  oferta(emporio, refeicoes, 'Marmita de frango com legumes', 'Porção de 450 g, preparada hoje.', 32, 16, 7, 2, 4);
  oferta(emporio, confeitaria, 'Sacola surpresa doce', 'Doces e sobremesas do dia, montados pela loja.', 40, 15, 3, 8, 10);
  oferta(emporio, confeitaria, 'Torta de limão (fatia)', null, 14, 6, 1, 9, 10);

  // Historico da padaria para os indicadores: retiradas nos ultimos dias, um cancelamento e um pedido em andamento.
  const pedido = (cliente: Usuario, o: Oferta, qtd: number, status: StatusPedido, horasAtras: number, forma = 'PIX') => {
    const p = criarRegistroPedido(cliente.id, [{ oferta: o, quantidade: qtd }], forma, `semente-${proximoId()}`, emHoras(-horasAtras));
    if (status !== 'PENDENTE') {
      const etapas: StatusPedido[] = status === 'CANCELADO' ? ['CANCELADO'] : ['CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO'].slice(0, ['CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO'].indexOf(status) + 1) as StatusPedido[];
      etapas.forEach((s, i) => transicionar(p, s, s === 'CANCELADO' ? cliente.id : lucia.id, s === 'CANCELADO' ? 'Imprevisto do cliente.' : null, emHoras(-horasAtras + 0.2 * (i + 1))));
    }
    return p;
  };
  pedido(camila, paoNatural, 1, 'CONCLUIDO', 70, 'DINHEIRO');
  pedido(camila, sacolaPadaria, 1, 'CONCLUIDO', 50);
  pedido(camila, paoNatural, 2, 'CONCLUIDO', 26, 'CARTAO_NA_RETIRADA');
  pedido(camila, sacolaPadaria, 1, 'CANCELADO', 20);
  pedido(camila, paoNatural, 1, 'CONFIRMADO', 0.5);
  pedido(camila, sacolaPadaria, 1, 'PENDENTE', 0.2);
  return e;
}

// ---------- Utilitarios de regra ----------

function lojaDaOferta(o: Oferta): { loja: Loja; produto: Produto } {
  const produto = estado.produtos.find((p) => p.id === o.produtoId)!;
  return { produto, loja: estado.lojas.find((l) => l.id === produto.estabelecimentoId)! };
}

function compravel(o: Oferta, quando = Date.now()): boolean {
  const { loja, produto } = lojaDaOferta(o);
  const categoria = estado.categorias.find((c) => c.id === produto.categoriaId);
  return o.status === 'PUBLICADA' && Date.parse(o.inicioVenda) <= quando && Date.parse(o.vendaAte) > quando
    && o.estoqueDisponivel > 0 && produto.ativo && !!categoria?.ativo && loja.status === 'ATIVO';
}

function ofertaPublica(o: Oferta) {
  const { loja, produto } = lojaDaOferta(o);
  const c = estado.categorias.find((x) => x.id === produto.categoriaId)!;
  const e = loja.endereco;
  return {
    id: o.id, produtoId: produto.id, produtoNome: produto.nome, produtoDescricao: produto.descricao,
    categoria: { id: c.id, nome: c.nome, slug: c.slug }, precoOriginal: o.precoOriginal, precoOferta: o.precoOferta,
    estoqueDisponivel: o.estoqueDisponivel, inicioVenda: o.inicioVenda, vendaAte: o.vendaAte, retiradaAte: o.retiradaAte,
    estabelecimento: { id: loja.id, nome: loja.nome, logradouro: e.logradouro, numero: e.numero, complemento: e.complemento, bairro: e.bairro, cidade: e.cidade, estado: e.estado, fusoIana: e.fusoIana },
  };
}

function lojaPublica(l: Loja) {
  const { historico: _h, ...resto } = l;
  return resto;
}

function pedidoResumo(p: Pedido) {
  const economia = Math.round(p.itens.reduce((s, i) => s + (i.precoOriginal - i.precoOferta) * i.quantidade, 0) * 100) / 100;
  return { id: p.id, clienteId: p.clienteId, estabelecimentoId: p.estabelecimentoId, estabelecimentoNome: p.estabelecimento.nome, status: p.status,
    formaPagamento: p.formaPagamento, total: p.total, economia, retiradaAte: p.retiradaAte, fusoIana: p.estabelecimento.fusoIana, criadoEm: p.criadoEm };
}

function pedidoCompleto(p: Pedido) {
  const { chave: _c, impressao: _i, ...resto } = p;
  return resto;
}

function pagina<T>(lista: T[], params: URLSearchParams, padrao = 20) {
  const numero = Math.max(Number(params.get('pagina') ?? 0) || 0, 0);
  const tamanho = Math.min(Math.max(Number(params.get('tamanho') ?? padrao) || padrao, 1), 100);
  const totalPaginas = Math.ceil(lista.length / tamanho);
  return { conteudo: lista.slice(numero * tamanho, numero * tamanho + tamanho), pagina: numero, tamanho, totalElementos: lista.length, totalPaginas, ultima: numero >= totalPaginas - 1 };
}

const semAcento = (t: string) => t.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

function criarRegistroPedido(clienteId: number, itens: { oferta: Oferta; quantidade: number }[], forma: string, chave: string, criadoEm = agora()): Pedido {
  const { loja } = lojaDaOferta(itens[0]!.oferta);
  const registros: Item[] = itens.map(({ oferta, quantidade }) => {
    oferta.estoqueDisponivel -= quantidade;
    const { produto } = lojaDaOferta(oferta);
    return { ofertaId: oferta.id, produtoNome: produto.nome, precoOriginal: oferta.precoOriginal, precoOferta: oferta.precoOferta, quantidade, subtotal: Math.round(oferta.precoOferta * quantidade * 100) / 100 };
  });
  const retirada = itens.map((i) => i.oferta.retiradaAte).sort()[0]!;
  const p: Pedido = {
    id: proximoId(), clienteId, estabelecimentoId: loja.id, status: 'PENDENTE', formaPagamento: forma, chave,
    impressao: itens.map((i) => `${i.oferta.id}:${i.quantidade}`).sort().join(',') + `|${forma}`,
    total: Math.round(registros.reduce((s, i) => s + i.subtotal, 0) * 100) / 100,
    estabelecimento: { nome: loja.nome, ...loja.endereco }, retiradaAte: retirada, concluidoEm: null, criadoEm, itens: registros,
    historico: [{ statusAnterior: null, statusNovo: 'PENDENTE', atorId: clienteId, motivo: null, criadoEm }],
  };
  estado.pedidos.push(p);
  return p;
}

function transicionar(p: Pedido, novo: StatusPedido, atorId: number, motivo: string | null, quando = agora()) {
  p.historico.push({ statusAnterior: p.status, statusNovo: novo, atorId, motivo, criadoEm: quando });
  p.status = novo;
  if (novo === 'CONCLUIDO' && !p.concluidoEm) p.concluidoEm = quando;
}

// ---------- Roteamento ----------

interface Contexto { metodo: string; partes: string[]; params: URLSearchParams; corpo: Record<string, unknown>; cabecalhos: Record<string, string>; usuario: Usuario | null }

export async function responder(metodo: string, caminho: string, corpo: unknown, cabecalhos: Record<string, string>): Promise<RespostaDemo> {
  // Atraso curto para os estados de carregamento aparecerem como no sistema real.
  await new Promise((r) => setTimeout(r, 120 + Math.random() * 180));
  const [rota, busca] = caminho.split('?');
  const token = (cabecalhos.Authorization ?? '').replace('Bearer ', '');
  const ctx: Contexto = {
    metodo, params: new URLSearchParams(busca ?? ''), corpo: (corpo ?? {}) as Record<string, unknown>, cabecalhos,
    partes: (rota ?? '').replace(/^\/api\/v1\//, '').split('/').filter(Boolean),
    usuario: estado.usuarios.find((u) => `demo.${u.id}` === token && u.ativo) ?? null,
  };
  try {
    const resposta = rotear(ctx);
    if (metodo !== 'GET') salvar();
    return resposta;
  } catch (erro) {
    if (erro instanceof Erro) {
      return { status: erro.status, dados: { type: 'about:blank', status: erro.status, title: erro.titulo, detail: erro.detalhe, ...(Object.keys(erro.campos).length ? { campos: erro.campos } : {}) } };
    }
    return { status: 500, dados: { status: 500, title: 'Erro interno', detail: 'Não foi possível atender a solicitação.' } };
  }
}

const ok = (dados: unknown, status = 200, cabecalhos?: Record<string, string>): RespostaDemo => ({ status, dados, cabecalhos });

function exigir(ctx: Contexto, papel?: Papel): Usuario {
  if (!ctx.usuario) throw new Erro(401, 'Autenticação necessária', 'Autenticação necessária');
  if (papel && !ctx.usuario.papeis.includes(papel)) throw new Erro(403, 'Acesso negado', 'Você não tem permissão para esta operação.');
  return ctx.usuario;
}

function rotear(ctx: Contexto): RespostaDemo {
  const [a, b, c, d] = ctx.partes;
  const m = ctx.metodo;
  if (a === 'auth') return autenticacao(ctx, b);
  if (a === 'categorias' && m === 'GET') return ok(pagina(estado.categorias.filter((x) => x.ativo).sort((x, y) => x.nome.localeCompare(y.nome)).map(({ ativo: _a, ...x }) => x), ctx.params));
  if (a === 'ofertas' && m === 'GET' && !b) return buscarOfertas(ctx);
  if (a === 'ofertas' && m === 'GET' && b) {
    const o = estado.ofertas.find((x) => x.id === Number(b));
    if (!o || !compravel(o)) throw naoEncontrado('Oferta não encontrada ou indisponível.');
    return ok(ofertaPublica(o));
  }
  if (a === 'admin') return administracao(ctx, b, c, d);
  if (a === 'estabelecimentos' && b === 'meus') return ok(pagina(estado.lojas.filter((l) => l.responsavelId === exigir(ctx).id).map(lojaPublica), ctx.params));
  if (a === 'estabelecimentos' && !b && m === 'POST') return solicitarLoja(ctx);
  if (a === 'estabelecimentos' && b) return estabelecimento(ctx, Number(b), c);
  if (a === 'produtos') return produtoRotas(ctx, Number(b), c);
  if (a === 'gestao' && b === 'ofertas') return ok(ofertaDoDono(ctx, Number(c)));
  if (a === 'ofertas' && b && m !== 'GET') return ofertaRotas(ctx, Number(b), c);
  if (a === 'pedidos') return pedidoRotas(ctx, b ? Number(b) : undefined, c);
  throw naoEncontrado('O endereço solicitado não existe.');
}

function autenticacao(ctx: Contexto, acao?: string): RespostaDemo {
  const corpo = ctx.corpo as { nome?: string; email?: string; senha?: string; maiorDeIdade?: boolean };
  if (acao === 'cadastro') {
    const campos: Record<string, string> = {};
    if (!corpo.nome?.trim()) campos.nome = 'não deve estar em branco';
    if (!corpo.email || !/^[^@\s]+@[^@\s]+$/.test(corpo.email)) campos.email = 'deve ser um endereço de e-mail bem formado';
    if (!corpo.senha || corpo.senha.length < 12) campos.senha = 'o tamanho deve ser entre 12 e 72';
    if (corpo.maiorDeIdade !== true) campos.maiorDeIdade = 'É necessário declarar que você tem 18 anos ou mais.';
    if (Object.keys(campos).length) throw invalido(campos);
    const email = corpo.email!.trim().toLowerCase();
    if (estado.usuarios.some((u) => u.email === email)) throw new Erro(409, 'Conflito de dados', 'Não foi possível cadastrar com os dados informados. O e-mail pode já estar em uso.');
    const u: Usuario = { id: proximoId(), nome: corpo.nome!.trim(), email, senha: corpo.senha!, papeis: ['CLIENTE'], ativo: true };
    estado.usuarios.push(u);
    return ok(usuarioPublico(u), 201);
  }
  if (acao === 'login') {
    const u = estado.usuarios.find((x) => x.email === (corpo.email ?? '').trim().toLowerCase() && x.senha === corpo.senha && x.ativo);
    if (!u) throw new Erro(401, 'Credenciais inválidas', 'E-mail ou senha inválidos.');
    return ok({ accessToken: `demo.${u.id}`, tipoToken: 'Bearer', expiraEmSegundos: 900, usuario: usuarioPublico(u) });
  }
  if (acao === 'me') return ok(usuarioPublico(exigir(ctx)));
  throw naoEncontrado('O endereço solicitado não existe.');
}

function usuarioPublico(u: Usuario) {
  return { id: u.id, nome: u.nome, email: u.email, ativo: u.ativo, papeis: [...u.papeis].sort() };
}

function buscarOfertas(ctx: Contexto): RespostaDemo {
  const p = ctx.params;
  const texto = p.get('texto') ? semAcento(p.get('texto')!) : null;
  const cidade = p.get('cidade') ? semAcento(p.get('cidade')!) : null;
  const bairro = p.get('bairro') ? semAcento(p.get('bairro')!) : null;
  const categoria = Number(p.get('categoriaId')) || null;
  const lojaId = Number(p.get('estabelecimentoId')) || null;
  const ordem = p.get('ordem') ?? 'PRAZO';
  if (!['PRAZO', 'DESCONTO', 'PRECO', 'RECENTES'].includes(ordem)) throw invalido({ ordem: 'Valor não permitido.' });
  const lista = estado.ofertas.filter((o) => compravel(o)).map(ofertaPublica).filter((o) =>
    (!texto || semAcento(`${o.produtoNome} ${o.produtoDescricao ?? ''} ${o.estabelecimento.nome}`).includes(texto))
    && (!cidade || semAcento(o.estabelecimento.cidade) === cidade) && (!bairro || semAcento(o.estabelecimento.bairro) === bairro)
    && (!categoria || o.categoria.id === categoria) && (!lojaId || o.estabelecimento.id === lojaId));
  const desconto = (o: { precoOriginal: number; precoOferta: number }) => (o.precoOriginal - o.precoOferta) / o.precoOriginal;
  const publicada = (id: number) => estado.ofertas.find((x) => x.id === id)?.publicadaEm ?? '';
  lista.sort((x, y) => {
    const criterio = ordem === 'DESCONTO' ? desconto(y) - desconto(x) : ordem === 'PRECO' ? x.precoOferta - y.precoOferta
      : ordem === 'RECENTES' ? publicada(y.id).localeCompare(publicada(x.id)) : x.vendaAte.localeCompare(y.vendaAte);
    return criterio || (ordem === 'RECENTES' ? y.id - x.id : x.id - y.id);
  });
  return ok(pagina(lista, p));
}

// ---------- Estabelecimentos ----------

function validarLoja(corpo: Record<string, unknown>) {
  const e = (corpo.endereco ?? {}) as Partial<Endereco>;
  const campos: Record<string, string> = {};
  if (!String(corpo.nome ?? '').trim()) campos.nome = 'não deve estar em branco';
  for (const campo of ['logradouro', 'numero', 'bairro', 'cidade'] as const) if (!String(e[campo] ?? '').trim()) campos[`endereco.${campo}`] = 'não deve estar em branco';
  if (!/^[A-Za-z]{2}$/.test(String(e.estado ?? ''))) campos['endereco.estado'] = 'Informe a sigla da UF com duas letras.';
  if (!/^\d{5}-?\d{3}$/.test(String(e.cep ?? ''))) campos['endereco.cep'] = 'Informe o CEP com 8 dígitos.';
  if (Object.keys(campos).length) throw invalido(campos);
  return {
    nome: String(corpo.nome).trim(), descricao: String(corpo.descricao ?? '').trim() || null,
    endereco: { logradouro: String(e.logradouro).trim(), numero: String(e.numero).trim(), complemento: String(e.complemento ?? '').trim() || null,
      bairro: String(e.bairro).trim(), cidade: String(e.cidade).trim(), estado: String(e.estado).toUpperCase(), cep: String(e.cep).replace('-', ''), fusoIana: String(e.fusoIana ?? FUSO) },
  };
}

function registrarLoja(l: Loja, novo: StatusLoja, atorId: number, motivo: string | null) {
  l.historico.push({ id: proximoId(), statusAnterior: l.historico.length ? l.status : null, statusNovo: novo, atorId, motivo, criadoEm: agora() });
  l.status = novo;
}

function solicitarLoja(ctx: Contexto): RespostaDemo {
  const u = exigir(ctx, 'CLIENTE');
  const dados = validarLoja(ctx.corpo);
  const l: Loja = { id: proximoId(), responsavelId: u.id, ...dados, status: 'PENDENTE_ANALISE', criadoEm: agora(), historico: [] };
  l.historico.push({ id: proximoId(), statusAnterior: null, statusNovo: 'PENDENTE_ANALISE', atorId: u.id, motivo: null, criadoEm: agora() });
  estado.lojas.push(l);
  return ok(lojaPublica(l), 201);
}

function lojaVisivel(ctx: Contexto, id: number, permitirAdmin = true): Loja {
  const u = exigir(ctx);
  const l = estado.lojas.find((x) => x.id === id);
  if (!l || (l.responsavelId !== u.id && !(permitirAdmin && u.papeis.includes('ADMINISTRADOR')))) throw naoEncontrado('Estabelecimento não encontrado.');
  return l;
}

function estabelecimento(ctx: Contexto, id: number, sub?: string): RespostaDemo {
  const m = ctx.metodo;
  if (!sub && m === 'GET') return ok(lojaPublica(lojaVisivel(ctx, id)));
  if (!sub && m === 'PUT') {
    const l = lojaVisivel(ctx, id, false);
    if (l.status === 'SUSPENSO') throw conflito('Estabelecimento suspenso não pode ser alterado. Aguarde a reativação pela moderação.');
    const dados = validarLoja(ctx.corpo);
    const mudouEndereco = JSON.stringify(dados.endereco) !== JSON.stringify(l.endereco);
    Object.assign(l, dados);
    if (l.status === 'REJEITADO') registrarLoja(l, 'PENDENTE_ANALISE', ctx.usuario!.id, 'Cadastro corrigido e reenviado para análise.');
    else if (l.status === 'ATIVO' && mudouEndereco) registrarLoja(l, 'PENDENTE_ANALISE', ctx.usuario!.id, 'Endereço ou fuso alterado; nova análise necessária.');
    return ok(lojaPublica(l));
  }
  if (sub === 'historico') return ok(lojaVisivel(ctx, id).historico);
  if (sub === 'produtos') {
    exigir(ctx, 'RESPONSAVEL');
    const l = lojaVisivel(ctx, id, false);
    if (m === 'GET') return ok(pagina(estado.produtos.filter((p) => p.estabelecimentoId === l.id), ctx.params));
    const c = ctx.corpo as { categoriaId?: number; nome?: string; descricao?: string };
    if (!estado.categorias.some((x) => x.id === Number(c.categoriaId) && x.ativo)) throw invalido({ categoriaId: 'Escolha uma categoria ativa.' });
    if (!c.nome?.trim()) throw invalido({ nome: 'não deve estar em branco' });
    const p: Produto = { id: proximoId(), estabelecimentoId: l.id, categoriaId: Number(c.categoriaId), nome: c.nome.trim(), descricao: c.descricao?.trim() || null, ativo: true };
    estado.produtos.push(p);
    return ok(p, 201);
  }
  if (sub === 'pedidos') {
    exigir(ctx, 'RESPONSAVEL');
    const l = lojaVisivel(ctx, id, false);
    const status = ctx.params.get('status');
    const lista = estado.pedidos.filter((p) => p.estabelecimentoId === l.id && (!status || p.status === status))
      .sort((x, y) => x.criadoEm.localeCompare(y.criadoEm) || x.id - y.id).map(pedidoResumo);
    return ok(pagina(lista, ctx.params));
  }
  if (sub === 'indicadores') return indicadores(ctx, id);
  throw naoEncontrado('O endereço solicitado não existe.');
}

// ---------- Catalogo do responsavel ----------

function produtoDoDono(ctx: Contexto, id: number): Produto {
  exigir(ctx, 'RESPONSAVEL');
  const p = estado.produtos.find((x) => x.id === id);
  if (!p || estado.lojas.find((l) => l.id === p.estabelecimentoId)?.responsavelId !== ctx.usuario!.id) throw naoEncontrado('Produto não encontrado.');
  return p;
}

function ofertaDoDono(ctx: Contexto, id: number): Oferta {
  exigir(ctx, 'RESPONSAVEL');
  const o = estado.ofertas.find((x) => x.id === id);
  if (!o || lojaDaOferta(o).loja.responsavelId !== ctx.usuario!.id) throw naoEncontrado('Oferta não encontrada.');
  return o;
}

function validarOferta(corpo: Record<string, unknown>) {
  const original = Number(corpo.precoOriginal); const preco = Number(corpo.precoOferta); const estoque = Number(corpo.estoque);
  const inicio = Date.parse(String(corpo.inicioVenda)); const venda = Date.parse(String(corpo.vendaAte)); const retirada = Date.parse(String(corpo.retiradaAte));
  const campos: Record<string, string> = {};
  if (!(original >= 0.01)) campos.precoOriginal = 'deve ser maior que ou igual a 0.01';
  if (!(preco >= 0.01)) campos.precoOferta = 'deve ser maior que ou igual a 0.01';
  else if (preco > original) campos.precoOferta = 'O preço da oferta não pode ser maior que o preço original.';
  if (!Number.isInteger(estoque) || estoque < 0 || estoque > 100000) campos.estoque = 'Informe um estoque entre 0 e 100000.';
  if (Number.isNaN(inicio) || Number.isNaN(venda) || Number.isNaN(retirada)) campos.vendaAte = 'Informe as datas.';
  else if (inicio >= venda) campos.vendaAte = 'O fim da venda deve ser posterior ao início.';
  else if (venda <= Date.now()) campos.vendaAte = 'O fim da venda já passou.';
  if (!Number.isNaN(retirada) && retirada < venda) campos.retiradaAte = 'O prazo de retirada não pode terminar antes do fim da venda.';
  if (Object.keys(campos).length) throw invalido(campos);
  return { precoOriginal: Math.round(original * 100) / 100, precoOferta: Math.round(preco * 100) / 100, estoqueDisponivel: estoque,
    inicioVenda: new Date(inicio).toISOString(), vendaAte: new Date(venda).toISOString(), retiradaAte: new Date(retirada).toISOString() };
}

const HISTORICO = 'Há pedidos com esta oferta, e eles precisam ficar no histórico. Encerre a oferta ou desative o produto.';
const emVenda = (o: Oferta) => o.status === 'PUBLICADA' || o.status === 'PAUSADA';
const temPedido = (o: Oferta) => estado.pedidos.some((p) => p.itens.some((i) => i.ofertaId === o.id));

function produtoRotas(ctx: Contexto, id: number, sub?: string): RespostaDemo {
  const p = produtoDoDono(ctx, id);
  if (!sub && ctx.metodo === 'GET') return ok(p);
  if (!sub && ctx.metodo === 'DELETE') {
    const doProduto = estado.ofertas.filter((o) => o.produtoId === p.id);
    if (doProduto.some(emVenda)) throw conflito('Encerre as ofertas publicadas ou pausadas antes de remover o produto.');
    if (doProduto.some(temPedido)) throw new Erro(409, 'Conflito de dados', HISTORICO);
    estado.ofertas = estado.ofertas.filter((o) => o.produtoId !== p.id);
    estado.produtos = estado.produtos.filter((x) => x.id !== p.id);
    return ok(null, 204);
  }
  if (!sub && ctx.metodo === 'PUT') {
    const c = ctx.corpo as { categoriaId?: number; nome?: string; descricao?: string; ativo?: boolean };
    if (!c.nome?.trim()) throw invalido({ nome: 'não deve estar em branco' });
    if (Number(c.categoriaId) !== p.categoriaId && !estado.categorias.some((x) => x.id === Number(c.categoriaId) && x.ativo)) throw invalido({ categoriaId: 'Escolha uma categoria ativa.' });
    Object.assign(p, { categoriaId: Number(c.categoriaId), nome: c.nome.trim(), descricao: c.descricao?.trim() || null, ...(c.ativo !== undefined ? { ativo: c.ativo } : {}) });
    return ok(p);
  }
  if (sub === 'ofertas' && ctx.metodo === 'GET') return ok(pagina(estado.ofertas.filter((o) => o.produtoId === p.id).sort((x, y) => y.id - x.id), ctx.params));
  if (sub === 'ofertas' && ctx.metodo === 'POST') {
    const loja = estado.lojas.find((l) => l.id === p.estabelecimentoId)!;
    if (loja.status !== 'ATIVO') throw conflito('O estabelecimento precisa estar ativo para cadastrar ofertas.');
    const o: Oferta = { id: proximoId(), produtoId: p.id, ...validarOferta(ctx.corpo), status: 'RASCUNHO', publicadaEm: null, encerradaEm: null };
    estado.ofertas.push(o);
    return ok(o, 201);
  }
  throw naoEncontrado('O endereço solicitado não existe.');
}

function ofertaRotas(ctx: Contexto, id: number, acao?: string): RespostaDemo {
  const o = ofertaDoDono(ctx, id);
  const { loja, produto } = lojaDaOferta(o);
  const transicaoInvalida = () => conflito(`Transição não permitida para oferta em ${o.status}.`);
  const publicavel = (comEstoque: boolean) => {
    if (loja.status !== 'ATIVO') throw conflito('O estabelecimento precisa estar ativo para publicar ofertas.');
    if (!produto.ativo) throw conflito('O produto está desativado.');
    if (Date.parse(o.vendaAte) <= Date.now()) throw invalido({ vendaAte: 'O fim da venda já passou. Crie uma nova oferta.' });
    if (comEstoque && o.estoqueDisponivel <= 0) throw invalido({ estoque: 'Informe estoque positivo para publicar.' });
    if (estado.ofertas.some((x) => x.id !== o.id && x.produtoId === o.produtoId && (x.status === 'PUBLICADA' || x.status === 'PAUSADA'))) {
      throw new Erro(409, 'Conflito de dados', 'Este produto já possui uma oferta publicada ou pausada.');
    }
  };
  if (!acao && ctx.metodo === 'DELETE') {
    if (emVenda(o)) throw conflito('Encerre a oferta antes de remover.');
    if (temPedido(o)) throw new Erro(409, 'Conflito de dados', HISTORICO);
    estado.ofertas = estado.ofertas.filter((x) => x.id !== o.id);
    return ok(null, 204);
  }
  if (!acao && ctx.metodo === 'PUT') {
    if (o.status !== 'RASCUNHO') throw conflito('Preço e prazos não mudam após a publicação. Encerre esta oferta e crie outra.');
    Object.assign(o, validarOferta(ctx.corpo));
  } else if (acao === 'publicar') { if (o.status !== 'RASCUNHO') throw transicaoInvalida(); publicavel(true); o.status = 'PUBLICADA'; o.publicadaEm ??= agora(); }
  else if (acao === 'retomar') { if (o.status !== 'PAUSADA') throw transicaoInvalida(); publicavel(false); o.status = 'PUBLICADA'; }
  else if (acao === 'pausar') { if (o.status !== 'PUBLICADA') throw transicaoInvalida(); o.status = 'PAUSADA'; }
  else if (acao === 'encerrar') { if (o.status === 'ENCERRADA') throw transicaoInvalida(); o.status = 'ENCERRADA'; o.encerradaEm = agora(); }
  else if (acao === 'estoque') {
    const ajuste = Number((ctx.corpo as { ajuste?: number }).ajuste);
    if (!Number.isInteger(ajuste) || ajuste === 0) throw invalido({ ajuste: 'Informe um ajuste diferente de zero.' });
    if (o.status === 'ENCERRADA') throw conflito('Oferta encerrada não aceita ajuste de estoque.');
    if (o.estoqueDisponivel + ajuste < 0 || o.estoqueDisponivel + ajuste > 100000) throw conflito('O ajuste deixaria o estoque abaixo de zero ou acima de 100000.');
    o.estoqueDisponivel += ajuste;
  } else throw naoEncontrado('O endereço solicitado não existe.');
  return ok(o);
}

// ---------- Pedidos ----------

function papelNoPedido(ctx: Contexto, p: Pedido): 'CLIENTE' | 'LOJA' {
  const u = exigir(ctx);
  if (p.clienteId === u.id) return 'CLIENTE';
  if (estado.lojas.find((l) => l.id === p.estabelecimentoId)?.responsavelId === u.id) return 'LOJA';
  throw naoEncontrado('Pedido não encontrado.');
}

function pedidoRotas(ctx: Contexto, id?: number, acao?: string): RespostaDemo {
  if (id === undefined && ctx.metodo === 'POST') return criarPedido(ctx);
  if (id === undefined) {
    const u = exigir(ctx);
    const status = ctx.params.get('status');
    const lista = estado.pedidos.filter((p) => p.clienteId === u.id && (!status || p.status === status))
      .sort((x, y) => y.criadoEm.localeCompare(x.criadoEm) || y.id - x.id).map(pedidoResumo);
    return ok(pagina(lista, ctx.params));
  }
  const p = estado.pedidos.find((x) => x.id === id);
  if (!p) { exigir(ctx); throw naoEncontrado('Pedido não encontrado.'); }
  const papel = papelNoPedido(ctx, p);
  if (!acao) return ok(pedidoCompleto(p));
  if (acao === 'status') {
    if (papel !== 'LOJA') throw new Erro(403, 'Acesso negado', 'Você não tem permissão para esta operação.');
    const destino = (ctx.corpo as { status?: StatusPedido }).status;
    const ordem: StatusPedido[] = ['PENDENTE', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO'];
    if (destino === 'CANCELADO') throw invalido({ status: 'Use a operação de cancelamento para cancelar o pedido.' });
    if (!destino || ordem[ordem.indexOf(p.status) + 1] !== destino) throw conflito(`Transição de ${p.status} para ${destino} não é permitida.`);
    transicionar(p, destino, ctx.usuario!.id, null);
    return ok(pedidoCompleto(p));
  }
  if (acao === 'cancelar') {
    const motivo = String((ctx.corpo as { motivo?: string }).motivo ?? '').trim() || null;
    if (p.status === 'CANCELADO') return ok(pedidoCompleto(p));
    if (p.status === 'CONCLUIDO') throw conflito('Pedido concluído não pode ser cancelado.');
    if (papel === 'CLIENTE' && p.status !== 'PENDENTE') throw conflito('O cliente só pode cancelar pedidos pendentes. Fale com o estabelecimento.');
    if (papel === 'LOJA' && !motivo) throw invalido({ motivo: 'Informe o motivo do cancelamento.' });
    for (const item of p.itens) { const o = estado.ofertas.find((x) => x.id === item.ofertaId); if (o) o.estoqueDisponivel += item.quantidade; }
    transicionar(p, 'CANCELADO', ctx.usuario!.id, motivo);
    return ok(pedidoCompleto(p));
  }
  throw naoEncontrado('O endereço solicitado não existe.');
}

function criarPedido(ctx: Contexto): RespostaDemo {
  const u = exigir(ctx, 'CLIENTE');
  const chave = ctx.cabecalhos['Idempotency-Key'];
  if (!chave || !/^[A-Za-z0-9_-]{8,120}$/.test(chave)) throw invalido({ chave: 'Use de 8 a 120 caracteres: letras, números, \'-\' ou \'_\'.' });
  const corpo = ctx.corpo as { formaPagamento?: string; itens?: { ofertaId: number; quantidade: number }[] };
  const itens = corpo.itens ?? [];
  if (!['PIX', 'DINHEIRO', 'CARTAO_NA_RETIRADA'].includes(String(corpo.formaPagamento))) throw invalido({ formaPagamento: 'não deve ser nulo' });
  if (itens.length < 1 || itens.length > 20) throw invalido({ itens: 'o tamanho deve ser entre 1 e 20' });
  if (new Set(itens.map((i) => i.ofertaId)).size !== itens.length) throw invalido({ itens: 'Cada oferta deve aparecer uma única vez no pedido.' });
  const impressao = itens.map((i) => `${i.ofertaId}:${i.quantidade}`).sort().join(',') + `|${corpo.formaPagamento}`;
  // Idempotencia: mesma chave e mesmo conteudo devolvem o pedido original, sem nova baixa de estoque.
  const anterior = estado.pedidos.find((p) => p.clienteId === u.id && p.chave === chave);
  if (anterior) {
    if (anterior.impressao !== impressao) throw conflito('Esta chave de idempotência já foi usada com outro conteúdo de pedido.');
    return ok(pedidoCompleto(anterior), 200, { 'Idempotent-Replayed': 'true' });
  }
  const ofertas = itens.map((i) => ({ oferta: estado.ofertas.find((o) => o.id === i.ofertaId), quantidade: i.quantidade }));
  if (ofertas.some((x) => !x.oferta)) throw conflito('Uma ou mais ofertas não estão disponíveis para compra.');
  const lojas = new Set(ofertas.map((x) => lojaDaOferta(x.oferta!).loja.id));
  if (lojas.size !== 1) throw conflito('Um pedido só pode conter ofertas de um mesmo estabelecimento.');
  const loja = lojaDaOferta(ofertas[0]!.oferta!).loja;
  if (loja.status !== 'ATIVO') throw conflito('O estabelecimento não está recebendo pedidos no momento.');
  if (loja.responsavelId === u.id) throw conflito('Não é possível fazer pedido no próprio estabelecimento.');
  for (const { oferta, quantidade } of ofertas) {
    if (!Number.isInteger(quantidade) || quantidade < 1 || quantidade > 100) throw invalido({ itens: 'Quantidade entre 1 e 100.' });
    if (!compravel(oferta!)) throw conflito(`A oferta ${oferta!.id} não está disponível para compra.`);
    if (oferta!.estoqueDisponivel < quantidade) throw conflito(`Estoque insuficiente para a oferta ${oferta!.id}.`);
  }
  const p = criarRegistroPedido(u.id, ofertas.map((x) => ({ oferta: x.oferta!, quantidade: x.quantidade })), String(corpo.formaPagamento), chave);
  return ok(pedidoCompleto(p), 201, { 'Idempotent-Replayed': 'false' });
}

function indicadores(ctx: Contexto, id: number): RespostaDemo {
  exigir(ctx, 'RESPONSAVEL');
  const l = lojaVisivel(ctx, id, false);
  const inicioLocal = ctx.params.get('inicio');
  if (!inicioLocal) throw invalido({ inicio: 'Parâmetro obrigatório.' });
  const consultadoEm = new Date();
  const fimLocal = ctx.params.get('fim') ?? isoParaLocal(consultadoEm, l.endereco.fusoIana);
  const inicio = Date.parse(localParaIso(inicioLocal.slice(0, 16), l.endereco.fusoIana));
  const fim = ctx.params.get('fim') ? Date.parse(localParaIso(fimLocal.slice(0, 16), l.endereco.fusoIana)) : consultadoEm.getTime();
  if (!(inicio < fim)) throw invalido({ fim: 'O fim do período deve ser posterior ao início.' });
  if (fim > consultadoEm.getTime()) throw invalido({ fim: 'O fim do período não pode estar no futuro.' });
  const daLoja = estado.pedidos.filter((p) => p.estabelecimentoId === l.id);
  const dentro = (iso: string | null) => !!iso && Date.parse(iso) >= inicio && Date.parse(iso) < fim;
  const concluidos = daLoja.filter((p) => dentro(p.concluidoEm));
  const coorte = daLoja.filter((p) => dentro(p.criadoEm));
  const conta = (s: StatusPedido) => coorte.filter((p) => p.status === s).length;
  const taxa = (n: number) => (coorte.length ? Math.round((n * 100 / coorte.length) * 10000) / 10000 : null);
  const soma = (f: (p: Pedido) => number) => Math.round(concluidos.reduce((s, p) => s + f(p), 0) * 100) / 100;
  const publicou = estado.ofertas.some((o) => lojaDaOferta(o).loja.id === l.id && dentro(o.publicadaEm));
  return ok({
    estabelecimentoId: l.id, fusoIana: l.endereco.fusoIana,
    periodo: { inicio: new Date(inicio).toISOString(), fim: new Date(fim).toISOString(), inicioLocal, fimLocal },
    consultadoEm: consultadoEm.toISOString(), pedidosConcluidos: concluidos.length,
    valorMovimentado: soma((p) => p.total), itensComercializados: concluidos.reduce((s, p) => s + p.itens.reduce((t, i) => t + i.quantidade, 0), 0),
    economiaEstimada: soma((p) => p.itens.reduce((t, i) => t + (i.precoOriginal - i.precoOferta) * i.quantidade, 0)),
    estabelecimentoComAtividade: publicou || concluidos.length > 0 ? 1 : 0,
    coorte: { pedidos: coorte.length, concluidos: conta('CONCLUIDO'), cancelados: conta('CANCELADO'),
      emAndamento: coorte.length - conta('CONCLUIDO') - conta('CANCELADO'), taxaConclusao: taxa(conta('CONCLUIDO')), taxaCancelamento: taxa(conta('CANCELADO')) },
  });
}

// ---------- Administracao ----------

function administracao(ctx: Contexto, recurso?: string, id?: string, acao?: string): RespostaDemo {
  const admin = exigir(ctx, 'ADMINISTRADOR');
  if (recurso === 'estabelecimentos' && !id) {
    const status = ctx.params.get('status');
    return ok(pagina(estado.lojas.filter((l) => !status || l.status === status).sort((x, y) => x.id - y.id).map(lojaPublica), ctx.params));
  }
  if (recurso === 'estabelecimentos' && id && acao) {
    const l = estado.lojas.find((x) => x.id === Number(id));
    if (!l) throw naoEncontrado('Estabelecimento não encontrado.');
    if (l.responsavelId === admin.id) throw new Erro(403, 'Acesso negado', 'Você não tem permissão para esta operação.');
    const motivo = String((ctx.corpo as { motivo?: string }).motivo ?? '').trim();
    if (!motivo) throw invalido({ motivo: 'não deve estar em branco' });
    const regras: Record<string, [StatusLoja, StatusLoja]> = { aprovar: ['PENDENTE_ANALISE', 'ATIVO'], rejeitar: ['PENDENTE_ANALISE', 'REJEITADO'], suspender: ['ATIVO', 'SUSPENSO'], reativar: ['SUSPENSO', 'ATIVO'] };
    const regra = regras[acao];
    if (!regra || l.status !== regra[0]) throw conflito(`Esta decisão não é permitida para um estabelecimento em ${l.status}.`);
    registrarLoja(l, regra[1], admin.id, motivo);
    if (regra[1] === 'ATIVO') {
      const dono = estado.usuarios.find((u) => u.id === l.responsavelId);
      if (dono && !dono.papeis.includes('RESPONSAVEL')) dono.papeis.push('RESPONSAVEL');
    }
    return ok(lojaPublica(l));
  }
  if (recurso === 'categorias') {
    const nome = String((ctx.corpo as { nome?: string }).nome ?? '').trim().replace(/\s+/g, ' ');
    const slug = semAcento(nome).replace(/[^a-z0-9]+/g, '-').replace(/(^-+|-+$)/g, '');
    if (ctx.metodo === 'GET') return ok(pagina([...estado.categorias].sort((x, y) => x.nome.localeCompare(y.nome)), ctx.params));
    if (ctx.metodo === 'DELETE') {
      if (!estado.categorias.some((x) => x.id === Number(id))) throw naoEncontrado('Categoria não encontrada.');
      if (estado.produtos.some((x) => x.categoriaId === Number(id))) throw new Erro(409, 'Conflito de dados', 'Esta categoria tem produtos. Desative a categoria em vez de remover.');
      estado.categorias = estado.categorias.filter((x) => x.id !== Number(id));
      return ok(null, 204);
    }
    if (!nome) throw invalido({ nome: 'não deve estar em branco' });
    if (!slug) throw invalido({ nome: 'Use ao menos uma letra ou número no nome.' });
    const outra = estado.categorias.find((c) => c.slug === slug && c.id !== Number(id));
    if (outra) throw new Erro(409, 'Conflito de dados', 'Já existe uma categoria com esse nome.');
    if (ctx.metodo === 'POST') { const c = { id: proximoId(), nome, slug, ativo: true }; estado.categorias.push(c); return ok(c, 201); }
    const c = estado.categorias.find((x) => x.id === Number(id));
    if (!c) throw naoEncontrado('Categoria não encontrada.');
    const ativo = (ctx.corpo as { ativo?: boolean }).ativo;
    Object.assign(c, { nome, slug, ...(ativo !== undefined ? { ativo } : {}) });
    return ok(c);
  }
  throw naoEncontrado('O endereço solicitado não existe.');
}
