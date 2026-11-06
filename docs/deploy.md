# Guia de publicação

Este guia descreve como publicar o Bee Good Food em um servidor próprio para demonstração. Ele **não foi executado** em um ambiente público até a data da Etapa 7. Foram validados localmente o JAR, o build do frontend e a contagem de tentativas por cliente atrás de um proxy (teste automatizado com `X-Forwarded-For` no loopback). O exemplo de configuração do Caddy não foi executado.

## Arquitetura recomendada

```mermaid
flowchart LR
    N[Navegador] -->|HTTPS| P[Proxy reverso<br/>Caddy ou Nginx]
    P -->|arquivos estáticos| F[frontend/dist]
    P -->|/api/*| A[API Spring Boot<br/>127.0.0.1:8080]
    A --> M[(MySQL 8)]
```

O proxy serve a interface e encaminha `/api` para a API **na mesma origem**. Assim não é preciso CORS. A API e o MySQL escutam só no loopback ou em rede privada; somente o proxy fica exposto.

## Pré-requisitos

- JDK 21 ou superior, Node 20 ou superior (só para gerar o build), MySQL 8.
- Domínio apontando para o servidor e certificado HTTPS. O Caddy obtém o certificado automaticamente.

## 1. Banco de dados

1. Crie um schema **novo** (`bee_good_food`) e um usuário limitado a ele, com senha forte. Veja `backend/README.md`.
2. O Flyway cria as tabelas na primeira inicialização da API. Não rode V1/V2 manualmente nem os seeds.

## 2. API

```powershell
cd backend
.\mvnw.cmd -B -DskipTests package   # gera target\bee-good-food-api-0.1.0-SNAPSHOT.jar
```

Variáveis de ambiente do serviço:

| Variável | Valor |
|---|---|
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/bee_good_food?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` |
| `DB_USER` / `DB_PASSWORD` | Usuário limitado ao schema. |
| `JWT_SECRET_BASE64` | 32 bytes aleatórios em Base64, **fixos** entre reinícios e guardados num cofre de segredos. Trocar a chave encerra todas as sessões. |
| `SERVER_ADDRESS` / `SERVER_PORT` | `127.0.0.1` e `8080` (atrás do proxy). |
| `FORWARD_HEADERS_STRATEGY` | `native`: o limite de tentativas de login passa a contar por cliente real, e não pelo IP do proxy. |
| `TRUSTED_PROXIES` | Expressão regular com o IP do proxy. Padrão: só loopback (proxy na mesma máquina). |
| `CORS_ORIGINS` | Vazio quando o proxy serve tudo na mesma origem. Só preencha se o site ficar em outro domínio. |

Execute com `java -jar bee-good-food-api-0.1.0-SNAPSHOT.jar` como serviço do sistema (systemd, NSSM no Windows ou similar), reiniciando em caso de falha. Verifique `GET /actuator/health`.

> Só use `FORWARD_HEADERS_STRATEGY=native` se o proxy **sobrescrever** o cabeçalho `X-Forwarded-For` e a API não estiver acessível diretamente. Do contrário, um cliente poderia forjar o IP e escapar do limite de tentativas.

## 3. Interface

```powershell
cd frontend
npm ci
npm run build   # gera frontend\dist
```

Com proxy na mesma origem, deixe `VITE_API_URL` vazio. Copie `dist` para o servidor.

## 4. Proxy reverso (exemplo com Caddy)

```caddyfile
bee.exemplo.com.br {
    encode gzip
    handle /api/* {
        reverse_proxy 127.0.0.1:8080 {
            header_up X-Forwarded-For {remote_host}
        }
    }
    handle {
        root * /srv/bee-good-food/dist
        try_files {path} /index.html
        file_server
    }
    header {
        Strict-Transport-Security "max-age=31536000"
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
    }
}
```

`try_files ... /index.html` é necessário porque a interface usa rotas no navegador (`/ofertas/12`, `/minhas-lojas/3`). Não exponha `/actuator` além de `health` (já é o padrão) nem a porta do MySQL.

## 5. Primeiro administrador

Não existe conta administrativa padrão. Crie uma conta pelo site e conceda `ADMINISTRADOR` pelo procedimento controlado de [autenticação](planejamento/autenticacao.md#administração-local-controlada), conferindo o ID antes.

## 6. Dados de demonstração (opcional, nunca em produção real)

Em um banco **só de demonstração**, com o administrador criado:

```powershell
cd frontend
$env:BEE_API = 'http://127.0.0.1:8080'
$env:BEE_ADMIN_EMAIL = 'admin@exemplo.invalid'
$env:BEE_ADMIN_SENHA = Read-Host 'Senha do administrador' -MaskInput
npm run demo:popular
```

As ofertas criadas valem por algumas horas. Rode de novo antes de cada apresentação.

## Checklist antes de abrir ao público

- [ ] HTTPS ativo e redirecionamento de HTTP para HTTPS.
- [ ] `JWT_SECRET_BASE64` aleatório, fixo e fora do Git; senhas do MySQL fortes e fora do Git.
- [ ] API e MySQL inacessíveis diretamente pela internet.
- [ ] `FORWARD_HEADERS_STRATEGY` e `TRUSTED_PROXIES` coerentes com o proxy real.
- [ ] Backups do MySQL agendados e testados.
- [ ] Runner oficial (`database/scripts/validar-banco.ps1 -Api`) executado com sucesso na versão publicada.
- [ ] Termos de uso e aviso de privacidade (LGPD) revisados: o sistema guarda nome, e-mail e pedidos.

## Limites conhecidos

- O limite de tentativas é em memória, por instância. Com mais de uma instância da API, cada uma conta separadamente.
- Não há recuperação de senha nem revogação imediata de token (DEC: sem refresh token no MVP).
- Logs vão para a saída padrão. Configure a retenção no serviço e não registre corpos de requisição.
