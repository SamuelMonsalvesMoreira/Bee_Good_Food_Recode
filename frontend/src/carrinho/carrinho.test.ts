import { describe, expect, it } from 'vitest';
import type { OfertaPublica } from '../api/types';
import { adicionar, alterarQuantidade, MAX_ITENS, retiradaDoPedido, totalEstimado, VAZIO, type Carrinho } from './carrinho';

function oferta(id: number, lojaId = 1, extra: Partial<OfertaPublica> = {}): OfertaPublica {
  return {
    id, produtoId: id, produtoNome: `Produto ${id}`, produtoDescricao: null,
    categoria: { id: 1, nome: 'Padaria', slug: 'padaria' },
    precoOriginal: 10, precoOferta: 4.1, estoqueDisponivel: 5,
    inicioVenda: '2026-11-06T10:00:00Z', vendaAte: '2026-11-06T21:00:00Z', retiradaAte: '2026-11-06T23:00:00Z',
    estabelecimento: { id: lojaId, nome: `Loja ${lojaId}`, logradouro: 'Rua A', numero: '1', complemento: null, bairro: 'Centro', cidade: 'Santos', estado: 'SP', fusoIana: 'America/Sao_Paulo' },
    ...extra,
  };
}

function adicionado(carrinho: Carrinho, o: OfertaPublica, q = 1): Carrinho {
  const r = adicionar(carrinho, o, q);
  if (r.tipo !== 'adicionado') throw new Error(r.tipo);
  return r.carrinho;
}

describe('carrinho de um estabelecimento (CA07)', () => {
  it('pede confirmacao antes de trocar de loja e nao remove itens sozinho', () => {
    const c = adicionado(VAZIO, oferta(1, 1));
    const r = adicionar(c, oferta(2, 2), 1);
    expect(r).toEqual({ tipo: 'outraLoja', lojaAtual: 'Loja 1' });
    expect(c.itens).toHaveLength(1);
  });

  it('substitui a loja somente com confirmacao explicita', () => {
    const c = adicionado(VAZIO, oferta(1, 1));
    const r = adicionar(c, oferta(2, 2), 1, true);
    expect(r.tipo).toBe('adicionado');
    if (r.tipo === 'adicionado') {
      expect(r.carrinho.estabelecimento?.id).toBe(2);
      expect(r.carrinho.itens.map((i) => i.ofertaId)).toEqual([2]);
    }
  });

  it('soma quantidades da mesma oferta sem passar do estoque', () => {
    let c = adicionado(VAZIO, oferta(1), 3);
    c = adicionado(c, oferta(1), 4);
    expect(c.itens[0]?.quantidade).toBe(5);
  });

  it('limita o numero de ofertas diferentes ao aceito pela API', () => {
    let c = VAZIO;
    for (let i = 1; i <= MAX_ITENS; i++) c = adicionado(c, oferta(i));
    expect(adicionar(c, oferta(99), 1).tipo).toBe('limite');
  });

  it('zerar a quantidade remove o item e esvazia o carrinho', () => {
    const c = adicionado(VAZIO, oferta(1));
    expect(alterarQuantidade(c, 1, 0)).toEqual(VAZIO);
  });

  it('qualquer mudanca gera nova tentativa de checkout (nova chave de idempotencia)', () => {
    const c = { ...adicionado(VAZIO, oferta(1), 2), chaveCheckout: 'web-1' };
    expect(alterarQuantidade(c, 1, 1).chaveCheckout).toBeNull();
  });

  it('calcula total em centavos e usa o menor prazo de retirada', () => {
    let c = adicionado(VAZIO, oferta(1), 3);
    c = adicionado(c, oferta(2, 1, { retiradaAte: '2026-11-06T22:00:00Z' }), 1);
    expect(totalEstimado(c)).toBe(16.4);
    expect(retiradaDoPedido(c)).toBe('2026-11-06T22:00:00Z');
  });
});
