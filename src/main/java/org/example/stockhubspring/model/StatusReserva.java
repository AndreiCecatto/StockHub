package org.example.stockhubspring.model;

/** Situação de uma reserva de estoque (RN-72): só a ATIVA tira quantidade do saldo disponível. */
public enum StatusReserva {
    ATIVA, BAIXADA, CANCELADA
}
