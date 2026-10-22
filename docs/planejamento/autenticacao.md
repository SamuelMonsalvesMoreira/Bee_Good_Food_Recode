# Autenticação — decisões e guia de estudo

Implementação da Etapa 4, em 21/10/2026. Esta etapa é da **API**: as telas React e o logout na interface pertencem à Etapa 6.

## Regras implementadas

- Cadastro público recebe somente `nome`, `email`, `senha` e `maiorDeIdade`. Campos desconhecidos, incluindo ID, hash e papéis, são rejeitados.
- Nome/e-mail são aparados nas extremidades; e-mail normalizado usa minúsculas. A constraint MySQL impede duplicatas inclusive em duas requisições simultâneas. A senha não é aparada nem modificada.
- `maiorDeIdade` precisa ser `true`. Registra-se o instante UTC da autodeclaração; isso **não é verificação documental**.
- Senha com 12 a 72 caracteres, limitada também a **72 bytes UTF-8**, por causa do BCrypt. Caracteres acentuados podem ocupar mais de um byte. Não ocorre truncamento silencioso.
- BCrypt com custo 12 e salt aleatório; senha e hash nunca aparecem nos DTOs. As contas fictícias inativas do seed não permitem login.
- Todo cadastro recebe somente `CLIENTE`. `RESPONSAVEL` será concedido na aprovação do estabelecimento (Etapa 5). `ADMINISTRADOR` depende de procedimento controlado.
- Login inválido responde 401 com a mesma mensagem para conta inexistente, senha incorreta ou conta inativa. Cadastro duplicado responde 409, conforme CA02; portanto esse fluxo permite inferir a existência de um e-mail, um trade-off conhecido do requisito.
- JWT assinado com HS256, chave externa de pelo menos 32 bytes aleatórios, emissor `bee-good-food`, audiência `bee-good-food-api` e validade de 900 segundos. Assinatura, emissor, audiência, início, emissão, expiração e identificador são verificados. Não há tolerância adicional para token expirado.
- Token não carrega senha, e-mail ou papéis. A API consulta status e papéis atuais no banco a cada chamada autenticada; conta desativada perde acesso, e privilégio removido deixa de valer mesmo com token anterior.
- Rotas privadas exigem Bearer no cabeçalho `Authorization`. Não há sessão HTTP, cookie de login, HTTP Basic ou autenticação por parâmetro da URL. CSRF está desativado porque a autenticação não usa cookies; se isso mudar, a decisão precisa ser revista.
- Cadastro e login compartilham limite de 20 chamadas por minuto por IP, por processo, incluindo entradas inválidas. A tentativa excedente recebe 429 e `Retry-After: 60`. O mapa tem limite de memória e não confia em `X-Forwarded-For` fornecido pelo cliente.

O limite não é distribuído e é reiniciado com o processo. Pessoas na mesma rede podem compartilhar o IP. Antes de usar múltiplas instâncias ou proxy público, configurar origens confiáveis e proteção contra abuso compartilhada; não basta liberar o cabeçalho encaminhado do cliente.

## Experimentar no Swagger

Inicie a aplicação conforme o [README da API](../../backend/README.md). Abra a documentação em `http://127.0.0.1:8080/swagger-ui/index.html`.

1. Execute `POST /api/v1/auth/cadastro` com o JSON abaixo, usando **somente dados de teste**.
2. Execute `POST /api/v1/auth/login` com o mesmo e-mail e senha.
3. Copie o `accessToken` para **Authorize** no Swagger. Não publique esse valor.
4. Execute `GET /api/v1/auth/me`: o resultado deve ser sua própria conta.
5. Como cliente, tente `GET /api/v1/admin/usuarios`: o esperado é **403**. Isso demonstra a proteção, não um defeito.

```json
{
  "nome": "Cliente de estudo",
  "email": "estudo@example.invalid",
  "senha": "Exemplo-apenas-local-123",
  "maiorDeIdade": true
}
```

O exemplo é deliberadamente público e serve só para banco local descartável, nunca para uma conta real. Repetir o cadastro gera conflito; use o login ou outro e-mail fictício.

Exemplo de corpo para login:

```json
{
  "email": "estudo@example.invalid",
  "senha": "Exemplo-apenas-local-123"
}
```

Mini-exercícios: envie `maiorDeIdade: false`; tente incluir `papeis: ["ADMINISTRADOR"]`; remova a autorização e consulte `/auth/me`. Observe as respostas 400, 400 e 401. Não altere as configurações de segurança para fazer esses casos passarem.

## Logout e duração da sessão

Não existe endpoint fictício de logout. O frontend deverá guardar o token **apenas em memória**, apagá-lo no logout e limpar dados privados. Recarregar a página ou receber 401 exige novo login. Não há refresh token no MVP.

Limpar o token na interface não revoga uma cópia que já tenha sido emitida: ela continua válida até expirar, salvo desativação da conta ou troca da chave de assinatura. A UI desse comportamento ainda não foi implementada.

## Administração local controlada

Nenhuma migration cria uma conta administrativa ou senha padrão. Para desenvolvimento, o mantenedor pode:

1. Criar uma conta adulta pela API.
2. Em uma conexão administrativa do MySQL Workbench, no **schema local de desenvolvimento correto**, consultar o ID, nome e e-mail dessa conta e conferir quem receberá o acesso.
3. Com esse ID confirmado, atribuir `ADMINISTRADOR` em `usuario_papel`, preservando `CLIENTE`. Não conceder pelo formulário público, por campo extra no cadastro ou por endpoint sem autorização.

Exemplo **não executado automaticamente**, substituindo `123` pelo ID confirmado:

```sql
INSERT INTO usuario_papel (usuario_id, papel_codigo)
SELECT id, 'ADMINISTRADOR'
FROM usuario
WHERE id = 123 AND ativo = TRUE;
```

Uma atribuição repetida será rejeitada pela chave primária, sem duplicar o papel. A mudança será consultada na próxima chamada autenticada. Esse procedimento pressupõe acesso administrativo legítimo ao banco e não é uma funcionalidade para usuários finais.

## Limites e próximos testes

- CA01 e CA02: cobertos pela API, incluindo rejeição sem criação e concorrência de e-mail.
- CA18: emissão/expiração e perfil cobertos no backend; armazenamento em memória e logout visual aguardam React.
- CA22: listagem administrativa paginada de usuários sem hash/senha/token implementada; estabelecimentos e moderação aguardam Etapa 5.
- CA03: isolamento entre estabelecimentos será testado quando os respectivos recursos existirem. Não está comprovado apenas por haver JWT.
- Recuperação de senha por e-mail foi adiada nas decisões consolidadas do MVP, apesar da menção ampla em RF02.
- HTTPS, gestão de segredos, proteção distribuída contra abuso e configuração de CORS são requisitos antes de um deploy público. O servidor continua em loopback por padrão.

Referências técnicas: [JWT no Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) e [armazenamento de senhas no Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
