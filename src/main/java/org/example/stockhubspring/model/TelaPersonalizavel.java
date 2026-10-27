package org.example.stockhubspring.model;

/** Telas cujos blocos o dono pode ocultar (AD-19). Os nomes são gravados em {@code preferencia_bloco.tela}. */
public enum TelaPersonalizavel {
    INICIO("Início"),
    PRODUTOS("Lista de produtos");

    private final String rotulo;

    TelaPersonalizavel(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
