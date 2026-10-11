# Banco de dados — Bee Good Food Recode

**Etapa 2 concluída localmente em 11/10/2026:** esquema executado em MySQL 8.0.46, dados fictícios e 47 verificações aprovadas. A API Java e sua integração com Flyway são a próxima etapa.

## O que está pronto

- 12 tabelas InnoDB, 104 colunas, 15 chaves estrangeiras e 34 índices (incluindo primários e únicos).
- `migrations/V1__criar_schema_inicial.sql`: esquema original preservado.
- `migrations/V2__cadastrar_papeis.sql`: papéis CLIENTE, RESPONSAVEL e ADMINISTRADOR; não cria contas.
- `seeds/demonstracao.sql`: dados fictícios para estudo, separados das migrations.
- `queries/consultas-estudo.sql`: catálogo, histórico, indicadores, auditoria de totais e inspeção do esquema.
- `tests/integridade.sql`: testes positivos e negativos com reversão das alterações.
- `scripts/validar-banco.ps1`: execução reproduzível em instância MySQL isolada.
- `evidencias/`: relatório de execução, dicionário físico, relacionamentos e índices extraídos do banco.

## Repetir a validação no Windows

Abra o PowerShell na raiz deste repositório e execute:

```powershell
& .\database\scripts\validar-banco.ps1
```

Se o MySQL Server estiver instalado em outra pasta:

```powershell
& .\database\scripts\validar-banco.ps1 -MySqlBin 'C:\caminho\mysql\bin'
```

O teste foi executado com PowerShell 7 e MySQL 8.0.46 no Windows. Requer os executáveis `mysqld.exe` e `mysql.exe` e permissão para comunicação local. Ter apenas o Workbench não é suficiente.

O script cria uma instância nova em `.local-mysql/<identificador>`, escolhe uma porta livre em `127.0.0.1`, define uma senha aleatória temporária e aplica V1 e V2. Ele não usa o serviço MySQL existente nem solicita sua senha. Ao terminar, encerra somente o servidor que iniciou. Os arquivos dessa instância são temporários de teste, ficam ignorados pelo Git e não são o banco de desenvolvimento. Cada nova execução cria outra instância.

O resultado esperado é `VALIDADO: 47 verificacoes em MySQL 8.0.46`. A versão exibida acompanha a instalação utilizada. Falhas encerram o comando com erro e ficam registradas; nenhuma falha é contada como aprovação.

## Abrir um banco de estudo no MySQL Workbench

Para manter um banco acessível depois do teste, use sua conexão local habitual no Workbench:

1. Abra e execute `preparar-demo.sql`. Ele cria `bee_good_food_demo`; se esse nome já existir, pare e escolha um nome novo. Não é necessário apagar bancos.
2. Atualize a lista de schemas e selecione `bee_good_food_demo` como schema padrão, com um duplo clique.
3. Abra e execute `migrations/V1__criar_schema_inicial.sql`.
4. Abra e execute `migrations/V2__cadastrar_papeis.sql`.
5. Abra e execute `seeds/demonstracao.sql` uma única vez nesse banco vazio, já com V1 e V2 aplicadas.
6. Abra `queries/consultas-estudo.sql` e execute as consultas para estudar os resultados.

As datas da carga são calculadas em UTC no momento da execução. Depois de quatro horas, a oferta de exemplo deixa de aparecer como comprável; isso é esperado. Os dados incluem três usuários fictícios inativos, dois estabelecimentos, duas categorias, três produtos, quatro ofertas e um pedido concluído com histórico.

As contas têm e-mails `example.invalid` e marcadores de senha que impedem login. Não existe senha padrão nem administrador ativo. O pedido demonstra R$ 33,80 em valor movimentado, duas unidades e R$ 22,20 de diferença nominal de preço. São valores simulados, não resultados reais de vendas ou impacto.

## O que foi validado

- Criação das tabelas e carga de referência/demonstração.
- Unicidade de e-mail, papéis, endereço, slug e chave de idempotência por cliente.
- Rejeição de referências inexistentes, preços inválidos, estoque negativo, prazos incoerentes, status inválidos e subtotal incorreto.
- Preservação dos snapshots ao alterar o catálogo e bloqueio da exclusão de dados referenciados.
- Reversão de alterações por `ROLLBACK`.
- Duas conexões concorrentes disputando a última unidade: uma atualiza o estoque e a outra afeta zero linhas.
- Execução das consultas de estudo.

Veja os nomes de todas as verificações em [ultima-validacao.json](evidencias/ultima-validacao.json) e a explicação em [Conclusão da Etapa 2](../docs/planejamento/conclusao-etapa-2.md).

## Limite entre banco e API

O esquema é a base de persistência. As próximas etapas Java ainda precisam implementar autenticação, autorização, publicação, transições de pedido, soma de totais, validação de maioridade, regras de estoque, idempotência HTTP e cancelamento com devolução única.

Exemplos: o banco exige uma data de confirmação de maioridade, mas não verifica a idade real; aceita apenas valores de status conhecidos, mas não valida a sequência entre eles; permite que dados históricos sejam atualizados diretamente por SQL, portanto a API deverá restringir essas operações. Um pedido com itens de estabelecimentos diferentes também precisa ser rejeitado pela API na mesma transação.

O teste concorrente comprova o mecanismo SQL de desconto condicional, não a implementação do fluxo completo de pedido. Esses testes serão repetidos através da API Java.

## Integração prevista com Flyway

Na etapa Java, configurar o Flyway para executar **apenas** `migrations/`, em ordem V1 e V2. A validação atual executa o SQL diretamente; ainda não executa Flyway. As tabelas de controle do Flyway não são simuladas por estes scripts.

Não misturar o banco montado manualmente no Workbench com um banco novo gerenciado pelo Flyway sem planejar a migração. Os arquivos de `seeds/`, `queries/` e `tests/` não devem rodar automaticamente em produção. Configurar conexões da aplicação em UTC e com modo SQL estrito, mantendo senhas fora do Git.

## Escopo financeiro

O MVP não processa pagamento. `pedido.forma_pagamento_declarada` registra somente a escolha do cliente (`PIX`, dinheiro ou cartão na retirada). Não comprova recebimento e não armazena dados de cartão.
