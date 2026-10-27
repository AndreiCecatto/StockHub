package org.example.stockhubspring.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Decisão por item (campos {@code item[n].acao}, {@code .produtoId}, {@code .nome}, {@code .categoriaId},
 * {@code .preco}). Aqui só o formato; o que cada ação exige (ex.: CRIAR precisa de nome, categoria e
 * preço maior que zero) é conferido no service, item a item.
 */
public class DecisaoItemNfeForm {

    private AcaoItemNfe acao;

    private Integer produtoId;

    @Size(max = 120, message = "O nome do produto deve ter no máximo 120 caracteres.")
    private String nome;

    private Integer categoriaId;

    @DecimalMin(value = "0.00", message = "O preço deve ser maior ou igual a zero.")
    @DecimalMax(value = "9999999.99", message = "O preço deve ser no máximo 9.999.999,99.")
    @Digits(integer = 7, fraction = 2, message = "O preço deve ter no máximo 2 casas decimais.")
    private BigDecimal preco;

    public AcaoItemNfe getAcao() {
        return acao;
    }

    public void setAcao(AcaoItemNfe acao) {
        this.acao = acao;
    }

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }
}
