-- V5: o XML guardado deixa de ficar para sempre e a nota ganha unicidade de negocio.
-- So relaxa e acrescenta restricoes; nenhuma linha e apagada.

-- 1. xml pode ser NULL: importacoes encerradas (confirmada, cancelada ou vencida) tem o xml zerado
ALTER TABLE importacao_nfe ALTER COLUMN xml DROP NOT NULL;

-- Zera so o xml das linhas ja encerradas (nao apaga linha nenhuma)
UPDATE importacao_nfe SET xml = NULL WHERE status <> 'PENDENTE' AND xml IS NOT NULL;

-- 2. Mesma nota (fornecedor, modelo, serie, numero) nao entra duas vezes na mesma loja, mesmo com chaves
--    diferentes. Se ja houver duplicata, a migration falha com mensagem clara em vez de apagar algo.
DO $$
DECLARE
    qtd BIGINT;
BEGIN
    SELECT count(*) INTO qtd FROM (
        SELECT 1 FROM nota_fiscal
         WHERE parceiro_id IS NOT NULL
         GROUP BY usuario_codigo, parceiro_id, modelo, serie, numero
        HAVING count(*) > 1) d;
    IF qtd > 0 THEN
        RAISE EXCEPTION 'V5: % grupo(s) de notas duplicadas (loja, fornecedor, modelo, serie, numero). Corrija antes de migrar.', qtd;
    END IF;
END $$;

ALTER TABLE nota_fiscal
    ADD CONSTRAINT uk_nota_fiscal_negocio UNIQUE (usuario_codigo, parceiro_id, modelo, serie, numero);
