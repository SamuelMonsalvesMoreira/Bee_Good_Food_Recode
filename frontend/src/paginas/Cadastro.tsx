import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { auth } from '../api/endpoints';
import { Campo, erroDoCampo } from '../componentes/Campo';
import { ErroEnvio } from '../componentes/Estados';
import { useAcao } from '../lib/useCarregar';

export function Cadastro() {
  const navegar = useNavigate();
  const estado = useLocation().state as { voltar?: string } | null;
  const [dados, setDados] = useState({ nome: '', email: '', senha: '', maiorDeIdade: false });
  const { enviando, erro, executar } = useAcao();
  const bytesSenha = new TextEncoder().encode(dados.senha).length;

  async function enviar(evento: FormEvent) {
    evento.preventDefault();
    const criado = await executar(() => auth.cadastrar(dados));
    if (criado) navegar('/entrar', { state: { ...estado, cadastrado: true } });
  }

  return (
    <div className="cartao-formulario">
      <h1>Criar conta</h1>
      <p className="suave">Para reservar ofertas e acompanhar seus pedidos.</p>
      <ErroEnvio erro={erro} />
      <form onSubmit={enviar} noValidate>
        <Campo rotulo="Nome" autoComplete="name" required maxLength={150} value={dados.nome}
          onChange={(e) => setDados({ ...dados, nome: e.target.value })} erro={erroDoCampo(erro, 'nome')} />
        <Campo rotulo="E-mail" type="email" autoComplete="email" required maxLength={320} value={dados.email}
          onChange={(e) => setDados({ ...dados, email: e.target.value })} erro={erroDoCampo(erro, 'email')} />
        <Campo rotulo="Senha" type="password" autoComplete="new-password" required minLength={12} value={dados.senha}
          onChange={(e) => setDados({ ...dados, senha: e.target.value })}
          ajuda={bytesSenha > 72 ? 'A senha está longa demais (máximo de 72 bytes).' : 'Use de 12 a 72 caracteres. Uma frase fácil de lembrar funciona bem.'}
          erro={erroDoCampo(erro, 'senha')} />
        <div className="campo" data-erro={erroDoCampo(erro, 'maiorDeIdade') ? 'true' : undefined}>
          <label className="marcar">
            <input type="checkbox" checked={dados.maiorDeIdade} onChange={(e) => setDados({ ...dados, maiorDeIdade: e.target.checked })} />
            <span>Declaro que tenho 18 anos ou mais.</span>
          </label>
          {erroDoCampo(erro, 'maiorDeIdade') && <span className="erro">{erroDoCampo(erro, 'maiorDeIdade')}</span>}
        </div>
        <button className="botao botao-principal botao-largo" disabled={enviando}>{enviando ? 'Criando…' : 'Criar conta'}</button>
      </form>
      <p>Já tem conta? <Link to="/entrar" state={estado}>Entrar</Link></p>
    </div>
  );
}
