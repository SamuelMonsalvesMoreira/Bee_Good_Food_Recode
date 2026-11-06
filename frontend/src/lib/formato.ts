// Formatacao para apresentacao. Instantes chegam em UTC; prazos sao exibidos no fuso do estabelecimento.

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function dinheiro(valor: number): string {
  return moeda.format(valor);
}

/** Desconto inteiro em %, arredondado para baixo para nunca prometer mais do que o real. */
export function desconto(original: number, oferta: number): number {
  if (original <= 0 || oferta >= original) return 0;
  return Math.floor(((original - oferta) / original) * 100);
}

/** Ex.: "hoje, 18:30", "amanhã, 09:00" ou "sex., 21/11, 18:30" no fuso informado. */
export function prazo(instante: string, fuso: string, agora: Date = new Date()): string {
  const data = new Date(instante);
  const hora = new Intl.DateTimeFormat('pt-BR', { timeZone: fuso, hour: '2-digit', minute: '2-digit' }).format(data);
  const dia = diaNoFuso(data, fuso);
  const hoje = diaNoFuso(agora, fuso);
  const amanha = diaNoFuso(new Date(agora.getTime() + 86_400_000), fuso);
  if (dia === hoje) return `hoje, ${hora}`;
  if (dia === amanha) return `amanhã, ${hora}`;
  const resto = new Intl.DateTimeFormat('pt-BR', { timeZone: fuso, weekday: 'short', day: '2-digit', month: '2-digit' }).format(data);
  return `${resto}, ${hora}`;
}

export function dataHora(instante: string, fuso: string): string {
  return new Intl.DateTimeFormat('pt-BR', { timeZone: fuso, dateStyle: 'short', timeStyle: 'short' }).format(new Date(instante));
}

/** Rotulo curto do fuso para deixar explicito em que horario o prazo vale. */
export function nomeFuso(fuso: string): string {
  if (fuso === 'America/Sao_Paulo') return 'horário de Brasília';
  return `horário de ${fuso.split('/').pop()?.replace(/_/g, ' ')}`;
}

function diaNoFuso(data: Date, fuso: string): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: fuso, year: 'numeric', month: '2-digit', day: '2-digit' }).format(data);
}

/** Minutos restantes ate o instante (negativo se passou). */
export function minutosAte(instante: string, agora: Date = new Date()): number {
  return Math.round((new Date(instante).getTime() - agora.getTime()) / 60_000);
}

/** Converte "AAAA-MM-DDTHH:MM" digitado no fuso da loja para ISO com deslocamento correto. */
export function localParaIso(local: string, fuso: string): string {
  const [data, hora] = local.split('T');
  const [ano, mes, dia] = (data ?? '').split('-').map(Number);
  const [h, m] = (hora ?? '').split(':').map(Number);
  const utcIngenuo = Date.UTC(ano!, mes! - 1, dia!, h!, m!);
  // Deslocamento do fuso naquele instante (duas passadas resolvem a maioria das transicoes de horario de verao).
  let instante = utcIngenuo - deslocamentoMs(new Date(utcIngenuo), fuso);
  instante = utcIngenuo - deslocamentoMs(new Date(instante), fuso);
  return new Date(instante).toISOString();
}

/** Valor para <input type="datetime-local"> no fuso informado. */
export function isoParaLocal(instante: string | Date, fuso: string): string {
  const partes = partesNoFuso(new Date(instante), fuso);
  return `${partes.year}-${partes.month}-${partes.day}T${partes.hour}:${partes.minute}`;
}

function deslocamentoMs(data: Date, fuso: string): number {
  const p = partesNoFuso(data, fuso);
  const comoUtc = Date.UTC(+p.year, +p.month - 1, +p.day, +p.hour, +p.minute, +p.second);
  return comoUtc - Math.floor(data.getTime() / 1000) * 1000;
}

function partesNoFuso(data: Date, fuso: string): Record<'year' | 'month' | 'day' | 'hour' | 'minute' | 'second', string> {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: fuso, hourCycle: 'h23', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit',
  }).formatToParts(data);
  const valor = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? '00';
  return { year: valor('year'), month: valor('month'), day: valor('day'), hour: valor('hour'), minute: valor('minute'), second: valor('second') };
}

export const ROTULO_STATUS_PEDIDO: Record<string, string> = {
  PENDENTE: 'Aguardando a loja',
  CONFIRMADO: 'Confirmado',
  EM_PREPARO: 'Em preparo',
  PRONTO_PARA_RETIRADA: 'Pronto para retirar',
  CONCLUIDO: 'Retirado',
  CANCELADO: 'Cancelado',
};

export const ROTULO_PAGAMENTO: Record<string, string> = {
  PIX: 'Pix na retirada',
  DINHEIRO: 'Dinheiro na retirada',
  CARTAO_NA_RETIRADA: 'Cartão na retirada',
};

export const ROTULO_STATUS_LOJA: Record<string, string> = {
  PENDENTE_ANALISE: 'Em análise',
  ATIVO: 'Ativa',
  REJEITADO: 'Precisa de ajustes',
  SUSPENSO: 'Suspensa',
};

export const ROTULO_STATUS_OFERTA: Record<string, string> = {
  RASCUNHO: 'Rascunho',
  PUBLICADA: 'Publicada',
  PAUSADA: 'Pausada',
  ENCERRADA: 'Encerrada',
};
