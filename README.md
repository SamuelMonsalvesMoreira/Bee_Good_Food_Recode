# Bee Good Food Recode

Projeto final do programa Recode Fullstack baseado no TCC [Bee Good Food](https://github.com/SamuelMonsalvesMoreira/Bee_Good_Food_TCC).

**Status:** Etapas 1 a 4 concluídas localmente — planejamento, banco MySQL, base da API e autenticação/perfis no backend. A validação atual da API tem 52 testes HTTP/MySQL e 6 unitários aprovados, além de cadastro, login e limitação de tentativas no JAR reiniciado. As 47 verificações SQL da Etapa 2 permanecem registradas separadamente. Próxima etapa: estabelecimentos, catálogo e pedidos. O MVP completo e as telas React ainda estão em construção.

## Sobre o projeto

O Bee Good Food Recode será uma plataforma web para aproximar consumidores adultos de pequenos estabelecimentos que desejam oferecer produtos excedentes por preço reduzido. A solução permitirá divulgar itens disponíveis, receber pedidos e acompanhar vendas.

O impacto social principal está no fortalecimento de pequenos negócios, no aproveitamento comercial de produtos que poderiam deixar de gerar receita e na ampliação do acesso desses estabelecimentos ao mercado digital.

## Alinhamento com o ODS 8

O projeto está alinhado ao **Objetivo de Desenvolvimento Sustentável 8 — Trabalho Decente e Crescimento Econômico**, especialmente pelo incentivo ao empreendedorismo, à digitalização de pequenos negócios e à geração de renda local.

A redução do desperdício é um benefício adicional da solução. O projeto não oferece orientação nutricional ou serviços de saúde e não é direcionado a menores de 18 anos.

## Tecnologias planejadas

- Frontend: ReactJS
- Backend: Java com Spring Boot
- Banco de dados: MySQL
- Integração: API REST com JSON
- Autenticação: Spring Security e JWT

## Estrutura do repositório

```text
Bee_Good_Food_Recode/
├── backend/                 # API Java/Spring Boot
├── frontend/                # Aplicação ReactJS
├── database/                # Modelo e scripts MySQL
├── docs/
│   ├── diagramas/           # Diagramas do sistema
│   ├── planejamento/        # Escopo e requisitos
│   └── referencias-tcc/     # Materiais selecionados do TCC
├── .gitignore
└── README.md
```

## Funcionalidades do MVP

- Cadastro e login de clientes e estabelecimentos.
- Cadastro e gerenciamento de estabelecimentos.
- Cadastro de produtos excedentes, quantidade, preço e período de disponibilidade.
- Busca e visualização de estabelecimentos e produtos.
- Carrinho e criação de pedidos.
- Acompanhamento do status do pedido.
- Histórico de pedidos do cliente.
- Gerenciamento de pedidos pelo estabelecimento.
- Indicadores básicos de vendas e aproveitamento de produtos.

O MVP apenas registrará a forma de pagamento escolhida. Não haverá integração com um meio de pagamento real nesta primeira versão.

## Documentação

- [Escopo do projeto](docs/planejamento/escopo.md)
- [Requisitos iniciais](docs/planejamento/requisitos-iniciais.md)
- [Inventário de migração do TCC](docs/planejamento/inventario-migracao.md)
- [Resumo da pesquisa original](docs/referencias-tcc/pesquisa-original-resumo.md)
- [Referências visuais do TCC](docs/referencias-tcc/README.md)
- [Banco de dados: execução e estudos](database/README.md)
- [Conclusão da Etapa 2 e evidências](docs/planejamento/conclusao-etapa-2.md)
- [API Java: execução, rotas e estudos](backend/README.md)
- [Conclusão da Etapa 3 e evidências](docs/planejamento/conclusao-etapa-3.md)
- [Autenticação: guia de estudo e administração local](docs/planejamento/autenticacao.md)
- [Conclusão da Etapa 4 e próximos passos](docs/planejamento/conclusao-etapa-4.md)

## Etapas

- [x] Etapa 1 — estrutura, escopo e migração seletiva das referências do TCC
- [x] Etapa 2 — modelagem MySQL, dados fictícios e validação local
- [x] Etapa 3 — base da API Java/Spring Boot, integração MySQL/Flyway e OpenAPI
- [x] Etapa 4 — cadastro adulto, autenticação JWT e perfis de acesso na API
- [ ] Etapa 5 — cadastros e fluxo de pedidos
- [ ] Etapa 6 — criação e integração do frontend ReactJS
- [ ] Etapa 7 — testes, documentação final e apresentação

## Protótipo original

- [Protótipo publicado](https://v0-ifood-like-website.vercel.app/login)
- [Projeto original no GitHub](https://github.com/SamuelMonsalvesMoreira/Bee_Good_Food_TCC)


---

## Consolidação da Etapa 1

A Etapa 1 foi revisada como uma baseline de produto e engenharia. O projeto está alinhado ao **ODS 8 — Trabalho Decente e Crescimento Econômico** e atende às restrições do curso: público adulto, sem saúde, esporte ou religião.

A implementação do banco MySQL foi validada na Etapa 2. A Etapa 3 adicionou a base Java/Spring Boot integrada ao MySQL e Flyway, com consultas públicas de categorias e documentação OpenAPI. A Etapa 4 adicionou cadastro adulto, login, consulta do próprio perfil e listagem administrativa de usuários. Gestão do catálogo, pedidos e frontend ReactJS/TypeScript permanecem nas próximas etapas.

### Documentação de decisão

- [Escopo do projeto](docs/planejamento/escopo.md)
- [Requisitos iniciais](docs/planejamento/requisitos-iniciais.md)
- [Decisões do MVP e arquitetura](docs/planejamento/decisoes-do-mvp.md)
- [Critérios de aceitação](docs/planejamento/criterios-de-aceitacao.md)
- [Métricas e validação](docs/planejamento/metricas-e-validacao.md)
- [Riscos e premissas](docs/planejamento/riscos-e-premissas.md)
- [Glossário](docs/planejamento/glossario.md)

Nenhum teste, métrica de desempenho ou impacto social é apresentado como resultado antes de ser executado e registrado.
