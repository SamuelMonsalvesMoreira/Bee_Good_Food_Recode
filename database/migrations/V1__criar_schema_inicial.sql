-- Bee Good Food Recode — Etapa 2
-- MySQL 8.0+ / Flyway
-- Pagamento é apenas declarado no pedido; não há captura financeira.

CREATE TABLE usuario (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_normalizado VARCHAR(320) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    maioridade_confirmada_em DATETIME(6) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email_normalizado UNIQUE (email_normalizado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE papel (
    codigo VARCHAR(30) NOT NULL,
    descricao VARCHAR(120) NOT NULL,
    PRIMARY KEY (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE usuario_papel (
    usuario_id BIGINT UNSIGNED NOT NULL,
    papel_codigo VARCHAR(30) NOT NULL,
    atribuido_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (usuario_id, papel_codigo),
    CONSTRAINT fk_usuario_papel_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_papel_papel FOREIGN KEY (papel_codigo) REFERENCES papel (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE estabelecimento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    responsavel_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(160) NOT NULL,
    descricao VARCHAR(500) NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDENTE_ANALISE',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_estabelecimento_responsavel (responsavel_id),
    KEY ix_estabelecimento_status (status),
    CONSTRAINT fk_estabelecimento_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuario (id),
    CONSTRAINT ck_estabelecimento_status CHECK (status IN ('PENDENTE_ANALISE', 'ATIVO', 'REJEITADO', 'SUSPENSO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE endereco_estabelecimento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    estabelecimento_id BIGINT UNSIGNED NOT NULL,
    logradouro VARCHAR(180) NOT NULL,
    numero VARCHAR(30) NOT NULL,
    complemento VARCHAR(120) NULL,
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado CHAR(2) NOT NULL,
    cep VARCHAR(12) NOT NULL,
    fuso_iana VARCHAR(64) NOT NULL DEFAULT 'America/Sao_Paulo',
    PRIMARY KEY (id),
    CONSTRAINT uk_endereco_estabelecimento UNIQUE (estabelecimento_id),
    CONSTRAINT fk_endereco_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE historico_status_estabelecimento (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    estabelecimento_id BIGINT UNSIGNED NOT NULL,
    ator_id BIGINT UNSIGNED NULL,
    status_anterior VARCHAR(24) NULL,
    status_novo VARCHAR(24) NOT NULL,
    motivo VARCHAR(500) NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_hist_estabelecimento_estabelecimento (estabelecimento_id, criado_em),
    CONSTRAINT fk_hist_estabelecimento_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento (id),
    CONSTRAINT fk_hist_estabelecimento_ator FOREIGN KEY (ator_id) REFERENCES usuario (id),
    CONSTRAINT ck_hist_estabelecimento_status_novo CHECK (status_novo IN ('PENDENTE_ANALISE', 'ATIVO', 'REJEITADO', 'SUSPENSO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE categoria (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nome VARCHAR(100) NOT NULL,
    nome_normalizado VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_categoria_nome_normalizado UNIQUE (nome_normalizado),
    CONSTRAINT uk_categoria_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE produto (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    estabelecimento_id BIGINT UNSIGNED NOT NULL,
    categoria_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(160) NOT NULL,
    descricao VARCHAR(500) NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_produto_estabelecimento_ativo (estabelecimento_id, ativo),
    KEY ix_produto_categoria_ativo (categoria_id, ativo),
    CONSTRAINT fk_produto_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento (id),
    CONSTRAINT fk_produto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE oferta (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    produto_id BIGINT UNSIGNED NOT NULL,
    preco_original DECIMAL(10,2) NOT NULL,
    preco_oferta DECIMAL(10,2) NOT NULL,
    estoque_disponivel INT UNSIGNED NOT NULL DEFAULT 0,
    inicio_venda DATETIME(6) NOT NULL,
    venda_ate DATETIME(6) NOT NULL,
    retirada_ate DATETIME(6) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'RASCUNHO',
    publicada_em DATETIME(6) NULL,
    encerrada_em DATETIME(6) NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    produto_oferta_ativa BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN status IN ('PUBLICADA', 'PAUSADA') THEN produto_id ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_oferta_produto_ativa (produto_oferta_ativa),
    KEY ix_oferta_catalogo (status, inicio_venda, venda_ate),
    CONSTRAINT fk_oferta_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT ck_oferta_status CHECK (status IN ('RASCUNHO', 'PUBLICADA', 'PAUSADA', 'ENCERRADA')),
    CONSTRAINT ck_oferta_precos CHECK (preco_original > 0 AND preco_oferta > 0 AND preco_oferta <= preco_original),
    CONSTRAINT ck_oferta_periodos CHECK (inicio_venda < venda_ate AND venda_ate <= retirada_ate)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE pedido (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cliente_id BIGINT UNSIGNED NOT NULL,
    estabelecimento_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDENTE',
    forma_pagamento_declarada VARCHAR(24) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    estabelecimento_nome_snapshot VARCHAR(160) NOT NULL,
    logradouro_snapshot VARCHAR(180) NOT NULL,
    numero_snapshot VARCHAR(30) NOT NULL,
    complemento_snapshot VARCHAR(120) NULL,
    bairro_snapshot VARCHAR(100) NOT NULL,
    cidade_snapshot VARCHAR(100) NOT NULL,
    estado_snapshot CHAR(2) NOT NULL,
    cep_snapshot VARCHAR(12) NOT NULL,
    fuso_iana_snapshot VARCHAR(64) NOT NULL,
    retirada_ate DATETIME(6) NOT NULL,
    concluido_em DATETIME(6) NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    atualizado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_pedido_cliente_idempotencia (cliente_id, idempotency_key),
    KEY ix_pedido_cliente_status (cliente_id, status, criado_em),
    KEY ix_pedido_estabelecimento_status (estabelecimento_id, status, criado_em),
    KEY ix_pedido_concluido_em (concluido_em),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id),
    CONSTRAINT fk_pedido_estabelecimento FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento (id),
    CONSTRAINT ck_pedido_status CHECK (status IN ('PENDENTE', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_pedido_pagamento CHECK (forma_pagamento_declarada IN ('PIX', 'DINHEIRO', 'CARTAO_NA_RETIRADA')),
    CONSTRAINT ck_pedido_total CHECK (total >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE item_pedido (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT UNSIGNED NOT NULL,
    oferta_id BIGINT UNSIGNED NOT NULL,
    produto_nome_snapshot VARCHAR(160) NOT NULL,
    preco_original DECIMAL(10,2) NOT NULL,
    preco_oferta DECIMAL(10,2) NOT NULL,
    quantidade INT UNSIGNED NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_item_pedido_pedido (pedido_id),
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_item_pedido_oferta FOREIGN KEY (oferta_id) REFERENCES oferta (id),
    CONSTRAINT ck_item_pedido_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_item_pedido_precos CHECK (preco_original > 0 AND preco_oferta > 0 AND preco_oferta <= preco_original),
    CONSTRAINT ck_item_pedido_subtotal CHECK (subtotal = preco_oferta * quantidade)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE historico_status_pedido (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    pedido_id BIGINT UNSIGNED NOT NULL,
    ator_id BIGINT UNSIGNED NULL,
    status_anterior VARCHAR(24) NULL,
    status_novo VARCHAR(24) NOT NULL,
    motivo VARCHAR(500) NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_hist_pedido_pedido (pedido_id, criado_em),
    CONSTRAINT fk_hist_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_hist_pedido_ator FOREIGN KEY (ator_id) REFERENCES usuario (id),
    CONSTRAINT ck_hist_pedido_status_novo CHECK (status_novo IN ('PENDENTE', 'CONFIRMADO', 'EM_PREPARO', 'PRONTO_PARA_RETIRADA', 'CONCLUIDO', 'CANCELADO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
