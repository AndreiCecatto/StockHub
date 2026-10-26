package org.example.stockhubspring.repository;

import java.math.BigDecimal;

/** Projeção da consulta agregada do painel (RN-13). Os campos podem vir nulos quando não há produtos. */
public interface ResumoProdutos {

    Long getTotalProdutos();

    Long getTotalUnidades();

    BigDecimal getValorTotal();
}
