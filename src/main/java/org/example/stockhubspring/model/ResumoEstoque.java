package org.example.stockhubspring.model;

import java.math.BigDecimal;

/** Totais do painel, calculados no banco por consultas agregadas (RN-13). */
public class ResumoEstoque {

    private long totalProdutos;
    private long totalCategorias;
    // Produtos que pedem reposição: níveis BAIXO e SEM_ESTOQUE (RN-12)
    private long produtosEstoqueBaixo;
    private long produtosSemEstoque;
    private long quantidadeTotalItens;
    private BigDecimal valorTotalEstoque = BigDecimal.ZERO;

    public long getTotalProdutos() {
        return totalProdutos;
    }

    public void setTotalProdutos(long totalProdutos) {
        this.totalProdutos = totalProdutos;
    }

    public long getTotalCategorias() {
        return totalCategorias;
    }

    public void setTotalCategorias(long totalCategorias) {
        this.totalCategorias = totalCategorias;
    }

    public long getProdutosEstoqueBaixo() {
        return produtosEstoqueBaixo;
    }

    public void setProdutosEstoqueBaixo(long produtosEstoqueBaixo) {
        this.produtosEstoqueBaixo = produtosEstoqueBaixo;
    }

    public long getProdutosSemEstoque() {
        return produtosSemEstoque;
    }

    public void setProdutosSemEstoque(long produtosSemEstoque) {
        this.produtosSemEstoque = produtosSemEstoque;
    }

    public long getQuantidadeTotalItens() {
        return quantidadeTotalItens;
    }

    public void setQuantidadeTotalItens(long quantidadeTotalItens) {
        this.quantidadeTotalItens = quantidadeTotalItens;
    }

    public BigDecimal getValorTotalEstoque() {
        return valorTotalEstoque;
    }

    public void setValorTotalEstoque(BigDecimal valorTotalEstoque) {
        this.valorTotalEstoque = valorTotalEstoque;
    }
}
