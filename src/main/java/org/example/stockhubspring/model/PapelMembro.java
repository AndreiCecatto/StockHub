package org.example.stockhubspring.model;

import java.util.Locale;

/** Papel de uma pessoa dentro da loja (RN-61). */
public enum PapelMembro {
    DONO("Dono"),
    GERENTE("Gerente"),
    OPERADOR("Operador"),
    VENDEDOR("Vendedor"),
    MECANICO("Mecânico");

    private final String rotulo;

    PapelMembro(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Dono e gerente podem tudo: excluir, mudar o tipo da loja e cuidar da equipe (RN-62). */
    public boolean gerenciaLoja() {
        return this == DONO || this == GERENTE;
    }

    /** Convida e desativa pessoas: dono e gerente (o gerente não mexe em gerente nem em dono, RN-64). */
    public boolean podeGerenciarMembros() {
        return gerenciaLoja();
    }

    /** Papéis que podem ser escolhidos ao criar um membro (o dono nasce com a loja e é único). */
    public boolean escolhivel() {
        return this != DONO;
    }

    /** Converte o texto do formulário; devolve null se for vazio ou desconhecido. */
    public static PapelMembro de(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
