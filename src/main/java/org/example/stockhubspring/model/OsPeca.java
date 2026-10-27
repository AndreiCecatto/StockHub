package org.example.stockhubspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Peça de uma OS, com preço e custo gravados na inclusão (RN-83). */
@Entity
@Table(name = "os_peca")
public class OsPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "os_id", nullable = false, updatable = false)
    private OrdemServico os;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, updatable = false, precision = 14, scale = 4)
    private BigDecimal precoUnitario;

    @Column(name = "custo_unitario", nullable = false, updatable = false, precision = 14, scale = 4)
    private BigDecimal custoUnitario;

    @Column(name = "reserva_id")
    private Integer reservaId;

    @Column(name = "movimentacao_id")
    private Integer movimentacaoId;

    /** Valor da linha: quantidade x preço unitário, em centavos. */
    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade)).setScale(2, RoundingMode.HALF_UP);
    }

    public Integer getId() { return id; }
    public OrdemServico getOs() { return os; }
    public void setOs(OrdemServico os) { this.os = os; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal v) { this.precoUnitario = v; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(BigDecimal v) { this.custoUnitario = v; }
    public Integer getReservaId() { return reservaId; }
    public void setReservaId(Integer reservaId) { this.reservaId = reservaId; }
    public Integer getMovimentacaoId() { return movimentacaoId; }
    public void setMovimentacaoId(Integer movimentacaoId) { this.movimentacaoId = movimentacaoId; }
}
