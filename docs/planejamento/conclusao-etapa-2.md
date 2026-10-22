# Conclusão da Etapa 2 — MySQL

Data: 11/10/2026. Estado: **concluída localmente**, com integração Java/Flyway pendente para a Etapa 3.

Atualização de 13/10/2026: a integração foi concluída e verificada na [Etapa 3](conclusao-etapa-3.md). O texto abaixo preserva o escopo e os resultados originais da Etapa 2.

## Entregas verificadas

| Entrega | Evidência |
|---|---|
| Modelo conceitual e relacionamentos | [Modelo de dados](modelo-de-dados.md) |
| Esquema físico executável | [Migration V1](../../database/migrations/V1__criar_schema_inicial.sql) |
| Papéis de referência | [Migration V2](../../database/migrations/V2__cadastrar_papeis.sql) |
| Dados sem informações pessoais reais | [Demonstração](../../database/seeds/demonstracao.sql) |
| Catálogo, histórico, indicadores e auditoria | [Consultas de estudo](../../database/queries/consultas-estudo.sql) |
| Tipos, nulabilidade, padrões e chaves | [Dicionário físico](../../database/evidencias/dicionario-fisico.tsv) |
| Chaves estrangeiras | [Relacionamentos](../../database/evidencias/relacionamentos.tsv) |
| Índices e unicidade | [Índices](../../database/evidencias/indices.tsv) |
| Testes reais, versão e hashes das migrations | [Relatório da execução](../../database/evidencias/ultima-validacao.json) |

## Resultado

Foram aprovadas **47 verificações** em MySQL **8.0.46**, por uma instância isolada executada no Windows. Criadas 12 tabelas InnoDB, com 104 colunas, 15 chaves estrangeiras e 34 índices. O servidor de teste foi encerrado ao final.

A primeira tentativa foi bloqueada pela permissão de conexão local do ambiente. Após liberar a comunicação em loopback, a execução final terminou sem falhas. Não foi necessário modificar a migration V1; os papéis permanentes foram adicionados separadamente na V2.

Os testes cobriram aceitação de dados válidos, rejeição de dados inválidos, integridade referencial, unicidade, snapshots, rollback e disputa concorrente pela última unidade via SQL. O relatório não representa testes da API nem resultados de usuários reais.

## Responsabilidades das próximas etapas

| Banco já garante | API Java ainda deverá garantir |
|---|---|
| Chave estrangeira aponta para um registro existente | Usuário tem permissão e é proprietário do recurso |
| Valores de status pertencem à lista permitida | Mudança entre estados é autorizada e gera histórico |
| Preços positivos e oferta não superior ao preço original | Preços publicados são imutáveis e total é recalculado no servidor |
| Estoque unsigned e transações InnoDB | Pedido, itens, histórico e desconto confirmam ou revertem juntos |
| Chave de idempotência única por cliente | Repetição equivalente retorna o pedido original; conteúdo diferente retorna conflito |
| Campos de snapshot independentes do catálogo | Dados históricos não podem ser reescritos pelas operações comuns |
| Data de confirmação de maioridade obrigatória | Autodeclaração validada no cadastro; sem afirmar verificação documental |

## Próxima ação

Etapa 3: criar a base da API Java/Spring Boot, configurar MySQL e Flyway, validar a inicialização em banco vazio e documentar os primeiros endpoints com OpenAPI. As etapas 4 e 5 implementarão autenticação, catálogo e pedidos transacionais.

As evidências registram testes executados localmente. O versionamento destes arquivos no GitHub não implica publicação de um site nem disponibilização de um servidor de banco de dados.
