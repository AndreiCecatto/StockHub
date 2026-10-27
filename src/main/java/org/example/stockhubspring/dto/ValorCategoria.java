package org.example.stockhubspring.dto;

import java.math.BigDecimal;

/** Dinheiro parado no estoque de uma categoria: saldo vezes custo dos produtos ativos (RN-46). */
public record ValorCategoria(String categoria, Long produtos, Long unidades, BigDecimal valor) {
}
