# Kit de validação com pessoas

Materiais para as rodadas 1 e 2 do [plano de validação](../planejamento/metricas-e-validacao.md). Cada sessão leva cerca de 20 minutos e usa a [demonstração online](https://bee-good-food-recode.vercel.app): nada precisa ser instalado.

| Rodada | Quem | Quantas pessoas | Hipóteses | Roteiro |
|---|---|---|---|---|
| 1 | Consumidores com 18 anos ou mais | Pelo menos 5 | H01, H02, H04 | [roteiro-consumidor.md](roteiro-consumidor.md) |
| 2 | Responsáveis por pequenos negócios de comida | Pelo menos 3 | H03, H05 | [roteiro-loja.md](roteiro-loja.md) |

## Antes de cada sessão

1. Leia o [termo de consentimento](termo-de-consentimento.md) para a pessoa e confirme que ela tem 18 anos ou mais. Sem o "sim", não há sessão.
2. Abra a demonstração em **uma aba nova**: cada aba começa com os dados fictícios do zero.
3. Tenha à mão uma cópia da [ficha de registro](ficha-de-registro.md) e um cronômetro.
4. Se for presencial, entregue o celular ou o computador já com o site aberto. Se for online, envie o link e peça para a pessoa compartilhar a tela.

## Durante a sessão

- Peça para a pessoa **pensar em voz alta** e anote frases curtas, sem o nome dela.
- **Não ajude.** Se ela travar, pergunte "O que você esperava encontrar?". Só ajude depois de 2 minutos parada, e registre a tarefa como "com ajuda".
- Leia cada tarefa exatamente como está escrita no roteiro, sem explicar a tela.

## Como registrar cada tarefa

| Resultado | Quando usar |
|---|---|
| **Sim** | Concluiu sozinha e sem erro importante. |
| **Com ajuda** | Concluiu depois de uma dica, ou com erro que ela mesma corrigiu. |
| **Não** | Desistiu, errou o resultado ou passou de 5 minutos. |

Depois de cada tarefa, pergunte: **"De 1 a 7, o quanto essa tarefa foi fácil?"** (1 = muito difícil, 7 = muito fácil). Anote o número.

## Depois das sessões

1. Guarde as fichas preenchidas **fora do repositório**, ou na pasta `docs/validacao/registros/`, que o Git ignora. Elas não podem ir para o GitHub, nem sem nome.
2. Some os resultados em [resultados-validacao.md](../planejamento/resultados-validacao.md): só números agregados e comentários sem identificação.
3. Para cada hipótese, decida: **confirmada**, **parcial** ou **rejeitada**, pelo critério do roteiro.
4. Problema encontrado vira mudança no projeto, registrada nas [decisões](../planejamento/decisoes-do-mvp.md) com a evidência que a motivou. Um resultado negativo também é válido.

## Limites que devem ser ditos na apresentação

- São poucas pessoas, escolhidas por conveniência. O resultado mostra problemas de uso, não demanda de mercado.
- A demonstração usa dados fictícios: a pessoa avalia a interface, não uma compra real.
