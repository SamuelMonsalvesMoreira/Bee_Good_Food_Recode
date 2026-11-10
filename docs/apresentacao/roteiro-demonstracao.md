# Roteiro de demonstração

Duração sugerida: **12 minutos de apresentação + 5 de perguntas**. A demonstração usa somente dados fictícios.

## Antes da apresentação (no dia)

1. Banco **só de demonstração** vazio e API em execução (veja `backend/README.md`).
2. Administrador criado pelo [procedimento controlado](../planejamento/autenticacao.md#administração-local-controlada).
3. Na pasta `frontend`, popular os dados. As ofertas valem por algumas horas, então rode **no máximo 2 horas antes**.

   ```powershell
   $env:BEE_ADMIN_EMAIL = 'admin@exemplo.invalid'
   $env:BEE_ADMIN_SENHA = Read-Host 'Senha do administrador' -MaskInput
   npm run demo:popular
   npm run dev
   ```

4. Abra `frontend/.demo/contas.json` para ter à mão os e-mails e senhas da **Camila (cliente)** e da **Lúcia (Padaria da Dona Lúcia)**.
5. Deixe prontos: o site, os slides, o repositório no GitHub e um cliente MySQL conectado ao banco de demonstração.
6. Ensaie no computador que será usado no dia, com o tempo marcado.
7. Lembre-se: **recarregar a página encerra a sessão** (o token fica só em memória). Navegue pelos menus, não pela barra de endereço.

## Roteiro

A apresentação conta uma história: a **Lúcia** tem pão sobrando no fim do dia, e a **Camila** quer comprar mais barato perto de casa. Mostre o sistema local, com Java e MySQL.

| Tempo | Parte | O que mostrar | O que dizer |
|---|---|---|---|
| 0:00 | Problema | Slides de abertura, do problema e dos números | A Lúcia tem uma padaria e termina o dia com produtos bons e sem venda. Falta um canal simples para vender esse excedente. |
| 1:00 | Proposta e ODS 8 | Slides da proposta e do público | A loja publica com desconto, o cliente reserva e retira no balcão. O foco é a renda do pequeno negócio (ODS 8). O público é adulto. |
| 2:00 | Vitrine | Página inicial: seções e faixa de prazo de uma oferta | "Compre até" e "Retire até" são prazos diferentes. A pesquisa do TCC apontou que o prazo precisa ficar claro. |
| 3:00 | Reserva | Entrar como **Camila**, reservar uma oferta e mostrar o código de retirada | O preço e o total finais são calculados pela API, nunca pelo navegador. O código é o que a Camila mostra no balcão. |
| 4:30 | Atendimento | Sair, entrar como **Lúcia**, abrir **Minhas lojas** → Padaria → **Pedidos**. Confirmar → Iniciar preparo → Marcar como pronto → Registrar retirada | A loja avança uma etapa por vez, e cada mudança fica no histórico. |
| 5:45 | Prova da integração | No MySQL: `SELECT id, status FROM pedido ORDER BY id DESC LIMIT 1;` | A ação feita na tela passou pela API em Java e está gravada no banco: o status agora é `CONCLUIDO`. |
| 6:15 | Indicadores | Aba **Indicadores** | A Lúcia vê pedidos retirados, valor e unidades do período. Sem pedidos, a taxa aparece como N/A, não como zero. |
| 7:00 | CRUDs completos | Slide dos CRUDs. Na loja: criar um rascunho de oferta e remover. Na moderação: criar a categoria Teste, renomear, remover e tentar remover Padaria | Categorias, produtos e ofertas: criar, listar, atualizar e remover pela tela. O que já tem pedido não é apagado, e a tela explica o motivo. |
| 9:00 | Mercado | Slide do mercado | Comparei com Too Good To Go, Food To Save, Phenix, Karma e Flashfood e trouxe o que cabia no escopo. |
| 9:45 | Construção e qualidade | Slides de arquitetura, regras e qualidade | React, API em Java e MySQL. A última unidade é vendida uma única vez, e isso tem teste com compras simultâneas. Os testes automatizados estão registrados no repositório. |
| 10:45 | Impacto e limites | Slides do ODS 8, do cenário e dos limites | O cenário de R$ 780 por mês é uma simulação, não uma medição. O sistema mede esses números de verdade quando for usado, e o teste com pessoas é o próximo passo. |
| 11:30 | Fechamento | Slide final | A Lúcia recuperou parte do que perderia, e a Camila economizou. Abrir para perguntas. |

Para a Camila e a Lúcia, entre logo antes de cada parte: a sessão dura 15 minutos.

## Antes de apresentar: saiba explicar estes pontos

Podem pedir para você abrir o código. Leia e explique com as suas palavras:

- **Caminho de um pedido, da tela ao banco:** `frontend/src/api/client.ts`, `backend/.../pedidos/PedidoController.java` e `PedidoService.java`.
- **Última unidade vendida uma vez:** `backend/.../catalogo/EstoqueVendaService.java`.
- **Login e permissões:** `backend/.../auth/AuthController.java` e `backend/.../config/SecurityConfig.java`.
- **Tabelas e relações:** `database/migrations/V1__criar_schema_inicial.sql`.
- **Um CRUD completo:** `backend/.../catalogo/CatalogoGestaoController.java`, `CatalogoGestaoService.java` e `frontend/src/componentes/BotaoRemover.tsx`.

## Plano B

- **A API local caiu:** abra a [demonstração online](https://bee-good-food-recode.vercel.app) ou rode `npm run dev:demo` na pasta `frontend`. Os botões da tela de login entram como cliente, loja ou moderação, sem senha para digitar.
- **Sem internet:** `npm run dev:demo` funciona sem rede depois do `npm install`. Em último caso, use as imagens em `docs/imagens` e o README.
- **Oferta expirada durante a apresentação:** rode `npm run demo:popular` de novo; ele cria novas lojas e ofertas.
- **Sessão expirou (15 minutos):** é o comportamento previsto. Explique e entre de novo.

## Perguntas prováveis

**O que vocês pegaram dos aplicativos que já existem?**
Comparei com Too Good To Go, Food To Save, Phenix, Karma e Flashfood. Entraram a vitrine em seções, a ordenação, o código de retirada, a economia acumulada do cliente, o selo de sacola surpresa, a barra inferior no celular e o tema escuro. Favoritos, avaliações e distância ficaram de fora para não sair do escopo ([análise](../planejamento/analise-de-mercado.md)).

**Onde estão os 3 CRUDs completos?**
Categorias (Moderação > Categorias), produtos e ofertas (Minhas lojas > loja > Produtos e ofertas). Os três criam, listam, atualizam e removem pela tela. O que já tem pedido não é apagado, para não quebrar o histórico: a tela explica e sugere desativar ou encerrar.

**O código de retirada é seguro?**
Não é uma senha. É uma forma legível do número do pedido, para conferir no balcão, e a loja não o valida no sistema. Quem controla o acesso é o login no servidor (DEC-034).

**Sacola surpresa é uma funcionalidade nova?**
É um selo na vitrine, ativado quando o nome do produto contém "surpresa". Nenhuma regra de negócio muda (DEC-035).

**A versão online usa o sistema real?**
Não. Ela usa dados simulados no navegador, para abrir na hora e sem custo (DEC-037). O sistema com Java e MySQL é o que foi apresentado.

**Por que só retirada e não entrega?**
Entrega exigiria endereço do cliente, taxa, área de cobertura e logística. O MVP foca no essencial para o pequeno negócio (DEC-001).

**Como o sistema evita vender o mesmo produto duas vezes?**
A API bloqueia as ofertas do pedido no banco e desconta o estoque com uma atualização condicional. Um teste dispara 5 compras simultâneas da última unidade e só uma é aceita.

**E se o cliente clicar duas vezes em "Confirmar reserva"?**
As duas requisições levam a mesma chave de idempotência. A segunda recebe o mesmo pedido, sem novo desconto de estoque.

**O pagamento é real?**
Não. O sistema só registra a forma escolhida (Pix, dinheiro ou cartão na retirada). Integração financeira exigiria conciliação e ficou fora do MVP (DEC-011).

**Como é garantido que o cliente é maior de idade?**
Por autodeclaração obrigatória no cadastro. Não é verificação de documento, e isso está registrado como decisão (DEC-014).

**De onde vêm os números do problema?**
Do relatório de desperdício do PNUMA (2024), divulgado pela Embrapa, e da Agência Sebrae (2026). As fontes estão no slide e em `docs/planejamento/contexto-de-mercado.md`.

**Vocês mediram impacto no desperdício ou na renda?**
Não. O sistema mede apenas o que registra: pedidos, valores e unidades. Impacto social exige pesquisa com os negócios, prevista como próxima etapa em [Métricas e validação](../planejamento/metricas-e-validacao.md).

**O que foi aproveitado do TCC?**
O fluxo de navegação e os aprendizados da pesquisa (proposta clara, cores, prazo visível). Código e arquitetura foram refeitos.
