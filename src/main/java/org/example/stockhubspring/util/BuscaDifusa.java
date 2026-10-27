package org.example.stockhubspring.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Busca tolerante a erro de digitação para nomes de produto. Sem banco e sem biblioteca: cada palavra digitada é
 * comparada com as palavras do nome, ignorando caixa e acento.
 *
 * <p>Uma palavra digitada casa com uma palavra do nome quando (1) é o começo dela (o que vale também para a palavra
 * ainda incompleta, ex.: "cane" em "caneta"), ou (2) com 3 letras ou mais, fica a poucas edições de distância dela ou
 * de um começo dela de tamanho parecido (troca, falta, sobra ou inversão de letras: "cenata" e "canta" acham
 * "caneta"). Todas as palavras digitadas precisam casar. A nota vai de 0 a 1 (1 = começo exato).
 *
 * <p>Existe uma cópia desta regra em {@code js/busca-difusa.js}, usada pelo seletor de produto da tela de movimentações
 * (que já recebe a lista inteira). Ao mudar uma, mude a outra.
 */
public final class BuscaDifusa {

    /** Nota devolvida quando alguma palavra digitada não casa com nada. */
    public static final double SEM_CORRESPONDENCIA = 0;

    private BuscaDifusa() {
    }

    /** Minúsculas, sem acento, só letras e números, separadas por espaço. */
    public static List<String> palavras(String texto) {
        List<String> saida = new ArrayList<>();
        for (String parte : Textos.semAcento(texto).split("[^a-z0-9]+")) {
            if (!parte.isEmpty()) {
                saida.add(parte);
            }
        }
        return saida;
    }

    /** Nota de 0 a 1 do quanto {@code nome} combina com {@code termo}; {@link #SEM_CORRESPONDENCIA} se não combina. */
    public static double pontuar(String termo, String nome) {
        List<String> digitadas = palavras(termo);
        List<String> doNome = palavras(nome);
        if (digitadas.isEmpty() || doNome.isEmpty()) {
            return SEM_CORRESPONDENCIA;
        }
        double soma = 0;
        for (String digitada : digitadas) {
            double melhor = 0;
            for (String palavra : doNome) {
                melhor = Math.max(melhor, notaDaPalavra(digitada, palavra));
            }
            if (melhor == 0) {
                return SEM_CORRESPONDENCIA;
            }
            soma += melhor;
        }
        return soma / digitadas.size();
    }

    private static double notaDaPalavra(String digitada, String palavra) {
        if (palavra.equals(digitada)) {
            return 1;
        }
        if (palavra.startsWith(digitada)) {
            return 0.95;
        }
        int tamanho = digitada.length();
        if (tamanho < 3) {
            return 0; // palavras curtas só casam pelo começo: com 1 edição, quase tudo casaria com quase tudo
        }
        int permitido = tamanho <= 4 ? 1 : 2;
        int menor = distancia(digitada, palavra);
        for (int corte = Math.max(1, tamanho - 1); corte <= Math.min(palavra.length(), tamanho + 1); corte++) {
            menor = Math.min(menor, distancia(digitada, palavra.substring(0, corte)));
        }
        return menor <= permitido ? 1 - (menor / (tamanho + 1.0)) : 0;
    }

    /** Distância de edição com inversão de letras vizinhas (Damerau-Levenshtein na forma "optimal string alignment"). */
    static int distancia(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + custo);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }
}
