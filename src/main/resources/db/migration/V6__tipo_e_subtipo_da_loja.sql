-- V6: tipo e subtipo da loja. Define so quais modulos e menus aparecem (nunca as regras de estoque).
-- Apenas acrescenta colunas; contas existentes ficam como 'ESTOQUE'. Nenhuma linha e apagada.
ALTER TABLE usuario ADD COLUMN tipo_loja VARCHAR(20) NOT NULL DEFAULT 'ESTOQUE';
ALTER TABLE usuario ADD COLUMN subtipo VARCHAR(30);

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_tipo_loja
    CHECK (tipo_loja IN ('ESTOQUE', 'COMERCIO', 'OFICINA', 'MISTO'));

ALTER TABLE usuario ADD CONSTRAINT ck_usuario_subtipo
    CHECK (subtipo IS NULL OR subtipo IN (
        'ALMOXARIFADO', 'DEPOSITO', 'ESTOQUE_ESCRITORIO',
        'MERCADO', 'PAPELARIA', 'MATERIAL_CONSTRUCAO', 'FARMACIA', 'VESTUARIO', 'INFORMATICA', 'OUTRO_COMERCIO',
        'MECANICA', 'MOTO', 'ELETRICA_ELETRONICA', 'ASSISTENCIA_TECNICA', 'REFRIGERACAO', 'OUTRO_SERVICO'));
