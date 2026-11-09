// Modo demonstracao (build publicada sem API, ex.: Vercel para o TCC). Ativado por VITE_MODO_DEMO=true.
export const MODO_DEMO = import.meta.env.VITE_MODO_DEMO === 'true';

// Contas ficticias da demonstracao. Sao publicas de proposito: existem so na API simulada do navegador.
export const CONTAS_DEMO = [
  { chave: 'cliente', email: 'cliente@demo.bee', senha: 'bee-demonstracao', rotulo: 'Entrar como cliente', descricao: 'Reserve ofertas e acompanhe pedidos.' },
  { chave: 'loja', email: 'loja@demo.bee', senha: 'bee-demonstracao', rotulo: 'Entrar como dona de loja', descricao: 'Atenda pedidos, publique ofertas e veja indicadores.' },
  { chave: 'admin', email: 'admin@demo.bee', senha: 'bee-demonstracao', rotulo: 'Entrar na moderação', descricao: 'Aprove lojas e gerencie categorias.' },
] as const;
