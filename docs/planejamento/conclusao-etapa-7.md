# Etapa 7 — Validação e entrega

Data: 06/11/2026 (America/Sao_Paulo). Estado: **parte técnica concluída localmente**. Ainda faltam as rodadas com pessoas, o runner oficial e a apresentação (veja o checklist no fim).

## O que foi entregue

| Item | Entrega |
|---|---|
| Testes ponta a ponta | 11 testes Playwright no navegador (desktop e celular) contra a API real, em `frontend/e2e`. Cobrem visitante, cliente, responsável e administração. |
| Acessibilidade | Auditoria axe (WCAG 2.1 A/AA) em 12 telas, sem violações. Corrigido o nome acessível do link do carrinho. |
| Dados de demonstração | `npm run demo:popular` cria lojas, ofertas e um pedido fictícios pela API, com senhas aleatórias gravadas fora do Git. |
| Preparo de deploy | Contagem de tentativas por cliente real atrás de proxy (`FORWARD_HEADERS_STRATEGY`, `TRUSTED_PROXIES`, DEC-031) e [guia de publicação](../deploy.md). |
| Robustez da interface | Tela "Algo deu errado" no lugar de página em branco; aviso claro quando a API não responde. Também corrigido o contexto de sessão, que quebrava com o recarregamento automático do servidor de desenvolvimento. |

## Validação executada

- **Backend:** 159 testes HTTP/MySQL e 6 unitários, sem falhas. O JAR contém só as migrations V1 e V2.
- **Frontend:** typecheck, 14 testes unitários, build de produção e 11 testes no navegador. A suíte do navegador passou em três execuções seguidas.
- **Demonstração:** o script popula um banco vazio sem erros.

[Relatório da validação da Etapa 7](../../backend/evidencias/validacao-etapa-7.json)

Esses resultados são locais. A auditoria automática encontra parte dos problemas de acessibilidade, mas não substitui o uso com leitor de tela nem os testes com pessoas.

## Como rodar os testes no navegador

1. Use um MySQL **só de testes** e inicie a API apontando para ele (por exemplo, na porta 8081).
2. Crie um administrador nesse banco pelo [procedimento controlado](autenticacao.md#administração-local-controlada).
3. Na pasta `frontend`:

```powershell
$env:E2E_API = 'http://127.0.0.1:8081'
$env:BEE_ADMIN_EMAIL = 'admin-e2e@exemplo.invalid'
$env:BEE_ADMIN_SENHA = Read-Host 'Senha do administrador de testes' -MaskInput
npx playwright install chromium   # somente na primeira vez
npm run e2e
```

O Playwright inicia o site na porta 5174. Cada teste cria os próprios dados com nomes únicos.

## Checklist para concluir a entrega

- [x] Testes ponta a ponta automatizados dos fluxos principais.
- [x] Auditoria de acessibilidade automatizada.
- [x] Script de dados de demonstração.
- [x] Guia e pré-requisitos de publicação.
- [ ] Instalar o PowerShell 7 e executar `database/scripts/validar-banco.ps1 -Api`, registrando `ultima-validacao.json`.
- [ ] Rodada 1: pelo menos 5 consumidores adultos (H01, H02, H04), conforme [Métricas e validação](metricas-e-validacao.md).
- [ ] Rodada 2: pelo menos 3 responsáveis por pequenos negócios (H03, H05).
- [ ] Registrar os resultados agregados, sem dados identificáveis, e as mudanças decididas a partir deles.
- [ ] Roteiro de demonstração e apresentação ligada ao ODS 8.
- [ ] (Opcional) Publicar seguindo o [guia](../deploy.md) e repetir os testes no navegador contra o endereço publicado.
