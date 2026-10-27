package org.example.stockhubspring.dto;

/** Decisão do dono para um item da nota na revisão. */
public enum AcaoItemNfe {
    /** Dar entrada num produto que já existe na loja ({@code produtoId}). */
    VINCULAR,
    /** Criar um produto novo ({@code nome}, {@code categoriaId}, {@code preco}) e dar entrada nele. */
    CRIAR,
    /** Não importar o item. */
    IGNORAR
}
