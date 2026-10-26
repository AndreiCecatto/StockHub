package org.example.stockhubspring.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.util.Textos;

/**
 * Formulário de movimentação de estoque (RN-05). A faixa por tipo (ENTRADA/SAIDA &gt; 0,
 * máximo 1.000.000; AJUSTE com motivo) é conferida no {@code EstoqueService}; aqui fica só
 * o formato básico.
 */
public class MovimentacaoForm {

    @NotNull(message = "Selecione o produto.")
    private Integer produtoId;

    @NotNull(message = "Selecione o tipo de movimentação.")
    private TipoMovimentacao tipo;

    @NotNull(message = "Informe a quantidade.")
    @Min(value = 0, message = "A quantidade não pode ser negativa.")
    @Max(value = 2_000_000_000, message = "A quantidade é grande demais.")
    private Integer quantidade;

    @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres.")
    private String observacao;

    public Integer getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Integer produtoId) {
        this.produtoId = produtoId;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = Textos.limpar(observacao);
    }
}
