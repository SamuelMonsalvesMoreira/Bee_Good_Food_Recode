import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useSessao } from '../auth/SessaoContext';
import { Bee } from '../componentes/Bee';
import { Campo, erroDoCampo } from '../componentes/Campo';
import { ErroEnvio } from '../componentes/Estados';
import { useAcao } from '../lib/useCarregar';

export function Entrar() {
  const { entrar } = useSessao();
  const navegar = useNavigate();
  const voltar = (useLocation().state as { voltar?: string; cadastrado?: boolean } | null);
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const { enviando, erro, executar } = useAcao();

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const usuario = await executar(() => entrar(email, senha));
    if (usuario) navegar(voltar?.voltar && voltar.voltar !== '/entrar' ? voltar.voltar : '/', { replace: true });
  }

  return (
    <div className="cartao-formulario">
      <Bee tamanho={64} />
      <h1>Entrar</h1>
      {voltar?.cadastrado && <div className="aviso aviso-sucesso" role="status"><p>Conta criada. Agora é só entrar.</p></div>}
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
