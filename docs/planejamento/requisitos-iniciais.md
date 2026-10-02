# Requisitos iniciais

## Requisitos funcionais

| ID | Requisito | Prioridade |
|---|---|---|
| RF01 | Permitir o cadastro de clientes adultos e responsáveis por estabelecimentos. | Essencial |
| RF02 | Permitir login, logout e recuperação de acesso. | Essencial |
| RF03 | Controlar os perfis Cliente, Estabelecimento e Administrador. | Essencial |
| RF04 | Permitir o cadastro e a manutenção dos dados do estabelecimento. | Essencial |
| RF05 | Permitir o cadastro de categorias e produtos excedentes. | Essencial |
| RF06 | Registrar nome, descrição, quantidade, preços e período de disponibilidade do produto. | Essencial |
| RF07 | Permitir a busca e filtragem de estabelecimentos e produtos disponíveis. | Essencial |
| RF08 | Permitir adicionar, atualizar e remover itens do carrinho. | Essencial |
| RF09 | Permitir criar um pedido a partir do carrinho. | Essencial |
| RF10 | Permitir que o estabelecimento consulte e atualize o status de seus pedidos. | Essencial |
| RF11 | Permitir que o cliente acompanhe e consulte seu histórico de pedidos. | Essencial |
| RF12 | Exibir indicadores básicos de vendas para o estabelecimento. | Importante |
| RF13 | Permitir que o administrador consulte e modere usuários e estabelecimentos. | Importante |

## Requisitos não funcionais

| ID | Requisito | Categoria |
|---|---|---|
| RNF01 | O frontend deve ser desenvolvido com ReactJS. | Tecnologia |
| RNF02 | O backend deve ser desenvolvido em Java e disponibilizar uma API REST. | Tecnologia |
| RNF03 | Os dados devem ser armazenados em MySQL. | Tecnologia |
| RNF04 | A interface deve funcionar em dispositivos móveis e computadores. | Usabilidade |
| RNF05 | A navegação deve possuir rótulos claros e manter consistência visual. | Usabilidade |
| RNF06 | As entradas recebidas pela API devem ser validadas no servidor. | Segurança |
| RNF07 | As senhas devem ser armazenadas com algoritmo de hash seguro. | Segurança |
| RNF08 | Endpoints privados devem exigir autenticação e respeitar os perfis de acesso. | Segurança |
| RNF09 | A aplicação não deve armazenar credenciais ou segredos no repositório. | Segurança |
| RNF10 | Erros da API devem seguir um formato JSON consistente. | Manutenibilidade |

## Regras de negócio iniciais

| ID | Regra |
|---|---|
| RN01 | O cadastro de cliente deve confirmar que o usuário possui pelo menos 18 anos. |
| RN02 | Cada produto deve pertencer a um estabelecimento e a uma categoria. |
| RN03 | O preço de oferta deve ser positivo e menor ou igual ao preço original. |
| RN04 | A quantidade disponível não pode ser negativa. |
| RN05 | Um produto só pode ser incluído no pedido quando estiver ativo e disponível. |
| RN06 | Um pedido deve conter produtos de apenas um estabelecimento. |
| RN07 | A criação do pedido deve reservar ou descontar a quantidade disponível. |
| RN08 | Apenas o estabelecimento responsável pode atualizar o status do pedido. |
| RN09 | O cancelamento deve devolver ao estoque a quantidade reservada quando aplicável. |
| RN10 | A primeira versão apenas registra a forma de pagamento declarada pelo cliente. |

## Entidades previstas

- Usuário
- Endereço
- Estabelecimento
- Categoria
- Produto
- Pedido
- Item do pedido

A estrutura definitiva será validada na Etapa 2, durante a modelagem do banco MySQL.


## Complementos consolidados da Etapa 1

Estas regras complementam os requisitos originais e serão detalhadas em contratos de API e testes automatizados.

| ID | Regra adicional |
|---|---|
| RN39 | Oferta segue `RASCUNHO → PUBLICADA`, `PUBLICADA ↔ PAUSADA` e qualquer estado não terminal pode ir para `ENCERRADA`, que é final. Há no máximo uma oferta `PUBLICADA` ou `PAUSADA` por produto; a restrição deve ser atômica. |
| RN40 | O pedido preserva nome, endereço, fuso e prazo de retirada; o prazo é o menor `retiradaAte` dos itens. |
| RN41 | A criação exige chave de idempotência única por cliente e impressão digital do pedido; repetição equivalente retorna o pedido original e repetição diferente retorna `409`. |
| RN42 | Suspensão bloqueia novas ofertas e pedidos, mas permite tratar pedidos existentes. |
| RN43 | Preço e prazos de oferta publicada ou pausada são imutáveis; mudanças exigem nova oferta. Cancelamento devolve estoque exatamente uma vez. |
| RN44 | `publicadaEm` registra a primeira publicação e `concluidoEm` a conclusão única. Taxas usam coorte por `criadoEm` em `[inicio, fim)`; contagens concluídas usam `concluidoEm`; denominador zero é N/A. |
| RN45 | JWT dura 15 minutos, fica apenas em memória e não possui refresh token no MVP. |

### Rastreabilidade e evidências

Os critérios verificáveis estão em [Critérios de aceitação](criterios-de-aceitacao.md). As fórmulas e hipóteses estão em [Métricas e validação](metricas-e-validacao.md). Decisões, transições, snapshots e riscos estão em [Decisões do MVP](decisoes-do-mvp.md) e [Riscos e premissas](riscos-e-premissas.md). Os complementos são planejados e ainda não representam implementação ou testes executados.
