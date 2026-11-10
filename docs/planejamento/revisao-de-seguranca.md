# Revisão de segurança

Data: 09/11/2026. Revisão manual do código e da configuração, com consulta automática de vulnerabilidades conhecidas. **Não é um teste de invasão.** Resultados em [`revisao-seguranca.json`](../../backend/evidencias/revisao-seguranca.json).

## Corrigido nesta revisão

| Gravidade | Problema | Correção |
|---|---|---|
| Alta | Spring Boot 4.1.1 trazia Tomcat 11.0.24 e Jackson com 18 alertas públicos, incluindo negação de serviço por JSON. Parte dos alertas do Tomcat trata de recursos que o projeto não usa (autenticação DIGEST e FORM, HTTP/2), mas a atualização elimina todos. | Spring Boot 4.1.2 (Tomcat 11.0.27, Jackson 3.1.7 e 2.21.7). Nova consulta: 0 alertas em 117 pacotes. |
| Média | O site publicado encaminhava `/api` para um endereço do Render que não existe. Quem registrasse esse nome passaria a responder dentro do domínio do site. | Regra removida do `frontend/vercel.json`. O [guia](../deploy.md) manda acrescentá-la só depois de o serviço existir. |
| Baixa | O site publicado não tinha política de conteúdo e podia ser embutido em página de terceiros (clickjacking). | `Content-Security-Policy` restrita à própria origem e `X-Frame-Options: DENY`. |
| Baixa | A documentação da API ficaria pública no sistema publicado. | Opção `OPENAPI_ENABLED`: ligada no uso local, desligada no `render.yaml`. |

## Riscos aceitos no MVP

| Risco | Situação | O que resolveria |
|---|---|---|
| Reservas sem pagamento podem prender estoque | A loja pode cancelar o pedido, e o estoque volta. Uma conta pode reservar muitas unidades, e o cadastro não confirma e-mail. | Confirmação de e-mail e limite por cliente, ou pagamento antecipado (fora do escopo, DEC-011). |
| O cadastro revela que um e-mail já existe | A mensagem é genérica, mas o erro 409 só acontece nesse caso. | Confirmação de e-mail. |
| Limite de tentativas só por IP e em memória | 20 por minuto, com senha mínima de 12 caracteres e BCrypt. Não bloqueia ataque distribuído, e o contador zera ao reiniciar. | Limite também por conta, com armazenamento compartilhado. |

## Conferir antes de publicar o sistema completo

- **Limite de tentativas atrás do Vercel.** O servidor pode contar o IP do Vercel em vez do IP do cliente, e clientes diferentes dividiriam o mesmo limite. Só dá para confirmar no ambiente real.
- Confirmar a conexão com o MySQL usando TLS (`sslMode=REQUIRED`).
- Gerar um `JWT_SECRET_BASE64` exclusivo e guardá-lo fora do repositório.

## Verificado, sem problema encontrado

- **Segredos:** nenhum arquivo `.env` real no Git e nenhum padrão conhecido de chave no histórico. As contas da demonstração são fictícias, públicas de propósito, e não entram na build normal.
- **Login e sessão:** BCrypt com custo 12, mesma resposta para conta inexistente e senha errada, JWT HS256 de 15 minutos com emissor, audiência e validade conferidos. Papéis são relidos do banco a cada requisição.
- **Autorização:** rotas fechadas por padrão. Recursos de outra pessoa respondem 404, com testes para pedidos, lojas, produtos, ofertas e indicadores.
- **Entrada de dados:** SQL parametrizado, ordenação por lista fechada, `LIKE` com curingas escapados e campos desconhecidos rejeitados. Preços e totais vêm sempre do banco.
- **Frontend:** token só em memória, sem HTML injetado (`dangerouslySetInnerHTML`). Dependências npm sem alertas.
- **Erros e infraestrutura:** sem stack trace nas respostas, Actuator limitado a `health`, imagem Docker sem usuário root.
