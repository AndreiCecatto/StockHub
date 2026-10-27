package org.example.stockhubspring.dto;

import java.math.BigDecimal;

/** Saídas de uma categoria no período: unidades e valor ao custo gravado na movimentação (RN-47). */
public record SaidaCategoria(String categoria, Long unidades, BigDecimal valor) {
}
