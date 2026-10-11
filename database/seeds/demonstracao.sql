-- Somente para um schema vazio de demonstracao. Nunca executar em producao.
-- Pessoas, enderecos e estabelecimentos ficticios; contas inativas, sem senha utilizavel.
-- Horarios em UTC. Precos e quantidades sao exemplos, nao resultados de impacto real.
SET time_zone = '+00:00';
SET @agora = UTC_TIMESTAMP(6);
START TRANSACTION;

INSERT INTO usuario (id, nome, email, email_normalizado, senha_hash, maioridade_confirmada_em, ativo) VALUES
(1, 'Cliente Demonstracao', 'cliente@example.invalid', 'cliente@example.invalid', '!CONTA_DEMO_SEM_LOGIN', @agora, FALSE),
(2, 'Responsavel Demonstracao', 'responsavel@example.invalid', 'responsavel@example.invalid', '!CONTA_DEMO_SEM_LOGIN', @agora, FALSE),
(3, 'Administrador Demonstracao', 'admin@example.invalid', 'admin@example.invalid', '!CONTA_DEMO_SEM_LOGIN', @agora, FALSE);
INSERT INTO usuario_papel (usuario_id, papel_codigo) VALUES
(1, 'CLIENTE'), (2, 'CLIENTE'), (2, 'RESPONSAVEL'), (3, 'ADMINISTRADOR');

INSERT INTO estabelecimento (id, responsavel_id, nome, descricao, status) VALUES
(1, 2, 'Cozinha Demo', 'Negocio ficticio para testes de retirada.', 'ATIVO'),
(2, 2, 'Padaria Demo', 'Negocio ficticio aguardando moderacao.', 'PENDENTE_ANALISE');
INSERT INTO endereco_estabelecimento (estabelecimento_id, logradouro, numero, bairro, cidade, estado, cep) VALUES
(1, 'Rua de Demonstracao', '100', 'Centro Demo', 'Cidade Demo', 'SP', '00000-000'),
(2, 'Rua de Demonstracao', '200', 'Bairro Demo', 'Cidade Demo', 'SP', '00000-000');
INSERT INTO historico_status_estabelecimento (estabelecimento_id, ator_id, status_anterior, status_novo, motivo, criado_em) VALUES
(1, 2, NULL, 'PENDENTE_ANALISE', 'Solicitacao ficticia', @agora - INTERVAL 2 DAY),
(1, 3, 'PENDENTE_ANALISE', 'ATIVO', 'Aprovacao de demonstracao', @agora - INTERVAL 1 DAY),
(2, 2, NULL, 'PENDENTE_ANALISE', 'Solicitacao ficticia', @agora);

INSERT INTO categoria (id, nome, nome_normalizado, slug) VALUES
(1, 'Refeicoes', 'refeicoes', 'refeicoes'), (2, 'Padaria', 'padaria', 'padaria');
INSERT INTO produto (id, estabelecimento_id, categoria_id, nome, descricao) VALUES
(1, 1, 1, 'Marmita Demo', 'Item ficticio, uma unidade por embalagem.'),
(2, 1, 1, 'Prato Demo', 'Item ficticio para teste de prazo vencido.'),
(3, 2, 2, 'Paes Demo', 'Item ficticio de estabelecimento pendente.');
INSERT INTO oferta (id, produto_id, preco_original, preco_oferta, estoque_disponivel,
    inicio_venda, venda_ate, retirada_ate, status, publicada_em, encerrada_em) VALUES
(1, 1, 28.00, 16.90, 8, @agora - INTERVAL 1 HOUR, @agora + INTERVAL 4 HOUR, @agora + INTERVAL 5 HOUR, 'PUBLICADA', @agora - INTERVAL 1 HOUR, NULL),
(2, 2, 30.00, 18.00, 3, @agora - INTERVAL 2 DAY, @agora - INTERVAL 1 DAY, @agora - INTERVAL 23 HOUR, 'ENCERRADA', @agora - INTERVAL 2 DAY, @agora - INTERVAL 1 DAY),
(3, 3, 20.00, 12.00, 5, @agora, @agora + INTERVAL 4 HOUR, @agora + INTERVAL 5 HOUR, 'RASCUNHO', NULL, NULL),
(4, 1, 28.00, 17.00, 2, @agora - INTERVAL 3 DAY, @agora - INTERVAL 2 DAY, @agora - INTERVAL 47 HOUR, 'ENCERRADA', @agora - INTERVAL 3 DAY, @agora - INTERVAL 2 DAY);

-- Exemplo historico: 10 unidades iniciais, 2 vendidas, saldo atual 8 na oferta 1.
INSERT INTO pedido (id, cliente_id, estabelecimento_id, status, forma_pagamento_declarada,
    idempotency_key, payload_hash, total, estabelecimento_nome_snapshot, logradouro_snapshot,
    numero_snapshot, bairro_snapshot, cidade_snapshot, estado_snapshot, cep_snapshot,
    fuso_iana_snapshot, retirada_ate, concluido_em, criado_em) VALUES
(1, 1, 1, 'CONCLUIDO', 'PIX', 'demo-pedido-001', SHA2('demo:cliente=1;oferta=1;qtd=2', 256),
    33.80, 'Cozinha Demo', 'Rua de Demonstracao', '100', 'Centro Demo', 'Cidade Demo', 'SP',
    '00000-000', 'America/Sao_Paulo', @agora + INTERVAL 5 HOUR, @agora - INTERVAL 5 MINUTE, @agora - INTERVAL 50 MINUTE);
INSERT INTO item_pedido (pedido_id, oferta_id, produto_nome_snapshot, preco_original, preco_oferta, quantidade, subtotal)
VALUES (1, 1, 'Marmita Demo', 28.00, 16.90, 2, 33.80);
INSERT INTO historico_status_pedido (pedido_id, ator_id, status_anterior, status_novo, motivo, criado_em) VALUES
(1, 1, NULL, 'PENDENTE', 'Pedido ficticio', @agora - INTERVAL 50 MINUTE),
(1, 2, 'PENDENTE', 'CONFIRMADO', NULL, @agora - INTERVAL 40 MINUTE),
(1, 2, 'CONFIRMADO', 'EM_PREPARO', NULL, @agora - INTERVAL 30 MINUTE),
(1, 2, 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', NULL, @agora - INTERVAL 15 MINUTE),
(1, 2, 'PRONTO_PARA_RETIRADA', 'CONCLUIDO', 'Retirada ficticia, nao comprova pagamento', @agora - INTERVAL 5 MINUTE);
COMMIT;
