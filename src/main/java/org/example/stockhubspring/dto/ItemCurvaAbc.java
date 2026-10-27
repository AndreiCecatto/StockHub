package org.example.stockhubspring.dto;

import java.math.BigDecimal;

/** Um produto na curva ABC por valor de saída em 90 dias (RN-51). Percentuais de 0 a 100. */
public record ItemCurvaAbc(Integer produtoId, String nome, BigDecimal valor, BigDecimal percentual,
                           BigDecimal acumulado, String classe) {
}
