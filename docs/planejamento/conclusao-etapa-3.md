# Conclusão da Etapa 3 — Base da API Java

Data: 13/10/2026. Estado: **concluída localmente**. Esta etapa entrega a infraestrutura da API e o primeiro recurso de consulta; não representa o backend completo nem uma publicação em produção.

Atualização de 21/10/2026: a [Etapa 4](conclusao-etapa-4.md) adicionou autenticação e ampliou a suíte. Os relatórios em `backend/evidencias/` mostram a execução mais recente; os 17 testes descritos abaixo registram o resultado histórico da Etapa 3.

## Entregas

- Projeto Java com Spring Boot 4.1.1, código compatível com Java 21 e validação executada com JDK 25.
- Conexão MySQL por variáveis de ambiente, UTC e modo SQL estrito. Senhas reais não são versionadas.
- Flyway executando as migrations V1 e V2 já existentes, sem duplicar os arquivos SQL. Hibernate apenas valida o mapeamento JPA.
- Consulta paginada e detalhe de categorias ativas, com controller, service, repository e DTO separados.
- Validação dos parâmetros e respostas de erro JSON sem detalhes internos.
- Health check e documentação interativa Swagger UI/OpenAPI com Springdoc 3.1.1.
- Maven Wrapper e procedimento reproduzível de validação com MySQL isolado.

## Resultados verificados

**17 testes HTTP/MySQL aprovados, sem falhas ou testes ignorados**, usando MySQL 8.0.46 real, não H2 nem repositório simulado.

Os testes cobrem:

- Banco inicialmente vazio: V1 e V2 aplicadas pelo Flyway, 12 tabelas de negócio mais a tabela de histórico e três papéis de referência; nenhum usuário criado automaticamente.
- Health check com conexão ao banco, sem expor credenciais ou componentes internos.
- Paginação, ordenação estável, detalhe, página vazia e omissão de categorias inativas.
- Parâmetros inválidos com resposta 400, recursos inexistentes/inativos com 404 e escrita indisponível com 405.
- Contrato OpenAPI e carregamento da interface Swagger.

O JAR empacotado foi iniciado **duas vezes no mesmo banco**, com health UP e dados preservados. Também foi verificado que contém somente V1/V2 como migrations, sem seeds ou classes de teste. Os processos de teste foram encerrados ao final; nenhum servidor ficou publicado.

A primeira tentativa de execução encontrou uma limitação de instrumentação de testes no ambiente Windows. Mockito foi removido das dependências de teste porque estes testes usam HTTP e MySQL reais e não precisam de mocks. A execução completa seguinte foi aprovada.

## Evidências e reprodução

- [Relatório da API](../../backend/evidencias/ultima-validacao.json)
- [Contrato OpenAPI gerado](../../backend/evidencias/openapi.json)
- [Testes executáveis](../../backend/src/test/java/br/com/beegoodfood/ApiMySqlIT.java)
- [Como executar e estudar a API](../../backend/README.md)

Os 47 testes SQL da Etapa 2 e os 17 testes desta etapa têm finalidades diferentes. Nenhum resultado comprova ainda o fluxo de pedidos via API, concorrência HTTP, segurança com usuários autenticados, desempenho ou impacto social real.

## Próxima ação — Etapa 4

1. Implementar cadastro de cliente adulto com autodeclaração de maioridade e senha armazenada com hash seguro.
2. Implementar login e autenticação com Spring Security/JWT, sem segredos fixos no código.
3. Aplicar os papéis CLIENTE, RESPONSAVEL e ADMINISTRADOR conforme as decisões do MVP; cadastro público não pode escolher papéis privilegiados.
4. Testar usuário inativo, credenciais inválidas, token inválido/expirado e acesso negado.

Cadastro/moderação de estabelecimentos, escrita do catálogo e pedidos continuam na Etapa 5. Frontend React e integração visual continuam na Etapa 6. Esta entrega não modifica o protótipo de páginas existente.
