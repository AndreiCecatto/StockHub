-- V15: preferencias de blocos por loja e tela (AD-19, RN-109 a RN-114).
-- So cria uma tabela nova e vazia: ausencia de linha = comportamento atual (bloco visivel).
-- Nenhuma tabela existente e alterada e nenhuma linha e inserida.

CREATE TABLE preferencia_bloco (
    usuario_codigo INTEGER      NOT NULL,                 -- a loja (AD-13)
    tela           VARCHAR(30)  NOT NULL,
    bloco          VARCHAR(40)  NOT NULL,
    visivel        BOOLEAN      NOT NULL,
    atualizado_em  TIMESTAMP    NOT NULL DEFAULT now(),
    atualizado_por BIGINT,                                -- id do membro, sem FK (como membro_id nas outras tabelas)
    CONSTRAINT pk_preferencia_bloco PRIMARY KEY (usuario_codigo, tela, bloco),
    CONSTRAINT fk_preferencia_bloco_usuario FOREIGN KEY (usuario_codigo) REFERENCES usuario (codigo),
    CONSTRAINT ck_preferencia_bloco_tela CHECK (tela IN ('INICIO', 'PRODUTOS')),
    CONSTRAINT ck_preferencia_bloco_bloco CHECK (bloco ~ '^[A-Z0-9_]{1,40}$')
);
