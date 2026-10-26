-- V2: regras de estoque (docs/regras-negocio.md, RN-01..RN-14)
-- Aplicada sobre a V1 sem apagar dados. Qualquer erro aborta a migration inteira (o Flyway
-- executa o script em uma transacao no PostgreSQL), entao o banco nunca fica pela metade.

-- ---------------------------------------------------------------------------
-- 1. Protecao: dados existentes que nao cabem nos novos limites NAO sao cortados;
--    a migration falha com mensagem clara para o dado ser corrigido manualmente.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    qtd BIGINT;
BEGIN
    SELECT count(*) INTO qtd FROM produto
     WHERE length(nome) > 120 OR length(descricao) > 500 OR preco > 9999999.99;
    IF qtd > 0 THEN
        RAISE EXCEPTION 'V2: % produto(s) excedem os limites (nome 120, descricao 500, preco 9999999.99). Corrija antes de migrar.', qtd;
    END IF;

    SELECT count(*) INTO qtd FROM categoria
     WHERE length(nome) > 120 OR length(descricao) > 500;
    IF qtd > 0 THEN
        RAISE EXCEPTION 'V2: % categoria(s) excedem os limites (nome 120, descricao 500). Corrija antes de migrar.', qtd;
    END IF;

    SELECT count(*) INTO qtd FROM movimentacao_estoque
     WHERE length(observacao) > 255;
    IF qtd > 0 THEN
        RAISE EXCEPTION 'V2: % movimentacao(oes) com observacao maior que 255. Corrija antes de migrar.', qtd;
    END IF;

    SELECT count(*) INTO qtd FROM movimentacao_estoque
     WHERE (tipo IN ('ENTRADA', 'SAIDA') AND quantidade <= 0)
        OR (tipo = 'AJUSTE' AND quantidade < 0);
    IF qtd > 0 THEN
        RAISE EXCEPTION 'V2: % movimentacao(oes) com quantidade invalida para o tipo. Corrija antes de migrar.', qtd;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 2. Produto: coluna ativo (RN-08)
-- ---------------------------------------------------------------------------
ALTER TABLE produto ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- ---------------------------------------------------------------------------
-- 3. Duplicatas de nome (RN-10): nada e apagado; a repetida ganha o sufixo " (2)", " (3)"...
--    na ordem do id (o registro mais antigo mantem o nome original). Cada renomeacao e
--    reportada com RAISE NOTICE. Produtos inativos ficam fora da regra (indice parcial).
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    r      RECORD;
    sufixo TEXT;
    novo   TEXT;
BEGIN
    FOR r IN
        SELECT id, usuario_codigo, nome,
               row_number() OVER (PARTITION BY usuario_codigo, lower(btrim(nome)) ORDER BY id) AS pos
          FROM categoria
    LOOP
        IF r.pos > 1 THEN
            sufixo := ' (' || r.pos || ')';
            novo := left(btrim(r.nome), 120 - length(sufixo)) || sufixo;
            RAISE NOTICE 'V2: categoria id=% (loja %) duplicada: "%" renomeada para "%"',
                r.id, r.usuario_codigo, r.nome, novo;
            UPDATE categoria SET nome = novo WHERE id = r.id;
        END IF;
    END LOOP;

    FOR r IN
        SELECT id, usuario_codigo, nome,
               row_number() OVER (PARTITION BY usuario_codigo, lower(btrim(nome)) ORDER BY id) AS pos
          FROM produto
         WHERE ativo
    LOOP
        IF r.pos > 1 THEN
            sufixo := ' (' || r.pos || ')';
            novo := left(btrim(r.nome), 120 - length(sufixo)) || sufixo;
            RAISE NOTICE 'V2: produto id=% (loja %) duplicado: "%" renomeado para "%"',
                r.id, r.usuario_codigo, r.nome, novo;
            UPDATE produto SET nome = novo WHERE id = r.id;
        END IF;
    END LOOP;
END $$;

