import type { OfertaPublica } from '../api/types';

// Regras puras do carrinho (DEC-007: o carrinho fica no frontend; um estabelecimento por pedido).

export interface ItemCarrinho {
  ofertaId: number;
  produtoNome: string;
  precoOriginal: number;
  precoOferta: number;
  quantidade: number;
  maximo: number;
  vendaAte: string;
  retiradaAte: string;
}

export interface Carrinho {
  estabelecimento: { id: number; nome: string; fusoIana: string; bairro: string; cidade: string } | null;
  itens: ItemCarrinho[];
  /** Chave da tentativa de checkout atual; reutilizada se a mesma compra for reenviada. */
  chaveCheckout: string | null;
}

export const VAZIO: Carrinho = { estabelecimento: null, itens: [], chaveCheckout: null };
export const MAX_ITENS = 20;
export const MAX_QUANTIDADE = 100;

export type ResultadoAdicao =
  | { tipo: 'adicionado'; carrinho: Carrinho }
  | { tipo: 'outraLoja'; lojaAtual: string }
  | { tipo: 'limite'; motivo: string };

export function adicionar(carrinho: Carrinho, oferta: OfertaPublica, quantidade: number, substituirLoja = false): ResultadoAdicao {
  const loja = oferta.estabelecimento;
  // CA07: nunca remover itens de outra loja sem confirmacao explicita.
  if (carrinho.estabelecimento && carrinho.estabelecimento.id !== loja.id && carrinho.itens.length > 0 && !substituirLoja) {
    return { tipo: 'outraLoja', lojaAtual: carrinho.estabelecimento.nome };
  }
  const base = carrinho.estabelecimento?.id === loja.id ? carrinho : VAZIO;
  const maximo = Math.min(oferta.estoqueDisponivel, MAX_QUANTIDADE);
  const existente = base.itens.find((i) => i.ofertaId === oferta.id);
  if (!existente && base.itens.length >= MAX_ITENS) {
    return { tipo: 'limite', motivo: `Um pedido aceita até ${MAX_ITENS} ofertas diferentes.` };
  }
  const novaQuantidade = Math.min((existente?.quantidade ?? 0) + quantidade, maximo);
  if (novaQuantidade <= 0) return { tipo: 'limite', motivo: 'Esta oferta está sem estoque.' };
  const item: ItemCarrinho = {
    ofertaId: oferta.id,
    produtoNome: oferta.produtoNome,
    precoOriginal: oferta.precoOriginal,
    precoOferta: oferta.precoOferta,
    quantidade: novaQuantidade,
    maximo,
    vendaAte: oferta.vendaAte,
    retiradaAte: oferta.retiradaAte,
  };
  const itens = existente ? base.itens.map((i) => (i.ofertaId === oferta.id ? item : i)) : [...base.itens, item];
  return {
    tipo: 'adicionado',
    carrinho: {
      estabelecimento: { id: loja.id, nome: loja.nome, fusoIana: loja.fusoIana, bairro: loja.bairro, cidade: loja.cidade },
      itens,
      chaveCheckout: null,
    },
  };
}

export function alterarQuantidade(carrinho: Carrinho, ofertaId: number, quantidade: number): Carrinho {
  const itens = carrinho.itens
    .map((i) => (i.ofertaId === ofertaId ? { ...i, quantidade: Math.min(Math.max(quantidade, 0), i.maximo) } : i))
    .filter((i) => i.quantidade > 0);
  return itens.length === 0 ? VAZIO : { ...carrinho, itens, chaveCheckout: null };
}

/** Estimativa exibida antes de confirmar; o valor final e sempre o calculado pela API (DEC-009). */
export function totalEstimado(carrinho: Carrinho): number {
  return carrinho.itens.reduce((soma, i) => soma + Math.round(i.precoOferta * 100) * i.quantidade, 0) / 100;
}

export function economiaEstimada(carrinho: Carrinho): number {
  return carrinho.itens.reduce((soma, i) => soma + Math.round((i.precoOriginal - i.precoOferta) * 100) * i.quantidade, 0) / 100;
}

/** Menor prazo de retirada entre os itens (DEC-018): e o que vale para o pedido. */
export function retiradaDoPedido(carrinho: Carrinho): string | null {
  return carrinho.itens.reduce<string | null>((menor, i) => (!menor || i.retiradaAte < menor ? i.retiradaAte : menor), null);
}

export function novaChave(): string {
  return `web-${crypto.randomUUID()}`;
}
