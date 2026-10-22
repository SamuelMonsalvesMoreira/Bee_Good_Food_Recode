# Bee Good Food — API Java

Base da Etapa 3: Spring Boot, MySQL, Flyway e documentação OpenAPI. As rotas desta etapa são públicas e somente de leitura. Cadastro, autenticação, escrita do catálogo e pedidos entram nas etapas 4 e 5.

## Tecnologias

- Java 21 como versão mínima do código; execução local com JDK 25 LTS.
- Spring Boot 4.1.1, Spring MVC, Spring Data JPA e Bean Validation.
- MySQL 8.0.46 validado no ambiente local.
- Flyway, com versões gerenciadas pelo Spring Boot.
- Springdoc 3.1.1 para OpenAPI e Swagger UI.
- Maven Wrapper 3.9.11; não é necessário instalar Maven globalmente.

Referências de compatibilidade: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) e [Springdoc](https://springdoc.org/).

## Estrutura para estudar

```text
src/main/java/br/com/beegoodfood/
├── BeeGoodFoodApplication.java   # Inicialização da aplicação
├── catalogo/                    # Controller, service, repository, entidade e DTO
├── comum/                       # Paginação e erros JSON
└── config/                      # Informações do OpenAPI
```

O controller recebe a chamada HTTP; o service organiza a consulta; o repository lê o MySQL. A resposta usa um DTO, expondo apenas os campos públicos. O Hibernate valida o mapeamento; somente o Flyway cria ou altera o esquema.

## Verificação completa sem configurar seu banco habitual

No PowerShell 7, **na raiz do repositório**, informe o JDK instalado e execute:

```powershell
$env:JAVA_HOME = 'C:\caminho\do\jdk-21-ou-superior'
& .\database\scripts\validar-banco.ps1 -Api
```

O runner usa o MySQL Server instalado no caminho padrão. Para outro caminho, acrescente `-MySqlBin 'C:\caminho\mysql\bin'`. O download inicial das dependências Maven precisa de internet.

Esse comando inicia um MySQL isolado em uma porta local, cria um usuário limitado ao schema de teste e entrega o banco vazio à aplicação. O Flyway aplica V1 e V2; os testes HTTP verificam respostas e erros. Em seguida, o JAR é iniciado duas vezes no mesmo banco para verificar a reinicialização sem reaplicar migrations. Todos os processos iniciados são encerrados ao final.

Opcionalmente, é possível informar `-MavenCommand 'C:\caminho\mvn.cmd'` e `-MavenRepository 'C:\cache\maven'`. O teste de integração exige as variáveis especiais do runner e recusa o uso acidental de um banco comum.

Os resultados ficam em `evidencias/ultima-validacao.json`; o contrato gerado em `evidencias/openapi.json`. Os logs detalhados ficam em `target/` e não entram no Git. O relatório do banco da Etapa 2 é preservado.

## Executar para desenvolvimento

1. Use JDK 21 ou superior. Confira `java -version` e configure `JAVA_HOME`. Java 8 não atende a este projeto.
2. Em uma conexão administrativa do MySQL, crie um schema **novo** chamado `bee_good_food` e um usuário da aplicação com senha local, limitado a esse schema.
3. Para aplicar migrations, esse usuário precisa de `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `CREATE`, `ALTER`, `INDEX`, `REFERENCES` e `DROP` no schema. Não precisa de privilégios globais. Em uma implantação futura, separar credenciais de migração e de execução.
4. Na pasta `backend`, configure as variáveis abaixo e inicie a aplicação.

```powershell
$env:JAVA_HOME = 'C:\caminho\do\jdk-21-ou-superior'
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/bee_good_food?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USER = 'bee_app'
$env:DB_PASSWORD = Read-Host 'Senha local do usuario MySQL' -MaskInput
.\mvnw.cmd spring-boot:run
```

`.env.example` é um modelo; o Spring Boot não carrega esse arquivo automaticamente. Configure as variáveis no terminal ou na configuração de execução do IntelliJ. Não escreva senhas reais nos arquivos versionados.

Por padrão, o servidor escuta em `127.0.0.1:8080`. `SERVER_PORT` permite trocar a porta. Para encerrar, pressione `Ctrl+C` no terminal da aplicação.

## Rotas disponíveis

| Método e endereço | Resultado |
|---|---|
| `GET /actuator/health` | Estado da aplicação e conexão com o banco, sem detalhes internos |
| `GET /api/v1/categorias` | Categorias ativas, ordenadas por nome e ID |
| `GET /api/v1/categorias?pagina=0&tamanho=20` | Página começa em zero; tamanho de 1 a 100 |
| `GET /api/v1/categorias/{id}` | Categoria ativa; inexistente ou inativa retorna 404 |
| `GET /v3/api-docs` | Contrato OpenAPI JSON |
| `GET /swagger-ui/index.html` | Tela interativa de documentação |

Com a aplicação iniciada, abra [Swagger local](http://127.0.0.1:8080/swagger-ui/index.html).

Um banco novo possui os papéis CLIENTE, RESPONSAVEL e ADMINISTRADOR, mas não possui categorias nem usuários. A lista vazia é uma resposta válida. Para estudar com dados fictícios, carregue `../database/seeds/demonstracao.sql` uma única vez, depois da primeira inicialização do Flyway, em um banco exclusivamente de demonstração. Essa carga nunca roda automaticamente.

## Migrations: fonte única

O Maven inclui `../database/migrations/V1__criar_schema_inicial.sql` e `V2__cadastrar_papeis.sql` dentro do JAR em `db/migration`. Não há cópias manuais no código Java. Seeds e testes não são empacotados.

Não execute V1/V2 manualmente antes do primeiro uso do Flyway. Ele registra as versões em `flyway_schema_history` e recusa um schema preenchido sem histórico. O banco de estudo montado manualmente na Etapa 2 continua separado; não habilite `baseline-on-migrate` para esconder essa diferença.

## Formato dos erros

Erros usam `application/problem+json`, com `status`, `title` e `detail`. Parâmetros inválidos também têm `campos`, identificando o que corrigir. A API não devolve stack trace, SQL ou credenciais. Exemplo: `tamanho=101` retorna 400; uma categoria inativa retorna 404; tentar criar categoria por POST retorna 405 nesta etapa.

## O que ainda falta

- Login, JWT, maioridade, perfis e autorização: Etapa 4.
- Cadastro e moderação de estabelecimentos, escrita do catálogo, ofertas e pedidos: Etapa 5.
- Frontend React, incluindo configuração de CORS para suas origens: Etapa 6.
- Testes de pedidos/estoque através da API serão adicionados junto com essas funcionalidades.
