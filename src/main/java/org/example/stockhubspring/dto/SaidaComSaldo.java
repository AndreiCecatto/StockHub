package org.example.stockhubspring.dto;

/** Produto com saldo e o total que saiu numa janela; matéria-prima da cobertura em dias (RN-49). */
public record SaidaComSaldo(Integer produtoId, String nome, Integer saldo, Long saidas) {
}
