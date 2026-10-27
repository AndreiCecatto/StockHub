package org.example.stockhubspring.dto;

/**
 * Movimento de um dia para o gráfico do painel: unidades que entraram e saíram e quantos ajustes houve.
 * {@code dia} no formato aaaa-MM-dd.
 */
public record MovimentoDiario(String dia, long entradas, long saidas, long ajustes) {
}
