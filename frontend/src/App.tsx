import { createBrowserRouter, Link, RouterProvider } from 'react-router-dom';
import { ExigeSessao } from './auth/ExigeSessao';
import { SessaoProvider } from './auth/SessaoContext';
import { CarrinhoProvider } from './carrinho/CarrinhoContext';
import { Bee } from './componentes/Bee';
import { Carregando, Vazio } from './componentes/Estados';
import { Layout } from './componentes/Layout';
import { Cadastro } from './paginas/Cadastro';
import { Carrinho } from './paginas/Carrinho';
import { Entrar } from './paginas/Entrar';
import { Inicio } from './paginas/Inicio';
import { Loja } from './paginas/Loja';
import { OfertaDetalhe } from './paginas/OfertaDetalhe';
import { PedidoDetalhe } from './paginas/PedidoDetalhe';
import { Pedidos } from './paginas/Pedidos';

// Areas de loja e moderacao so sao baixadas por quem as abre: o cliente nao carrega esse codigo.
const loja = () => import('./paginas/responsavel');
const moderacao = () => import('./paginas/admin/Admin');

/** Falha inesperada na interface: nao depende de sessao nem carrinho, para nunca quebrar de novo. */
function FalhaInesperada() {
  return (
    <main className="conteudo falha" role="alert">
      <Bee tamanho={72} humor="pensativa" />
      <h1>Algo deu errado nesta tela.</h1>
      <p>Recarregue a página para continuar. Por segurança, será preciso entrar de novo; o carrinho é mantido.</p>
      <button className="botao botao-principal" onClick={() => window.location.reload()}>Recarregar a página</button>
    </main>
  );
}

function NaoEncontrada() {
  return <Vazio titulo="Página não encontrada." acao={<Link className="botao botao-principal" to="/">Ver ofertas</Link>} />;
}

const rotas = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <FalhaInesperada />,
    hydrateFallbackElement: <Carregando />,
    children: [
      { index: true, element: <Inicio /> },
      { path: 'ofertas/:id', element: <OfertaDetalhe /> },
      { path: 'lojas/:id', element: <Loja /> },
      { path: 'entrar', element: <Entrar /> },
      { path: 'cadastro', element: <Cadastro /> },
      { path: 'carrinho', element: <Carrinho /> },
      {
        element: <ExigeSessao />,
        children: [
          { path: 'pedidos', element: <Pedidos /> },
          { path: 'pedidos/:id', element: <PedidoDetalhe /> },
          { path: 'minhas-lojas', lazy: async () => ({ Component: (await loja()).MinhasLojas }) },
          { path: 'minhas-lojas/nova', lazy: async () => ({ Component: (await loja()).SolicitarLoja }) },
          { path: 'minhas-lojas/:id', lazy: async () => ({ Component: (await loja()).PainelLoja }) },
        ],
      },
      {
        element: <ExigeSessao papel="RESPONSAVEL" />,
        children: [{ path: 'minhas-lojas/:id/produtos/:produtoId', lazy: async () => ({ Component: (await loja()).ProdutoOfertas }) }],
      },
      {
        element: <ExigeSessao papel="ADMINISTRADOR" />,
        children: [{
          path: 'admin',
          lazy: async () => ({ Component: (await moderacao()).Admin }),
          children: [
            { index: true, lazy: async () => ({ Component: (await moderacao()).AdminLojas }) },
            { path: 'categorias', lazy: async () => ({ Component: (await moderacao()).AdminCategorias }) },
          ],
        }],
      },
      { path: '*', element: <NaoEncontrada /> },
    ],
  },
]);

export function App() {
  return (
    <SessaoProvider>
      <CarrinhoProvider>
        <RouterProvider router={rotas} />
      </CarrinhoProvider>
    </SessaoProvider>
  );
}
