# Métricas e validação

Este documento separa métricas calculáveis pelo sistema de hipóteses que exigem pesquisa. O objetivo é evitar alegações de impacto sem evidência suficiente. As métricas e rodadas abaixo são planejadas; não representam resultados já coletados, implementados ou aprovados.

## Regras de cálculo e acesso

Cada consulta do responsável selecionará um único estabelecimento sob sua responsabilidade. A API validará essa propriedade; o MVP não agregará automaticamente vários estabelecimentos no mesmo painel.

O período usará o intervalo semiaberto `[inicio, fim)`: inclui o instante inicial e exclui o final. As datas escolhidas serão interpretadas no fuso do estabelecimento e convertidas para UTC para comparação. O resultado deverá informar o período, o fuso e a data e hora da consulta. Será obrigatório `inicio < fim`, com `fim` não posterior ao instante da consulta.

Contagens, valores e quantidades concluídas usarão `concluidoEm` dentro do período. As taxas usarão uma **coorte**: o conjunto fixo de pedidos cujo `criadoEm` pertence ao período, classificado pelo estado de cada pedido no instante da consulta. Um pedido criado em setembro e concluído em outubro integrará o volume concluído de outubro e a taxa de conclusão da coorte de setembro. A taxa dessa coorte poderá mudar até que seus pedidos cheguem a estados finais.

Cada pedido poderá registrar `concluidoEm` uma única vez, ao entrar em `CONCLUIDO`. Preços e quantidades dos indicadores virão dos snapshots dos itens, sem consulta ao preço atual do catálogo.

## Métricas operacionais

| Métrica | Fórmula | Fonte | Interpretação permitida |
|---|---|---|---|
| Pedidos concluídos | Contagem de pedidos com `concluidoEm ∈ [inicio, fim)` | Pedido | Volume concluído na plataforma |
| Valor movimentado | Soma dos totais de pedidos com `concluidoEm ∈ [inicio, fim)` | Pedido | Valor associado aos pedidos concluídos, não recebimento comprovado |
| Itens comercializados | Soma das quantidades dos itens de pedidos com `concluidoEm ∈ [inicio, fim)` | ItemPedido e Pedido | Unidades concluídas por meio da plataforma |
| Economia estimada | Soma de `(preço original registrado - preço de oferta registrado) × quantidade` dos itens de pedidos com `concluidoEm ∈ [inicio, fim)` | ItemPedido e Pedido | Diferença nominal entre preços registrados |
| Estabelecimentos com atividade | Contagem distinta de estabelecimentos com oferta cujo `publicadaEm ∈ [inicio, fim)` ou pedido cujo `concluidoEm ∈ [inicio, fim)` | Oferta e Pedido | Atividade operacional, diferente do estado de moderação `ATIVO` |
| Taxa de conclusão da coorte | Pedidos da coorte em `CONCLUIDO` na data da consulta ÷ total de pedidos da coorte × 100 | Pedido | Parcela concluída dos pedidos criados no período |
| Taxa de cancelamento da coorte | Pedidos da coorte em `CANCELADO` na data da consulta ÷ total de pedidos da coorte × 100 | Pedido | Parcela cancelada dos pedidos criados no período |

`publicadaEm` representa a primeira publicação da oferta e não será sobrescrito ao pausar ou retomar. Uma oferta publicada fora do período, sem pedido concluído dentro dele, não caracteriza atividade nesse recorte. A métrica de estabelecimentos com atividade valerá 0 ou 1 no painel individual; a contagem distinta só será agregada se uma futura visão autorizada de avaliação do projeto for definida.

Quando o denominador for zero, a taxa deverá ser apresentada como **N/A — sem pedidos no período**, e não como zero. Havendo pedidos, um numerador zero produzirá 0%. Contagens e somas sem registros produzirão zero. As taxas de conclusão e cancelamento poderão somar menos de 100%, pois pedidos ainda em andamento permanecerão no denominador. Valores monetários serão calculados com decimal; taxas e valores serão arredondados somente na apresentação.

