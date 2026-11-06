import { expect, test } from '@playwright/test';
import { criarConta, irPeloMenu, lojaAtiva, ofertaPublicada, pedidoPelaApi, unico } from './apoio';

test.describe('visitante e cliente', () => {
  test('a pagina inicial explica a proposta e a busca filtra por cidade sem acento @celular', async ({ page }) => {
    const cidade = unico('Cidade');
    const loja = await lojaAtiva(undefined, cidade, 'Centro');
    const oferta = await ofertaPublicada(loja, unico('Broa de milho'));

    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toContainText('pronto para retirar');
    await expect(page.getByText('O pagamento é feito na retirada')).toBeVisible();

    await page.getByLabel('Cidade').fill(cidade.toUpperCase());
    await page.getByRole('button', { name: 'Buscar ofertas' }).click();
    await expect(page.getByText('1 oferta', { exact: true })).toBeVisible();
    const cartao = page.getByRole('article').filter({ hasText: oferta.produto });
    await expect(cartao.getByText('Compre até')).toBeVisible();
    await expect(cartao.getByText('Retire até')).toBeVisible();

    await cartao.getByRole('link', { name: oferta.produto }).click();
    await expect(page.getByRole('heading', { level: 1, name: oferta.produto })).toBeVisible();
    await expect(page.getByText(/Prazos no horário de Brasília/)).toBeVisible();
  });

  test('cadastro, carrinho de uma loja, reserva e cancelamento (CA07, CA15, CA18)', async ({ page }) => {
    const lojaA = await lojaAtiva(unico('Padaria A'));
    const lojaB = await lojaAtiva(unico('Padaria B'));
    const ofertaA = await ofertaPublicada(lojaA, unico('Pão A'), 5, 10, 4);
    const ofertaB = await ofertaPublicada(lojaB, unico('Pão B'));
    const email = `e2e-${Date.now()}@demo.invalid`;

    await page.goto('/cadastro');
    await page.getByLabel('Nome').fill('Cliente E2E');
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Senha').fill('senha-de-teste-123');
    await page.getByRole('button', { name: 'Criar conta' }).click();
    await expect(page.getByText('É necessário declarar que você tem 18 anos ou mais.')).toBeVisible();
    await page.getByLabel('Declaro que tenho 18 anos ou mais.').check();
    await page.getByRole('button', { name: 'Criar conta' }).click();
    await expect(page.getByText('Conta criada. Agora é só entrar.')).toBeVisible();
    await page.getByLabel('E-mail').fill(email);
    await page.getByLabel('Senha').fill('senha-de-teste-123');
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Sair' })).toBeVisible();

    // Navegacao interna: a sessao vive so em memoria.
    await page.getByLabel('O que você procura').fill(ofertaA.produto);
    await page.getByRole('button', { name: 'Buscar ofertas' }).click();
    await page.getByRole('link', { name: ofertaA.produto }).click();
    await page.getByRole('button', { name: 'Aumentar quantidade' }).click();
    await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    await expect(page.getByText('Adicionado ao carrinho.')).toBeVisible();

    await irPeloMenu(page, 'Ofertas');
    await page.getByLabel('O que você procura').fill(ofertaB.produto);
    await page.getByRole('button', { name: 'Buscar ofertas' }).click();
    await page.getByRole('link', { name: ofertaB.produto }).click();
    await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    const dialogo = page.getByRole('dialog', { name: 'Trocar de loja?' });
    await expect(dialogo).toBeVisible();
    await dialogo.getByRole('button', { name: 'Manter meu carrinho' }).click();
    await expect(dialogo).toBeHidden();

    await page.getByRole('link', { name: /^Carrinho/ }).click();
    await expect(page.getByRole('heading', { name: `Retirada em ${lojaA.nome}` })).toBeVisible();
    await expect(page.getByText(ofertaB.produto)).toHaveCount(0);
    await page.getByLabel('Dinheiro na retirada').check();
    await page.getByRole('button', { name: 'Confirmar reserva' }).click();

    await expect(page.getByText('Reserva feita.')).toBeVisible();
    await expect(page.locator('.pedido-topo').getByText('Aguardando a loja')).toBeVisible();
    await expect(page.getByRole('cell', { name: 'R$ 8,00' }).first()).toBeVisible();
    await expect(page.getByText('Pagamento: Dinheiro na retirada.')).toBeVisible();

    await page.getByRole('button', { name: 'Cancelar pedido' }).click();
    const cancelar = page.getByRole('dialog', { name: 'Cancelar este pedido?' });
    await cancelar.getByLabel('Motivo (opcional)').fill('Mudei de ideia');
    await cancelar.getByRole('button', { name: 'Cancelar pedido' }).click();
    await expect(page.locator('.pedido-topo').getByText('Cancelado')).toBeVisible();
    await expect(page.getByText('Motivo: Mudei de ideia')).toBeVisible();

    // CA18: recarregar encerra a sessao.
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible();
  });

  test('pedido de outra pessoa aparece como nao encontrado (CA03)', async ({ page }) => {
    const loja = await lojaAtiva();
    const oferta = await ofertaPublicada(loja);
    const id = await pedidoPelaApi(await criarConta(), oferta.id);
    const intrusa = await criarConta();
    // Link direto: o app pede login e depois volta para o endereco pedido.
    await page.goto(`/pedidos/${id}`);
    await page.getByLabel('E-mail').fill(intrusa.email);
    await page.getByLabel('Senha').fill(intrusa.senha);
    await page.getByRole('button', { name: 'Entrar', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Pedido não encontrado.' })).toBeVisible();
    await expect(page.getByText(oferta.produto)).toHaveCount(0);
  });
});
