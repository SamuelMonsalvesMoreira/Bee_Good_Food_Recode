# Inventário de migração do TCC

## Origem

- Repositório: [Bee Good Food TCC](https://github.com/SamuelMonsalvesMoreira/Bee_Good_Food_TCC)
- Protótipo publicado: [v0-ifood-like-website.vercel.app](https://v0-ifood-like-website.vercel.app/login)
- Tecnologias do protótipo: Next.js 15, React 19 e TypeScript.

## Materiais migrados nesta etapa

| Material do TCC | Destino no Recode | Decisão |
|---|---|---|
| `Wireframes.png` | Link em `docs/referencias-tcc/README.md` | Mantido como referência visual no repositório original. |
| `Fluxo de navegação.png` | Link em `docs/referencias-tcc/README.md` | Mantido como referência do fluxo original. |
| Requisitos das Tabelas 1 e 2 | `docs/planejamento/requisitos-iniciais.md` | Convertidos, revisados e reduzidos ao MVP. |
| Pesquisa com usuários | `docs/referencias-tcc/pesquisa-original-resumo.md` | Migrada somente como resumo anônimo. |
| Informações do README original | README e documentação | Links e contexto preservados. |

## Código reaproveitável em uma etapa futura

Os seguintes componentes do protótipo original podem servir de referência durante a construção do frontend ReactJS:

- Login, cadastro e recuperação de senha.
- Página inicial.
- Lista de categorias.
- Cards de estabelecimentos.
- Detalhes do estabelecimento e cardápio.
- Carrinho.
- Checkout.
- Componentes visuais baseados em Radix UI.

O código não foi copiado nesta etapa porque utiliza estrutura de Next.js e dados simulados. Na etapa do frontend, migraremos apenas os componentes necessários para uma aplicação ReactJS integrada à API Java.

## Materiais não migrados

| Material | Motivo |
|---|---|
| Planilha bruta de respostas | Possui dados individuais e amostra de apenas quatro participantes. O resumo anônimo é suficiente para o repositório público. |
| Configuração e dependências do Next.js | A arquitetura definitiva do frontend será preparada na etapa correspondente. |
| Autenticação do `auth-context.tsx` | O login é simulado, aceita qualquer credencial não vazia e usa `localStorage`. Será substituído pela API Java. |
| Dados fixos de restaurantes e produtos | Serão substituídos por dados persistidos no MySQL. |
| Imagem genérica dos 17 ODS | O novo projeto deve destacar especificamente o ODS 8. |
| Imagens das tabelas de requisitos | Os requisitos foram convertidos para Markdown e atualizados. |
| Arquivos `placeholder` | Não representam a identidade visual final do produto. |
| Cópias dos wireframes e do fluxo | Permanecem no TCC original para evitar duplicação; o novo projeto mantém links diretos para eles. |

## Alterações de conceito

O protótipo original se aproxima visualmente de uma plataforma genérica de delivery, enquanto a pesquisa menciona ofertas de excedentes e redução do desperdício. No Recode, o foco será explicitado como:

> Apoiar pequenos estabelecimentos na geração de receita por meio da oferta digital de produtos excedentes.

Essa formulação conecta o sistema ao ODS 8. A redução do desperdício permanece como benefício adicional, e não como o enquadramento principal do projeto.
