package org.example.stockhubspring.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

/** "Caixa" de um mês: o que foi comprado (entradas) e o que saiu do estoque, em valor ao custo gravado (RN-48). */
public record CaixaMes(YearMonth mes, BigDecimal compras, BigDecimal saidas) {
}
