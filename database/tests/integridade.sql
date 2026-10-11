-- Testes em banco descartavel carregado com seeds/demonstracao.sql.
-- Cada alteracao de dados e desfeita; os procedimentos de teste sao removidos no fim.
SET time_zone = '+00:00';
DELIMITER $$
CREATE PROCEDURE teste_assert(IN passou BOOLEAN, IN descricao VARCHAR(200))
BEGIN
    IF passou IS NULL OR NOT passou THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = descricao;
    END IF;
    SELECT CONCAT('PASS | ', descricao) AS resultado;
END$$

CREATE PROCEDURE teste_rejeita(IN comando TEXT, IN codigo_esperado INT, IN descricao VARCHAR(200))
BEGIN
    DECLARE codigo INT DEFAULT 0;
    START TRANSACTION;
    SET @teste_sql = comando;
    PREPARE consulta_teste FROM @teste_sql;
    BEGIN
        DECLARE CONTINUE HANDLER FOR SQLEXCEPTION
            GET DIAGNOSTICS CONDITION 1 codigo = MYSQL_ERRNO;
        EXECUTE consulta_teste;
    END;
    DEALLOCATE PREPARE consulta_teste;
    ROLLBACK;
    CALL teste_assert(codigo = codigo_esperado, descricao);
END$$
DELIMITER ;

