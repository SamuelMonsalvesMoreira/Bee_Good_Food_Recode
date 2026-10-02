# Bee Good Food Recode

Projeto final do programa Recode Fullstack baseado no TCC [Bee Good Food](https://github.com/SamuelMonsalvesMoreira/Bee_Good_Food_TCC).

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

## Etapas

- [x] Etapa 1 — estrutura, escopo e migração seletiva das referências do TCC
- [ ] Etapa 2 — modelagem do banco de dados MySQL
- [ ] Etapa 3 — criação da API Java/Spring Boot
- [ ] Etapa 4 — autenticação e perfis de acesso
- [ ] Etapa 5 — cadastros e fluxo de pedidos
- [ ] Etapa 6 — criação e integração do frontend ReactJS
- [ ] Etapa 7 — testes, documentação final e apresentação

## Protótipo original

- [Protótipo publicado](https://v0-ifood-like-website.vercel.app/login)
- [Projeto original no GitHub](https://github.com/SamuelMonsalvesMoreira/Bee_Good_Food_TCC)


---

## Consolidação da Etapa 1

A Etapa 1 foi revisada como uma baseline de produto e engenharia. O projeto está alinhado ao **ODS 8 — Trabalho Decente e Crescimento Econômico** e atende às restrições do curso: público adulto, sem saúde, esporte ou religião.

A implementação ainda não começou. As tecnologias obrigatórias estão planejadas para as próximas etapas: ReactJS/TypeScript no frontend, Java/Spring Boot na API e MySQL no banco.

### Documentação de decisão

- [Escopo do projeto](docs/planejamento/escopo.md)
- [Requisitos iniciais](docs/planejamento/requisitos-iniciais.md)
- [Decisões do MVP e arquitetura](docs/planejamento/decisoes-do-mvp.md)
- [Critérios de aceitação](docs/planejamento/criterios-de-aceitacao.md)
- [Métricas e validação](docs/planejamento/metricas-e-validacao.md)
- [Riscos e premissas](docs/planejamento/riscos-e-premissas.md)
- [Glossário](docs/planejamento/glossario.md)

Nenhum teste, métrica de desempenho ou impacto social é apresentado como resultado antes de ser executado e registrado.
