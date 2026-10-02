# Riscos e premissas

O registro será revisado ao final de cada etapa. Probabilidade e impacto representam a avaliação atual do MVP.

## Premissas

- O projeto será desenvolvido como MVP acadêmico por uma equipe pequena.
- O fluxo principal termina na retirada; pagamento e logística não são operados pela plataforma.
- Estabelecimentos informam corretamente produto, preço, estoque e disponibilidade.
- Clientes e responsáveis possuem 18 anos ou mais.
- O ambiente inicial usará uma instância MySQL e um monólito modular.
- A amostra da pesquisa original orienta melhorias, mas não valida demanda de mercado.

## Registro de riscos

| ID | Risco | Prob. | Impacto | Mitigação | Sinal de alerta |
|---|---|---:|---:|---|---|
| R01 | Venda acima do estoque | Alta | Alto | Transação, bloqueio de ofertas e teste concorrente | Estoque negativo ou dois pedidos para a última unidade |
| R02 | Crescimento descontrolado do escopo | Alta | Alto | Manter entrega, pagamento, chat e geolocalização fora do MVP | Nova funcionalidade sem substituir ou replanejar outra |
| R03 | Acesso a dados de outro estabelecimento | Média | Alto | Validar papel e pertencimento em todo endpoint privado | Teste altera recurso apenas conhecendo o ID |
| R04 | Histórico mudar após edição do produto | Média | Alto | Snapshot obrigatório em `ItemPedido` | Pedido antigo exibe preço ou nome atual |
| R05 | Pedido duplicado por repetição da requisição | Média | Alto | Chave de idempotência e restrição única | Duplo clique cria pedidos diferentes |
| R06 | Cancelamento devolver estoque mais de uma vez | Média | Alto | Operação idempotente e transacional | Repetição aumenta estoque novamente |
| R07 | JWT implementado de forma insegura | Média | Alto | Token curto, segredo externo e estratégia documentada | Token duradouro exposto em log ou armazenamento inadequado |
| R08 | Exclusão quebrar referências históricas | Média | Alto | Desativação lógica e FKs restritivas | Produto ou estabelecimento com pedidos é apagado |
| R09 | Horários inconsistentes | Média | Médio | Persistir UTC e converter na interface | Oferta expira em horário diferente do exibido |
| R10 | Proposta parecer delivery genérico | Média | Médio | Destacar excedente, janela limitada, retirada e métricas próprias | Usuários não conseguem explicar o diferencial |
| R11 | Pesquisa induzir conclusão excessiva | Média | Médio | Amostra declarada e nova validação com públicos separados | Documento trata quatro respostas como validação de mercado |
| R12 | Exposição de dados ou segredos | Baixa | Alto | Minimização, `.gitignore`, variáveis de ambiente e revisão de logs | Token, senha ou planilha bruta aparece no Git |
| R13 | Categorias duplicadas ou inconsistentes | Média | Médio | Catálogo global administrado e nome/slug únicos | Variações da mesma categoria aparecem no filtro |
| R14 | Ambiente difícil de reproduzir | Média | Médio | README, versões fixadas, migrations e Docker Compose | Novo ambiente não inicia seguindo a documentação |
| R15 | Métricas serem interpretadas como impacto comprovado | Média | Médio | Definições, fórmulas e limitações explícitas | Valor movimentado é divulgado como renda recebida |

## Priorização de tratamento

Riscos de alto impacto relacionados a integridade, autorização e segurança bloqueiam a conclusão da etapa técnica correspondente. Não devem ser aceitos apenas para cumprir prazo.

### Antes de concluir o banco

- R01, R04, R06, R08, R09 e R13 devem estar cobertos por modelo, restrições e decisões documentadas.

### Antes de concluir a API

- R01, R03, R05, R06, R07 e R12 devem possuir testes automatizados.

### Antes de concluir o frontend

- R09 e R10 devem ser avaliados nos fluxos reais.

### Antes da apresentação final

- R11, R14 e R15 devem estar refletidos no README, demonstração e conclusões.

## Processo de revisão

Ao identificar um novo risco:

1. registrar causa, consequência e evidência;
2. estimar probabilidade e impacto;
3. definir mitigação e etapa responsável;
4. criar critério de aceitação ou teste quando aplicável;
5. revisar a avaliação após a mitigação.
