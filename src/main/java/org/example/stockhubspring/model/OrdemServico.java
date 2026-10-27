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

/** Ordem de serviço de uma loja de oficina (RN-77 a RN-90). O total fica gravado e é recalculado a cada mudança. */
@Entity
@Table(name = "ordem_servico")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false)
    private int numero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parceiro_id", nullable = false, updatable = false)
    private Parceiro cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private EquipamentoCliente equipamento;

    @Column(nullable = false, length = 1000)
    private String defeito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private MembroLoja responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOs status = StatusOs.ORCAMENTO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal desconto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "garantia_dias", nullable = false)
    private int garantiaDias;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;
    @Column(name = "aprovada_em")
    private LocalDateTime aprovadaEm;
    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;
    @Column(name = "concluida_em")
    private LocalDateTime concluidaEm;
    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;
    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "criada_por_id", updatable = false)
    private Integer criadaPorId;

    @PrePersist
    void definirCriadaEm() {
        if (criadaEm == null) {
            criadaEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        }
    }

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public Parceiro getCliente() { return cliente; }
    public void setCliente(Parceiro cliente) { this.cliente = cliente; }
    public EquipamentoCliente getEquipamento() { return equipamento; }
    public void setEquipamento(EquipamentoCliente equipamento) { this.equipamento = equipamento; }
    public String getDefeito() { return defeito; }
    public void setDefeito(String defeito) { this.defeito = defeito; }
    public MembroLoja getResponsavel() { return responsavel; }
    public void setResponsavel(MembroLoja responsavel) { this.responsavel = responsavel; }
    public StatusOs getStatus() { return status; }
    public void setStatus(StatusOs status) { this.status = status; }
    public BigDecimal getDesconto() { return desconto; }
    public void setDesconto(BigDecimal desconto) { this.desconto = desconto; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public int getGarantiaDias() { return garantiaDias; }
    public void setGarantiaDias(int garantiaDias) { this.garantiaDias = garantiaDias; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
    public LocalDateTime getAprovadaEm() { return aprovadaEm; }
    public void setAprovadaEm(LocalDateTime v) { this.aprovadaEm = v; }
    public LocalDateTime getIniciadaEm() { return iniciadaEm; }
    public void setIniciadaEm(LocalDateTime v) { this.iniciadaEm = v; }
    public LocalDateTime getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(LocalDateTime v) { this.concluidaEm = v; }
    public LocalDateTime getEntregueEm() { return entregueEm; }
    public void setEntregueEm(LocalDateTime v) { this.entregueEm = v; }
    public LocalDateTime getCanceladaEm() { return canceladaEm; }
    public void setCanceladaEm(LocalDateTime v) { this.canceladaEm = v; }
    public Integer getCriadaPorId() { return criadaPorId; }
    public void setCriadaPorId(Integer criadaPorId) { this.criadaPorId = criadaPorId; }
}
