import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { pedidos } from '../api/endpoints';
import type { FormaPagamento } from '../api/types';
import { useSessao } from '../auth/sessao';
import { economiaEstimada, retiradaDoPedido, totalEstimado } from '../carrinho/carrinho';
import { useCarrinho } from '../carrinho/carrinhoContexto';
import { Vazio } from '../componentes/Estados';
import { dinheiro, nomeFuso, prazo, ROTULO_PAGAMENTO } from '../lib/formato';
import { useAcao } from '../lib/useCarregar';

const FORMAS: FormaPagamento[] = ['PIX', 'DINHEIRO', 'CARTAO_NA_RETIRADA'];

export function Carrinho() {
  const { carrinho, alterar, esvaziar, chaveDaTentativa } = useCarrinho();
  const { usuario } = useSessao();
  const navegar = useNavigate();
  const [forma, setForma] = useState<FormaPagamento>('PIX');
  const { enviando, erro, executar } = useAcao();

  const loja = carrinho.estabelecimento;
  if (!loja || carrinho.itens.length === 0) {
    return (
      <Vazio titulo="Seu carrinho está vazio." acao={<Link className="botao botao-principal" to="/">Ver ofertas</Link>}>
        Escolha ofertas de uma loja para reservar e retirar no balcão.
      </Vazio>
    );
  }
  const retirada = retiradaDoPedido(carrinho);

  async function confirmar(evento: FormEvent) {
    evento.preventDefault();
    // Mesma chave em uma nova tentativa da mesma compra: a API devolve o pedido ja criado, sem cobrar estoque de novo.
    const chave = chaveDaTentativa();
    const resultado = await executar(() => pedidos.criar(chave, forma,
      carrinho.itens.map((i) => ({ ofertaId: i.ofertaId, quantidade: i.quantidade }))));
    if (resultado) {
      esvaziar();
      navegar(`/pedidos/${resultado.pedido.id}`, { state: { novo: true } });
    }
  }

  return (
    <div className="carrinho">
      <h1>Carrinho</h1>
      <div className="carrinho-grade">
        <section aria-labelledby="titulo-itens">
          <h2 id="titulo-itens" className="h-menor">Retirada em {loja.nome}</h2>
          <p className="suave">{loja.bairro}, {loja.cidade}</p>
          <ul className="itens">
            {carrinho.itens.map((item) => (
              <li key={item.ofertaId} className="item">
                <div>
                  <Link to={`/ofertas/${item.ofertaId}`} className="item-nome">{item.produtoNome}</Link>
                  <p className="pequeno suave">{dinheiro(item.precoOferta)} cada. Retire até {prazo(item.retiradaAte, loja.fusoIana)}.</p>
                </div>
                <div className="quantidade" role="group" aria-label={`Quantidade de ${item.produtoNome}`}>
                  <button type="button" className="botao botao-secundario botao-pequeno" aria-label="Diminuir"
                    onClick={() => alterar(item.ofertaId, item.quantidade - 1)}>−</button>
                  <output className="numero">{item.quantidade}</output>
                  <button type="button" className="botao botao-secundario botao-pequeno" aria-label="Aumentar"
                    disabled={item.quantidade >= item.maximo} onClick={() => alterar(item.ofertaId, item.quantidade + 1)}>+</button>
                </div>
                <strong className="numero item-subtotal">{dinheiro(item.precoOferta * item.quantidade)}</strong>
                <button type="button" className="botao botao-texto pequeno" onClick={() => alterar(item.ofertaId, 0)}>
                  Remover<span className="sr-only"> {item.produtoNome}</span>
                </button>
              </li>
            ))}
          </ul>
        </section>

        <form className="painel painel-cinza resumo" onSubmit={confirmar} aria-labelledby="titulo-resumo">
          <h2 id="titulo-resumo" className="h-menor">Resumo</h2>
          <dl className="resumo-linhas">
            <div><dt>Total estimado</dt><dd className="numero">{dinheiro(totalEstimado(carrinho))}</dd></div>
            <div><dt>Você economiza</dt><dd className="numero">{dinheiro(economiaEstimada(carrinho))}</dd></div>
            {retirada && <div><dt>Retire até</dt><dd>{prazo(retirada, loja.fusoIana)}</dd></div>}
          </dl>
          <p className="pequeno suave">
            O prazo do pedido é o menor entre os itens, no {nomeFuso(loja.fusoIana)}. O valor final é confirmado pela loja no pedido.
          </p>
          <fieldset>
            <legend className="h-menor">Como vai pagar na retirada</legend>
            {FORMAS.map((f) => (
              <label key={f} className="marcar opcao">
                <input type="radio" name="forma" value={f} checked={forma === f} onChange={() => setForma(f)} />
                <span>{ROTULO_PAGAMENTO[f]}</span>
              </label>
            ))}
            <p className="pequeno suave">Nada é cobrado pelo site. Você paga diretamente à loja.</p>
          </fieldset>
          {erro && (
            <div className="aviso aviso-erro" role="alert">
              <p>{erro.detalhe}</p>
              {erro.status === 409 && <p className="pequeno">Ajuste as quantidades ou remova o item indisponível e tente de novo.</p>}
            </div>
          )}
          {usuario ? (
            <button className="botao botao-principal botao-largo" disabled={enviando}>{enviando ? 'Reservando…' : 'Confirmar reserva'}</button>
          ) : (
            <Link className="botao botao-principal botao-largo" to="/entrar" state={{ voltar: '/carrinho' }}>Entrar para reservar</Link>
          )}
        </form>
      </div>
    </div>
  );
}
