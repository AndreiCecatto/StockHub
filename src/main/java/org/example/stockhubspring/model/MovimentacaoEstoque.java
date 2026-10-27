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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Movimentação de estoque: registro imutável e auditável (RN-06). Não há edição nem exclusão;
 * um erro se corrige com outra movimentação.
 */
@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo", nullable = false, updatable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoMovimentacao tipo;

    // Para ENTRADA/SAIDA é a quantidade movimentada; para AJUSTE é a contagem final.
    @Column(nullable = false, updatable = false)
    private int quantidade;

    @Column(name = "quantidade_anterior", nullable = false, updatable = false)
    private int quantidadeAnterior;

    @Column(name = "quantidade_nova", nullable = false, updatable = false)
    private int quantidadeNova;

    @Column(length = 255, updatable = false)
    private String observacao;

    // Documento que originou a movimentação (opcional): os dois campos vêm juntos ou ficam nulos
    @Enumerated(EnumType.STRING)
    @Column(name = "origem_tipo", length = 20, updatable = false)
    private OrigemMovimentacao origemTipo;

    @Column(name = "origem_id", updatable = false)
    private Integer origemId;

    // Custo unitário do produto no momento da movimentação (base das análises de caixa, RN-45)
    @Column(name = "custo_unitario", nullable = false, precision = 14, scale = 4, updatable = false)
    private BigDecimal custoUnitario = BigDecimal.ZERO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void definirCriadoEm() {
        if (criadoEm == null) {
            // Horário de parede de São Paulo: é o fuso que define o "dia" do gráfico de movimentos
            criadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public int getQuantidadeAnterior() {
        return quantidadeAnterior;
    }

    public void setQuantidadeAnterior(int quantidadeAnterior) {
        this.quantidadeAnterior = quantidadeAnterior;
    }

    public int getQuantidadeNova() {
        return quantidadeNova;
    }

    public void setQuantidadeNova(int quantidadeNova) {
        this.quantidadeNova = quantidadeNova;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public OrigemMovimentacao getOrigemTipo() {
        return origemTipo;
    }

    public Integer getOrigemId() {
        return origemId;
    }

    /** Define a origem na criação (a coluna não é atualizável: movimentação é imutável, RN-06). */
    public void definirOrigem(OrigemMovimentacao tipo, Integer id) {
        this.origemTipo = tipo;
        this.origemId = id;
    }

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario == null ? BigDecimal.ZERO : custoUnitario;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
