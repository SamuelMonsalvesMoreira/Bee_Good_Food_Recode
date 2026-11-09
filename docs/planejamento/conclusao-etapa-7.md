# Etapa 7 — Validação e entrega

Data: 06/11/2026 (America/Sao_Paulo). Estado: **parte técnica concluída localmente**, com o runner oficial aprovado e o material de apresentação pronto. Faltam as rodadas de validação com pessoas (veja o checklist no fim).

## O que foi entregue

| Item | Entrega |
|---|---|
| Testes ponta a ponta | 11 testes Playwright no navegador (desktop e celular) contra a API real, em `frontend/e2e`. Cobrem visitante, cliente, responsável e administração. |
| Acessibilidade | Auditoria axe (WCAG 2.1 A/AA) em 12 telas, sem violações. Corrigido o nome acessível do link do carrinho. |
| Dados de demonstração | `npm run demo:popular` cria lojas, ofertas e um pedido fictícios pela API, com senhas aleatórias gravadas fora do Git. |
| Preparo de deploy | Contagem de tentativas por cliente real atrás de proxy (`FORWARD_HEADERS_STRATEGY`, `TRUSTED_PROXIES`, DEC-031) e [guia de publicação](../deploy.md). |
| Compatibilidade com Java 21 | O relógio de microssegundos dividia por zero no Java 21 (a versão mínima), quebrando cadastro e login fora da máquina de desenvolvimento, que usa Java 25. Corrigido com teste de regressão; a suíte completa passou no Java 21 (159 HTTP/MySQL + 9 unitários). |
| Publicação | `backend/Dockerfile`, `render.yaml` e `frontend/vercel.json`, com [passo a passo gratuito](../deploy.md#publicação-gratuita-vercel-render-e-aiven) (Vercel, Render e Aiven). Imagem testada localmente. |
| Comparação com o mercado | [Análise](analise-de-mercado.md) de Too Good To Go, Food To Save, Phenix, Karma e Flashfood. Entraram vitrine em seções, ordenação, código de retirada, economia do cliente, selo de sacola surpresa, contagem regressiva, barra inferior no celular, avisos rápidos e tema escuro (DEC-032 a DEC-036). |
| Demonstração online | Modo demonstração com API simulada no navegador e contas fictícias de um clique, para publicar só a interface na Vercel ([DEC-037](decisoes-do-mvp.md), [passo a passo](../deploy.md#demonstração-do-tcc-só-vercel-modo-demonstração)). Aviso de projeto acadêmico, sem fins lucrativos. |
| CRUDs completos | Remoção de categorias, produtos e ofertas pela interface, preservando o que tem pedido ([DEC-038](decisoes-do-mvp.md), [atendimento ao desafio](atendimento-ao-desafio.md)). |
| Robustez da interface | Tela "Algo deu errado" no lugar de página em branco; aviso claro quando a API não responde. Também corrigido o contexto de sessão, que quebrava com o recarregamento automático do servidor de desenvolvimento. |

## Validação executada

- **Backend:** 159 testes HTTP/MySQL e 6 unitários, sem falhas. O JAR contém só as migrations V1 e V2.
- **Frontend:** typecheck, 14 testes unitários, build de produção e 11 testes no navegador. A suíte do navegador passou em três execuções seguidas.
- **Demonstração:** o script popula um banco vazio sem erros.

[Relatório da validação da Etapa 7](../../backend/evidencias/validacao-etapa-7.json)

**Depois das melhorias de mercado e da remoção nos CRUDs** ([relatório](../../backend/evidencias/validacao-vitrine-e-demo.json)): runner oficial aprovado com 171 testes HTTP/MySQL e 9 unitários; 20 unitários (6 da API simulada), as duas builds e 15 testes no navegador no frontend; axe sem violações em 17 telas, incluindo o tema escuro.

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
- [x] Executar `database/scripts/validar-banco.ps1 -Api` com PowerShell 7.6: 159 testes HTTP/MySQL, empacotamento e duas inicializações do JAR, registrados em [`ultima-validacao.json`](../../backend/evidencias/ultima-validacao.json).
- [ ] Rodada 1: pelo menos 5 consumidores adultos (H01, H02, H04), conforme [Métricas e validação](metricas-e-validacao.md).
- [ ] Rodada 2: pelo menos 3 responsáveis por pequenos negócios (H03, H05).
- [ ] Registrar os resultados agregados, sem dados identificáveis, e as mudanças decididas a partir deles.
- [x] [Roteiro de demonstração](../apresentacao/roteiro-demonstracao.md) com plano B e perguntas prováveis.
- [x] Slides da apresentação (13 slides, ligados ao ODS 8).
- [ ] Ensaiar a apresentação e ajustar os slides ao tempo disponível.
- [x] Publicar a demonstração na Vercel e conferir o link do README: [bee-good-food-recode.vercel.app](https://bee-good-food-recode.vercel.app), publicada em 09/11/2026 a partir da `main`.
- [ ] (Opcional) Publicar o sistema completo (Vercel, Render e Aiven) seguindo o [guia](../deploy.md) e repetir os testes no navegador contra o endereço publicado.
