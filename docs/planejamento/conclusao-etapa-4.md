# Conclusão da Etapa 4 — Autenticação e perfis na API

Data: 21/10/2026 (America/Sao_Paulo). Estado: **concluída localmente no backend**. O frontend e o fluxo completo de estabelecimentos/pedidos ainda não estão implementados. Os instantes das evidências são gravados em UTC.

## O que foi entregue

- Cadastro de cliente adulto, validação dos campos e instante de autodeclaração de maioridade.
- E-mail normalizado e unicidade protegida pelo MySQL, incluindo requisições concorrentes.
- Senhas com BCrypt, custo 12, sem truncamento de entradas maiores que 72 bytes.
- Login com JWT HS256 de 15 minutos e segredo externo obrigatório, sem chave padrão.
- Consulta da própria conta com token; consulta paginada de usuários restrita ao administrador.
- Papéis sempre consultados no banco. O cadastro público não aceita atributos privilegiados; status inativo ou privilégio removido passa a bloquear tokens já emitidos.
- Respostas JSON 400/401/403/409/415/429, sem senha, hash ou token em respostas de erro.
- Limite local de tentativas com memória limitada; 20 chamadas/minuto/IP, compartilhado por cadastro e login.
- Documentação Swagger atualizada com autorização Bearer e guia de estudo.

Não foram alteradas as migrations V1/V2: as tabelas `usuario`, `papel` e `usuario_papel` já continham a estrutura necessária. Não foi criado administrador nem senha padrão.

## Validação executada

**52 testes HTTP/MySQL + 6 testes unitários, sem falhas ou testes ignorados.** A suíte HTTP inclui os cenários de categorias e infraestrutura da etapa anterior, ajustados para a política de autenticação. Não são 52 testes adicionais aos 17 anteriores.

Verificados, entre outros:

- Registro do hash, papel CLIENTE e confirmação de maioridade; nenhum usuário criado para entrada inválida.
- Rejeição de menoridade, declaração ausente/nula, senha curta, excesso de bytes e inclusão de ID, papel, hash ou status no cadastro.
- Dois cadastros simultâneos do mesmo e-mail, com caixa/espaços diferentes: um 201 e um 409, com uma única conta e atribuição de papel.
- Login e leitura do perfil; erros equivalentes para senha incorreta, usuário desconhecido e conta inativa.
- Tokens expirados, futuros, adulterados, malformados, de audiência/emissor incorretos, sem claims obrigatórias e com duração excessiva rejeitados.
- Cliente/responsável não acessam a lista administrativa. Administrador acessa, e a remoção do papel bloqueia o mesmo token. Claims de papéis não substituem os papéis do banco.
- Ausência de cookie de sessão, respostas não armazenáveis em cache e indisponibilidade de HTTP Basic/token pela URL.
- Limite de tentativas, separação por IP, expiração da janela e recusa de chaves curtas/fora do formato.

Além da suíte, o **JAR final** foi iniciado duas vezes no mesmo banco. Foram verificados health UP, categorias persistidas, cadastro CLIENTE, login, sessão preservada com a mesma chave, e 429 com `Retry-After` na 21ª tentativa. Todos os processos iniciados pelo teste foram encerrados ao final.

## Evidências

- [Relatório da execução](../../backend/evidencias/ultima-validacao.json)
- [Contrato OpenAPI gerado](../../backend/evidencias/openapi.json)
- [Testes HTTP/MySQL](../../backend/src/test/java/br/com/beegoodfood/ApiMySqlIT.java)
- [Teste do limite de tentativas](../../backend/src/test/java/br/com/beegoodfood/auth/AuthRateLimitFilterTest.java)
- [Teste da configuração da chave](../../backend/src/test/java/br/com/beegoodfood/config/SecurityConfigTest.java)
- [Guia para executar e estudar](../../backend/README.md)
- [Decisões de autenticação e exemplos](autenticacao.md)

Esses resultados são locais. Não comprovam segurança de uma implantação pública, desempenho, isolamento entre estabelecimentos ainda não implementados ou funcionamento das futuras telas.

## Próxima etapa — checklist de implementação

### Etapa 5: cadastros e pedidos

- [ ] 5.1 — Solicitação de estabelecimento com endereço/fuso; consulta apenas pelo proprietário ou administrador.
- [ ] 5.2 — Aprovação/rejeição/suspensão com histórico, motivo e concessão controlada do papel RESPONSAVEL.
- [ ] 5.3 — Administração de categorias, produtos e ofertas; regras de preço, disponibilidade e estados.
- [ ] 5.4 — Busca paginada de ofertas por texto, categoria, cidade e bairro.
- [ ] 5.5 — Pedido transacional de um estabelecimento, total no servidor, snapshots e chave de idempotência.
- [ ] 5.6 — Status/cancelamento, devolução única de estoque e testes HTTP concorrentes da última unidade.
- [ ] 5.7 — Indicadores conforme as fórmulas já definidas no plano.

### Etapa 6: interface React

- [ ] Integrar início, explorar ofertas, detalhe, cadastro/login, carrinho e pedidos à API.
- [ ] Implementar áreas de cliente/responsável/administrador e estados de erro/carregamento/vazio.
- [ ] Guardar token apenas em memória, limpar sessão no logout/401 e configurar CORS para origens explícitas.
- [ ] Conferir responsividade, teclado, rótulos e mascote Bee.

### Etapa 7: entrega do TCC

- [ ] Testes ponta a ponta, documentação final, demonstração e apresentação vinculadas ao ODS 8.
- [ ] Revisão de segredos, ambiente e pré-requisitos de deploy antes de disponibilizar publicamente.

O próximo bloco de implementação é **5.1**, seguindo as decisões de escopo existentes, sem acrescentar delivery ou pagamento real.
