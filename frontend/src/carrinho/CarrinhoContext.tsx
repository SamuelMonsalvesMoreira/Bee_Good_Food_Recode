import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { OfertaPublica } from '../api/types';
import { useSessao } from '../auth/SessaoContext';
import { adicionar, alterarQuantidade, novaChave, VAZIO, type Carrinho, type ResultadoAdicao } from './carrinho';

// Guardado na aba (sessionStorage): nao contem dados pessoais nem token. Limpo no logout.
const CHAVE = 'bee.carrinho.v1';

interface CarrinhoApi {
  carrinho: Carrinho;
  quantidadeTotal: number;
  adicionar: (oferta: OfertaPublica, quantidade: number, substituirLoja?: boolean) => ResultadoAdicao;
  alterar: (ofertaId: number, quantidade: number) => void;
  esvaziar: () => void;
  /** Mesma compra reenviada usa a mesma chave; qualquer mudanca no carrinho gera outra. */
  chaveDaTentativa: () => string;
}

const CarrinhoContext = createContext<CarrinhoApi | null>(null);

function ler(): Carrinho {
  try {
    const salvo = sessionStorage.getItem(CHAVE);
    return salvo ? (JSON.parse(salvo) as Carrinho) : VAZIO;
  } catch {
    return VAZIO;
  }
}

export function CarrinhoProvider({ children }: { children: ReactNode }) {
  const [carrinho, setCarrinho] = useState<Carrinho>(ler);
  const { usuario } = useSessao();

  useEffect(() => {
    try {
      sessionStorage.setItem(CHAVE, JSON.stringify(carrinho));
    } catch {
      // Sem armazenamento disponivel: o carrinho continua funcionando em memoria.
    }
  }, [carrinho]);

  const [usuarioAnterior, setUsuarioAnterior] = useState(usuario?.id);
  if (usuario?.id !== usuarioAnterior) {
    // Logout ou troca de conta: o carrinho nao passa para outra pessoa.
    if (usuarioAnterior !== undefined) setCarrinho(VAZIO);
    setUsuarioAnterior(usuario?.id);
  }

  const adicionarItem = useCallback((oferta: OfertaPublica, quantidade: number, substituirLoja = false) => {
    const resultado = adicionar(carrinho, oferta, quantidade, substituirLoja);
    if (resultado.tipo === 'adicionado') setCarrinho(resultado.carrinho);
    return resultado;
  }, [carrinho]);

  const chaveDaTentativa = useCallback(() => {
    if (carrinho.chaveCheckout) return carrinho.chaveCheckout;
    const chave = novaChave();
    setCarrinho((c) => ({ ...c, chaveCheckout: chave }));
    return chave;
  }, [carrinho.chaveCheckout]);

  const valor = useMemo<CarrinhoApi>(() => ({
    carrinho,
    quantidadeTotal: carrinho.itens.reduce((s, i) => s + i.quantidade, 0),
    adicionar: adicionarItem,
    alterar: (ofertaId, quantidade) => setCarrinho((c) => alterarQuantidade(c, ofertaId, quantidade)),
    esvaziar: () => setCarrinho(VAZIO),
    chaveDaTentativa,
  }), [carrinho, adicionarItem, chaveDaTentativa]);

  return <CarrinhoContext.Provider value={valor}>{children}</CarrinhoContext.Provider>;
}

export function useCarrinho(): CarrinhoApi {
  const contexto = useContext(CarrinhoContext);
  if (!contexto) throw new Error('useCarrinho fora do CarrinhoProvider');
  return contexto;
}
