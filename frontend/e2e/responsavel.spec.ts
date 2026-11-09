import { expect, test } from '@playwright/test';
import { categoria, criarConta, entrar, irPeloMenu, lojaAtiva, ofertaPublicada, pedidoPelaApi, unico } from './apoio';

test.describe('responsável pela loja', () => {
  test('atende o pedido do início à retirada e vê os indicadores (CA13, CA14, CA17)', async ({ page }) => {
    const loja = await lojaAtiva();
    const oferta = await ofertaPublicada(loja, unico('Bolo E2E'), 5, 30, 12);
    const pedido = await pedidoPelaApi(await criarConta(), oferta.id, 2);

    await entrar(page, loja.dono);
    await irPeloMenu(page, 'Minhas lojas');
    await page.getByRole('link', { name: loja.nome }).click();
    await expect(page.getByRole('heading', { name: 'Fila de pedidos' })).toBeVisible();
    await page.getByRole('link', { name: `Pedido ${pedido}` }).click();

    for (const [acao, situacao] of [
      ['Confirmar pedido', 'Confirmado'],
      ['Iniciar preparo', 'Em preparo'],
      ['Marcar como pronto', 'Pronto para retirar'],
      ['Registrar retirada', 'Retirado'],
    ] as const) {
      await page.getByRole('button', { name: acao }).click();
      await expect(page.locator('.pedido-topo').getByText(situacao)).toBeVisible();
    }
    await expect(page.getByRole('button', { name: 'Cancelar pedido' })).toHaveCount(0);

    await page.getByRole('link', { name: 'Painel da loja' }).click();
    await page.getByRole('button', { name: 'Indicadores' }).click();
    const metricas = page.locator('dl.metricas').first();
    const metrica = (rotulo: string) => metricas.locator('div').filter({ has: page.getByText(rotulo, { exact: true }) }).locator('dd');
    await expect(metrica('Pedidos retirados')).toHaveText('1');
    await expect(metrica('Valor dos pedidos retirados')).toHaveText('R$ 24,00');
    await expect(metrica('Unidades vendidas')).toHaveText('2');
    await expect(page.getByText('Taxa de retirada')).toBeVisible();
  });

  test('cadastra produto, valida a oferta e publica no catálogo (CA06, CA25)', async ({ page }) => {
    const loja = await lojaAtiva();
    await categoria('Padaria');
    const produto = unico('Sonho E2E');

    await entrar(page, loja.dono);
    await irPeloMenu(page, 'Minhas lojas');
    await page.getByRole('link', { name: loja.nome }).click();
    await page.getByRole('button', { name: 'Produtos e ofertas' }).click();
    await page.getByLabel('Categoria').selectOption({ label: 'Padaria' });
    await page.getByLabel('Nome').fill(produto);
    await page.getByRole('button', { name: 'Cadastrar produto' }).click();
    await page.getByRole('link', { name: produto, exact: true }).first().click();

    await page.getByLabel('Preço original (R$)').fill('8,00');
    await page.getByLabel('Preço da oferta (R$)').fill('9,00');
    await page.getByLabel('Estoque').fill('3');
    await page.getByRole('button', { name: 'Criar rascunho' }).click();
    await expect(page.getByText('O preço da oferta não pode ser maior que o preço original.')).toBeVisible();

    await page.getByLabel('Preço da oferta (R$)').fill('3,50');
    await page.getByRole('button', { name: 'Criar rascunho' }).click();
    await expect(page.getByText('Oferta criada como rascunho.')).toBeVisible();
    await page.getByRole('button', { name: 'Publicar' }).click();
    await expect(page.getByText('Publicada', { exact: true })).toBeVisible();

    await irPeloMenu(page, 'Ofertas');
    await page.getByLabel('O que você procura').fill(produto);
    await page.getByRole('button', { name: 'Buscar ofertas' }).click();
    await expect(page.getByRole('heading', { name: 'Resultado da busca' })).toBeVisible();
    await expect(page.getByRole('link', { name: produto })).toBeVisible();
  });
});

