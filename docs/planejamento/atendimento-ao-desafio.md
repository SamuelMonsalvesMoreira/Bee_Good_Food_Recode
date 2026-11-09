# Atendimento ao desafio: Projeto Final do Programa Transforma Futuros

Cada exigência do desafio e onde ela é atendida no projeto. Serve de roteiro para quem avalia.

## Clareza da proposta

| O desafio pede | Resposta |
|---|---|
| Problema | Padarias, hortifrutis e restaurantes de bairro terminam o dia com comida boa que não foi vendida e não têm um canal simples para vender esse excedente. |
| ODS | **ODS 8: Trabalho Decente e Crescimento Econômico.** O foco é gerar receita para pequenos negócios locais. |
| Público-alvo | Responsáveis por pequenos negócios de comida e consumidores adultos (18 anos ou mais, com declaração obrigatória no cadastro). |
| Impacto esperado | A loja recupera parte do valor de produtos que seriam perdidos e acompanha os resultados em indicadores. O cliente compra mais barato perto de casa. É um impacto **esperado**: a medição com pessoas reais está planejada em [Métricas e validação](metricas-e-validacao.md) e ainda não foi feita. |

## Restrições

| Restrição | Situação |
|---|---|
| Não voltado a menores de 18 anos | Cadastro exige declaração de maioridade, validada na API (DEC-014). |
| Não relacionado a esporte, saúde ou religião | O tema é comércio local de excedentes de comida. O sistema não dá orientação nutricional ou de saúde. |

## Tecnologias obrigatórias

| Tecnologia | Uso |
|---|---|
| MySQL | 12 tabelas relacionadas, criadas por [migrations](../../database/migrations), com regras de negócio em `CHECK`, `UNIQUE` e chaves estrangeiras. |
| Java | API REST com Spring Boot em [`backend/`](../../backend), documentada por OpenAPI. |
| ReactJS | Interface em [`frontend/`](../../frontend), com TypeScript e Vite. |

## CRUDs completos pela interface

O desafio pede pelo menos 3. O projeto tem 3 com as quatro operações, além de outros cadastros parciais (lojas e pedidos).

| CRUD | Quem usa | Criar | Listar | Atualizar | Remover | Onde testar na tela |
|---|---|---|---|---|---|---|
| **Categorias** | Moderação | `POST /admin/categorias` | `GET /admin/categorias` | `PUT /admin/categorias/{id}` | `DELETE /admin/categorias/{id}` | Moderação > Categorias |
| **Produtos** | Dona de loja | `POST /estabelecimentos/{id}/produtos` | `GET /estabelecimentos/{id}/produtos` | `PUT /produtos/{id}` | `DELETE /produtos/{id}` | Minhas lojas > loja > Produtos e ofertas |
| **Ofertas** | Dona de loja | `POST /produtos/{id}/ofertas` | `GET /produtos/{id}/ofertas` | `PUT /ofertas/{id}` | `DELETE /ofertas/{id}` | Minhas lojas > loja > Produtos e ofertas > produto |

**Por que às vezes a remoção é recusada.** O que já tem pedido não pode sumir, senão o histórico da loja e do cliente quebraria. Nesses casos o banco recusa, e a tela explica o que fazer: desativar a categoria ou o produto, ou encerrar a oferta. O que não tem histórico é removido de verdade ([DEC-038](decisoes-do-mvp.md)).

## Componentização

A interface fica em [`frontend/src/componentes`](../../frontend/src/componentes), com componentes reutilizados pelas páginas:

| Pedido no desafio | Componente |
|---|---|
| Navbar e Footer | `Layout` (cabeçalho, barra inferior no celular e rodapé) |
| Card | `OfertaItem` |
| Formulários | `Campo`, `CampoSelecao`, `CampoTexto`, `FormularioLoja` |
| Botões | `BotaoRemover`, `AdicionarAoCarrinho` |
| Tabelas e listas | `Paginacao`, `StatusPedido`, `StatusLoja` |
| Modal | `Dialogo` |

## Critérios de avaliação

| Critério | Onde ver |
|---|---|
| Organização do projeto | Monólito em módulos, [estrutura no README](../../README.md#estrutura-do-repositório) e [decisões registradas](decisoes-do-mvp.md). |
| Funcionamento da aplicação | [Demonstração online](https://bee-good-food-recode.vercel.app) e testes no navegador em [`frontend/e2e`](../../frontend/e2e). |
| Qualidade técnica | Testes automatizados, concorrência testada na venda da última unidade e auditoria de acessibilidade. Resultados em [`backend/evidencias`](../../backend/evidencias). |
| Clareza da proposta | Página inicial, README e a tabela acima. |
| Criatividade | Faixa de prazo com "Compre até" e "Retire até", código de retirada e economia acumulada, a partir da [análise de mercado](analise-de-mercado.md). |
| Impacto esperado | Indicadores da loja: pedidos retirados, valor, unidades e cancelamentos. |
| Integração entre frontend, backend e banco | Testes no navegador percorrem interface, API e MySQL real. |

## Atenção para a apresentação

A demonstração na Vercel usa uma API simulada no navegador, para abrir na hora e sem custo ([DEC-037](decisoes-do-mvp.md)). **Para mostrar a integração real com Java e MySQL, rode o sistema completo** pelos passos do [README](../../README.md#como-executar-localmente).
