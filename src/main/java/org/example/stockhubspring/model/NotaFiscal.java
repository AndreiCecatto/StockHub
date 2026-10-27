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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** NF-e de compra já importada. Única por loja e chave de acesso (idempotência). Imutável depois de criada. */
@Entity
@Table(name = "nota_fiscal")
public class NotaFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo", nullable = false, updatable = false)
    private Usuario usuario;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "chave_acesso", nullable = false, length = 44, updatable = false)
    private String chaveAcesso;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer modelo;

    private Integer serie;

    private Long numero;

    @Column(name = "natureza_operacao", length = 120)
    private String naturezaOperacao;

    @Column(name = "emitida_em")
    private LocalDateTime emitidaEm;

    // Emitente = fornecedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parceiro_id")
    private Parceiro parceiro;

    @Column(name = "cnpj_destinatario", length = 14)
    private String cnpjDestinatario;

    @Column(name = "valor_produtos", precision = 14, scale = 2)
    private BigDecimal valorProdutos;

    @Column(name = "valor_frete", precision = 14, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "valor_seguro", precision = 14, scale = 2)
    private BigDecimal valorSeguro;

    @Column(name = "valor_outras", precision = 14, scale = 2)
    private BigDecimal valorOutras;

    @Column(name = "valor_desconto", precision = 14, scale = 2)
    private BigDecimal valorDesconto;

    @Column(name = "valor_total", precision = 14, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "importada_em", nullable = false, updatable = false)
    private LocalDateTime importadaEm;

    public Integer getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public Integer getModelo() {
        return modelo;
    }

    public void setModelo(Integer modelo) {
        this.modelo = modelo;
    }

    public Integer getSerie() {
        return serie;
    }

    public void setSerie(Integer serie) {
        this.serie = serie;
    }

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public String getNaturezaOperacao() {
        return naturezaOperacao;
    }

    public void setNaturezaOperacao(String naturezaOperacao) {
        this.naturezaOperacao = naturezaOperacao;
    }

    public LocalDateTime getEmitidaEm() {
        return emitidaEm;
    }

    public void setEmitidaEm(LocalDateTime emitidaEm) {
        this.emitidaEm = emitidaEm;
    }

    public Parceiro getParceiro() {
        return parceiro;
    }

    public void setParceiro(Parceiro parceiro) {
        this.parceiro = parceiro;
    }

    public String getCnpjDestinatario() {
        return cnpjDestinatario;
    }

    public void setCnpjDestinatario(String cnpjDestinatario) {
        this.cnpjDestinatario = cnpjDestinatario;
    }

    public BigDecimal getValorProdutos() {
        return valorProdutos;
    }

    public void setValorProdutos(BigDecimal valorProdutos) {
        this.valorProdutos = valorProdutos;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public void setValorFrete(BigDecimal valorFrete) {
        this.valorFrete = valorFrete;
    }

    public BigDecimal getValorDesconto() {
        return valorDesconto;
    }

    public void setValorDesconto(BigDecimal valorDesconto) {
        this.valorDesconto = valorDesconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public LocalDateTime getImportadaEm() {
        return importadaEm;
    }

    public void setImportadaEm(LocalDateTime importadaEm) {
        this.importadaEm = importadaEm;
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
}
