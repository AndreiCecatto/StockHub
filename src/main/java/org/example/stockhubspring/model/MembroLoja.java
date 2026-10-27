package org.example.stockhubspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Pessoa que opera uma loja (AD-13). A loja é a conta {@link Usuario}; {@code lojaCodigo} guarda o
 * código dela. O login é feito com o e-mail e a senha (BCrypt) do membro.
 */
@Entity
@Table(name = "membro_loja")
public class MembroLoja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "loja_codigo", nullable = false, updatable = false)
    private Integer lojaCodigo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelMembro papel;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void definirCriadoEm() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getLojaCodigo() {
        return lojaCodigo;
    }

    public void setLojaCodigo(Integer lojaCodigo) {
        this.lojaCodigo = lojaCodigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public PapelMembro getPapel() {
        return papel;
    }

    public void setPapel(PapelMembro papel) {
        this.papel = papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
