package org.example.stockhubspring.model;

/** Situação do item numa nota importada: entrou no estoque, foi ignorado ou é fracionado (lançar à mão). */
public enum SituacaoItemNfe {
    IMPORTADO,
    IGNORADO,
    FRACIONADO
}
