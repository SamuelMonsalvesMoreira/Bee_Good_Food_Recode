import { createBrowserRouter, Link, RouterProvider } from 'react-router-dom';
import { ExigeSessao } from './auth/ExigeSessao';
import { SessaoProvider } from './auth/SessaoContext';
import { CarrinhoProvider } from './carrinho/CarrinhoContext';
import { Vazio } from './componentes/Estados';
import { Layout } from './componentes/Layout';
import { Admin, AdminCategorias, AdminLojas } from './paginas/admin/Admin';
import { Cadastro } from './paginas/Cadastro';
import { Carrinho } from './paginas/Carrinho';
import { Entrar } from './paginas/Entrar';
import { Inicio } from './paginas/Inicio';
import { Loja } from './paginas/Loja';
import { OfertaDetalhe } from './paginas/OfertaDetalhe';
import { PedidoDetalhe } from './paginas/PedidoDetalhe';
import { Pedidos } from './paginas/Pedidos';
import { MinhasLojas, SolicitarLoja } from './paginas/responsavel/MinhasLojas';
import { PainelLoja } from './paginas/responsavel/PainelLoja';
import { ProdutoOfertas } from './paginas/responsavel/ProdutoOfertas';

function NaoEncontrada() {
  return <Vazio titulo="Página não encontrada." acao={<Link className="botao botao-principal" to="/">Ver ofertas</Link>} />;
}

const rotas = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <Layout />,
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
          { path: 'minhas-lojas', element: <MinhasLojas /> },
          { path: 'minhas-lojas/nova', element: <SolicitarLoja /> },
          { path: 'minhas-lojas/:id', element: <PainelLoja /> },
        ],
      },
      {
        element: <ExigeSessao papel="RESPONSAVEL" />,
        children: [{ path: 'minhas-lojas/:id/produtos/:produtoId', element: <ProdutoOfertas /> }],
      },
      {
        element: <ExigeSessao papel="ADMINISTRADOR" />,
        children: [{
          path: 'admin',
          element: <Admin />,
          children: [{ index: true, element: <AdminLojas /> }, { path: 'categorias', element: <AdminCategorias /> }],
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