## Exemplos para verificação futura

Para uma coorte de 10 pedidos, se na data da consulta 6 estiverem concluídos, 2 cancelados e 2 em andamento, as taxas serão 60% e 20%. Os 2 pedidos em andamento continuarão no denominador. Uma nova consulta após a conclusão de um deles produzirá taxa de conclusão de 70%, sem mudar a composição da coorte.

Um pedido criado antes de `inicio` e concluído dentro de `[inicio, fim)` entrará nas contagens e somas concluídas, mas não nas taxas da coorte desse período. Um pedido concluído exatamente em `fim` pertencerá ao período seguinte. Se o período não contiver pedidos criados, ambas as taxas serão N/A, ainda que existam conclusões de pedidos criados anteriormente.

## Indicadores que não serão afirmados pelo MVP

O sistema, sozinho, não comprova:

- empregos gerados;
- aumento líquido de renda;
- pagamentos efetivamente recebidos;
- quantidade de desperdício evitado;
- formalização de empresas;
- redução de impacto ambiental.

Esses resultados exigem comparação temporal, dados externos ou pesquisa com os estabelecimentos.

## Relação com o ODS 8

O produto pretende contribuir com a meta 8.3 ao oferecer infraestrutura digital para divulgação de ofertas e gestão de pedidos de pequenos negócios. A evidência inicial será operacional: adesão, ofertas, pedidos concluídos e valor movimentado.

Uma avaliação de impacto posterior deverá combinar métricas do sistema com entrevistas e dados declarados pelos estabelecimentos.

## Hipóteses do produto

| ID | Hipótese | Como validar | Sinal favorável |
|---|---|---|---|
| H01 | Consumidores entendem a proposta de excedentes para retirada | Teste moderado do protótipo | Participante explica a proposta sem ajuda |
| H02 | As janelas de venda e retirada são compreendidas | Tarefa de localizar fim da venda e horário máximo de retirada | Participante distingue os dois prazos e identifica a condição de retirada |
| H03 | O estabelecimento consegue publicar oferta sem treinamento | Teste de tarefa com responsável | Publicação concluída sem erro crítico |
| H04 | O fluxo de pedido transmite confiança | Teste de checkout e acompanhamento | Participante identifica preço, local, horário e status |
| H05 | Indicadores são úteis para o responsável | Entrevista e demonstração | Responsável identifica ao menos uma decisão apoiada pelos dados |

## Plano de validação

### Rodada 1 — compreensão do protótipo

- Participantes: pelo menos 5 consumidores com 18 anos ou mais.
- Objetivo: avaliar proposta, busca, oferta, carrinho, retirada e acompanhamento.
- Registro: taxa de conclusão por tarefa, dúvidas recorrentes e observações sem identificação pessoal.

### Rodada 2 — operação do estabelecimento

- Participantes: pelo menos 3 responsáveis por micro ou pequenos estabelecimentos.
- Objetivo: avaliar cadastro, oferta, estoque, pedidos e indicadores.
- Registro: tarefas concluídas, tempo aproximado, erros e adequação do vocabulário.

### Rodada 3 — teste técnico

- Validar concorrência sobre o último item disponível.
- Validar autorização entre estabelecimentos.
- Validar todas as transições de pedido.
- Validar cálculos e recortes dos indicadores.
- Validar limites de período, conversão de fuso, coortes e denominador zero.
- Validar responsividade e acessibilidade dos fluxos principais.

## Proteção dos participantes

- Participação voluntária e informada.
- Somente pessoas com 18 anos ou mais.
- Coleta mínima de dados.
- Resultados publicados de forma agregada e sem identificação.
- Arquivos brutos não serão versionados em repositório público.

## Critério de aprendizagem

Um resultado negativo também é válido. Hipóteses rejeitadas deverão gerar mudança documentada no escopo, requisito, interface ou regra de negócio, com referência à evidência que motivou a decisão.
