package org.example.stockhubspring.model;

import java.util.Locale;

/** Ramo da loja. Cada subtipo pertence a um tipo; o tipo MISTO aceita os de comércio e de serviços (RN-44). */
public enum Subtipo {
    ALMOXARIFADO("Almoxarifado", TipoLoja.ESTOQUE),
    DEPOSITO("Depósito", TipoLoja.ESTOQUE),
    ESTOQUE_ESCRITORIO("Estoque de escritório", TipoLoja.ESTOQUE),

    MERCADO("Mercado", TipoLoja.COMERCIO),
    PAPELARIA("Papelaria", TipoLoja.COMERCIO),
    MATERIAL_CONSTRUCAO("Material de construção", TipoLoja.COMERCIO),
    FARMACIA("Farmácia", TipoLoja.COMERCIO),
    VESTUARIO("Vestuário", TipoLoja.COMERCIO),
    INFORMATICA("Informática", TipoLoja.COMERCIO),
    OUTRO_COMERCIO("Outro comércio", TipoLoja.COMERCIO),

    MECANICA("Mecânica", TipoLoja.OFICINA),
    MOTO("Moto", TipoLoja.OFICINA),
    ELETRICA_ELETRONICA("Elétrica e eletrônica", TipoLoja.OFICINA),
    ASSISTENCIA_TECNICA("Assistência técnica", TipoLoja.OFICINA),
    REFRIGERACAO("Refrigeração", TipoLoja.OFICINA),
    OUTRO_SERVICO("Outro serviço", TipoLoja.OFICINA);

    private final String rotulo;
    private final TipoLoja grupo;

    Subtipo(String rotulo, TipoLoja grupo) {
        this.rotulo = rotulo;
        this.grupo = grupo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public TipoLoja getGrupo() {
        return grupo;
    }

    public boolean pertenceA(TipoLoja tipo) {
        return tipo == grupo || (tipo == TipoLoja.MISTO && grupo != TipoLoja.ESTOQUE);
    }

    /** Converte texto do formulário; vazio vira null (subtipo é opcional); desconhecido lança IllegalArgumentException. */
    public static Subtipo de(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return valueOf(texto.trim().toUpperCase(Locale.ROOT));
    }
}
