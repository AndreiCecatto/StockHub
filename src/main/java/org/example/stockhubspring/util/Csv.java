package org.example.stockhubspring.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Montagem de CSV para o Excel brasileiro: UTF-8 com BOM, separador ";", linhas terminadas em CRLF
 * e números com vírgula decimal. Valores de texto são protegidos contra injeção de fórmula.
 */
public final class Csv {

    public static final char SEPARADOR = ';';
    public static final String QUEBRA_DE_LINHA = "\r\n";
    /** Marca de ordem de bytes: faz o Excel reconhecer o arquivo como UTF-8 (acentos corretos). */
    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private Csv() {
    }

    /**
     * Texto seguro para uma célula. Texto que começa com {@code = + - @}, tabulação ou CR (ignorando
     * espaços antes) recebe um apóstrofo na frente, para o Excel não executar como fórmula; depois
     * aplica o escape de aspas, separador e quebras de linha. Nulo vira célula vazia.
     */
    public static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        return escapar(protegerFormula(valor));
    }

    /** Número inteiro: nunca recebe proteção de fórmula (um saldo negativo não é texto do usuário). */
    public static String inteiro(long valor) {
        return Long.toString(valor);
    }

    /** Valor monetário com 2 casas e vírgula decimal (ex.: 1234,50); sem separador de milhar. */
    public static String decimal(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Junta células já prontas (resultado de {@link #texto}, {@link #inteiro}, {@link #decimal}) numa linha. */
    public static String linha(String... celulas) {
        return String.join(String.valueOf(SEPARADOR), celulas) + QUEBRA_DE_LINHA;
    }

    /** Conteúdo final do arquivo: BOM seguido do texto em UTF-8. */
    public static byte[] bytes(List<String> linhas) {
        byte[] corpo = String.join("", linhas).getBytes(StandardCharsets.UTF_8);
        byte[] arquivo = new byte[BOM_UTF8.length + corpo.length];
        System.arraycopy(BOM_UTF8, 0, arquivo, 0, BOM_UTF8.length);
        System.arraycopy(corpo, 0, arquivo, BOM_UTF8.length, corpo.length);
        return arquivo;
    }

    static String protegerFormula(String valor) {
        int i = 0;
        while (i < valor.length() && valor.charAt(i) == ' ') {
            i++;
        }
        if (i < valor.length()) {
            char primeiro = valor.charAt(i);
            if (primeiro == '=' || primeiro == '+' || primeiro == '-' || primeiro == '@'
                    || primeiro == '\t' || primeiro == '\r') {
                return "'" + valor;
            }
        }
        return valor;
    }

    private static String escapar(String valor) {
        boolean precisaAspas = valor.indexOf(SEPARADOR) >= 0 || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0;
        return precisaAspas ? "\"" + valor.replace("\"", "\"\"") + "\"" : valor;
    }
}
