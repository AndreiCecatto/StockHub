-- Alinha o tipo de atualizado_por ao id do membro (INTEGER), como membro_id nas outras tabelas.
ALTER TABLE preferencia_bloco ALTER COLUMN atualizado_por TYPE INTEGER;
