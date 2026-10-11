-- Execute depois da migration e da carga demonstrativa, no schema escolhido.
SET time_zone = '+00:00';

-- 1. Catalogo: somente ofertas compraveis no instante da consulta.
SELECT o.id, p.nome AS produto, e.nome AS estabelecimento, c.nome AS categoria,
       a.cidade, a.bairro, o.preco_original, o.preco_oferta, o.estoque_disponivel,
       o.venda_ate, o.retirada_ate, a.fuso_iana
FROM oferta o
JOIN produto p ON p.id = o.produto_id
JOIN categoria c ON c.id = p.categoria_id
JOIN estabelecimento e ON e.id = p.estabelecimento_id
JOIN endereco_estabelecimento a ON a.estabelecimento_id = e.id
WHERE o.status = 'PUBLICADA' AND o.estoque_disponivel > 0
  AND p.ativo = TRUE AND c.ativo = TRUE AND e.ativo = TRUE AND e.status = 'ATIVO'
  AND UTC_TIMESTAMP(6) >= o.inicio_venda AND UTC_TIMESTAMP(6) < o.venda_ate
ORDER BY o.preco_oferta, o.id;

-- 2. Pedido: os dados historicos nao dependem dos nomes/precos atuais do catalogo.
SELECT p.id, p.status, p.estabelecimento_nome_snapshot, p.total,
       i.produto_nome_snapshot, i.quantidade, i.subtotal,
       p.forma_pagamento_declarada, p.retirada_ate
FROM pedido p JOIN item_pedido i ON i.pedido_id = p.id
ORDER BY p.id, i.id;

-- 3. Indicadores demonstrativos: nao comprovam recebimento nem impacto real.
-- A API devera validar que o usuario tem acesso ao estabelecimento selecionado.
SET @estabelecimento_id = 1;
SET @inicio = UTC_TIMESTAMP(6) - INTERVAL 1 DAY;
SET @fim = UTC_TIMESTAMP(6);
SELECT COUNT(*) AS pedidos_concluidos, COALESCE(SUM(total), 0) AS valor_movimentado
FROM pedido
WHERE estabelecimento_id = @estabelecimento_id AND status = 'CONCLUIDO'
  AND concluido_em >= @inicio AND concluido_em < @fim;
SELECT COALESCE(SUM(i.quantidade), 0) AS itens_comercializados,
       COALESCE(SUM((i.preco_original - i.preco_oferta) * i.quantidade), 0) AS economia_estimada
FROM pedido p JOIN item_pedido i ON i.pedido_id = p.id
WHERE p.estabelecimento_id = @estabelecimento_id AND p.status = 'CONCLUIDO'
  AND p.concluido_em >= @inicio AND p.concluido_em < @fim;

-- 4. Auditoria: deve retornar zero linhas na carga de demonstracao.
SELECT p.id, p.total, COALESCE(SUM(i.subtotal), 0) AS soma_itens
FROM pedido p LEFT JOIN item_pedido i ON i.pedido_id = p.id
GROUP BY p.id, p.total
HAVING COUNT(i.id) = 0 OR p.total <> COALESCE(SUM(i.subtotal), 0);

-- 5. Tipos, nulabilidade e chaves: dicionario fisico gerado do banco real.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY, EXTRA
FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, ORDINAL_POSITION;
