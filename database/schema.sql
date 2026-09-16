-- sqlite

PRAGMA foreign_keys = ON;

CREATE TABLE administrador (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    senha TEXT NOT NULL
);

CREATE TABLE familia (
    id_familia INTEGER PRIMARY KEY AUTOINCREMENT,
    nome_responsavel TEXT NOT NULL,
    cpf_responsavel TEXT NOT NULL UNIQUE,
    quantidade_membros INTEGER NOT NULL,
    telefone TEXT NOT NULL,
    endereco TEXT NOT NULL,
    data_cadastro TEXT NOT NULL
);

CREATE TABLE doacao (
    id_doacao INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo TEXT NOT NULL,
    quantidade INTEGER NOT NULL,
    data_doacao TEXT NOT NULL,
    observacao TEXT
);

CREATE TABLE entrega (
    id_entrega INTEGER PRIMARY KEY AUTOINCREMENT,
    id_familia INTEGER NOT NULL,
    id_doacao INTEGER NOT NULL,
    quantidade_entregue INTEGER NOT NULL,
    data_entrega TEXT NOT NULL,
    observacao TEXT,
    FOREIGN KEY (id_familia) REFERENCES familia(id_familia),
    FOREIGN KEY (id_doacao) REFERENCES doacao(id_doacao)
);