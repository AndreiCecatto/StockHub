package org.example.stockhubspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/** Preferência de visibilidade de um bloco por loja e tela (AD-19). Ausência de linha = visível (RN-109). */
@Entity
@Table(name = "preferencia_bloco")
@IdClass(PreferenciaBloco.Chave.class)
public class PreferenciaBloco {

    @Id
    @Column(name = "usuario_codigo")
    private Integer usuarioCodigo;

    @Id
    @Column(length = 30)
    private String tela;

    @Id
    @Column(length = 40)
    private String bloco;

    @Column(nullable = false)
    private boolean visivel;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "atualizado_por")
    private Long atualizadoPor;

    public Integer getUsuarioCodigo() { return usuarioCodigo; }
    public String getTela() { return tela; }
    public String getBloco() { return bloco; }
    public boolean isVisivel() { return visivel; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public Long getAtualizadoPor() { return atualizadoPor; }

    /** Chave composta (loja, tela, bloco). */
    public static class Chave implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer usuarioCodigo;
        private String tela;
        private String bloco;

        public Chave() {
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Chave c && Objects.equals(usuarioCodigo, c.usuarioCodigo)
                    && Objects.equals(tela, c.tela) && Objects.equals(bloco, c.bloco);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioCodigo, tela, bloco);
        }
    }
}
