# Análise de mercado e melhorias

Data da pesquisa: 06/11/2026. Fontes públicas (sites oficiais, lojas de aplicativos e imprensa), listadas no fim. Números de escala divulgados pelas próprias empresas são **declarados por elas** e não foram verificados.

## Objetivo

Comparar o Bee Good Food com os principais aplicativos de venda de excedentes de comida e trazer para o MVP as ideias que melhoram a experiência sem sair do escopo definido: retirada no balcão, pagamento apenas declarado e público adulto (DEC-001, DEC-011, DEC-014).

## Aplicativos analisados

| Aplicativo | Origem | Modelo | O que faz bem |
|---|---|---|---|
| **Too Good To Go** | Dinamarca, presente em vários países | "Sacolas surpresa" de restaurantes, padarias e mercados, retiradas em uma janela de horário | Vitrine com recomendações e ordenação por distância, preço, avaliação e relevância; favoritos com alerta; comprovante de retirada deslizando na tela; cancelamento pelo recibo do pedido |
| **Food To Save** | Brasil | Sacolas surpresa com até 70% de desconto, por categoria (doce, salgada ou mista) | Conteúdo revelado só na retirada, o que dá flexibilidade à loja; segundo a imprensa, cerca de 60% dos pedidos são retirados na loja; parcerias com grandes redes |
| **Phenix** | França | Cestas de excedentes de padarias, mercados e restaurantes | Pontos de recompensa e estatísticas de impacto (comida salva, dinheiro economizado); filtros por dieta |
| **Karma** | Suécia | Restaurantes e cafés publicam excedentes em horários definidos | Compra direta no aplicativo de itens específicos, não só sacolas |
| **Flashfood** | Canadá e EUA | Supermercados publicam itens perto do vencimento | Foco em mercado: itens individuais com grande desconto, retirados na loja |

## O que o Bee Good Food já fazia igual ou melhor

| Ponto | Bee Good Food | Mercado |
|---|---|---|
| Prazo de retirada | Dois prazos separados ("Compre até" e "Retire até") em todo cartão, no fuso da loja | Janela única de retirada |
| Item ou sacola | A loja publica itens específicos com estoque real; uma "sacola surpresa" também pode ser publicada como produto | Too Good To Go e Food To Save focam em sacolas surpresa |
| Venda da última unidade | Garantida no servidor sob concorrência, com teste automatizado | Não divulgado |
| Moderação das lojas | Toda loja passa por aprovação, com motivo registrado | Não divulgado |
| Indicadores para a loja | Pedidos retirados, valor, unidades e taxas por período | Painéis para parceiros, sem detalhes públicos |
| Acessibilidade | Auditoria WCAG 2.1 AA sem violações | Não divulgado |

## Melhorias trazidas do mercado para o MVP

| Melhoria | Inspiração | O que muda no Bee Good Food |
|---|---|---|
| Vitrine com seções | Too Good To Go (Discover) | Página inicial com "Terminam logo" e "Maiores descontos" antes da lista completa |
| Ordenação da busca | Too Good To Go | Ordenar por prazo, maior desconto, menor preço ou mais recentes (a API passou a aceitar `ordem`) |
| Comprovante de retirada | Too Good To Go (deslizar na tela) | Código de retirada grande na página do pedido, para mostrar no balcão |
| Impacto para o cliente | Phenix e Too Good To Go | "Você já economizou R$ X" no histórico de pedidos (a API passou a informar a economia de cada pedido) |
| Sacola surpresa | Too Good To Go e Food To Save | Selo "Sacola surpresa" nas ofertas assim chamadas e exemplos nos dados de demonstração |
| Experiência de aplicativo no celular | Todos | Barra de navegação inferior, tema escuro automático e avisos rápidos de confirmação |
| Contagem regressiva | Comum no setor | "Termina em 2 h" nos cartões quando a venda está perto do fim |

## Ideias registradas para depois do MVP

Estas ideias aparecem no mercado, mas ficam fora do MVP pelas decisões do projeto ou por exigirem pesquisa antes:

- **Favoritos e alertas** de novas ofertas (Too Good To Go). O escopo inicial deixa favoritos de fora; exigiria notificações.
- **Avaliações** das lojas (Too Good To Go). Fora do escopo inicial; exige moderação de conteúdo.
- **Filtros por dieta** (Phenix). Exige novos atributos no produto e responsabilidade sobre a informação.
- **Busca por distância** (Too Good To Go). DEC-015 adotou cidade e bairro para evitar geolocalização no MVP.
- **Entrega** (Food To Save). DEC-001 restringe o MVP à retirada.
- **Pagamento no aplicativo** (todos). DEC-011 mantém o pagamento apenas declarado.
- **Preço dinâmico** (Too Good To Go anunciou em 2026). Sem detalhes públicos suficientes para avaliar.

## Como usar no TCC

- Seção de trabalhos relacionados: a tabela "Aplicativos analisados".
- Justificativa do diferencial: a tabela "O que o Bee Good Food já fazia igual ou melhor", com o cuidado de não apresentar como vantagem o que o concorrente apenas não divulga.
- Evolução do produto: "Melhorias trazidas do mercado" mostra decisões tomadas a partir da análise; "Ideias registradas para depois do MVP" mostra o que foi conscientemente adiado.

## Fontes

- Too Good To Go. *How does the app work?* https://toogoodtogo.com/how-does-the-app-work
- Too Good To Go. *How to collect Too Good To Go?* https://toogoodtogo.com/how-to-collect-too-good-to-go
- Mobile Time. Reportagem sobre o Food To Save. https://www.mobiletime.com.br/?p=720203
- Tecnoblog. *App de delivery com produtos mais baratos perto da validade é lançado no RJ.* https://tecnoblog.net/noticias/app-de-delivery-com-produtos-mais-baratos-perto-da-validade-e-lancado-no-rj/
- Gironews. *Food To Save tem 60% dos pedidos retirados nas lojas.* https://gironews.com/food-service/food-to-save-tem-60-dos-pedidos-retirados-nas-lojas/
- Página do Phenix na loja de aplicativos (resumo). https://spark.mwm.ai/en/apps/phenix-anti-food-waste-app/1437997699
- CB Insights. Concorrentes do Too Good To Go. https://www.cbinsights.com/company/too-good-to-go/alternatives-competitors
