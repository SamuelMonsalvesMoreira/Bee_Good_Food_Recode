# Conclusão da Etapa 5 — Cadastros, catálogo, pedidos e indicadores

Data: 05/11/2026 (America/Sao_Paulo). Estado: **concluída localmente no backend**. O frontend React é a próxima etapa. Os instantes das evidências são gravados em UTC.

## O que foi entregue

| Bloco | Entrega | Rotas principais |
|---|---|---|
| 5.1 | Solicitação de estabelecimento por cliente autenticado, endereço completo, UF, CEP e fuso IANA (padrão `America/Sao_Paulo`). Consulta, edição e histórico somente pelo proprietário ou administrador. | `POST /api/v1/estabelecimentos`, `GET /meus`, `GET/PUT /{id}`, `GET /{id}/historico` |
| 5.2 | Aprovar, rejeitar, suspender e reativar com motivo obrigatório, bloqueio pessimista e histórico na mesma transação. A primeira aprovação concede `RESPONSAVEL`, preservando `CLIENTE`. | `GET /api/v1/admin/estabelecimentos`, `POST /{id}/aprovar\|rejeitar\|suspender\|reativar` |
| 5.3 | Categorias globais com slug e unicidade normalizada; produtos; ofertas `RASCUNHO → PUBLICADA ↔ PAUSADA → ENCERRADA`, com preço e prazos imutáveis após a publicação; ajuste relativo e atômico de estoque. | `/api/v1/admin/categorias`, `/estabelecimentos/{id}/produtos`, `/produtos/{id}/ofertas`, `/ofertas/{id}/publicar\|pausar\|retomar\|encerrar\|estoque` |
| 5.4 | Busca pública somente de ofertas compráveis agora, com texto, categoria, cidade, bairro e estabelecimento combinados; ordenação determinística e paginação padrão 20. | `GET /api/v1/ofertas`, `GET /api/v1/ofertas/{id}` |
| 5.5 | Pedido transacional de um estabelecimento: ofertas bloqueadas em ordem de ID, preços e total do servidor, snapshots, menor prazo de retirada, `Idempotency-Key` com impressão digital SHA-256. | `POST /api/v1/pedidos`, `GET /api/v1/pedidos`, `GET /api/v1/pedidos/{id}` |
| 5.6 | Avanço de status uma etapa por vez, `concluidoEm` único, cancelamento com devolução única de estoque e tratamento de pedidos de estabelecimento suspenso. | `POST /api/v1/pedidos/{id}/status`, `POST /{id}/cancelar`, `GET /estabelecimentos/{id}/pedidos` |
| 5.7 | Indicadores conforme [Métricas e validação](metricas-e-validacao.md): volume, valor, itens e economia por `concluidoEm`; taxas pela coorte de `criadoEm`; denominador zero como `null` (N/A). | `GET /api/v1/estabelecimentos/{id}/indicadores?inicio=…&fim=…` |

As decisões tomadas durante a implementação estão em [DEC-021 a DEC-027](decisoes-do-mvp.md). As migrations V1/V2 não foram alteradas: o schema da Etapa 2 já tinha toda a estrutura necessária.

## Correções em código das etapas anteriores

- **Horários gravados com deslocamento.** O Hibernate convertia `LocalDateTime` pelo fuso da JVM antes de aplicar `jdbc.time_zone=UTC`. Em `America/Sao_Paulo`, todo instante era gravado 3 horas adiantado, inclusive `maioridade_confirmada_em`. Pela própria aplicação a ida e a volta se compensavam, mas o erro aparecia em consultas SQL diretas, como a busca e os indicadores. Correção: `hibernate.type.java_time_use_direct_jdbc: true`, com teste de regressão contra `UTC_TIMESTAMP()` do MySQL. **Bancos locais criados antes desta correção** têm esses instantes 3h adiantados; recrie o banco de estudo ou corrija os registros antigos.
- **Precisão do relógio.** O `Clock` passou a trabalhar em microssegundos, como `DATETIME(6)`: o valor respondido pela API é exatamente o gravado.
- **Conflitos 409 genéricos.** `ApiExceptionHandler` agora identifica a constraint violada (e-mail, categoria, oferta ativa, chave de idempotência) sem refletir a mensagem do banco.
- **`Categoria` sem `@GeneratedValue`.** Corrigido para permitir a administração de categorias.

