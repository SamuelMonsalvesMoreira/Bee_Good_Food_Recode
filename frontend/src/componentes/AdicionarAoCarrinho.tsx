import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { OfertaPublica } from '../api/types';
import { useCarrinho } from '../carrinho/CarrinhoContext';
import { Dialogo } from './Dialogo';

export function AdicionarAoCarrinho({ oferta }: { oferta: OfertaPublica }) {
  const { adicionar, carrinho } = useCarrinho();
  const [quantidade, setQuantidade] = useState(1);
  const [lojaAtual, setLojaAtual] = useState<string>();
  const [mensagem, setMensagem] = useState<{ tipo: 'ok' | 'erro'; texto: string }>();
  const maximo = Math.min(oferta.estoqueDisponivel, 100);
  const noCarrinho = carrinho.itens.find((i) => i.ofertaId === oferta.id)?.quantidade ?? 0;

  function tentar(substituir: boolean) {
    const resultado = adicionar(oferta, quantidade, substituir);
    if (resultado.tipo === 'outraLoja') { setLojaAtual(resultado.lojaAtual); return; }
    setLojaAtual(undefined);
    if (resultado.tipo === 'limite') { setMensagem({ tipo: 'erro', texto: resultado.motivo }); return; }
    setMensagem({ tipo: 'ok', texto: 'Adicionado ao carrinho.' });
  }

  return (
    <div className="adicionar">
      <div className="quantidade" role="group" aria-label="Quantidade">
        <button type="button" className="botao botao-secundario botao-pequeno" aria-label="Diminuir quantidade"
          disabled={quantidade <= 1} onClick={() => setQuantidade((q) => q - 1)}>−</button>
        <output className="numero" aria-live="polite">{quantidade}</output>
        <button type="button" className="botao botao-secundario botao-pequeno" aria-label="Aumentar quantidade"
          disabled={quantidade >= maximo} onClick={() => setQuantidade((q) => q + 1)}>+</button>
      </div>
      <button className="botao botao-principal" onClick={() => tentar(false)}>Adicionar ao carrinho</button>
      {noCarrinho > 0 && <p className="pequeno suave">Você já tem {noCarrinho} no carrinho. O limite é o estoque da loja.</p>}
      {mensagem && (
        <p className={mensagem.tipo === 'ok' ? 'confirmacao' : 'erro-texto'} role="status">
          {mensagem.texto} {mensagem.tipo === 'ok' && <Link to="/carrinho">Ver carrinho</Link>}
        </p>
      )}

      <Dialogo
        aberto={lojaAtual !== undefined}
        titulo="Trocar de loja?"
        aoFechar={() => setLojaAtual(undefined)}
        acoes={(
          <>
            <button className="botao botao-principal" onClick={() => tentar(true)}>Esvaziar e adicionar</button>
            <button className="botao botao-secundario" onClick={() => setLojaAtual(undefined)}>Manter meu carrinho</button>
          </>
        )}
      >
        <p>
          Seu carrinho tem itens de <strong>{lojaAtual}</strong>. Cada pedido é retirado em uma única loja,
          então para adicionar esta oferta os itens atuais serão removidos.
        </p>
      </Dialogo>
    </div>
  );
}
