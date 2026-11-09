# Bee Good Food — API Java

API REST do MVP: Spring Boot, MySQL, Flyway, Spring Security e OpenAPI. Etapas 3 a 5 implementadas: cadastro adulto e JWT, estabelecimentos com moderação, categorias, produtos e ofertas, busca pública, pedidos idempotentes para retirada e indicadores. O frontend React entra na Etapa 6.

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
├── auth/                        # Cadastro, login, usuários, tokens e limite de tentativas
├── estabelecimentos/            # Solicitação, edição pelo proprietário e moderação com histórico
├── catalogo/                    # Categorias, produtos, ofertas, busca pública e contrato de estoque
├── pedidos/                     # Pedido transacional, idempotência, status e cancelamento
├── indicadores/                 # Métricas por período e coorte
├── comum/                       # Paginação, datas UTC, regras de negócio e erros JSON
└── config/                      # Segurança, relógio e OpenAPI
```

O controller recebe a chamada HTTP; o service aplica regras, propriedade e transações; o repository lê o MySQL. A resposta usa um DTO (`record`), expondo apenas os campos necessários. Um módulo usa outro somente por serviços e records públicos. Consultas que cruzam módulos, como a busca, a venda e os indicadores, usam SQL explícito com `NamedParameterJdbcTemplate`. O Hibernate valida o mapeamento; somente o Flyway cria ou altera o esquema.

## Verificação completa sem configurar seu banco habitual

No PowerShell 7, **na raiz do repositório**, informe o JDK instalado e execute:

```powershell
$env:JAVA_HOME = 'C:\caminho\do\jdk-21-ou-superior'
& .\database\scripts\validar-banco.ps1 -Api
```

O runner usa o MySQL Server instalado no caminho padrão. Para outro caminho, acrescente `-MySqlBin 'C:\caminho\mysql\bin'`. O download inicial das dependências Maven precisa de internet.

Esse comando inicia um MySQL isolado em uma porta local, cria um usuário limitado ao schema de teste e entrega o banco vazio à aplicação. Também gera uma chave JWT aleatória exclusiva do teste. O Flyway aplica V1 e V2; os testes HTTP verificam respostas e erros. Em seguida, o JAR é iniciado duas vezes no mesmo banco para verificar migrations, cadastro, login, persistência da sessão com a mesma chave e limitação real de tentativas. Todos os processos iniciados são encerrados ao final.

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
# Chave apenas na memoria deste terminal. Nunca exiba nem envie para o Git.
$jwtBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

`.env.example` é um modelo; o Spring Boot não carrega esse arquivo automaticamente. Configure as variáveis no terminal ou na configuração de execução do IntelliJ. Não escreva senhas reais nos arquivos versionados.

O segredo JWT é obrigatório, em Base64, com pelo menos 32 bytes aleatórios. O exemplo gera uma chave nova a cada execução: isso invalida tokens anteriores, mas não apaga usuários ou senhas. Para manter sessões entre reinícios, reutilize a chave por um gerenciador de segredos/configuração local protegida. Não há chave padrão nem conta administrativa automática.

Por padrão, o servidor escuta em `127.0.0.1:8080`. Para servir o frontend em outra origem, defina `CORS_ORIGINS` com a origem exata (ex.: `https://bee.exemplo.com`); vazio mantém o CORS desligado e curinga é recusado. `SERVER_PORT` permite trocar a porta. Para encerrar, pressione `Ctrl+C` no terminal da aplicação.

## Rotas disponíveis

| Método e endereço | Resultado |
|---|---|
| `GET /actuator/health` | Estado da aplicação e conexão com o banco, sem detalhes internos |
| `GET /api/v1/categorias` | Categorias ativas, ordenadas por nome e ID |
| `GET /api/v1/categorias?pagina=0&tamanho=20` | Página começa em zero; tamanho de 1 a 100 |
| `GET /api/v1/categorias/{id}` | Categoria ativa; inexistente ou inativa retorna 404 |
| `POST /api/v1/auth/cadastro` | Cria cliente com autodeclaração de maioridade; retorna 201 |
| `POST /api/v1/auth/login` | Retorna JWT Bearer de 15 minutos |
| `GET /api/v1/auth/me` | Perfil da conta autenticada, sem senha/hash |
| `GET /api/v1/admin/usuarios` | Consulta paginada exclusiva de ADMINISTRADOR |
| `POST /api/v1/estabelecimentos` | Cliente solicita estabelecimento (nasce em `PENDENTE_ANALISE`) |
| `GET /api/v1/estabelecimentos/meus` · `GET/PUT /{id}` · `GET /{id}/historico` | Proprietário (ou administrador) consulta, edita e vê o histórico |
| `GET /api/v1/admin/estabelecimentos?status=` | Fila de moderação |
| `POST /api/v1/admin/estabelecimentos/{id}/aprovar\|rejeitar\|suspender\|reativar` | Decisão com motivo; aprovar concede RESPONSAVEL |
| `GET/POST /api/v1/admin/categorias` · `PUT/DELETE /{id}` | Administração de categorias; remover só sem produtos (DEC-038) |
| `POST/GET /api/v1/estabelecimentos/{id}/produtos` · `GET/PUT /api/v1/produtos/{id}` | Produtos do responsável |
| `POST/GET /api/v1/produtos/{id}/ofertas` · `PUT /api/v1/ofertas/{id}` · `GET /api/v1/gestao/ofertas/{id}` | Ofertas (edição só em rascunho) |
| `POST /api/v1/ofertas/{id}/publicar\|pausar\|retomar\|encerrar` · `POST /{id}/estoque` | Ciclo da oferta e ajuste relativo de estoque |
| `GET /api/v1/ofertas?texto=&categoriaId=&cidade=&bairro=&estabelecimentoId=` · `GET /{id}` | Busca pública de ofertas compráveis agora |
| `POST /api/v1/pedidos` (cabeçalho `Idempotency-Key`) | Cria pedido; repetição equivalente devolve o original (200) |
| `GET /api/v1/pedidos` · `GET /{id}` | Histórico e detalhe (cliente ou responsável) |
| `GET /api/v1/estabelecimentos/{id}/pedidos?status=` | Fila de pedidos do estabelecimento |
| `POST /api/v1/pedidos/{id}/status` · `POST /{id}/cancelar` | Avanço uma etapa por vez; cancelamento com devolução única |
| `GET /api/v1/estabelecimentos/{id}/indicadores?inicio=&fim=` | Indicadores de `[inicio, fim)` no fuso do estabelecimento |
| `GET /v3/api-docs` | Contrato OpenAPI JSON |
| `GET /swagger-ui/index.html` | Tela interativa de documentação |

