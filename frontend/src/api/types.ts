// Contratos da API (backend/evidencias/openapi.json). Datas chegam como instantes ISO em UTC.

export type Papel = 'CLIENTE' | 'RESPONSAVEL' | 'ADMINISTRADOR';

export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  ultima: boolean;
}

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  ativo: boolean;
  papeis: Papel[];
}

export interface TokenResponse {
  accessToken: string;
  tipoToken: string;
  expiraEmSegundos: number;
  usuario: Usuario;
}

export interface Categoria {
  id: number;
  nome: string;
  slug: string;
}

export interface CategoriaAdmin extends Categoria {
  ativo: boolean;
}

export interface OfertaPublica {
  id: number;
  produtoId: number;
  produtoNome: string;
  produtoDescricao: string | null;
  categoria: Categoria;
  precoOriginal: number;
  precoOferta: number;
  estoqueDisponivel: number;
  inicioVenda: string;
  vendaAte: string;
  retiradaAte: string;
  estabelecimento: {
    id: number;
    nome: string;
    logradouro: string;
    numero: string;
    complemento: string | null;
    bairro: string;
    cidade: string;
    estado: string;
    fusoIana: string;
  };
}

export type StatusEstabelecimento = 'PENDENTE_ANALISE' | 'ATIVO' | 'REJEITADO' | 'SUSPENSO';

export interface Endereco {
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cidade: string;
  estado: string;
  cep: string;
  fusoIana: string;
}

export interface Estabelecimento {
  id: number;
  responsavelId: number;
  nome: string;
  descricao: string | null;
  status: StatusEstabelecimento;
  endereco: Endereco;
  criadoEm: string;
}

export interface HistoricoEstabelecimento {
  id: number;
  statusAnterior: StatusEstabelecimento | null;
  statusNovo: StatusEstabelecimento;
  atorId: number | null;
  motivo: string | null;
  criadoEm: string;
}

export interface Produto {
  id: number;
  estabelecimentoId: number;
  categoriaId: number;
  nome: string;
  descricao: string | null;
  ativo: boolean;
}

export type StatusOferta = 'RASCUNHO' | 'PUBLICADA' | 'PAUSADA' | 'ENCERRADA';

export interface Oferta {
  id: number;
  produtoId: number;
  precoOriginal: number;
  precoOferta: number;
  estoqueDisponivel: number;
  inicioVenda: string;
  vendaAte: string;
  retiradaAte: string;
  status: StatusOferta;
  publicadaEm: string | null;
  encerradaEm: string | null;
}

export type StatusPedido = 'PENDENTE' | 'CONFIRMADO' | 'EM_PREPARO' | 'PRONTO_PARA_RETIRADA' | 'CONCLUIDO' | 'CANCELADO';
export type FormaPagamento = 'PIX' | 'DINHEIRO' | 'CARTAO_NA_RETIRADA';

export interface Pedido {
  id: number;
  clienteId: number;
  estabelecimentoId: number;
  status: StatusPedido;
  formaPagamento: FormaPagamento;
  total: number;
  estabelecimento: {
    nome: string;
    logradouro: string;
    numero: string;
    complemento: string | null;
    bairro: string;
    cidade: string;
    estado: string;
    cep: string;
    fusoIana: string;
  };
  retiradaAte: string;
  concluidoEm: string | null;
  criadoEm: string;
  itens: {
    ofertaId: number;
    produtoNome: string;
    precoOriginal: number;
    precoOferta: number;
    quantidade: number;
    subtotal: number;
  }[];
  historico: {
    statusAnterior: StatusPedido | null;
    statusNovo: StatusPedido;
    atorId: number | null;
    motivo: string | null;
    criadoEm: string;
  }[];
}

export interface PedidoResumo {
  id: number;
  clienteId: number;
  estabelecimentoId: number;
  estabelecimentoNome: string;
  status: StatusPedido;
  formaPagamento: FormaPagamento;
  total: number;
  economia: number;
  retiradaAte: string;
  fusoIana: string;
  criadoEm: string;
}

export interface Indicadores {
  estabelecimentoId: number;
  fusoIana: string;
  periodo: { inicio: string; fim: string; inicioLocal: string; fimLocal: string };
  consultadoEm: string;
  pedidosConcluidos: number;
  valorMovimentado: number;
  itensComercializados: number;
  economiaEstimada: number;
  estabelecimentoComAtividade: number;
  coorte: {
    pedidos: number;
    concluidos: number;
    cancelados: number;
    emAndamento: number;
    taxaConclusao: number | null;
    taxaCancelamento: number | null;
  };
}
