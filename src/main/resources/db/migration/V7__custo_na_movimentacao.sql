-- V7: custo unitario gravado na movimentacao (base das analises de estoque, RN-45 a RN-51).
-- Apenas acrescenta uma coluna e um indice. Nenhuma linha e apagada.
-- Movimentacoes antigas recebem o custo ATUAL do produto (e a melhor aproximacao disponivel:
-- o custo da epoca nao foi guardado); as novas gravam o custo do momento.
ALTER TABLE movimentacao_estoque ADD COLUMN custo_unitario NUMERIC(14,4);

UPDATE movimentacao_estoque m
   SET custo_unitario = COALESCE(p.custo, 0)
  FROM produto p
 WHERE p.id = m.produto_id;

UPDATE movimentacao_estoque SET custo_unitario = 0 WHERE custo_unitario IS NULL;

ALTER TABLE movimentacao_estoque
    ALTER COLUMN custo_unitario SET NOT NULL,
    ALTER COLUMN custo_unitario SET DEFAULT 0,
    ADD CONSTRAINT ck_movimentacao_custo_nao_negativo CHECK (custo_unitario >= 0);

-- Analises agregam por loja e por periodo
CREATE INDEX idx_movimentacao_usuario_criado_em
    ON movimentacao_estoque (usuario_codigo, criado_em);
