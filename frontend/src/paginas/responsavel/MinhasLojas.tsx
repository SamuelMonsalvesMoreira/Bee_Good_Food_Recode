import { Link, useNavigate } from 'react-router-dom';
import { estabelecimentos } from '../../api/endpoints';
import { useSessao } from '../../auth/sessao';
import { Carregando, ErroCarga } from '../../componentes/Estados';
import { FormularioLoja } from '../../componentes/FormularioLoja';
import { StatusLoja } from '../../componentes/StatusLoja';
import { useAcao, useCarregar } from '../../lib/useCarregar';

export function MinhasLojas() {
  const { dados, erro, carregando, recarregar } = useCarregar(() => estabelecimentos.meus(), []);
  const { atualizar } = useSessao();

  if (carregando && !dados) return <Carregando />;
  if (erro) return <ErroCarga erro={erro} tentarDeNovo={recarregar} />;
  const lojas = dados?.conteudo ?? [];

  if (lojas.length === 0) return <SolicitarLoja primeira />;

  return (
    <>
      <div className="titulo-com-acao">
        <h1>Minhas lojas</h1>
        <Link className="botao botao-secundario" to="/minhas-lojas/nova">Cadastrar outra loja</Link>
      </div>
      <ul className="lista-lojas">
        {lojas.map((loja) => (
          <li key={loja.id} className="linha-pedido">
            <div>
              <Link className="item-nome" to={`/minhas-lojas/${loja.id}`} onClick={() => { void atualizar(); }}>{loja.nome}</Link>
              <p className="pequeno suave">{loja.endereco.bairro}, {loja.endereco.cidade}</p>
            </div>
            <StatusLoja status={loja.status} />
          </li>
        ))}
      </ul>
    </>
  );
}

export function SolicitarLoja({ primeira = false }: { primeira?: boolean }) {
  const navegar = useNavigate();
  const { enviando, erro, executar } = useAcao();
  return (
    <>
      <h1>{primeira ? 'Venda o excedente da sua loja' : 'Cadastrar outra loja'}</h1>
      <p className="suave">
        Envie os dados da loja para análise. Depois da aprovação você publica ofertas, recebe pedidos e acompanha os indicadores.
        Os clientes retiram no endereço informado.
      </p>
      <FormularioLoja enviando={enviando} erro={erro} rotuloEnviar="Enviar para análise"
        aoEnviar={async (dados) => {
          const loja = await executar(() => estabelecimentos.solicitar(dados));
          if (loja) navegar(`/minhas-lojas/${loja.id}`, { state: { enviada: true } });
        }} />
    </>
  );
}
