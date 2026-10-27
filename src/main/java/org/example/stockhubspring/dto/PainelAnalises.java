package org.example.stockhubspring.dto;

import java.math.BigDecimal;
import java.util.List;

/** Tudo que a tela /analises mostra, já agregado e restrito à loja (RN-45). */
public record PainelAnalises(
        int periodo,
        int mesesCaixa,
        // 1. Dinheiro parado no estoque
        BigDecimal valorEstoque,
        long produtosComSaldo,
        long produtosSemCusto,
        List<ValorCategoria> valorPorCategoria,
        // 2. O que mais sai
        List<SaidaDia> saidasPorDia,
        long unidadesSaidas,
        BigDecimal valorSaidas,
        List<SaidaProduto> topProdutos,
        List<SaidaCategoria> saidasPorCategoria,
        // 3. Compras x saídas
        List<CaixaMes> caixa,
        // 4. Vai acabar em breve
        List<ItemCobertura> cobertura,
        long totalEmAlerta,
        // 5. Produtos parados
        List<ProdutoParado> parados,
        long totalParados,
        BigDecimal valorParado,
        // 6. Curva ABC
        List<ItemCurvaAbc> curvaAbc,
        long produtosClasseA,
        BigDecimal percentualClasseA) {
}
