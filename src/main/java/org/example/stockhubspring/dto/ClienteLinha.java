package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.Parceiro;

/** Uma linha da lista de clientes: o cadastro e quantas ordens de serviço ele já teve. */
public record ClienteLinha(Parceiro cliente, long totalOs) {
}
