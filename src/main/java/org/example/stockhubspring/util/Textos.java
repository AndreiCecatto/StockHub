package org.example.stockhubspring.util;

import java.util.Locale;

/** Utilitários de texto usados pelos DTOs, services e consultas. */
public final class Textos {

    /** Caractere de escape usado nos padrões LIKE (precisa bater com o "escape" das consultas). */
    public static final char ESCAPE_LIKE = '!';

    private Textos() {
    }

    /** Remove espaços das pontas; texto vazio vira null. */
    public static String limpar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /** Forma usada na regra de nome único (RN-10): sem espaços nas pontas e em minúsculas. */
    public static String normalizarNome(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Monta o padrão LIKE ("contém") em minúsculas, escapando os curingas digitados pelo usuário.
     * Sem texto devolve "%" (casa com tudo), o que evita parâmetros nulos nas consultas.
     */
    public static String padraoLike(String busca) {
        String limpa = limpar(busca);
        if (limpa == null) {
            return "%";
        }
        String minusculo = limpa.toLowerCase(Locale.ROOT);
        StringBuilder padrao = new StringBuilder("%");
        for (char c : minusculo.toCharArray()) {
            if (c == '%' || c == '_' || c == ESCAPE_LIKE) {
                padrao.append(ESCAPE_LIKE);
            }
            padrao.append(c);
        }
        return padrao.append('%').toString();
    }
}
