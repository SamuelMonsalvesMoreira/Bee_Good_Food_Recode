# Conclusão da Etapa 6 — Frontend React

Data: 05/11/2026 (America/Sao_Paulo). Estado: **concluída localmente**. A interface React está integrada à API da Etapa 5. A Etapa 7 (validação com pessoas, documentação final e apresentação) é a próxima.

## O que foi entregue

| Área | Telas e comportamentos |
|---|---|
| Pública | Início que explica a proposta antes da primeira ação; busca por texto, cidade, bairro e categoria (filtros na URL); detalhe da oferta; ofertas de uma loja. |
| Conta | Cadastro adulto com autodeclaração; login; sessão de 15 minutos só em memória; aviso quando a sessão termina; logout que limpa token e carrinho. |
| Cliente | Carrinho de uma loja com confirmação antes de trocar (CA07); reserva com forma de pagamento declarada e `Idempotency-Key`; histórico; detalhe do pedido com etapas, prazo de retirada, histórico e cancelamento enquanto pendente. |
| Responsável | Solicitação e edição da loja (com aviso de nova análise ao mudar endereço ou fuso); fila de pedidos com filtro e avanço de status; cancelamento com motivo; produtos; ofertas com prazos digitados no fuso da loja; publicar, pausar, retomar, encerrar e ajustar estoque; indicadores por período. |
| Administrador | Fila de moderação por situação; aprovar, pedir ajustes, suspender e reativar com motivo obrigatório; administração de categorias. |
| Backend | CORS com origens explícitas (`CORS_ORIGINS`), sem credenciais de navegador. |

### Identidade visual

O protótipo do TCC usava vermelho e parecia um aplicativo de delivery genérico, e a pesquisa original pediu para rever cores e organização visual. O Recode passou a usar:

- **Cores:** mel (`#F2B705`) para as ações principais, própolis (`#2B1D0E`) para o texto e folha (`#2F6B3A`) para retirada e sucesso.
- **Tipografia:** Bricolage Grotesque nos títulos e Atkinson Hyperlegible no texto, uma fonte desenhada para leitura fácil.
- **Mascote Bee:** abelha desenhada em SVG; aparece na marca, nos estados vazios e como ícone da aba.
- **Faixa de prazo:** elemento-assinatura em toda oferta, que mostra "Compre até" e "Retire até" separados, no fuso da loja. Atende ao aprendizado do TCC de que o prazo precisa ficar claro.

### Critérios de interface atendidos

- Estados de carregamento, vazio, sucesso e erro nos fluxos principais.
- Erros de validação exibidos no campo, com a mensagem da API.
- Fim da venda e prazo de retirada exibidos separadamente, no fuso da loja.
- Troca de loja no carrinho nunca remove itens sem confirmação.
- Situação do pedido em texto (etiquetas e etapas), não só por cor.
- Navegação por teclado com foco visível, link "Pular para o conteúdo", diálogos nativos (`<dialog>`) e anúncio de mudança de situação para leitores de tela.

As decisões novas estão em [DEC-028 a DEC-030](decisoes-do-mvp.md): carrinho na aba, chave de idempotência por tentativa e acompanhamento por atualização periódica.

## Validação executada

- **Backend:** 158 testes HTTP/MySQL (incluindo 3 novos de CORS) e 6 unitários, sem falhas.
- **Frontend:** typecheck, 14 testes (regras do carrinho, fusos e troca de loja na interface) e build de produção sem erros.
- **No navegador**, com a API e dados de demonstração: busca, detalhe, carrinho, login, reserva, ciclo completo do pedido pela loja, indicadores, erros de validação e de conflito, moderação e largura de celular. Os passos e as correções feitas durante essa verificação estão no relatório.

[Relatório da validação da Etapa 6](../../backend/evidencias/validacao-etapa-6.json)

Esses resultados são locais e foram verificados pelo desenvolvimento. Não substituem os testes com consumidores e responsáveis previstos em [Métricas e validação](metricas-e-validacao.md), nem uma auditoria de acessibilidade.

## Próxima etapa — checklist da Etapa 7

- [ ] Executar o runner oficial com PowerShell 7 e registrar `ultima-validacao.json`.
- [ ] Rodadas de validação: pelo menos 5 consumidores adultos e 3 responsáveis (H01 a H05), registrando tarefas, dúvidas e erros sem dados identificáveis.
- [ ] Auditoria de acessibilidade (axe ou Lighthouse) e teste ponta a ponta automatizado dos fluxos principais.
- [ ] Roteiro de demonstração com dados fictícios e apresentação vinculada ao ODS 8.
- [ ] Pré-requisitos de deploy: HTTPS, `CORS_ORIGINS` da origem pública, segredo JWT persistente, proxy reverso (`AuthRateLimitFilter` usa o IP da conexão) e revisão de segredos.