CALL teste_assert((SELECT COUNT(*) = 12 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'), '12 tabelas criadas');
CALL teste_assert((SELECT COUNT(*) = 12 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND ENGINE = 'InnoDB'), '12 tabelas transacionais InnoDB');
CALL teste_assert((SELECT COUNT(*) = 3 FROM usuario), 'Carga de usuarios ficticios');
CALL teste_assert((SELECT COUNT(*) = 3 FROM papel), 'Tres papeis disponiveis');
CALL teste_assert((SELECT COUNT(*) = 2 FROM usuario_papel WHERE usuario_id = 2), 'Papeis cumulativos do responsavel');
CALL teste_assert((SELECT COUNT(*) = 0 FROM usuario WHERE ativo OR email NOT LIKE '%@example.invalid'), 'Contas de exemplo inativas e sem e-mails reais');
CALL teste_assert((SELECT COUNT(*) = 1 FROM pedido p JOIN item_pedido i ON i.pedido_id = p.id WHERE p.total = i.subtotal AND p.total = 33.80), 'Pedido de exemplo com total correto');
CALL teste_assert((SELECT SUM((preco_original-preco_oferta)*quantidade) = 22.20 FROM item_pedido), 'Economia nominal do exemplo e 22.20');
CALL teste_assert((SELECT COUNT(*) = 1 FROM oferta o JOIN produto p ON p.id=o.produto_id JOIN categoria c ON c.id=p.categoria_id JOIN estabelecimento e ON e.id=p.estabelecimento_id WHERE o.status='PUBLICADA' AND o.estoque_disponivel>0 AND p.ativo AND c.ativo AND e.ativo AND e.status='ATIVO' AND UTC_TIMESTAMP(6)>=o.inicio_venda AND UTC_TIMESTAMP(6)<o.venda_ate), 'Catalogo exclui rascunho e oferta encerrada');

CALL teste_rejeita('UPDATE usuario SET email_normalizado = ''CLIENTE@EXAMPLE.INVALID'' WHERE id = 2', 1062, 'E-mail duplicado ignorando maiusculas bloqueado');
CALL teste_rejeita('UPDATE usuario SET maioridade_confirmada_em = NULL WHERE id = 1', 1048, 'Confirmacao de maioridade obrigatoria');
CALL teste_rejeita('INSERT INTO usuario_papel (usuario_id,papel_codigo) VALUES (1,''CLIENTE'')', 1062, 'Papel duplicado bloqueado');
CALL teste_rejeita('INSERT INTO usuario_papel (usuario_id,papel_codigo) VALUES (999,''CLIENTE'')', 1452, 'Papel sem usuario bloqueado');
CALL teste_rejeita('UPDATE estabelecimento SET responsavel_id=999 WHERE id=1', 1452, 'Responsavel inexistente bloqueado');
CALL teste_rejeita('UPDATE estabelecimento SET status=''INVALIDO'' WHERE id=1', 3819, 'Status de estabelecimento invalido bloqueado');
CALL teste_rejeita('UPDATE endereco_estabelecimento SET estabelecimento_id=1 WHERE estabelecimento_id=2', 1062, 'Segundo endereco do mesmo estabelecimento bloqueado');
CALL teste_rejeita('UPDATE categoria SET slug=''refeicoes'' WHERE id=2', 1062, 'Slug duplicado bloqueado');
CALL teste_rejeita('UPDATE produto SET categoria_id=999 WHERE id=1', 1452, 'Produto sem categoria valida bloqueado');
CALL teste_rejeita('UPDATE produto SET estabelecimento_id=999 WHERE id=1', 1452, 'Produto sem estabelecimento valido bloqueado');
CALL teste_rejeita('UPDATE oferta SET produto_id=999 WHERE id=1', 1452, 'Oferta sem produto valido bloqueada');
CALL teste_rejeita('UPDATE oferta SET preco_oferta=0 WHERE id=1', 3819, 'Preco zero bloqueado');
CALL teste_rejeita('UPDATE oferta SET preco_oferta=29 WHERE id=1', 3819, 'Oferta acima do preco original bloqueada');
CALL teste_rejeita('UPDATE oferta SET estoque_disponivel=-1 WHERE id=1', 1264, 'Estoque negativo bloqueado em modo estrito');
CALL teste_rejeita('UPDATE oferta SET venda_ate=inicio_venda WHERE id=1', 3819, 'Janela de venda vazia bloqueada');
CALL teste_rejeita('UPDATE oferta SET retirada_ate=venda_ate - INTERVAL 1 SECOND WHERE id=1', 3819, 'Retirada anterior ao fim da venda bloqueada');
CALL teste_rejeita('UPDATE oferta SET status=''PAUSADA'' WHERE id=4', 1062, 'Duas ofertas ativas do mesmo produto bloqueadas');
CALL teste_rejeita('UPDATE pedido SET cliente_id=999 WHERE id=1', 1452, 'Pedido sem cliente valido bloqueado');
CALL teste_rejeita('UPDATE pedido SET total=-1 WHERE id=1', 3819, 'Total negativo bloqueado');
CALL teste_rejeita('UPDATE pedido SET forma_pagamento_declarada=''INVALIDO'' WHERE id=1', 3819, 'Forma declarada invalida bloqueada');
CALL teste_rejeita('UPDATE pedido SET status=''PAGO'' WHERE id=1', 3819, 'Status de pedido inexistente bloqueado');
CALL teste_rejeita('UPDATE item_pedido SET quantidade=0, subtotal=0 WHERE pedido_id=1', 3819, 'Item com quantidade zero bloqueado');
CALL teste_rejeita('UPDATE item_pedido SET subtotal=1 WHERE pedido_id=1', 3819, 'Subtotal incorreto bloqueado');
CALL teste_rejeita('UPDATE item_pedido SET oferta_id=999 WHERE pedido_id=1', 1452, 'Item sem oferta valida bloqueado');
CALL teste_rejeita('UPDATE historico_status_pedido SET status_novo=''INVALIDO'' WHERE id=1', 3819, 'Novo status historico invalido bloqueado');
CALL teste_rejeita('DELETE FROM usuario WHERE id=1', 1451, 'Exclusao de cliente com historico bloqueada');
CALL teste_rejeita('DELETE FROM oferta WHERE id=1', 1451, 'Exclusao de oferta com pedido bloqueada');

-- Duplicacao de pedido: testa a unicidade no banco, nao o tratamento HTTP futuro.
SET @duplicar_pedido = 'INSERT INTO pedido (cliente_id, estabelecimento_id, status, forma_pagamento_declarada, idempotency_key, payload_hash, total, estabelecimento_nome_snapshot, logradouro_snapshot, numero_snapshot, bairro_snapshot, cidade_snapshot, estado_snapshot, cep_snapshot, fuso_iana_snapshot, retirada_ate) SELECT cliente_id, estabelecimento_id, ''PENDENTE'', forma_pagamento_declarada, idempotency_key, payload_hash, total, estabelecimento_nome_snapshot, logradouro_snapshot, numero_snapshot, bairro_snapshot, cidade_snapshot, estado_snapshot, cep_snapshot, fuso_iana_snapshot, retirada_ate FROM pedido WHERE id=1';
CALL teste_rejeita(@duplicar_pedido, 1062, 'Chave de idempotencia duplicada por cliente bloqueada');
START TRANSACTION;
SET @outro_cliente = REPLACE(@duplicar_pedido, 'SELECT cliente_id,', 'SELECT 2,');
PREPARE copiar FROM @outro_cliente;
EXECUTE copiar;
DEALLOCATE PREPARE copiar;
CALL teste_assert((SELECT COUNT(*)=2 FROM pedido WHERE idempotency_key='demo-pedido-001'), 'Chave igual permitida para clientes diferentes');
ROLLBACK;

START TRANSACTION;
UPDATE produto SET nome='Nome alterado' WHERE id=1;
UPDATE estabelecimento SET nome='Nome alterado' WHERE id=1;
UPDATE oferta SET preco_oferta=20 WHERE id=1;
CALL teste_assert((SELECT produto_nome_snapshot='Marmita Demo' AND preco_oferta=16.90 FROM item_pedido WHERE pedido_id=1), 'Snapshot do item preservado ao alterar catalogo');
CALL teste_assert((SELECT estabelecimento_nome_snapshot='Cozinha Demo' FROM pedido WHERE id=1), 'Snapshot do estabelecimento preservado');
ROLLBACK;
START TRANSACTION;
UPDATE oferta SET estoque_disponivel=estoque_disponivel-1 WHERE id=1;
ROLLBACK;
CALL teste_assert((SELECT estoque_disponivel=8 FROM oferta WHERE id=1), 'Rollback restaura estoque');

DROP PROCEDURE teste_rejeita;
DROP PROCEDURE teste_assert;
