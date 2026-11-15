# Bee Good Food: resumo de uma página

**Pequenos negócios de comida vendem com desconto o que sobrou no dia. O cliente reserva pelo site e retira no balcão.**

Projeto final do Programa Transforma Futuros, de Samuel Monsalves Moreira. Alinhado ao ODS 8: trabalho decente e crescimento econômico.

## O problema

Padarias, hortifrutis e restaurantes de bairro terminam o dia com comida boa que não foi vendida, e não têm um canal simples para vender esse excedente.

- 1,05 bilhão de toneladas de comida foram desperdiçadas no mundo em 2022. Desse total, 40% se perdem no varejo e nos serviços de alimentação (PNUMA, 2024, divulgado pela Embrapa).
- O Brasil tem mais de 120 mil padarias, quase todas pequenos negócios (Agência Sebrae, 2026).

Fontes e ressalvas em [contexto de mercado](../planejamento/contexto-de-mercado.md).

## A solução

- **A loja** publica o excedente com preço, estoque e prazos, atende os pedidos e acompanha os resultados em indicadores.
- **O cliente** busca ofertas por cidade e bairro, reserva e paga no balcão. Na retirada, mostra um código que só ele vê.
- **A moderação** aprova cada loja antes de ela começar a vender.

## O que já está pronto

| Item | Situação |
|---|---|
| Sistema completo | Interface em React, API em Java e banco MySQL |
| Testes | 217 testes automáticos, executados a cada alteração no código |
| Segurança | Revisão feita, sem alertas nas dependências |
| Acessibilidade | 17 telas auditadas, sem violações (WCAG 2.1 AA) |
| Decisões | 41 decisões registradas, cada uma com motivo e consequência |
| Demonstração | Aberta ao público, com dados fictícios |

## O que ainda falta

- **Teste com pessoas reais.** O sistema ainda não foi usado por lojas e consumidores. O impacto em renda é esperado, não medido.
- **Publicação do sistema completo.** A demonstração online usa dados simulados no navegador; o sistema com Java e MySQL roda localmente.
- **Fora do escopo, por decisão:** entrega, pagamento online e geolocalização.

## Próximo passo

Um piloto com 3 lojas de bairro e 5 consumidores, com [roteiro de testes](../validacao/README.md) já preparado. Como exemplo hipotético, uma loja que venda 5 unidades por dia a R$ 6,00 recupera R$ 780 por mês de comida que seria perda.

## Onde ver

- Demonstração: [bee-good-food-recode.vercel.app](https://bee-good-food-recode.vercel.app)
- Código e documentação: [github.com/SamuelMonsalvesMoreira/Bee_Good_Food_Recode](https://github.com/SamuelMonsalvesMoreira/Bee_Good_Food_Recode)
