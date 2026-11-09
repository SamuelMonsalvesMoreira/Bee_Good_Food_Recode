import { useRef, useState } from 'react';
import { toast } from 'sonner';
import { useAcao } from '../lib/useCarregar';
import { Dialogo } from './Dialogo';
import { ErroEnvio } from './Estados';

interface Props {
  /** Nome do registro: completa o nome acessivel do botao e o titulo da confirmacao. */
  nome: string;
  /** O que acontece ao confirmar, em uma frase curta. */
  explicacao: string;
  remover: () => Promise<unknown>;
  aoRemover: () => void;
}

/** Remocao com confirmacao, usada nos CRUDs de categorias, produtos e ofertas (DEC-038). */
export function BotaoRemover({ nome, explicacao, remover, aoRemover }: Props) {
  const [aberto, setAberto] = useState(false);
  const botao = useRef<HTMLButtonElement>(null);
  const acao = useAcao();

  function fechar() {
    setAberto(false);
    acao.limparErro();
    botao.current?.focus();
  }

  async function confirmar() {
    const removido = await acao.executar(async () => { await remover(); return true; });
    if (!removido) return;
    setAberto(false);
    toast.success(`${nome} removido.`);
    aoRemover();
  }

  return (
    <>
      <button ref={botao} type="button" className="botao botao-perigo botao-pequeno" onClick={() => setAberto(true)}>
        Remover<span className="sr-only"> {nome}</span>
      </button>
      {/* So existe no DOM enquanto aberto: varias linhas na tela nao repetem o id do titulo. */}
      {aberto && (
        <Dialogo
          aberto
          titulo={`Remover ${nome}?`}
          aoFechar={fechar}
          acoes={(
            <>
              <button type="button" className="botao botao-perigo" disabled={acao.enviando} onClick={() => void confirmar()}>
                {acao.enviando ? 'Removendo…' : 'Remover de vez'}
              </button>
              <button type="button" className="botao botao-secundario" onClick={fechar}>Manter</button>
            </>
          )}
        >
          <ErroEnvio erro={acao.erro} />
          <p>{explicacao} Não dá para desfazer.</p>
        </Dialogo>
      )}
    </>
  );
}
