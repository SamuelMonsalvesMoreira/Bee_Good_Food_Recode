import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { admin, criarConta, entrar, irPeloMenu, lojaAtiva, ofertaPublicada, pedidoPelaApi } from './apoio';

// Auditoria automatica (WCAG 2.1 A/AA) das telas principais. Nao substitui teste com leitor de tela.
async function auditar(page: Page, tela: string) {
  await page.waitForLoadState('networkidle');
  const resultado = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa']).analyze();
  const problemas = resultado.violations.map((v) => `${v.impact} ${v.id}: ${v.help} (${v.nodes.length}x) ${v.nodes[0]?.target.join(' ')}`);
  expect(problemas, `Acessibilidade em ${tela}`).toEqual([]);
}

test.describe('acessibilidade @celular', () => {
  test('telas públicas e do cliente', async ({ page }) => {
    const loja = await lojaAtiva();
    const oferta = await ofertaPublicada(loja);
    await page.goto('/');
    await auditar(page, 'início');
    await page.goto(`/ofertas/${oferta.id}`);
    await auditar(page, 'detalhe da oferta');
    await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    await page.goto('/carrinho');
    await auditar(page, 'carrinho');
    await page.goto('/cadastro');
    await auditar(page, 'cadastro');
    await page.goto('/entrar');
    await auditar(page, 'entrar');
  });

  test('tema escuro e aviso rápido', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' });
    const loja = await lojaAtiva();
    const oferta = await ofertaPublicada(loja);
    const cliente = await criarConta();
    const pedido = await pedidoPelaApi(cliente, oferta.id);

    await page.goto('/');
    await expect(page.locator('html')).toHaveCSS('color-scheme', 'dark');
    await auditar(page, 'início no tema escuro');
    await page.goto(`/ofertas/${oferta.id}`);
    await page.getByRole('button', { name: 'Adicionar ao carrinho' }).click();
    // O aviso rapido (toast) aparece com acao para o carrinho e tambem passa na auditoria.
    await expect(page.getByRole('button', { name: 'Ver carrinho' })).toBeVisible();
    await auditar(page, 'detalhe da oferta com aviso no tema escuro');
    await entrar(page, cliente);
    await irPeloMenu(page, 'Meus pedidos');
    await auditar(page, 'meus pedidos no tema escuro');
    await page.getByRole('link', { name: new RegExp(`Pedido ${pedido}\\b`) }).click();
    await expect(page.getByText('Código de retirada')).toBeVisible();
    await auditar(page, 'pedido no tema escuro');
  });

  test('telas privadas do responsável e da moderação', async ({ page }) => {
    const loja = await lojaAtiva();
    const oferta = await ofertaPublicada(loja);
    const pedido = await pedidoPelaApi(await criarConta(), oferta.id);

    await entrar(page, loja.dono);
    await irPeloMenu(page, 'Minhas lojas');
    await page.getByRole('link', { name: loja.nome }).click();
    await auditar(page, 'fila de pedidos');
    await page.getByRole('button', { name: 'Produtos e ofertas' }).click();
    await auditar(page, 'produtos');
    await page.getByRole('button', { name: 'Indicadores' }).click();
    await auditar(page, 'indicadores');
    await page.getByRole('button', { name: 'Dados da loja' }).click();
    await auditar(page, 'dados da loja');
    await page.getByRole('button', { name: 'Pedidos' }).click();
    await page.getByRole('link', { name: `Pedido ${pedido}` }).click();
    await auditar(page, 'detalhe do pedido');

    await page.getByRole('button', { name: 'Sair' }).click();
    await entrar(page, await admin());
    await irPeloMenu(page, 'Moderação');
    await auditar(page, 'moderação');
    await page.getByRole('link', { name: 'Categorias' }).click();
    await auditar(page, 'categorias');
  });
});
