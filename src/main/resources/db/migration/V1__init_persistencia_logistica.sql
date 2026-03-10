CREATE TABLE IF NOT EXISTS veiculo (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(20) NOT NULL UNIQUE,
    carga_max NUMERIC(12, 3) NOT NULL,
    volume_max NUMERIC(12, 3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS operacao_execucao (
    id BIGSERIAL PRIMARY KEY,
    canal VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    request_json TEXT,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS simulacao (
    id BIGSERIAL PRIMARY KEY,
    execucao_id BIGINT NOT NULL UNIQUE REFERENCES operacao_execucao(id),
    veiculo_id BIGINT NOT NULL REFERENCES veiculo(id),
    origem_x NUMERIC(12, 3) NOT NULL,
    origem_y NUMERIC(12, 3) NOT NULL,
    carga_inicial NUMERIC(12, 3) NOT NULL,
    carga_final NUMERIC(12, 3) NOT NULL,
    volume_final NUMERIC(12, 3) NOT NULL,
    ocupacao_percentual NUMERIC(7, 3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS simulacao_pacote (
    id BIGSERIAL PRIMARY KEY,
    simulacao_id BIGINT NOT NULL REFERENCES simulacao(id),
    pacote_codigo VARCHAR(60) NOT NULL,
    peso NUMERIC(12, 3) NOT NULL,
    volume NUMERIC(12, 3) NOT NULL,
    coordenada_x NUMERIC(12, 3) NOT NULL,
    coordenada_y NUMERIC(12, 3) NOT NULL,
    prioridade INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    erro_validacao TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS romaneio_item (
    id BIGSERIAL PRIMARY KEY,
    simulacao_id BIGINT NOT NULL REFERENCES simulacao(id),
    pacote_codigo VARCHAR(60) NOT NULL,
    ordem_saida INTEGER NOT NULL,
    prioridade_rotulo VARCHAR(20) NOT NULL,
    distancia NUMERIC(12, 3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_execucao_status ON operacao_execucao(status);
CREATE INDEX IF NOT EXISTS idx_execucao_started_at ON operacao_execucao(started_at);
CREATE INDEX IF NOT EXISTS idx_simulacao_veiculo ON simulacao(veiculo_id);
CREATE INDEX IF NOT EXISTS idx_pacote_simulacao ON simulacao_pacote(simulacao_id);
CREATE INDEX IF NOT EXISTS idx_romaneio_simulacao ON romaneio_item(simulacao_id);
