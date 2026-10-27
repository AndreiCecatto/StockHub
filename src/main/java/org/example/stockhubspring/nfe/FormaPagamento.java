package org.example.stockhubspring.nfe;

import java.util.Map;

/** Texto em linguagem simples para o código de forma de pagamento (tPag) da NF-e 4.00 (RN-57). */
public final class FormaPagamento {

    public static final String OUTRA = "Outra forma de pagamento";

    private static final Map<String, String> ROTULOS = Map.ofEntries(
            Map.entry("01", "Dinheiro"),
            Map.entry("02", "Cheque"),
            Map.entry("03", "Cartão de crédito"),
            Map.entry("04", "Cartão de débito"),
            Map.entry("05", "Crédito na loja"),
            Map.entry("10", "Vale-alimentação"),
            Map.entry("11", "Vale-refeição"),
            Map.entry("12", "Vale-presente"),
            Map.entry("13", "Vale-combustível"),
            Map.entry("14", "Duplicata"),
            Map.entry("15", "Boleto"),
            Map.entry("16", "Depósito bancário"),
            Map.entry("17", "Pix"),
            Map.entry("18", "Transferência"),
            Map.entry("19", "Cashback ou programa de fidelidade"),
            Map.entry("20", "Pix"),
            Map.entry("90", "Sem pagamento"),
            Map.entry("99", OUTRA));

    private FormaPagamento() {
    }

    /** Nome da forma de pagamento; para "outros" (99) usa a descrição da própria nota, se houver. */
    public static String rotulo(String codigo, String descricaoDaNota) {
        if ("99".equals(codigo) && descricaoDaNota != null && !descricaoDaNota.isBlank()) {
            return descricaoDaNota;
        }
        return ROTULOS.getOrDefault(codigo, OUTRA);
    }
}
