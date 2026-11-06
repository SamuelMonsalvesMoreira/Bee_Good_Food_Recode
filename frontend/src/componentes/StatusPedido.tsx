import type { StatusPedido as Status } from '../api/types';
import { ROTULO_STATUS_PEDIDO } from '../lib/formato';

const CLASSE: Record<Status, string> = {
  PENDENTE: 'etiqueta-alerta',
  CONFIRMADO: '',
  EM_PREPARO: '',
  PRONTO_PARA_RETIRADA: 'etiqueta-ok',
  CONCLUIDO: 'etiqueta-ok',
  CANCELADO: 'etiqueta-erro',
};

/** Estado sempre em texto, nunca so por cor (criterio de interface). */
export function StatusPedido({ status }: { status: Status }) {
  return <span className={`etiqueta ${CLASSE[status]}`}>{ROTULO_STATUS_PEDIDO[status]}</span>;
}

const ETAPAS: Status[] = ['PENDENTE', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO'];

/** Linha do tempo do pedido: etapa real em sequencia, por isso numerada. */
export function EtapasPedido({ status }: { status: Status }) {
  if (status === 'CANCELADO') return null;
  const atual = ETAPAS.indexOf(status);
  return (
    <ol className="etapas" aria-label="Andamento do pedido">
      {ETAPAS.map((etapa, i) => (
        <li key={etapa} className={i < atual ? 'feita' : i === atual ? 'atual' : ''} aria-current={i === atual ? 'step' : undefined}>
          {ROTULO_STATUS_PEDIDO[etapa]}
        </li>
      ))}
    </ol>
  );
}
