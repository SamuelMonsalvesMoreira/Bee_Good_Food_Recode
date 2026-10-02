# Banco de dados

O banco planejado para o MVP é MySQL 8.0+, com migrations Flyway.

## Migration atual

- `migrations/V1__criar_schema_inicial.sql`: cria as 12 tabelas do domínio, chaves, índices e restrições iniciais.

Esta migration ainda não foi executada por uma aplicação. A API Java será criada na próxima etapa e deverá executar as migrations em um banco local controlado.

## Escopo financeiro

O MVP não processa pagamento. `pedido.forma_pagamento_declarada` registra apenas a intenção do cliente (`PIX`, dinheiro ou cartão na retirada). Não há gateway, captura, conciliação ou armazenamento de dados de cartão.

## Próximas verificações

Antes de considerar o esquema pronto, será necessário:

1. executar a migration em MySQL 8.0;
2. validar as constraints com dados válidos e inválidos;
3. testar concorrência de estoque e idempotência na API;
4. adicionar migrations posteriores somente quando uma alteração for necessária;
5. inserir dados de demonstração sem dados pessoais reais.