test.describe('remoção pela interface (DEC-038)', () => {
  test('responsável remove oferta em rascunho e produto', async ({ page }) => {
    const loja = await lojaAtiva();
    await categoria('Padaria');
    const produto = unico('Remover E2E');

    await entrar(page, loja.dono);
    await irPeloMenu(page, 'Minhas lojas');
    await page.getByRole('link', { name: loja.nome }).click();
    await page.getByRole('button', { name: 'Produtos e ofertas' }).click();
    await page.getByLabel('Categoria').selectOption({ label: 'Padaria' });
    await page.getByLabel('Nome').fill(produto);
    await page.getByRole('button', { name: 'Cadastrar produto' }).click();
    await page.getByRole('link', { name: produto, exact: true }).first().click();

    await page.getByLabel('Preço original (R$)').fill('8,00');
    await page.getByLabel('Preço da oferta (R$)').fill('3,50');
    await page.getByLabel('Estoque').fill('3');
    await page.getByRole('button', { name: 'Criar rascunho' }).click();
    await page.getByRole('button', { name: /^Remover oferta/ }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Remover de vez' }).click();
    await expect(page.getByRole('heading', { name: 'Nenhuma oferta criada.' })).toBeVisible();

    await page.getByRole('link', { name: loja.nome }).click();
    await page.getByRole('button', { name: `Remover ${produto}` }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Manter' }).click();
    await expect(page.getByRole('link', { name: produto, exact: true })).toBeVisible();
    await page.getByRole('button', { name: `Remover ${produto}` }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Remover de vez' }).click();
    await expect(page.getByRole('link', { name: produto, exact: true })).toHaveCount(0);
  });

  test('moderação remove categoria sem produtos e recebe explicação quando há produtos', async ({ page }) => {
    const { admin } = await import('./apoio');
    const conta = await admin();
    const livre = unico('Categoria E2E');
    await entrar(page, conta);
    await irPeloMenu(page, 'Moderação');
    await page.getByRole('link', { name: 'Categorias' }).click();
    await page.getByRole('form', { name: 'Nova categoria' }).getByLabel('Nome').fill(livre);
    await page.getByRole('button', { name: 'Criar categoria' }).click();
    await page.getByRole('button', { name: `Remover ${livre}` }).click();
    await page.getByRole('dialog').getByRole('button', { name: 'Remover de vez' }).click();
    await expect(page.getByRole('button', { name: `Remover ${livre}` })).toHaveCount(0);

    await categoria('Padaria');
    const loja = await lojaAtiva();
    await ofertaPublicada(loja);
    await page.getByRole('button', { name: 'Remover Padaria' }).click();
    const dialogo = page.getByRole('dialog');
    await dialogo.getByRole('button', { name: 'Remover de vez' }).click();
    await expect(dialogo.getByText('Desative a categoria em vez de remover.')).toBeVisible();
  });
});

test.describe('administração', () => {
  test('aprova loja em análise com motivo obrigatório (CA19, CA26)', async ({ page }) => {
    const { admin, situacaoDaLoja, solicitarLoja } = await import('./apoio');
    const pendente = await solicitarLoja(await criarConta(), unico('Doceria E2E'));

    await entrar(page, await admin());
    await irPeloMenu(page, 'Moderação');
    const cartao = page.getByRole('listitem').filter({ hasText: pendente.nome });
    await cartao.getByRole('button', { name: 'Aprovar' }).click();
    const dialogo = page.getByRole('dialog');
    await expect(dialogo.getByRole('button', { name: 'Aprovar' })).toBeDisabled();
    await dialogo.getByLabel('Motivo').fill('Endereço conferido pelo teste.');
    await dialogo.getByRole('button', { name: 'Aprovar' }).click();
    await expect(page.getByRole('listitem').filter({ hasText: pendente.nome })).toHaveCount(0);

    await expect(page.getByRole('heading', { name: 'Nenhuma loja aguardando análise.' }).or(page.getByRole('listitem').first())).toBeVisible();
    expect(await situacaoDaLoja(pendente.id)).toBe('ATIVO');
  });
});
