# Critérios de aceitação

Os cenários abaixo tornam as regras críticas verificáveis. Eles serão transformados em testes automatizados e testes de interface nas etapas de implementação. São critérios planejados, não evidências de funcionalidades implementadas ou testes executados.

## Identidade e acesso

### CA01 — Maioridade obrigatória

**Dado** um cadastro sem autodeclaração de idade igual ou superior a 18 anos,  
**quando** a solicitação for enviada,  
**então** a API deverá rejeitá-la sem criar o usuário.

### CA02 — E-mail único

**Dado** um e-mail já cadastrado,  
**quando** outro cadastro usar o mesmo endereço com qualquer combinação de maiúsculas e minúsculas,  
**então** a API deverá retornar conflito sem duplicar a conta.

### CA03 — Isolamento entre responsáveis

**Dado** um responsável autenticado pelo estabelecimento A,  
**quando** tentar alterar estabelecimento, produto, oferta ou pedido do estabelecimento B,  
**então** a API deverá negar a operação sem revelar ou modificar dados protegidos.

## Estabelecimentos e catálogo

### CA04 — Moderação obrigatória

**Dado** um estabelecimento em `PENDENTE_ANALISE`, `REJEITADO` ou `SUSPENSO`,  
**quando** o responsável tentar publicar uma oferta,  
**então** a operação deverá ser rejeitada.

### CA05 — Disponibilidade da oferta

**Dado** uma oferta não publicada, expirada, futura, sem estoque, com produto ou categoria desativados ou vinculada a estabelecimento não ativo,  
**quando** o catálogo for consultado ou um pedido for criado,  
**então** a oferta não deverá ser apresentada como comprável nem aceita no pedido. A compra exige simultaneamente oferta `PUBLICADA`, janela de venda vigente, estoque positivo, produto e categoria ativos e estabelecimento `ATIVO`.

### CA06 — Validação de preço e período

**Dado** preço não positivo, preço de oferta superior ao original, estoque negativo ou períodos que violem `inicioVenda < vendaAte <= retiradaAte`,  
**quando** uma oferta for publicada,  
**então** a API deverá rejeitar os campos inválidos de forma explícita, inclusive quando `vendaAte` já tiver passado.

## Carrinho, pedido e estoque

### CA07 — Carrinho de um estabelecimento

**Dado** um carrinho com ofertas do estabelecimento A,  
**quando** o cliente tentar incluir uma oferta do estabelecimento B,  
**então** a interface deverá solicitar confirmação antes de remover os itens atuais.

### CA08 — Autoridade do backend

**Dado** um pedido com preço ou total adulterado no frontend,  
**quando** a criação chegar à API,  
**então** os valores recebidos deverão ser ignorados e recalculados a partir das ofertas válidas.

### CA09 — Atomicidade da criação

**Dado** um carrinho válido,  
**quando** o pedido for criado,  
**então** desconto de estoque, pedido, itens e primeiro histórico deverão ser confirmados juntos ou todos desfeitos.

### CA10 — Concorrência na última unidade

**Dado** duas requisições concorrentes para a última unidade de uma oferta,  
**quando** ambas forem processadas,  
**então** somente uma deverá criar o pedido e o estoque nunca poderá ficar negativo.

### CA11 — Snapshot do item

**Dado** um pedido existente,  
**quando** o nome do produto for editado ou uma nova oferta com outro preço for publicada,  
**então** nome, preços, quantidade e subtotal registrados no item do pedido deverão permanecer inalterados.

### CA12 — Suspensão sem perda de histórico

**Dado** um estabelecimento com pedidos anteriores,  
**quando** ele for suspenso,  
**então** novas ofertas e pedidos deverão ser bloqueados e o histórico anterior deverá permanecer consultável pelos atores autorizados.

## Status e cancelamento

### CA13 — Transição válida

**Dado** um pedido `CONFIRMADO`,  
**quando** o responsável pelo estabelecimento o alterar para `EM_PREPARO`,  
**então** o novo estado e o histórico deverão ser gravados na mesma operação.

### CA14 — Transição inválida

**Dado** um pedido `PENDENTE`,  
**quando** houver tentativa de alterá-lo diretamente para `CONCLUIDO`,  
**então** a API deverá retornar conflito e manter o estado original.

