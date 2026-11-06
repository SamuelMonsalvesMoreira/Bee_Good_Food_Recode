import { describe, expect, it } from 'vitest';
import { desconto, isoParaLocal, localParaIso, prazo } from './formato';

describe('horarios no fuso da loja', () => {
  it('converte horario digitado em Sao Paulo para o instante UTC correto', () => {
    expect(localParaIso('2026-11-06T18:00', 'America/Sao_Paulo')).toBe('2026-11-06T21:00:00.000Z');
  });

  it('respeita lojas em outro fuso brasileiro', () => {
    expect(localParaIso('2026-11-06T18:00', 'America/Manaus')).toBe('2026-11-06T22:00:00.000Z');
    expect(localParaIso('2026-11-06T18:00', 'America/Noronha')).toBe('2026-11-06T20:00:00.000Z');
  });

  it('ida e volta preservam o horario local', () => {
    const local = '2026-12-31T23:30';
    expect(isoParaLocal(localParaIso(local, 'America/Sao_Paulo'), 'America/Sao_Paulo')).toBe(local);
  });

  it('mostra hoje e amanha no dia da loja, nao no do navegador', () => {
    const agora = new Date('2026-11-06T12:00:00Z');
    expect(prazo('2026-11-06T21:00:00Z', 'America/Sao_Paulo', agora)).toBe('hoje, 18:00');
    // 02:30 UTC do dia 7 ainda e dia 6 em Sao Paulo.
    expect(prazo('2026-11-07T02:30:00Z', 'America/Sao_Paulo', agora)).toBe('hoje, 23:30');
    expect(prazo('2026-11-07T12:00:00Z', 'America/Sao_Paulo', agora)).toBe('amanhã, 09:00');
  });
});

describe('desconto', () => {
  it('arredonda para baixo e nunca promete desconto inexistente', () => {
    expect(desconto(10, 4.1)).toBe(59);
    expect(desconto(10, 10)).toBe(0);
  });
});
