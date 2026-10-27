package org.example.stockhubspring.util;

import java.util.Locale;

/** Utilitários de texto usados pelos DTOs, services e consultas. */
public final class Textos {

    /** Caractere de escape usado nos padrões LIKE (precisa bater com o "escape" das consultas). */
    public static final char ESCAPE_LIKE = '!';

    private static final String ACENTUADAS_MINUSCULAS = "áàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String BASE_MINUSCULAS = "aaaaaeeeeiiiiooooouuuucn";

    /** Letras acentuadas (minúsculas e maiúsculas) que a busca por nome trata como a letra simples. */
    public static final String ACENTUADAS = ACENTUADAS_MINUSCULAS + ACENTUADAS_MINUSCULAS.toUpperCase(Locale.ROOT);
    /** Letras simples (minúsculas) na mesma posição de {@link #ACENTUADAS}; par do translate() do banco. */
    public static final String SEM_ACENTO = BASE_MINUSCULAS + BASE_MINUSCULAS;

    private Textos() {
    }

    /**
     * Minúsculas e sem acento, pela MESMA tabela usada no banco ({@code translate}), para a busca
     * de nome ignorar caixa e acento nos dois lados da comparação.
     */
    public static String semAcento(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder saida = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            int posicao = ACENTUADAS.indexOf(c);
            saida.append(posicao >= 0 ? SEM_ACENTO.charAt(posicao) : Character.toLowerCase(c));
        }
        return saida.toString();
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
