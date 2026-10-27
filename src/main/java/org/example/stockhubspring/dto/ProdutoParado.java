package org.example.stockhubspring.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Produto com saldo e sem saída há 60 dias ou mais (RN-50). {@code ultimaSaida} é nula quando nunca saiu;
 * {@code diasParado} conta desde a última saída (ou, sem saída, desde a primeira movimentação).
 */
public record ProdutoParado(Integer produtoId, String nome, Integer saldo, BigDecimal valor,
                            LocalDateTime ultimaSaida, Long diasParado) {

    /** Forma usada pela consulta (os dias são calculados depois, pelo service). */
    public ProdutoParado(Integer produtoId, String nome, Integer saldo, BigDecimal valor, LocalDateTime ultimaSaida) {
        this(produtoId, nome, saldo, valor, ultimaSaida, null);
    }

    public ProdutoParado comDias(long dias) {
        return new ProdutoParado(produtoId, nome, saldo, valor, ultimaSaida, dias);
    }
}
