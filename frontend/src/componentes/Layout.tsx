import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useRef } from 'react';
import { useSessao } from '../auth/sessao';
import { useCarrinho } from '../carrinho/carrinhoContexto';
import { Bee } from './Bee';

export function Layout() {
  const { usuario, tem, sair, expirou } = useSessao();
  const { quantidadeTotal } = useCarrinho();
  const navegar = useNavigate();
  const local = useLocation();
  const principal = useRef<HTMLElement>(null);

  // Troca de pagina: leva o foco para o conteudo, como uma navegacao comum.
  useEffect(() => {
    principal.current?.focus({ preventScroll: true });
    window.scrollTo(0, 0);
  }, [local.pathname]);

  return (
    <>
      <a className="pular" href="#conteudo">Pular para o conteúdo</a>
      <header className="cabecalho">
        <div className="conteudo cabecalho-linha">
          <Link to="/" className="marca" aria-label="Bee Good Food, página inicial">
            <Bee tamanho={38} />
            <span className="marca-nome">Bee Good Food</span>
          </Link>
          <nav aria-label="Principal" className="nav">
            <NavLink to="/" end>Ofertas</NavLink>
            {usuario && <NavLink to="/pedidos">Meus pedidos</NavLink>}
            {usuario && <NavLink to="/minhas-lojas">{tem('RESPONSAVEL') ? 'Minhas lojas' : 'Tenho uma loja'}</NavLink>}
            {tem('ADMINISTRADOR') && <NavLink to="/admin">Moderação</NavLink>}
          </nav>
          <div className="cabecalho-acoes">
            <NavLink to="/carrinho" className="carrinho-link">
              <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M5 7h14l-1.5 11.5a2 2 0 0 1-2 1.5h-7a2 2 0 0 1-2-1.5z" /><path d="M9 7a3 3 0 0 1 6 0" />
              </svg>
              <span>Carrinho</span>
              {quantidadeTotal > 0 && (
                <span className="carrinho-contador">
                  {quantidadeTotal}<span className="sr-only">{quantidadeTotal === 1 ? ' item' : ' itens'}</span>
                </span>
              )}
            </NavLink>
            {usuario ? (
              <button className="botao botao-secundario botao-pequeno" onClick={() => { navegar('/'); sair(); }}>
                Sair
              </button>
            ) : (
              <Link className="botao botao-principal botao-pequeno" to="/entrar" state={{ voltar: local.pathname }}>Entrar</Link>
            )}
          </div>
        </div>
      </header>
      {expirou && !usuario && (
        <div className="conteudo">
          <div className="aviso aviso-info" role="status">
            <p>Sua sessão terminou (ela dura 15 minutos). <Link to="/entrar" state={{ voltar: local.pathname }}>Entre novamente</Link> para continuar.</p>
          </div>
        </div>
      )}
      <main id="conteudo" className="conteudo" ref={principal} tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="rodape">
        <div className="conteudo rodape-linha">
          <div>
            <p className="rodape-marca">Bee Good Food</p>
            <p className="suave pequeno">
              Excedentes do dia de pequenos negócios, com desconto, para retirar na loja.
              Pagamento combinado e feito na retirada. Para maiores de 18 anos.
            </p>
          </div>
          <p className="ods pequeno">
            <span className="ods-selo" aria-hidden="true">8</span>
            Alinhado ao ODS 8: trabalho decente e crescimento econômico para pequenos negócios locais.
          </p>
        </div>
      </footer>
    </>
  );
}
