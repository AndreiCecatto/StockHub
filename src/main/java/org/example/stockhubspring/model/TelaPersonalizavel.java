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

    /** Trecho da URL: "inicio" ou "produtos". */
    public String slug() {
        return name().toLowerCase();
    }

    /** Converte o trecho da URL; devolve null se desconhecido. */
    public static TelaPersonalizavel deSlug(String slug) {
        if (slug == null) {
            return null;
        }
        for (TelaPersonalizavel t : values()) {
            if (t.slug().equals(slug.trim())) {
                return t;
            }
        }
        return null;
    }
}
