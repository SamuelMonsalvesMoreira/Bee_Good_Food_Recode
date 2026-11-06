import { createContext, useContext } from 'react';
import type { OfertaPublica } from '../api/types';
import type { Carrinho, ResultadoAdicao } from './carrinho';

// Contexto e hook em modulo proprio: o recarregamento do Vite (Fast Refresh) nao recria o contexto.

export interface CarrinhoApi {
  carrinho: Carrinho;
  quantidadeTotal: number;
  adicionar: (oferta: OfertaPublica, quantidade: number, substituirLoja?: boolean) => ResultadoAdicao;
  alterar: (ofertaId: number, quantidade: number) => void;
  esvaziar: () => void;
  /** Mesma compra reenviada usa a mesma chave; qualquer mudanca no carrinho gera outra. */
  chaveDaTentativa: () => string;
}

export const CarrinhoContext = createContext<CarrinhoApi | null>(null);

export function useCarrinho(): CarrinhoApi {
  const contexto = useContext(CarrinhoContext);
  if (!contexto) throw new Error('useCarrinho fora do CarrinhoProvider');
  return contexto;
}
