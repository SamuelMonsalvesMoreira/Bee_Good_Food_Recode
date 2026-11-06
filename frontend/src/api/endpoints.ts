import { query, requisitar, requisitarCompleto } from './client';
import type {
  Categoria, CategoriaAdmin, Endereco, Estabelecimento, FormaPagamento, HistoricoEstabelecimento, Indicadores,
  Oferta, OfertaPublica, Pagina, Pedido, PedidoResumo, Produto, StatusEstabelecimento, StatusPedido, TokenResponse, Usuario,
} from './types';

// ---- Autenticacao ----
export const auth = {
  cadastrar: (dados: { nome: string; email: string; senha: string; maiorDeIdade: boolean }) =>
    requisitar<Usuario>('/api/v1/auth/cadastro', { metodo: 'POST', corpo: dados }),
  entrar: (email: string, senha: string) =>
    requisitar<TokenResponse>('/api/v1/auth/login', { metodo: 'POST', corpo: { email, senha } }),
  eu: () => requisitar<Usuario>('/api/v1/auth/me'),
};

// ---- Catalogo publico ----
export interface FiltrosBusca {
  texto?: string;
  categoriaId?: number;
  cidade?: string;
  bairro?: string;
  estabelecimentoId?: number;
  pagina?: number;
  tamanho?: number;
}

export const catalogo = {
  categorias: () => requisitar<Pagina<Categoria>>('/api/v1/categorias?tamanho=100'),
  buscar: (f: FiltrosBusca, sinal?: AbortSignal) =>
    requisitar<Pagina<OfertaPublica>>(`/api/v1/ofertas${query({ ...f })}`, { sinal }),
  oferta: (id: number) => requisitar<OfertaPublica>(`/api/v1/ofertas/${id}`),
};

// ---- Estabelecimentos ----
export interface EstabelecimentoForm {
  nome: string;
  descricao: string;
  endereco: Omit<Endereco, 'complemento'> & { complemento: string };
}

export const estabelecimentos = {
  meus: () => requisitar<Pagina<Estabelecimento>>('/api/v1/estabelecimentos/meus?tamanho=100'),
  detalhe: (id: number) => requisitar<Estabelecimento>(`/api/v1/estabelecimentos/${id}`),
  historico: (id: number) => requisitar<HistoricoEstabelecimento[]>(`/api/v1/estabelecimentos/${id}/historico`),
  solicitar: (dados: EstabelecimentoForm) =>
    requisitar<Estabelecimento>('/api/v1/estabelecimentos', { metodo: 'POST', corpo: dados }),
  atualizar: (id: number, dados: EstabelecimentoForm) =>
    requisitar<Estabelecimento>(`/api/v1/estabelecimentos/${id}`, { metodo: 'PUT', corpo: dados }),
};

// ---- Gestao do catalogo (responsavel) ----
export interface OfertaForm {
  precoOriginal: number;
  precoOferta: number;
  estoque: number;
  inicioVenda: string;
  vendaAte: string;
  retiradaAte: string;
}

export const gestao = {
  produtos: (estabelecimentoId: number) =>
    requisitar<Pagina<Produto>>(`/api/v1/estabelecimentos/${estabelecimentoId}/produtos?tamanho=100`),
  produto: (id: number) => requisitar<Produto>(`/api/v1/produtos/${id}`),
  criarProduto: (estabelecimentoId: number, dados: { categoriaId: number; nome: string; descricao: string }) =>
    requisitar<Produto>(`/api/v1/estabelecimentos/${estabelecimentoId}/produtos`, { metodo: 'POST', corpo: dados }),
  atualizarProduto: (id: number, dados: { categoriaId: number; nome: string; descricao: string; ativo: boolean }) =>
    requisitar<Produto>(`/api/v1/produtos/${id}`, { metodo: 'PUT', corpo: dados }),
  ofertas: (produtoId: number) => requisitar<Pagina<Oferta>>(`/api/v1/produtos/${produtoId}/ofertas?tamanho=100`),
  criarOferta: (produtoId: number, dados: OfertaForm) =>
    requisitar<Oferta>(`/api/v1/produtos/${produtoId}/ofertas`, { metodo: 'POST', corpo: dados }),
  editarOferta: (id: number, dados: OfertaForm) =>
    requisitar<Oferta>(`/api/v1/ofertas/${id}`, { metodo: 'PUT', corpo: dados }),
  acaoOferta: (id: number, acao: 'publicar' | 'pausar' | 'retomar' | 'encerrar') =>
    requisitar<Oferta>(`/api/v1/ofertas/${id}/${acao}`, { metodo: 'POST' }),
  ajustarEstoque: (id: number, ajuste: number) =>
    requisitar<Oferta>(`/api/v1/ofertas/${id}/estoque`, { metodo: 'POST', corpo: { ajuste } }),
};

// ---- Pedidos ----
export const pedidos = {
  criar: async (chave: string, formaPagamento: FormaPagamento, itens: { ofertaId: number; quantidade: number }[]) => {
    const r = await requisitarCompleto<Pedido>('/api/v1/pedidos', {
      metodo: 'POST',
      corpo: { formaPagamento, itens },
      cabecalhos: { 'Idempotency-Key': chave },
    });
    return { pedido: r.dados, repetido: r.status === 200 };
  },
  meus: (pagina: number, status?: StatusPedido) =>
    requisitar<Pagina<PedidoResumo>>(`/api/v1/pedidos${query({ pagina, tamanho: 20, status })}`),
  detalhe: (id: number) => requisitar<Pedido>(`/api/v1/pedidos/${id}`),
  doEstabelecimento: (estabelecimentoId: number, pagina: number, status?: StatusPedido) =>
    requisitar<Pagina<PedidoResumo>>(
      `/api/v1/estabelecimentos/${estabelecimentoId}/pedidos${query({ pagina, tamanho: 50, status })}`),
  avancar: (id: number, status: StatusPedido) =>
    requisitar<Pedido>(`/api/v1/pedidos/${id}/status`, { metodo: 'POST', corpo: { status } }),
  cancelar: (id: number, motivo?: string) =>
    requisitar<Pedido>(`/api/v1/pedidos/${id}/cancelar`, { metodo: 'POST', corpo: motivo ? { motivo } : {} }),
};

export const indicadores = {
  consultar: (estabelecimentoId: number, inicio: string, fim?: string) =>
    requisitar<Indicadores>(`/api/v1/estabelecimentos/${estabelecimentoId}/indicadores${query({ inicio, fim })}`),
};

// ---- Administracao ----
export const admin = {
  estabelecimentos: (status: StatusEstabelecimento | undefined, pagina: number) =>
    requisitar<Pagina<Estabelecimento>>(`/api/v1/admin/estabelecimentos${query({ status, pagina, tamanho: 20 })}`),
  moderar: (id: number, decisao: 'aprovar' | 'rejeitar' | 'suspender' | 'reativar', motivo: string) =>
    requisitar<Estabelecimento>(`/api/v1/admin/estabelecimentos/${id}/${decisao}`, { metodo: 'POST', corpo: { motivo } }),
  categorias: () => requisitar<Pagina<CategoriaAdmin>>('/api/v1/admin/categorias?tamanho=100'),
  criarCategoria: (nome: string) =>
    requisitar<CategoriaAdmin>('/api/v1/admin/categorias', { metodo: 'POST', corpo: { nome } }),
  atualizarCategoria: (id: number, nome: string, ativo: boolean) =>
    requisitar<CategoriaAdmin>(`/api/v1/admin/categorias/${id}`, { metodo: 'PUT', corpo: { nome, ativo } }),
};
