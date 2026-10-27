-- V13: campos opcionais de produto por ramo de atividade (moda, alimentos, eletronicos etc.).
-- So acrescenta colunas NULLABLE em produto; nenhuma linha existente e apagada ou reescrita.

ALTER TABLE produto
    ADD COLUMN tamanho       VARCHAR(20),
    ADD COLUMN cor           VARCHAR(40),
    ADD COLUMN lote          VARCHAR(40),
    ADD COLUMN data_validade DATE,
    ADD COLUMN numero_serie  VARCHAR(60);

-- Consultas de vencimento (produtos proximos da validade)
CREATE INDEX idx_produto_data_validade ON produto (data_validade);
