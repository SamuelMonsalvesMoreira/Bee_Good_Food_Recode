import { defineConfig, devices } from '@playwright/test';

// Testes ponta a ponta contra a API real. Exigem:
// - API em execucao (E2E_API, padrao http://127.0.0.1:8081) apontando para um banco SOMENTE de testes;
// - BEE_ADMIN_EMAIL e BEE_ADMIN_SENHA de uma conta ADMINISTRADOR desse banco (docs/planejamento/autenticacao.md).
// O Vite e iniciado na porta 5174 com o proxy /api apontando para E2E_API.
const API = process.env.E2E_API ?? 'http://127.0.0.1:8081';

export default defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:5174',
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'celular', use: { ...devices['Pixel 7'] }, grep: /@celular/ },
  ],
  webServer: {
    command: 'npx vite --port 5174 --strictPort --host localhost',
    url: 'http://localhost:5174',
    reuseExistingServer: false,
    env: { BEE_API_PROXY: API },
    timeout: 60_000,
  },
});
