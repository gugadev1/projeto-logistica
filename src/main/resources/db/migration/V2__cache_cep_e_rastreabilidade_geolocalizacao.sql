-- Fase 6 – Fase 2: cache de CEP e rastreabilidade de geolocalização

-- Tabela de cache: um registro por CEP normalizado (8 dígitos, sem hífen).
-- TTL é tratado pela aplicação; sem vencimento forçado no banco para simplificar.
CREATE TABLE IF NOT EXISTS cache_cep (
    id           BIGSERIAL    PRIMARY KEY,
    cep          CHAR(8)      NOT NULL UNIQUE,
    coordenada_x NUMERIC(12, 6) NOT NULL,
    coordenada_y NUMERIC(12, 6) NOT NULL,
    logradouro   VARCHAR(200),
    bairro       VARCHAR(100),
    localidade   VARCHAR(100),
    uf           CHAR(2),
    criado_em    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cache_cep_cep ON cache_cep(cep);

-- Adiciona rastreabilidade de CEP e fonte de resolução à tabela simulacao.
ALTER TABLE simulacao
    ADD COLUMN IF NOT EXISTS origem_cep    CHAR(8)      NULL,
    ADD COLUMN IF NOT EXISTS origem_fonte  VARCHAR(30)  NULL;

-- Adiciona rastreabilidade de CEP e fonte de resolução por pacote.
ALTER TABLE simulacao_pacote
    ADD COLUMN IF NOT EXISTS cep   CHAR(8)     NULL,
    ADD COLUMN IF NOT EXISTS fonte VARCHAR(30) NULL;
