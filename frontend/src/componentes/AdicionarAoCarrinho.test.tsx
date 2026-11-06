import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import type { OfertaPublica } from '../api/types';
import { SessaoProvider } from '../auth/SessaoContext';
import { CarrinhoProvider } from '../carrinho/CarrinhoContext';
import { useCarrinho } from '../carrinho/carrinhoContexto';
import { AdicionarAoCarrinho } from './AdicionarAoCarrinho';

function oferta(id: number, lojaId: number): OfertaPublica {
  return {
    id, produtoId: id, produtoNome: `Pão ${id}`, produtoDescricao: null, categoria: { id: 1, nome: 'Padaria', slug: 'padaria' },
    precoOriginal: 10, precoOferta: 5, estoqueDisponivel: 3,
    inicioVenda: '2026-11-06T10:00:00Z', vendaAte: '2099-11-06T21:00:00Z', retiradaAte: '2099-11-06T23:00:00Z',
    estabelecimento: { id: lojaId, nome: `Loja ${lojaId}`, logradouro: 'Rua', numero: '1', complemento: null, bairro: 'Centro', cidade: 'Santos', estado: 'SP', fusoIana: 'America/Sao_Paulo' },
  };
}

function Conteudo() {
  const { carrinho } = useCarrinho();
  return <p data-testid="loja">{carrinho.estabelecimento?.nome ?? 'vazio'}:{carrinho.itens.map((i) => `${i.ofertaId}x${i.quantidade}`).join(',')}</p>;
}

function montar(o: OfertaPublica) {
  return render(
    <SessaoProvider><CarrinhoProvider><MemoryRouter>
      <AdicionarAoCarrinho oferta={o} /><Conteudo />
    </MemoryRouter></CarrinhoProvider></SessaoProvider>,
  );
}

describe('adicionar ao carrinho', () => {
  beforeEach(() => sessionStorage.clear());

  it('respeita o estoque no seletor de quantidade', async () => {
    montar(oferta(1, 1));
    const mais = screen.getByRole('button', { name: 'Aumentar quantidade' });
    await userEvent.click(mais);
    await userEvent.click(mais);
    expect(mais).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar ao carrinho' }));
    expect(screen.getByTestId('loja')).toHaveTextContent('Loja 1:1x3');
  });

  it('oferta de outra loja abre confirmacao e so troca se a pessoa confirmar (CA07)', async () => {
    const { unmount } = montar(oferta(1, 1));
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar ao carrinho' }));
    unmount();
    montar(oferta(2, 2));
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar ao carrinho' }));
    expect(screen.getByRole('dialog', { name: 'Trocar de loja?' })).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: 'Manter meu carrinho' }));
    expect(screen.getByTestId('loja')).toHaveTextContent('Loja 1:1x1');
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar ao carrinho' }));
    await userEvent.click(screen.getByRole('button', { name: 'Esvaziar e adicionar' }));
    expect(screen.getByTestId('loja')).toHaveTextContent('Loja 2:2x1');
  });
});
