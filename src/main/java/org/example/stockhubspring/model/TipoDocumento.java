package org.example.stockhubspring.model;

/**
 * Documento que originou uma movimentação (RN-67). MANUAL é o lançamento feito à mão, sem documento.
 * COMPRA_NFE, VENDA e OS sempre trazem o código do documento; DEVOLUCAO, PERDA e INVENTARIO podem trazê-lo.
 */
public enum TipoDocumento {
    COMPRA_NFE("Compra (NF-e)", true),
    VENDA("Venda", true),
    OS("Ordem de serviço", true),
    DEVOLUCAO("Devolução", false),
    PERDA("Perda", false),
    INVENTARIO("Inventário", false),
    MANUAL("Lançamento manual", false);

    private final String rotulo;
    private final boolean exigeCodigo;

    TipoDocumento(String rotulo, boolean exigeCodigo) {
        this.rotulo = rotulo;
        this.exigeCodigo = exigeCodigo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** O tipo só existe com o código de um documento (compra, venda e OS). */
    public boolean exigeCodigo() {
        return exigeCodigo;
    }
}
