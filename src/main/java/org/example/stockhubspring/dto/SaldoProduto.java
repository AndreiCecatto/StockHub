package org.example.stockhubspring.dto;

/** Em estoque, reservado e disponível de um produto (RN-72): disponível = em estoque menos reservas ativas, nunca negativo. */
public record SaldoProduto(int emEstoque, long reservado, long disponivel) {

    public static SaldoProduto de(int emEstoque, long reservado) {
        return new SaldoProduto(emEstoque, reservado, Math.max(0, emEstoque - reservado));
    }
}
