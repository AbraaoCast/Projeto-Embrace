CREATE TABLE IF NOT EXISTS administrador (
    id_administrador INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL CHECK (length(trim(nome)) > 0),
    email TEXT NOT NULL COLLATE NOCASE UNIQUE,
    senha_hash TEXT NOT NULL CHECK (length(trim(senha_hash)) > 0),
    data_cadastro TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS familia (
    id_familia INTEGER PRIMARY KEY AUTOINCREMENT,
    cpf_responsavel TEXT NOT NULL UNIQUE
        CHECK (length(cpf_responsavel) = 11 AND cpf_responsavel NOT GLOB '*[^0-9]*'),
    nome_responsavel TEXT NOT NULL CHECK (length(trim(nome_responsavel)) > 0),
    quantidade_membros INTEGER NOT NULL CHECK (quantidade_membros > 0),
    telefone TEXT NOT NULL CHECK (length(trim(telefone)) > 0),
    logradouro TEXT NOT NULL CHECK (length(trim(logradouro)) > 0),
    numero_residencia TEXT,
    bairro TEXT NOT NULL CHECK (length(trim(bairro)) > 0),
    cidade TEXT NOT NULL CHECK (length(trim(cidade)) > 0),
    data_cadastro TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doacao (
    id_doacao INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL CHECK (length(trim(tipo)) > 0),
    descricao TEXT NOT NULL DEFAULT '',
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    data_doacao TEXT NOT NULL,
    observacao TEXT
);

CREATE TABLE IF NOT EXISTS entrega (
    id_entrega INTEGER PRIMARY KEY AUTOINCREMENT,
    id_familia INTEGER NOT NULL,
    id_doacao INTEGER NOT NULL,
    quantidade_entregue INTEGER NOT NULL CHECK (quantidade_entregue > 0),
    data_entrega TEXT NOT NULL,
    observacao TEXT,
    CONSTRAINT fk_entrega_familia
        FOREIGN KEY (id_familia) REFERENCES familia(id_familia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_entrega_doacao
        FOREIGN KEY (id_doacao) REFERENCES doacao(id_doacao)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_entrega_familia ON entrega (id_familia);
CREATE INDEX IF NOT EXISTS idx_entrega_doacao ON entrega (id_doacao);