-- ---------------------------------------------------------------------------
-- 4. Tamanhos de coluna (RN-11)
--    ck_produto_preco (preco >= 0) ja existe desde a V1 e e mantida.
-- ---------------------------------------------------------------------------
ALTER TABLE produto
    ALTER COLUMN nome      TYPE VARCHAR(120),
    ALTER COLUMN descricao TYPE VARCHAR(500),
    ALTER COLUMN preco     TYPE NUMERIC(9, 2);

ALTER TABLE categoria
    ALTER COLUMN nome      TYPE VARCHAR(120),
    ALTER COLUMN descricao TYPE VARCHAR(500);

ALTER TABLE movimentacao_estoque
    ALTER COLUMN observacao TYPE VARCHAR(255);

-- ---------------------------------------------------------------------------
-- 5. Movimentacao: saldo antes/depois (RN-06)
--    Colunas entram anulaveis, sao preenchidas e so entao viram NOT NULL.
--    Backfill: por produto, em ordem cronologica (criado_em, id), parte de saldo 0 e aplica
--    ENTRADA (+q), SAIDA (-q, minimo 0) e AJUSTE (saldo passa a ser q), de modo que
--    quantidade_anterior de cada linha e igual a quantidade_nova da linha anterior.
-- ---------------------------------------------------------------------------
ALTER TABLE movimentacao_estoque
    ADD COLUMN quantidade_anterior INTEGER,
    ADD COLUMN quantidade_nova     INTEGER;

WITH RECURSIVE ordenada AS (
    SELECT id, produto_id, tipo, quantidade::BIGINT AS qtd,
           row_number() OVER (PARTITION BY produto_id ORDER BY criado_em, id) AS rn
      FROM movimentacao_estoque
),
saldo AS (
    SELECT o.id, o.produto_id, o.rn,
           0::BIGINT AS anterior,
           CASE o.tipo WHEN 'ENTRADA' THEN o.qtd
                       WHEN 'SAIDA'   THEN 0::BIGINT
                       ELSE o.qtd END AS nova
      FROM ordenada o
     WHERE o.rn = 1
    UNION ALL
    SELECT o.id, o.produto_id, o.rn,
           s.nova,
           CASE o.tipo WHEN 'ENTRADA' THEN s.nova + o.qtd
                       WHEN 'SAIDA'   THEN GREATEST(s.nova - o.qtd, 0)
                       ELSE o.qtd END
      FROM saldo s
      JOIN ordenada o ON o.produto_id = s.produto_id AND o.rn = s.rn + 1
)
UPDATE movimentacao_estoque m
   SET quantidade_anterior = s.anterior::INTEGER,
       quantidade_nova     = s.nova::INTEGER
  FROM saldo s
 WHERE m.id = s.id;

ALTER TABLE movimentacao_estoque
    ALTER COLUMN quantidade_anterior SET NOT NULL,
    ALTER COLUMN quantidade_nova     SET NOT NULL;

ALTER TABLE movimentacao_estoque
    ADD CONSTRAINT ck_movimentacao_saldos_nao_negativos
        CHECK (quantidade_anterior >= 0 AND quantidade_nova >= 0),
    ADD CONSTRAINT ck_movimentacao_quantidade_por_tipo
        CHECK ((tipo IN ('ENTRADA', 'SAIDA') AND quantidade > 0)
            OR (tipo = 'AJUSTE' AND quantidade >= 0));

-- ---------------------------------------------------------------------------
-- 6. Indices
-- ---------------------------------------------------------------------------
-- RN-10: nome unico por loja, sem diferenciar maiusculas nem espacos nas pontas
CREATE UNIQUE INDEX ux_produto_usuario_nome_ativo
    ON produto (usuario_codigo, lower(btrim(nome)))
    WHERE ativo;

CREATE UNIQUE INDEX ux_categoria_usuario_nome
    ON categoria (usuario_codigo, lower(btrim(nome)));

-- RN-01/RN-08: listagens por loja filtrando ativos
CREATE INDEX idx_produto_usuario_ativo ON produto (usuario_codigo, ativo);

-- Historico de um produto, do mais recente para o mais antigo
CREATE INDEX idx_movimentacao_produto_criado_em
    ON movimentacao_estoque (produto_id, criado_em DESC);
