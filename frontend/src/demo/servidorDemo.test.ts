import { beforeEach, describe, expect, it } from 'vitest';
import { CONTAS_DEMO } from '../lib/demo';
import { reiniciarDemo, responder } from './servidorDemo';

type Pagina = { conteudo: Record<string, unknown>[]; totalElementos: number };

async function chamar(metodo: string, caminho: string, corpo?: unknown, cabecalhos: Record<string, string> = {}) {
  return responder(metodo, caminho, corpo, cabecalhos);
}

async function entrar(indice: number) {
  const conta = CONTAS_DEMO[indice]!;
  const r = await chamar('POST', '/api/v1/auth/login', { email: conta.email, senha: conta.senha });
  return { Authorization: `Bearer ${(r.dados as { accessToken: string }).accessToken}` };
}

describe('API simulada da demonstração', () => {
  beforeEach(() => reiniciarDemo());

  it('lista ofertas compráveis em todas as ordens', async () => {
    for (const ordem of ['PRAZO', 'DESCONTO', 'PRECO', 'RECENTES']) {
      const r = await chamar('GET', `/api/v1/ofertas?ordem=${ordem}`);
      expect(r.status).toBe(200);
      expect((r.dados as Pagina).totalElementos).toBeGreaterThan(5);
    }
    const desconto = (await chamar('GET', '/api/v1/ofertas?ordem=DESCONTO')).dados as Pagina;
    const taxas = desconto.conteudo.map((o) => ((o.precoOriginal as number) - (o.precoOferta as number)) / (o.precoOriginal as number));
    expect(taxas).toEqual([...taxas].sort((a, b) => b - a));
    expect((await chamar('GET', '/api/v1/ofertas?ordem=QUALQUER')).status).toBe(400);
  });

  it('busca sem acento e filtra por cidade', async () => {
    const r = (await chamar('GET', '/api/v1/ofertas?cidade=sao%20paulo')).dados as Pagina;
    expect(r.totalElementos).toBeGreaterThan(0);
    expect(r.conteudo.every((o) => (o.estabelecimento as { cidade: string }).cidade === 'São Paulo')).toBe(true);
  });

  it('exige login e esconde pedido de outra pessoa', async () => {
    expect((await chamar('GET', '/api/v1/pedidos')).status).toBe(401);
    expect((await chamar('POST', '/api/v1/auth/login', { email: 'cliente@demo.bee', senha: 'errada' })).status).toBe(401);
    const cliente = await entrar(0);
    const meus = (await chamar('GET', '/api/v1/pedidos', undefined, cliente)).dados as Pagina;
    expect(meus.totalElementos).toBeGreaterThan(0);
    const admin = await entrar(2);
    expect((await chamar('GET', `/api/v1/pedidos/${meus.conteudo[0]!.id}`, undefined, admin)).status).toBe(404);
    expect((await chamar('GET', '/api/v1/admin/estabelecimentos', undefined, cliente)).status).toBe(403);
  });

  it('cria pedido com baixa de estoque e repete com a mesma chave', async () => {
    const cliente = await entrar(0);
    const oferta = ((await chamar('GET', '/api/v1/ofertas?cidade=Sao%20Paulo')).dados as Pagina).conteudo[0]!;
    const corpo = { formaPagamento: 'PIX', itens: [{ ofertaId: oferta.id, quantidade: 1 }] };
    const chave = { ...cliente, 'Idempotency-Key': 'teste-demo-0001' };
    const primeiro = await chamar('POST', '/api/v1/pedidos', corpo, chave);
    expect(primeiro.status).toBe(201);
    const repetido = await chamar('POST', '/api/v1/pedidos', corpo, chave);
    expect(repetido.status).toBe(200);
    expect((repetido.dados as { id: number }).id).toBe((primeiro.dados as { id: number }).id);
    const depois = (await chamar('GET', `/api/v1/ofertas/${oferta.id}`)).dados as { estoqueDisponivel: number };
    expect(depois.estoqueDisponivel).toBe((oferta.estoqueDisponivel as number) - 1);
    const outro = await chamar('POST', '/api/v1/pedidos', { ...corpo, formaPagamento: 'DINHEIRO' }, chave);
    expect(outro.status).toBe(409);
  });

  it('remove categoria, produto e oferta respeitando o histórico', async () => {
    const admin = await entrar(2);
    const criada = await chamar('POST', '/api/v1/admin/categorias', { nome: 'Temporária' }, admin);
    const idCategoria = (criada.dados as { id: number }).id;
    expect((await chamar('DELETE', `/api/v1/admin/categorias/${idCategoria}`, undefined, admin)).status).toBe(204);
    expect((await chamar('DELETE', `/api/v1/admin/categorias/${idCategoria}`, undefined, admin)).status).toBe(404);
    const emUso = ((await chamar('GET', '/api/v1/categorias')).dados as Pagina).conteudo[0]!;
    expect((await chamar('DELETE', `/api/v1/admin/categorias/${emUso.id}`, undefined, admin)).status).toBe(409);

    const loja = await entrar(1);
    expect((await chamar('DELETE', `/api/v1/admin/categorias/${emUso.id}`, undefined, loja)).status).toBe(403);
    const lojaId = ((await chamar('GET', '/api/v1/estabelecimentos/meus', undefined, loja)).dados as Pagina).conteudo[0]!.id;
    const produto = await chamar('POST', `/api/v1/estabelecimentos/${lojaId}/produtos`, { categoriaId: emUso.id, nome: 'Novo' }, loja);
    const produtoId = (produto.dados as { id: number }).id;
    const agora = Date.now();
    const rascunho = await chamar('POST', `/api/v1/produtos/${produtoId}/ofertas`, {
      precoOriginal: 10, precoOferta: 5, estoque: 2, inicioVenda: new Date(agora - 60_000).toISOString(),
      vendaAte: new Date(agora + 3_600_000).toISOString(), retiradaAte: new Date(agora + 7_200_000).toISOString(),
    }, loja);
    const ofertaId = (rascunho.dados as { id: number }).id;
    expect((await chamar('POST', `/api/v1/ofertas/${ofertaId}/publicar`, undefined, loja)).status).toBe(200);
    expect((await chamar('DELETE', `/api/v1/produtos/${produtoId}`, undefined, loja)).status).toBe(409);
    expect((await chamar('DELETE', `/api/v1/ofertas/${ofertaId}`, undefined, loja)).status).toBe(409);
    expect((await chamar('POST', `/api/v1/ofertas/${ofertaId}/encerrar`, undefined, loja)).status).toBe(200);
    expect((await chamar('DELETE', `/api/v1/produtos/${produtoId}`, undefined, loja)).status).toBe(204);
    expect((await chamar('GET', `/api/v1/produtos/${produtoId}`, undefined, loja)).status).toBe(404);

    const comPedido = ((await chamar('GET', `/api/v1/estabelecimentos/${lojaId}/produtos`, undefined, loja)).dados as Pagina).conteudo[0]!;
    expect((await chamar('DELETE', `/api/v1/produtos/${comPedido.id}`, undefined, loja)).status).toBe(409);
  });

  it('loja avança o pedido e consulta indicadores', async () => {
    const loja = await entrar(1);
    const lojas = (await chamar('GET', '/api/v1/estabelecimentos/meus', undefined, loja)).dados as Pagina;
    const id = lojas.conteudo[0]!.id;
    const pendentes = (await chamar('GET', `/api/v1/estabelecimentos/${id}/pedidos?status=PENDENTE`, undefined, loja)).dados as Pagina;
    const pedido = pendentes.conteudo[0]!;
    expect((await chamar('POST', `/api/v1/pedidos/${pedido.id}/status`, { status: 'EM_PREPARO' }, loja)).status).toBe(409);
    expect((await chamar('POST', `/api/v1/pedidos/${pedido.id}/status`, { status: 'CONFIRMADO' }, loja)).status).toBe(200);
    const ind = await chamar('GET', `/api/v1/estabelecimentos/${id}/indicadores?inicio=2020-01-01T00:00`, undefined, loja);
    expect(ind.status).toBe(200);
    expect((ind.dados as { pedidosConcluidos: number }).pedidosConcluidos).toBe(3);
  });
});
