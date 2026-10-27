package org.example.stockhubspring.nfe;

/**
 * Finalidade da NF-e ({@code finNFe}). Só a NORMAL é uma compra que pode entrar no estoque (RN-58): as outras são
 * reconhecidas para que a recusa diga com clareza o que a nota é. O sistema nunca gera saída de estoque sozinho
 * a partir de uma devolução.
 */
public enum FinalidadeNfe {
    NORMAL("1", null),
    COMPLEMENTAR("2", "Esta é uma nota complementar (ajusta valores de uma compra anterior). "
            + "Só é aceita NF-e normal de compra: nada foi importado e o estoque não mudou."),
    AJUSTE("3", "Esta é uma nota de ajuste. Só é aceita NF-e normal de compra: nada foi importado "
            + "e o estoque não mudou."),
    DEVOLUCAO("4", "Esta é uma nota de devolução. Só é aceita NF-e normal de compra: nada foi importado "
            + "e o sistema não dá baixa no estoque sozinho. Se precisar, registre a saída em Movimentações.");

    private final String codigo;
    private final String mensagemDeRecusa;

    FinalidadeNfe(String codigo, String mensagemDeRecusa) {
        this.codigo = codigo;
        this.mensagemDeRecusa = mensagemDeRecusa;
    }

    /** Mensagem em linguagem simples para a recusa; nula para a nota normal. */
    public String mensagemDeRecusa() {
        return mensagemDeRecusa;
    }

    /** Finalidade do código {@code finNFe}; nulo se o código não existe no layout 4.00. */
    public static FinalidadeNfe doCodigo(String codigo) {
        for (FinalidadeNfe f : values()) {
            if (f.codigo.equals(codigo)) {
                return f;
            }
        }
        return null;
    }
}
