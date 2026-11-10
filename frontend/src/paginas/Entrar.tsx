import { ShieldCheck, ShoppingBag, Store } from 'lucide-react';
import { useState, type FormEvent, type ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useSessao } from '../auth/sessao';
import { Bee } from '../componentes/Bee';
import { Campo, erroDoCampo } from '../componentes/Campo';
import { ErroEnvio } from '../componentes/Estados';
import { useAcao } from '../lib/useCarregar';
import { CONTAS_DEMO, MODO_DEMO } from '../lib/demo';

const DESTINO_DEMO: Record<(typeof CONTAS_DEMO)[number]['chave'], string> = { cliente: '/', loja: '/minhas-lojas', admin: '/admin' };
const ICONE_DEMO: Record<(typeof CONTAS_DEMO)[number]['chave'], ReactNode> = {
  cliente: <ShoppingBag aria-hidden="true" />, loja: <Store aria-hidden="true" />, admin: <ShieldCheck aria-hidden="true" />,
};

export function Entrar() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const voltar = (useLocation().state as { voltar?: string; cadastrado?: boolean } | null);
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const { enviando, erro, executar } = useAcao();

  async function autenticar(comEmail: string, comSenha: string, destino?: string) {
    const usuario = await executar(() => entrar(comEmail, comSenha));
    if (!usuario) return;
    // Quem chegou aqui vindo de outra tela (ex.: carrinho) volta para ela, inclusive pelos atalhos da demonstracao.
    // A pagina inicial nao conta como origem: quem veio dela vai para a area do perfil escolhido.
    const anterior = voltar?.voltar && voltar.voltar !== '/entrar' && voltar.voltar !== '/' ? voltar.voltar : undefined;
    navegar(anterior ?? destino ?? '/', { replace: true });
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    void autenticar(email, senha);
  }

  return (
    <div className="cartao-formulario">
      <Bee tamanho={64} />
      <h1>Entrar</h1>
      {voltar?.cadastrado && <div className="aviso aviso-sucesso" role="status"><p>Conta criada. Agora é só entrar.</p></div>}
      {MODO_DEMO && (
        <section className="contas-demo" aria-labelledby="titulo-contas-demo">
          <h2 id="titulo-contas-demo" className="h-menor">Experimente sem cadastro</h2>
          <p className="pequeno suave">Contas fictícias desta demonstração. Escolha um perfil:</p>
          <div className="contas-demo-lista">
            {CONTAS_DEMO.map((conta) => (
              <button key={conta.chave} type="button" className="conta-demo" disabled={enviando}
                onClick={() => void autenticar(conta.email, conta.senha, DESTINO_DEMO[conta.chave])}>
                {ICONE_DEMO[conta.chave]}
                <span><strong>{conta.rotulo}</strong><span className="pequeno suave">{conta.descricao}</span></span>
              </button>
            ))}
          </div>
        </section>
      )}
      <ErroEnvio erro={erro} />
      <form onSubmit={enviar} noValidate>
        <Campo rotulo="E-mail" type="email" autoComplete="email" required value={email}
          onChange={(e) => setEmail(e.target.value)} erro={erroDoCampo(erro, 'email')} />
        <Campo rotulo="Senha" type="password" autoComplete="current-password" required value={senha}
          onChange={(e) => setSenha(e.target.value)} erro={erroDoCampo(erro, 'senha')} />
        <button className="botao botao-principal botao-largo" disabled={enviando}>{enviando ? 'Entrando…' : 'Entrar'}</button>
      </form>
      <p className="pequeno suave">
        A sessão dura 15 minutos e termina ao recarregar a página, para proteger sua conta.
      </p>
      <p>Ainda não tem conta? <Link to="/cadastro" state={voltar}>Criar conta</Link></p>
    </div>
  );
}
