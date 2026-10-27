package org.example.stockhubspring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Saídas de um dia (todos os dias do período aparecem, com zero quando não houve saída). */
public record SaidaDia(LocalDate dia, long unidades, BigDecimal valor) {
}