## Validação executada

**155 testes HTTP/MySQL + 6 testes unitários, sem falhas ou testes ignorados**, com `mvnw -B -Pintegration clean verify`:

| Suíte | Testes | Cobertura principal |
|---|---|---|
| `ApiMySqlIT` | 52 | Etapas 3 e 4 (categorias públicas, cadastro, JWT, papéis) e contrato OpenAPI |
| `EstabelecimentoIT` | 21 | CA03, CA19, CA22, CA26; instantes em UTC |
| `CatalogoIT` | 23 | CA04, CA06, CA20, CA25 |
| `CatalogoPublicoIT` | 13 | CA05, CA21, CA24 |
| `PedidoIT` | 36 | CA05, CA08 a CA16, CA23, CA24, CA26 |
| `IndicadoresIT` | 10 | CA17 e exemplos de `metricas-e-validacao.md` (coorte 60%/20%, limites semiabertos, N/A) |

Fluxos concorrentes verificados com requisições HTTP simultâneas:

- última unidade disputada por 5 clientes: 1 pedido, estoque 0;
- 6 repetições simultâneas da mesma chave: 1 pedido, 1 baixa de estoque;
- 6 cancelamentos simultâneos (cliente e responsável): estoque devolvido uma vez;
- 4 publicações simultâneas do mesmo produto: 1 oferta ativa;
- 8 ajustes simultâneos de −1 com saldo 5: 5 aceitos e saldo 0;
- moderação, criação de categoria e avanço de status simultâneos: uma única transição ou registro.

O **JAR final** também foi iniciado duas vezes no mesmo banco. Na primeira execução, o fluxo completo funcionou: cadastro, aprovação, publicação, busca, pedido, repetição idempotente, conclusão e indicadores. Na segunda, os dados e o token da primeira execução continuaram válidos com a mesma chave. Os logs não continham senha, hash ou token.

### Ambiente da validação

Os testes rodaram em um MySQL 8.0 descartável no Docker, exposto somente em `127.0.0.1`, com o schema `bee_good_food_validacao`. O runner oficial `database/scripts/validar-banco.ps1 -Api` **não foi executado nesta etapa**, porque exige PowerShell 7, que não estava instalado nesta máquina. O script foi atualizado para a Etapa 5: compara a contagem de categorias antes e depois do reinício e consulta o catálogo de ofertas. Recomenda-se executá-lo antes da entrega para gerar `ultima-validacao.json`.

## Evidências

- [Relatório da validação da Etapa 5](../../backend/evidencias/validacao-etapa-5.json)
- [Contrato OpenAPI gerado pelo JAR](../../backend/evidencias/openapi.json)
- [Testes HTTP/MySQL](../../backend/src/test/java/br/com/beegoodfood/)
- [Guia para executar e estudar a API](../../backend/README.md)

Esses resultados são locais, em uma única instância. Não comprovam desempenho sob carga, segurança de uma implantação pública ou funcionamento das futuras telas.

## Próxima etapa — checklist

### Etapa 6: interface React

- [ ] Integrar início, explorar ofertas, detalhe, cadastro/login, carrinho e pedidos à API.
- [ ] Carrinho de um único estabelecimento, com confirmação antes de trocar de loja (CA07); gerar uma `Idempotency-Key` por tentativa de checkout e reutilizá-la em novas tentativas da mesma compra.
- [ ] Áreas de cliente, responsável (catálogo, fila de pedidos, indicadores) e administrador (moderação, categorias).
- [ ] Estados de carregamento, vazio, sucesso e erro; mensagens de `campos` por campo; taxas `null` exibidas como "N/A — sem pedidos no período".
- [ ] Fim da venda e prazo de retirada exibidos separadamente, no fuso do estabelecimento.
- [ ] Guardar o token apenas em memória, limpar a sessão no logout/401 e configurar CORS para origens explícitas.
- [ ] Conferir responsividade, teclado, rótulos e mascote Bee.

### Etapa 7: entrega do TCC

- [ ] Executar o runner oficial com PowerShell 7 e registrar `ultima-validacao.json`.
- [ ] Testes ponta a ponta com a interface, documentação final, demonstração e apresentação vinculadas ao ODS 8.
- [ ] Revisão de segredos, ambiente, proxy (`AuthRateLimitFilter` usa `getRemoteAddr()`) e pré-requisitos de deploy.
