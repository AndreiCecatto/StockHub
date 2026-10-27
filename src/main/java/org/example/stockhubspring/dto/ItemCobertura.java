package org.example.stockhubspring.dto;

import java.math.BigDecimal;

/**
 * Por quantos dias o saldo dura no ritmo dos últimos 30 dias (RN-49). {@code alerta} é verdadeiro
 * quando dura menos de 7 dias.
 */
public record ItemCobertura(Integer produtoId, String nome, int saldo, BigDecimal mediaDiaria,
                            BigDecimal dias, boolean alerta) {
}
