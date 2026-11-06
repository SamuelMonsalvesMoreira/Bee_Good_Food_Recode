# Bee Good Food — frontend React

Interface web do MVP: busca de ofertas, carrinho de uma loja, reserva para retirada, acompanhamento de pedidos, painel do responsável e moderação. Consome a API Java em `../backend`.

## Tecnologias

- React 19 + TypeScript 5.9, empacotado com Vite 8.
- React Router 7 para as rotas.
- lucide-react (ícones) e sonner (avisos rápidos).
- Vitest 5 + Testing Library para testes.
- Fontes locais via Fontsource (Bricolage Grotesque e Atkinson Hyperlegible), sem chamadas a serviços externos.
- Node 24 validado localmente.

## Executar em desenvolvimento

1. Inicie a API (veja `../backend/README.md`) em `127.0.0.1:8080`.
2. Nesta pasta:

```powershell
npm install
npm run dev
```

3. Abra `http://localhost:5173`.

O Vite encaminha `/api` para `http://127.0.0.1:8080`, então não é preciso CORS no desenvolvimento. Para outra porta da API, defina `BEE_API_PROXY` antes de `npm run dev`.

## Modo demonstração (sem API)

```powershell
npm run dev:demo     # desenvolvimento, sem backend
npm run build:demo   # build publicada na Vercel do TCC
```

Liga `VITE_MODO_DEMO=true` (arquivo `.env.demo`). O cliente HTTP passa a responder com `src/demo/servidorDemo.ts`, uma simulação da API no navegador com lojas, ofertas e pedidos fictícios gerados com horários relativos ao momento do acesso. A tela de login mostra atalhos para três contas públicas de demonstração (cliente, dona de loja e moderação), e uma faixa no topo avisa que nada é vendido nem cobrado. Os dados ficam no `sessionStorage` da aba e são gerados de novo depois de 6 horas. A simulação é carregada sob demanda e não entra na build normal. Detalhes e limites em DEC-037.

## Produção

```powershell
npm run build   # gera dist/
```

Se a interface for servida em outra origem que a API, defina `VITE_API_URL` (veja `.env.example`) no build e configure `CORS_ORIGINS` na API com a origem exata do site, por exemplo `https://bee.exemplo.com`. A API recusa curinga.

## Dados de demonstração

`npm run demo:popular` cria lojas, ofertas e um pedido fictícios pela API. Exige `BEE_ADMIN_EMAIL` e `BEE_ADMIN_SENHA` de um administrador (veja `../docs/deploy.md`). As senhas das contas criadas ficam em `.demo/contas.json`, fora do Git.

## Testes

```powershell
npm test          # Vitest
npm run typecheck # TypeScript
npm run e2e       # Playwright no navegador (veja docs/planejamento/conclusao-etapa-7.md)
```

Os testes no navegador (`e2e/`) percorrem os fluxos de visitante, cliente, responsável e administração contra a API real, em desktop e celular, e auditam a acessibilidade (WCAG 2.1 AA) com axe. Também auditam o tema escuro e o aviso rápido. Os testes unitários cobrem as regras do carrinho (uma loja por pedido, confirmação antes de trocar, limites de estoque e de itens, nova chave de idempotência a cada mudança), a conversão de horários no fuso da loja, o fluxo de troca de loja na interface e as regras principais da API simulada (busca, ordenação, login, idempotência, avanço do pedido e indicadores).

## Estrutura para estudar

```text
src/
├── api/            # Cliente HTTP, tipos dos contratos e chamadas por módulo
├── auth/           # Sessão em memória e proteção de rotas
├── carrinho/       # Regras puras do carrinho e contexto React
├── componentes/    # Layout, faixa de prazo, campos, diálogos e estados
├── demo/           # API simulada do modo demonstração (carregada sob demanda)
├── lib/            # Formatação (moeda, datas no fuso da loja), tema e hooks de carga
├── paginas/        # Telas públicas, do cliente, do responsável e da moderação
└── styles/         # Tokens de cor/tipo e estilos dos componentes
```

## Decisões que a interface respeita

- **Token só em memória** (CA18). Recarregar a página ou passar 15 minutos encerra a sessão. Não há `localStorage` de credenciais nem refresh token. Única exceção: a sessão fictícia do modo demonstração (DEC-037).
- **Tema** (DEC-036): segue o sistema; a escolha manual guarda só a preferência visual.
- **Vitrine** (DEC-032 a DEC-035): seções, ordenação, selo de sacola surpresa, código de retirada e economia do cliente, a partir da [análise de mercado](../docs/planejamento/analise-de-mercado.md).
- **Carrinho na aba** (DEC-007, DEC-028): fica em `sessionStorage`, sem dados pessoais, e é limpo no logout.
- **Uma loja por pedido** (CA07): adicionar oferta de outra loja abre uma confirmação. Nada é removido sem ela.
- **Idempotência** (CA23, DEC-029): cada tentativa de reserva gera uma `Idempotency-Key`. Reenviar a mesma compra repete a chave, e mudar o carrinho gera outra.
- **Preço e total vêm da API** (CA08). O carrinho mostra uma estimativa, e o pedido mostra o valor registrado.
- **Prazos separados** (DEC-018): "Compre até" e "Retire até" aparecem sempre juntos, no fuso da loja.
- **Acompanhamento** (DEC-030): a página do pedido em andamento se atualiza a cada 30 segundos. Não há notificação push.
- **Autorização é do servidor.** As rotas protegidas só evitam mostrar telas que a API recusaria.
