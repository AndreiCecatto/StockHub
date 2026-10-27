-- V14: placa e modelo do veiculo no equipamento do cliente (AD-18, RN-104 a RN-108).
-- Usado pelos subtipos de oficina (MECANICA, MOTO). So acrescenta colunas NULLABLE;
-- nenhuma linha existente e apagada ou reescrita.

ALTER TABLE equipamento_cliente
    ADD COLUMN placa  VARCHAR(7),
    ADD COLUMN modelo VARCHAR(80);

-- Placa normalizada em maiusculas: padrao antigo (ABC1234) ou Mercosul (ABC1D23)
ALTER TABLE equipamento_cliente
    ADD CONSTRAINT ck_equipamento_placa
    CHECK (placa IS NULL OR placa ~ '^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$');

-- Busca de veiculo por placa dentro da loja (sem UNIQUE: o mesmo veiculo pode ter varios cadastros)
CREATE INDEX idx_equipamento_placa ON equipamento_cliente (usuario_codigo, placa) WHERE placa IS NOT NULL;
