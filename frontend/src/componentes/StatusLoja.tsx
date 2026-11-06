import type { StatusEstabelecimento, StatusOferta } from '../api/types';
import { ROTULO_STATUS_LOJA, ROTULO_STATUS_OFERTA } from '../lib/formato';

const LOJA: Record<StatusEstabelecimento, string> = {
  PENDENTE_ANALISE: 'etiqueta-alerta',
  ATIVO: 'etiqueta-ok',
  REJEITADO: 'etiqueta-erro',
  SUSPENSO: 'etiqueta-erro',
};

export function StatusLoja({ status }: { status: StatusEstabelecimento }) {
  return <span className={`etiqueta ${LOJA[status]}`}>{ROTULO_STATUS_LOJA[status]}</span>;
}

const OFERTA: Record<StatusOferta, string> = { RASCUNHO: '', PUBLICADA: 'etiqueta-ok', PAUSADA: 'etiqueta-alerta', ENCERRADA: 'etiqueta-erro' };

export function StatusOfertaEtiqueta({ status }: { status: StatusOferta }) {
  return <span className={`etiqueta ${OFERTA[status]}`}>{ROTULO_STATUS_OFERTA[status]}</span>;
}
