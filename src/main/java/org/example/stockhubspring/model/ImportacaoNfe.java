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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Importação de NF-e em preparação: guarda o XML enviado até o dono confirmar (ou até expirar).
 * O XML nunca é exposto em tela; o token (UUID aleatório) só vale para a loja que o criou.
 */
@Entity
@Table(name = "importacao_nfe")
public class ImportacaoNfe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID token;

    // Zerado (null) quando a importação é encerrada: o XML não fica guardado para sempre
    @Column(columnDefinition = "text")
    private String xml;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64, updatable = false)
    private String sha256;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "chave_acesso", length = 44, updatable = false)
    private String chaveAcesso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusImportacao status = StatusImportacao.PENDENTE;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private LocalDateTime criadaEm;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private LocalDateTime expiraEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_fiscal_id")
    private NotaFiscal notaFiscal;

    public Integer getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(String xml) {
        this.xml = xml;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getChaveAcesso() {
        return chaveAcesso;
    }

    public void setChaveAcesso(String chaveAcesso) {
        this.chaveAcesso = chaveAcesso;
    }

    public StatusImportacao getStatus() {
        return status;
    }

    public void setStatus(StatusImportacao status) {
        this.status = status;
    }

    public LocalDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(LocalDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public LocalDateTime getExpiraEm() {
        return expiraEm;
    }

    public void setExpiraEm(LocalDateTime expiraEm) {
        this.expiraEm = expiraEm;
    }

    public NotaFiscal getNotaFiscal() {
        return notaFiscal;
    }

    public void setNotaFiscal(NotaFiscal notaFiscal) {
        this.notaFiscal = notaFiscal;
    }
}
