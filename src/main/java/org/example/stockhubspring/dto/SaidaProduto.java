package org.example.stockhubspring.dto;

import java.math.BigDecimal;

/** Saídas de um produto no período: unidades e valor ao custo gravado na movimentação (RN-47). */
public record SaidaProduto(Integer produtoId, String nome, Long unidades, BigDecimal valor) {
}
