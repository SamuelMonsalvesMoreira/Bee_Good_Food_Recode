# Glossário

| Termo | Definição no Bee Good Food Recode |
|---|---|
| Administrador | Usuário com papel controlado responsável pela moderação e pelas categorias globais. |
| Autodeclaração de maioridade | Confirmação do usuário de que possui 18 anos ou mais; não representa verificação documental. |
| Categoria | Classificação global administrada pela plataforma, com nome normalizado e slug únicos; precisa estar ativa para permitir novas vendas. |
| Cliente | Usuário adulto que pesquisa ofertas e registra pedidos. |
| Estabelecimento | Negócio local cadastrado e moderado na plataforma; não é uma conta de usuário. |
| Estabelecimento ativo | Estabelecimento aprovado e não suspenso, autorizado a publicar ofertas e receber pedidos. |
| Estabelecimento suspenso | Negócio impedido de receber novas vendas; seus pedidos existentes continuam tratáveis conforme as regras de cada estado. |
| Excedente | Produto próprio para comercialização que o estabelecimento disponibiliza por tempo e quantidade limitados. |
| Forma de pagamento declarada | Opção informada para a retirada; não representa pagamento processado ou confirmado. |
| Histórico de status do pedido | Registro imutável da criação e das mudanças de estado de um pedido, incluindo ator, instante e motivo quando exigido. Todo pedido tem ao menos um registro. |
| Histórico de status do estabelecimento | Registro imutável da criação e das transições de moderação, incluindo estado anterior, novo estado, ator, instante e motivo quando exigido. Todo estabelecimento tem ao menos um registro. |
| Idempotência | Garantia de que repetir a mesma requisição, com a mesma chave e payload para o mesmo cliente, retorna o pedido original sem novo desconto de estoque. A mesma chave com payload diferente resulta em `409 Conflict`. |
| Item do pedido | Fotografia dos dados comprados, preservando nome, preços, quantidade e subtotal. |
| Oferta | Condição comercial temporária de um produto, com preços, estoque, período de venda e prazo de retirada. Seus estados são `RASCUNHO`, `PUBLICADA`, `PAUSADA` e `ENCERRADA`; no máximo uma oferta por produto pode estar publicada ou pausada. |
| Pedido | Compromisso registrado entre cliente e um único estabelecimento para retirada das ofertas selecionadas. |
| Produto | Item permanente do catálogo do estabelecimento, separado das condições temporárias da oferta. |
| Responsável | Usuário com papel `RESPONSAVEL`, concedido na primeira aprovação de um estabelecimento, autorizado a administrar apenas os negócios vinculados a ele. Cada estabelecimento tem um responsável no MVP. |
| Retirada | Modalidade do MVP em que o cliente busca o pedido no estabelecimento. |
| Prazo de retirada | Instante limite para retirar o pedido, calculado como o menor `retiradaAte` entre suas ofertas e preservado no pedido. |
| Snapshot | Cópia histórica de dados feita na criação do pedido. Preserva nome e endereço do estabelecimento, fuso, prazo de retirada e os dados comerciais de cada item. |
| Solicitante | Cliente autenticado que solicita um estabelecimento e pode editar, acompanhar e reenviar o próprio cadastro por propriedade, antes da concessão do papel `RESPONSAVEL`. |
| Fuso do estabelecimento | Identificador IANA usado para interpretar e apresentar seus horários; o padrão é `America/Sao_Paulo`. |
| Valor movimentado | Soma dos totais de pedidos concluídos; não comprova recebimento financeiro. |
| Período de venda | Intervalo `[inicioVenda, vendaAte)` no qual uma oferta publicada pode ser incluída em novo pedido, desde que cumpra as demais regras; inclui o início e exclui o fim. Deve obedecer a `inicioVenda < vendaAte <= retiradaAte`. |

## Vocabulário evitado

- **Restaurante:** usar apenas quando o estabelecimento for de fato um restaurante; o produto atende outros pequenos negócios do setor alimentício.
- **Entrega:** não faz parte do MVP.
- **Pagamento aprovado:** a plataforma não processa pagamento.
- **Receita gerada:** preferir `valor movimentado` enquanto não houver confirmação financeira.
- **Desperdício evitado:** exige metodologia externa; usar `itens comercializados` como métrica observável.
- **Perfil estabelecimento:** usar `responsável` para o papel e `estabelecimento` para a entidade.