Com a aplicação iniciada, abra [Swagger local](http://127.0.0.1:8080/swagger-ui/index.html).

Um banco novo possui os papéis CLIENTE, RESPONSAVEL e ADMINISTRADOR, mas não possui categorias nem usuários. A lista vazia é uma resposta válida. Para estudar com dados fictícios, carregue `../database/seeds/demonstracao.sql` uma única vez, depois da primeira inicialização do Flyway, em um banco exclusivamente de demonstração. Essa carga nunca roda automaticamente.

## Migrations: fonte única

O Maven inclui `../database/migrations/V1__criar_schema_inicial.sql` e `V2__cadastrar_papeis.sql` dentro do JAR em `db/migration`. A Etapa 5 não precisou de novas migrations. Não há cópias manuais no código Java. Seeds e testes não são empacotados.

Não execute V1/V2 manualmente antes do primeiro uso do Flyway. Ele registra as versões em `flyway_schema_history` e recusa um schema preenchido sem histórico. O banco de estudo montado manualmente na Etapa 2 continua separado; não habilite `baseline-on-migrate` para esconder essa diferença.

## Formato dos erros

Erros usam `application/problem+json`, com `status`, `title` e `detail`. Parâmetros/campos inválidos também têm `campos`, identificando o que corrigir. A API não devolve stack trace, SQL ou credenciais. Exemplos: `tamanho=101` retorna 400; categoria inativa, 404; conta duplicada, 409; falta de autenticação, 401; falta de permissão, 403; excesso de tentativas, 429. Recurso de outro usuário (estabelecimento, produto, oferta, pedido) retorna 404, sem revelar que existe. Regras de estado, como transição proibida, estoque insuficiente ou oferta já ativa, retornam 409 com uma mensagem explicativa. A administração de categorias fica em `/api/v1/admin/categorias`; `POST /api/v1/categorias` continua 405.

Veja [autenticação: decisões, exemplos e administração local](../docs/planejamento/autenticacao.md).

## Validação alternativa com Docker

O runner oficial exige PowerShell 7. Sem ele, use um MySQL 8 descartável no Docker, ligado apenas ao loopback, com um schema **novo** chamado `bee_good_food_validacao`. No PowerShell:

```powershell
$senha = [guid]::NewGuid().ToString('N')   # senha de teste, só neste terminal
docker run -d --name bee-mysql-test -p 127.0.0.1:33099:3306 -e MYSQL_ROOT_PASSWORD=$senha mysql:8.0
# aguarde o MySQL iniciar e crie o schema vazio de validacao
docker exec -e MYSQL_PWD=$senha bee-mysql-test mysql -uroot -e "CREATE DATABASE bee_good_food_validacao CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
$env:BEE_TEST_MYSQL_URL = 'jdbc:mysql://127.0.0.1:33099/bee_good_food_validacao?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:BEE_TEST_MYSQL_USER = 'root'; $env:BEE_TEST_MYSQL_PASSWORD = $senha
$env:DB_URL = $env:BEE_TEST_MYSQL_URL; $env:DB_USER = 'root'; $env:DB_PASSWORD = $senha
$jwtBytes = [byte[]]::new(32); [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd -B -Pintegration clean verify
docker rm -f bee-mysql-test   # descarta o banco de teste
```

Cada classe IT limpa o schema de validação antes de começar e recusa qualquer outro banco. O resultado da Etapa 5 está em [`evidencias/validacao-etapa-5.json`](evidencias/validacao-etapa-5.json).

## O que ainda falta

- Executar o runner oficial com PowerShell 7 e registrar `ultima-validacao.json`: Etapa 7.
- Recuperação de senha por e-mail e refresh token ficam fora do MVP atual, conforme as decisões do projeto.
- Antes de exposição pública: HTTPS, configuração de origens, proxy reverso (`AuthRateLimitFilter` usa o IP da conexão), armazenamento/rotação de segredos e proteção contra abuso adequada ao ambiente.
