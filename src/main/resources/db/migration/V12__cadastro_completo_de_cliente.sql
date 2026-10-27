-- V12: cadastro completo de cliente (AD-16). So acrescenta colunas opcionais em parceiro e relaxa o documento;
-- nenhuma linha existente e apagada ou reescrita (so tipo_pessoa e preenchido a partir do documento que ja existe).

-- O cliente pode ser cadastrado sem CPF/CNPJ. A unicidade (usuario_codigo, documento) continua valendo para os
-- documentos informados (no PostgreSQL varios NULL nao colidem) e o CHECK de formato passa com NULL.
ALTER TABLE parceiro ALTER COLUMN documento DROP NOT NULL;

ALTER TABLE parceiro
    ADD COLUMN tipo_pessoa     VARCHAR(10),
    ADD COLUMN email           VARCHAR(255),
    ADD COLUMN telefone        VARCHAR(11),
    ADD COLUMN whatsapp        VARCHAR(11),
    ADD COLUMN cep             VARCHAR(8),
    ADD COLUMN logradouro      VARCHAR(255),
    ADD COLUMN numero          VARCHAR(20),
    ADD COLUMN complemento     VARCHAR(100),
    ADD COLUMN bairro          VARCHAR(100),
    ADD COLUMN cidade          VARCHAR(100),
    ADD COLUMN data_nascimento DATE,
    ADD COLUMN observacoes     VARCHAR(1000),
    ADD COLUMN origem          VARCHAR(120),
    ADD COLUMN ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN atualizado_em   TIMESTAMP(6);

ALTER TABLE parceiro
    ADD CONSTRAINT ck_parceiro_tipo_pessoa CHECK (tipo_pessoa IS NULL OR tipo_pessoa IN ('FISICA', 'JURIDICA')),
    ADD CONSTRAINT ck_parceiro_telefone CHECK (telefone IS NULL OR telefone ~ '^[0-9]{10,11}$'),
    ADD CONSTRAINT ck_parceiro_whatsapp CHECK (whatsapp IS NULL OR whatsapp ~ '^[0-9]{10,11}$'),
    ADD CONSTRAINT ck_parceiro_cep CHECK (cep IS NULL OR cep ~ '^[0-9]{8}$');

-- Quem ja tem documento ganha o tipo de pessoa correspondente
UPDATE parceiro SET tipo_pessoa = CASE WHEN length(documento) = 14 THEN 'JURIDICA' ELSE 'FISICA' END
 WHERE documento IS NOT NULL AND tipo_pessoa IS NULL;

CREATE INDEX idx_parceiro_cliente_lista ON parceiro (usuario_codigo, ativo, nome) WHERE eh_cliente;
