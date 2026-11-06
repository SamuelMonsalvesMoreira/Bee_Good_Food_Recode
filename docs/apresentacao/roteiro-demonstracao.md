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
5. Deixe três abas prontas: o site, o repositório no GitHub e o Swagger (`http://127.0.0.1:8080/swagger-ui/index.html`).
6. Lembre-se: **recarregar a página encerra a sessão** (o token fica só em memória). Navegue pelos menus, não pela barra de endereço.

## Roteiro

| Tempo | Parte | O que mostrar | O que dizer |
|---|---|---|---|
| 0:00 | Problema | Slide do problema | Pequenos negócios de comida terminam o dia com produtos bons e sem venda. Falta um canal simples para vender esse excedente. |
| 1:00 | Proposta e ODS 8 | Página inicial | A abertura explica a proposta antes da primeira ação, um aprendizado da pesquisa do TCC. O foco é a renda do pequeno negócio (ODS 8). |
| 2:00 | Busca | Digitar "santos" na cidade, sem acento; clicar em **Padaria** nos filtros rápidos | A busca ignora maiúsculas e acentos. Só aparecem ofertas compráveis agora: publicadas, dentro do prazo e com estoque. |
| 3:00 | Prazos | Apontar a faixa de prazo de uma oferta | "Compre até" e "Retire até" são prazos diferentes e aparecem no fuso da loja. A pesquisa do TCC apontou que o prazo de cada produto precisa ficar claro. |
| 3:30 | Carrinho | Adicionar duas unidades; tentar adicionar oferta de **outra loja** | Um pedido é retirado numa única loja. O site pergunta antes de trocar e nunca apaga itens sozinho. |
| 4:30 | Reserva | Entrar como **Camila**, escolher Pix e confirmar | O total mostrado é uma estimativa: preço e total finais são calculados pela API. A reserva usa uma chave de idempotência, e repetir o envio não cria pedido duplicado. |
| 5:30 | Pedido | Página do pedido: etapas, prazo de retirada e endereço | A página se atualiza sozinha a cada 30 segundos enquanto o pedido está em andamento. |
| 6:00 | Loja | Sair, entrar como **Lúcia**, abrir **Minhas lojas** → Padaria → **Pedidos** | A loja vê a fila e avança o pedido uma etapa por vez. |
| 7:00 | Atendimento | Confirmar → Iniciar preparo → Marcar como pronto → Registrar retirada | Cada mudança fica registrada no histórico. Não dá para pular etapas. |
| 8:00 | Indicadores | Aba **Indicadores** | Pedidos retirados, valor, unidades e taxas por período. Sem pedidos, a taxa aparece como N/A, não como zero. |
| 9:00 | Oferta nova | Aba **Produtos e ofertas** → um produto → criar oferta com preço maior que o original | O erro aparece no campo. Corrigir e publicar. Depois de publicada, preço e prazo não mudam. |
| 10:00 | Qualidade | GitHub: README e pasta `backend/evidencias` | 190 testes automatizados, incluindo concorrência na última unidade e auditoria de acessibilidade sem violações. As decisões estão documentadas (DEC-001 a DEC-031). |
| 11:00 | Limites e próximos passos | Slide final | O que o MVP não faz (entrega, pagamento online) e o que falta validar com pessoas. |

Se sobrar tempo, mostre a **moderação**: entre como administrador, aprove a "Doceria Vila Mathias" e mostre que o motivo é obrigatório.

## Plano B

- **Sem internet ou a API caiu:** use as imagens em `docs/imagens` e o README como apresentação.
- **Oferta expirada durante a apresentação:** rode `npm run demo:popular` de novo; ele cria novas lojas e ofertas.
- **Sessão expirou (15 minutos):** é o comportamento previsto. Explique e entre de novo.

## Perguntas prováveis

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

**Vocês mediram impacto no desperdício ou na renda?**
Não. O sistema mede apenas o que registra: pedidos, valores e unidades. Impacto social exige pesquisa com os negócios, prevista como próxima etapa em [Métricas e validação](../planejamento/metricas-e-validacao.md).

**O que foi aproveitado do TCC?**
O fluxo de navegação e os aprendizados da pesquisa (proposta clara, cores, prazo visível). Código e arquitetura foram refeitos.
