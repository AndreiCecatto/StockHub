package org.example.stockhubspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** Item de uma nota importada, com o produto vinculado e a movimentação gerada (quando entrou no estoque). */
@Entity
@Table(name = "nota_fiscal_item")
public class NotaFiscalItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id", nullable = false, updatable = false)
    private NotaFiscal notaFiscal;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "numero_item", nullable = false)
    private Integer numeroItem;

    @Column(name = "codigo_fornecedor", length = 60)
    private String codigoFornecedor;

    @Column(length = 14)
    private String ean;

    @Column(length = 120)
    private String descricao;

    @Column(length = 8)
    private String ncm;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 4)
    private String cfop;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cfop_entrada", length = 4)
    private String cfopEntrada;

    @Column(length = 6)
    private String unidade;

    @Column(precision = 14, scale = 4)
    private BigDecimal quantidade;

    @Column(name = "valor_unitario", precision = 21, scale = 10)
    private BigDecimal valorUnitario;

    @Column(name = "valor_total", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimentacao_id")
    private MovimentacaoEstoque movimentacao;

    @Column(name = "valor_desconto", precision = 14, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "valor_frete", precision = 14, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "valor_seguro", precision = 14, scale = 2)
    private BigDecimal valorSeguro;

    @Column(name = "valor_outras", precision = 14, scale = 2)
    private BigDecimal valorOutras;

    @Column(name = "custo_unitario", precision = 14, scale = 4)
    private BigDecimal custoUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoItemNfe situacao;

    public Integer getId() {
        return id;
    }

    public NotaFiscal getNotaFiscal() {
        return notaFiscal;
    }

    public void setNotaFiscal(NotaFiscal notaFiscal) {
        this.notaFiscal = notaFiscal;
    }

    public Integer getNumeroItem() {
        return numeroItem;
    }

    public void setNumeroItem(Integer numeroItem) {
        this.numeroItem = numeroItem;
    }

    public String getCodigoFornecedor() {
        return codigoFornecedor;
    }

    public void setCodigoFornecedor(String codigoFornecedor) {
        this.codigoFornecedor = codigoFornecedor;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getNcm() {
        return ncm;
    }

    public void setNcm(String ncm) {
        this.ncm = ncm;
    }

    public String getCfop() {
        return cfop;
    }

    public void setCfop(String cfop) {
        this.cfop = cfop;
    }

    public String getCfopEntrada() {
        return cfopEntrada;
    }

    public void setCfopEntrada(String cfopEntrada) {
        this.cfopEntrada = cfopEntrada;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public MovimentacaoEstoque getMovimentacao() {
        return movimentacao;
    }

    public void setMovimentacao(MovimentacaoEstoque movimentacao) {
        this.movimentacao = movimentacao;
    }

    public SituacaoItemNfe getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoItemNfe situacao) {
        this.situacao = situacao;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public void setValorFrete(BigDecimal valorFrete) {
        this.valorFrete = valorFrete;
    }

    public BigDecimal getValorSeguro() {
        return valorSeguro;
    }

    public void setValorSeguro(BigDecimal valorSeguro) {
        this.valorSeguro = valorSeguro;
    }

    public BigDecimal getValorOutras() {
        return valorOutras;
    }

    public void setValorOutras(BigDecimal valorOutras) {
        this.valorOutras = valorOutras;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
    }
}