### CA15 — Cancelamento pelo cliente

**Dado** um pedido do próprio cliente,  
**quando** ele solicitar cancelamento,  
**então** a operação só deverá ser aceita se o estado atual for `PENDENTE`.

### CA16 — Devolução idempotente de estoque

**Dado** um cancelamento permitido,  
**quando** a solicitação for processada ou repetida,  
**então** o estoque deverá ser devolvido exatamente uma vez.

## Indicadores

### CA17 — Recorte e origem dos indicadores

**Dado** um responsável autenticado, um único estabelecimento selecionado e um período informado,  
**quando** os indicadores forem consultados,  
**então** a API deverá verificar se o estabelecimento pertence ao responsável e considerar somente seus registros, conforme a data e a fórmula definidas em [Métricas e validação](metricas-e-validacao.md). A seleção de estabelecimento de outro responsável deverá ser negada sem revelar seus indicadores. Não haverá agregação automática de todos os estabelecimentos do responsável.

## Complementos de identidade, catálogo e integridade

### CA18 — Login, logout e expiração da sessão

**Dado** credenciais válidas de um usuário,  
**quando** o login for concluído,  
**então** a API deverá emitir um JWT de acesso com validade de 15 minutos, e o frontend deverá mantê-lo apenas em memória, sem refresh token no MVP.

**Dado** uma sessão autenticada,  
**quando** o usuário fizer logout ou o token expirar,  
**então** o frontend deverá limpar o token e exigir novo login para as operações privadas; a API deverá rejeitar tokens expirados. O logout local não implica revogação antecipada do JWT no servidor: um token já emitido permanece válido até expirar.

### CA19 — Solicitação e aprovação de estabelecimento

**Dado** um usuário cadastrado como `CLIENTE`,  
**quando** ele solicitar o cadastro de um estabelecimento com dados obrigatórios, endereço completo e fuso horário válido,  
**então** o estabelecimento deverá ser criado em `PENDENTE_ANALISE`, vinculado a esse usuário, sem habilitar publicação ou recebimento de pedidos.

**Dado** uma solicitação pendente,  
**quando** um administrador aprová-la,  
**então** o estabelecimento deverá tornar-se `ATIVO` e o usuário deverá receber o papel cumulativo `RESPONSAVEL`, preservando `CLIENTE`. A aprovação deverá ser rejeitada se endereço ou fuso estiverem incompletos ou inválidos.

### CA20 — Administração das categorias

**Dado** um administrador autenticado,  
**quando** criar, consultar, editar ou desativar uma categoria global,  
**então** a API deverá permitir a operação conforme as validações e impedir duplicações do nome normalizado ou do slug, inclusive em requisições concorrentes. Usuários sem esse papel não poderão alterar categorias.

**Dado** uma categoria referenciada por produtos,  
**quando** ela for desativada,  
**então** as referências históricas deverão permanecer e as ofertas desses produtos deverão deixar de aceitar novos pedidos. A categoria referenciada não poderá ser excluída fisicamente.

### CA21 — Busca e paginação do catálogo

**Dado** ofertas compráveis em categorias e localidades diferentes,  
**quando** o visitante ou cliente combinar filtros de texto, categoria, cidade e bairro,  
**então** o catálogo deverá retornar apenas ofertas que satisfaçam todos os filtros informados, com ordenação determinística, metadados de paginação e resultado vazio explícito quando não houver correspondência.

**Dado** uma consulta sem tamanho de página ou com tamanho inválido,  
**quando** a API processá-la,  
**então** deverá usar 20 registros por padrão e rejeitar valores fora do intervalo de 1 a 100; páginas sucessivas do mesmo conjunto estável não deverão repetir registros.

### CA22 — Consulta administrativa com dados limitados

**Dado** um administrador autenticado,  
**quando** consultar usuários, estabelecimentos ou registros de moderação,  
**então** a resposta deverá incluir somente campos necessários à função administrativa e jamais senha, hash de senha ou token. Usuários sem permissão administrativa deverão receber acesso negado.

### CA23 — Criação idempotente do pedido

