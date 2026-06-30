package org.example.stockhubspring.model;

import java.math.BigDecimal;

public class ResumoEstoque {

    private long totalProdutos;
    private long totalCategorias;
    private long produtosEstoqueBaixo;
    private int quantidadeTotalItens;
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

    public int getQuantidadeTotalItens() {
        return quantidadeTotalItens;
    }

    public void setQuantidadeTotalItens(int quantidadeTotalItens) {
        this.quantidadeTotalItens = quantidadeTotalItens;
    }

    public BigDecimal getValorTotalEstoque() {
        return valorTotalEstoque;
    }

    public void setValorTotalEstoque(BigDecimal valorTotalEstoque) {
        this.valorTotalEstoque = valorTotalEstoque;
    }
}
