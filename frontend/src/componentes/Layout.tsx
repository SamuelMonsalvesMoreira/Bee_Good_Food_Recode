import { Moon, ReceiptText, Search, ShieldCheck, ShoppingBag, Store, Sun } from 'lucide-react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useEffect, useRef } from 'react';
import { Toaster } from 'sonner';
import { useSessao } from '../auth/sessao';
import { useCarrinho } from '../carrinho/carrinhoContexto';
import { useTema } from '../lib/tema';
import { Bee } from './Bee';
import { AvisoDemonstracao } from './AvisoDemonstracao';

export function Layout() {
  const { usuario, tem, sair, expirou } = useSessao();
  const { quantidadeTotal } = useCarrinho();
  const { tema, alternar } = useTema();
  const navegar = useNavigate();
  const local = useLocation();
  const principal = useRef<HTMLElement>(null);
  const rotuloLoja = tem('RESPONSAVEL') ? 'Minhas lojas' : 'Tenho uma loja';

  // Troca de pagina: leva o foco para o conteudo, como uma navegacao comum.
  useEffect(() => {
    principal.current?.focus({ preventScroll: true });
    window.scrollTo(0, 0);
  }, [local.pathname]);

  const contador = quantidadeTotal > 0 && (
    <span className="carrinho-contador">
      {quantidadeTotal}<span className="sr-only">{quantidadeTotal === 1 ? ' item' : ' itens'}</span>
    </span>
  );

  return (
    <>
      <a className="pular" href="#conteudo">Pular para o conteúdo</a>
      <AvisoDemonstracao />
      <header className="cabecalho">
        <div className="conteudo cabecalho-linha">
          <Link to="/" className="marca" aria-label="Bee Good Food, página inicial">
            <Bee tamanho={38} />
            <span className="marca-nome">Bee Good Food</span>
          </Link>
          {/* Desktop: navegacao no topo. No celular, a mesma navegacao vai para a barra inferior. */}
          <nav aria-label="Principal" className="nav">
            <NavLink to="/" end>Ofertas</NavLink>
            {usuario && <NavLink to="/pedidos">Meus pedidos</NavLink>}
            {usuario && <NavLink to="/minhas-lojas">{rotuloLoja}</NavLink>}
            {tem('ADMINISTRADOR') && <NavLink to="/admin">Moderação</NavLink>}
          </nav>
          <div className="cabecalho-acoes">
            <button type="button" className="botao-icone" onClick={alternar}
              aria-label={tema === 'escuro' ? 'Usar tema claro' : 'Usar tema escuro'} title={tema === 'escuro' ? 'Tema claro' : 'Tema escuro'}>
              {tema === 'escuro' ? <Sun size={20} aria-hidden="true" /> : <Moon size={20} aria-hidden="true" />}
            </button>
            <NavLink to="/carrinho" className="carrinho-link">
              <ShoppingBag size={22} aria-hidden="true" />
              <span>Carrinho</span>
              {contador}
            </NavLink>
            {usuario ? (
              <button className="botao botao-secundario botao-pequeno" onClick={() => {
                // Sai da pagina privada antes de limpar a sessao: a proxima pessoa nao volta para ela ao entrar.
                void navegar('/', { replace: true, flushSync: true });
                sair();
              }}>
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
      <nav aria-label="Principal" className="barra-inferior">
        <NavLink to="/" end><Search aria-hidden="true" /><span>Ofertas</span></NavLink>
        {usuario && <NavLink to="/pedidos"><ReceiptText aria-hidden="true" /><span>Meus pedidos</span></NavLink>}
        <NavLink to="/carrinho" className="barra-carrinho"><ShoppingBag aria-hidden="true" /><span>Carrinho</span>{contador}</NavLink>
        {usuario && <NavLink to="/minhas-lojas"><Store aria-hidden="true" /><span>{rotuloLoja}</span></NavLink>}
        {tem('ADMINISTRADOR') && <NavLink to="/admin"><ShieldCheck aria-hidden="true" /><span>Moderação</span></NavLink>}
      </nav>
      <Toaster position="top-center" theme={tema === 'escuro' ? 'dark' : 'light'} closeButton
        toastOptions={{ className: 'aviso-rapido', duration: 3500 }} />
    </>
  );
}
