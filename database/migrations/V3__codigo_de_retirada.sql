-- Codigo que o cliente mostra no balcao e a loja informa para registrar a retirada (DEC-040).
-- Nulo somente em linhas inseridas fora da API (seeds e testes SQL); a API sempre grava um codigo.
ALTER TABLE pedido ADD COLUMN codigo_retirada VARCHAR(8) NULL AFTER payload_hash;

-- Pedidos ja existentes recebem um codigo; UUID() e avaliado a cada linha.
UPDATE pedido SET codigo_retirada = UPPER(SUBSTRING(REPLACE(UUID(), '-', ''), 1, 6)) WHERE codigo_retirada IS NULL;