**Dado** um cliente e uma chave de idempotência ainda não utilizada por ele,  
**quando** a API criar o pedido,  
**então** deverá registrar a chave única por cliente e a impressão digital do conteúdo relevante da solicitação, junto com o pedido na mesma transação.

**Dado** a repetição, inclusive concorrente, da mesma chave pelo mesmo cliente,  
**quando** o conteúdo for equivalente ao original,  
**então** a API deverá retornar o mesmo pedido, sem criar outro ou descontar estoque novamente; se o conteúdo for diferente, deverá retornar conflito `409` sem alteração de dados. A mesma chave usada por clientes diferentes não poderá revelar ou reutilizar pedidos entre eles.

### CA24 — Prazos de venda, retirada e dados preservados

**Dado** uma oferta com `inicioVenda < vendaAte <= retiradaAte`,  
**quando** o relógio atingir `vendaAte`,  
**então** novos pedidos deverão ser bloqueados para essa oferta, preservando o prazo de retirada dos pedidos já criados. A janela de venda será `[inicioVenda, vendaAte)`.

**Dado** um pedido com uma ou mais ofertas do mesmo estabelecimento,  
**quando** ele for criado,  
**então** deverá preservar nome, endereço e fuso do estabelecimento e adotar o menor `retiradaAte` entre as ofertas como prazo máximo de retirada. Alterações posteriores no cadastro ou nas ofertas não poderão mudar esses dados históricos.

### CA25 — Ciclo de vida das ofertas

**Dado** uma oferta em `RASCUNHO`, `PUBLICADA` ou `PAUSADA`,  
**quando** uma mudança de estado for solicitada,  
**então** somente `RASCUNHO → PUBLICADA`, `PUBLICADA → PAUSADA`, `PAUSADA → PUBLICADA` ou a transição de qualquer desses estados para `ENCERRADA` deverá ser permitida. Publicação e retomada exigirão as validações de publicação, incluindo estabelecimento, produto e categoria ativos e fim da venda futuro; uma oferta agendada só será comprável a partir de `inicioVenda`. `ENCERRADA` será final.

**Dado** um produto com oferta `PUBLICADA` ou `PAUSADA`,  
**quando** outra oferta desse produto tentar entrar em qualquer desses dois estados,  
**então** a API deverá retornar conflito, inclusive sob concorrência. Poderão existir rascunhos e ofertas encerradas, mas no máximo uma oferta `PUBLICADA` ou `PAUSADA` por produto.

### CA26 — Histórico de moderação e continuidade dos pedidos

**Dado** uma aprovação, rejeição, suspensão ou reativação de estabelecimento,  
**quando** a mudança for autorizada,  
**então** estado anterior, novo estado, administrador, data e motivo deverão ser registrados junto com a mudança, preservando o histórico.

**Dado** um estabelecimento suspenso com pedidos anteriores ainda em andamento,  
**quando** seus atores autorizados consultarem, cancelarem ou avançarem esses pedidos,  
**então** as operações previstas na matriz de transição deverão continuar disponíveis. A suspensão bloqueará novas publicações e novos pedidos, sem impedir o tratamento dos pedidos existentes.

## Critérios de interface

- A interface deverá apresentar estado de carregamento, vazio, sucesso e erro nos fluxos principais.
- Mensagens de validação deverão indicar o campo e a correção esperada.
- O fim da venda e o horário máximo de retirada deverão ser apresentados separadamente no card e no detalhe da oferta, usando o fuso do estabelecimento.
- A tela inicial deverá explicar, antes da primeira ação, que as ofertas são excedentes com retirada no estabelecimento.
- A troca de estabelecimento no carrinho nunca poderá remover itens silenciosamente.
- Estados do pedido deverão usar texto, não depender apenas de cor.

## Critérios para futura automação

Os testes deverão cobrir, no mínimo:

- autorização por papel e propriedade;
- normalização e unicidade de e-mail;
- login, logout local e expiração do JWT;
- solicitação e aprovação do estabelecimento;
- moderação e gestão de categorias;
- validações de oferta;
- cálculo de subtotais e total;
- concorrência de estoque;
- idempotência da criação e do cancelamento;
- todas as transições permitidas e proibidas;
- preservação do histórico;
- preservação de endereço, fuso e prazo de retirada;
- filtros e paginação;
- fórmulas dos indicadores.
