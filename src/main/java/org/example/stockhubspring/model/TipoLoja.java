package org.example.stockhubspring.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** Tipo da loja: define quais módulos aparecem (RN-43). Nunca muda as regras de estoque. */
public enum TipoLoja {
    ESTOQUE("Só controle de estoque", EnumSet.of(Modulo.ESTOQUE)),
    COMERCIO("Comércio", EnumSet.of(Modulo.ESTOQUE, Modulo.VENDAS)),
    OFICINA("Oficina e serviços", EnumSet.of(Modulo.ESTOQUE, Modulo.OS)),
    MISTO("Comércio e serviços", EnumSet.of(Modulo.ESTOQUE, Modulo.VENDAS, Modulo.OS));

    private final String rotulo;
    private final Set<Modulo> modulos;

    TipoLoja(String rotulo, Set<Modulo> modulos) {
        this.rotulo = rotulo;
        this.modulos = modulos;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean temModulo(Modulo modulo) {
        return modulos.contains(modulo);
    }

    /** Converte texto do formulário; vazio ou nulo vira ESTOQUE; valor desconhecido vira null (inválido). */
    public static TipoLoja de(String texto) {
        if (texto == null || texto.isBlank()) {
            return ESTOQUE;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
