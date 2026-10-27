package org.example.stockhubspring.util;

/** Validação de documentos fiscais (só dígitos): CNPJ, CPF e chave de acesso da NF-e. */
public final class Documentos {

    private Documentos() {
    }

    /** Só os dígitos do texto (null vira vazio). */
    public static String soDigitos(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder saida = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c >= '0' && c <= '9') {
                saida.append(c);
            }
        }
        return saida.toString();
    }

    public static boolean soDigitosComTamanho(String texto, int tamanho) {
        if (texto == null || texto.length() != tamanho) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /** CNPJ com 14 dígitos e dígitos verificadores corretos (rejeita sequências repetidas). */
    public static boolean cnpjValido(String cnpj) {
        if (!soDigitosComTamanho(cnpj, 14) || repetido(cnpj)) {
            return false;
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        return digito(cnpj, pesos1) == cnpj.charAt(12) - '0' && digito(cnpj, pesos2) == cnpj.charAt(13) - '0';
    }

    /** CPF com 11 dígitos e dígitos verificadores corretos. */
    public static boolean cpfValido(String cpf) {
        if (!soDigitosComTamanho(cpf, 11) || repetido(cpf)) {
            return false;
        }
        int[] pesos1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
        return digito(cpf, pesos1) == cpf.charAt(9) - '0' && digito(cpf, pesos2) == cpf.charAt(10) - '0';
    }

    private static int digito(String numero, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (numero.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean repetido(String numero) {
        return numero.chars().allMatch(c -> c == numero.charAt(0));
    }

    /** Dígito verificador (módulo 11) dos 43 primeiros dígitos de uma chave de acesso. */
    public static int digitoChave(String primeiros43) {
        int soma = 0;
        int peso = 2;
        for (int i = primeiros43.length() - 1; i >= 0; i--) {
            soma += (primeiros43.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Chave de acesso: 44 dígitos com o dígito verificador (módulo 11) correto. */
    public static boolean chaveAcessoValida(String chave) {
        return soDigitosComTamanho(chave, 44) && digitoChave(chave.substring(0, 43)) == chave.charAt(43) - '0';
    }
}
