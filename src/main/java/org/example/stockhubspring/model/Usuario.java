package org.example.stockhubspring.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.example.stockhubspring.util.CnpjValido;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer codigo;

    @NotBlank(message = "Informe o nome da loja.")
    private String nome;

    @Email(message = "Informe um email valido.")
    @NotBlank(message = "Informe o email.")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Informe a senha.")
    private String senha;

    private boolean ativo = true;

    // CNPJ da loja (opcional, só dígitos, com dígito verificador válido quando informado)
    @CnpjValido
    @Column(length = 14)
    private String cnpj;

    // Tipo e ramo da loja (V6): só decidem quais módulos e menus aparecem
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_loja", nullable = false, length = 20)
    private TipoLoja tipoLoja = TipoLoja.ESTOQUE;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Subtipo subtipo;

    // Quem está operando a loja na sessão (não vai ao banco): só preenchido pelo UsuarioAtualService (AD-13)
    @Transient
    private Integer membroId;

    @Transient
    private PapelMembro papel;

    public Integer getMembroId() {
        return membroId;
    }

    public void setMembroId(Integer membroId) {
        this.membroId = membroId;
    }

    public PapelMembro getPapel() {
        return papel;
    }

    public void setPapel(PapelMembro papel) {
        this.papel = papel;
    }

    public TipoLoja getTipoLoja() {
        return tipoLoja;
    }

    public void setTipoLoja(TipoLoja tipoLoja) {
        this.tipoLoja = tipoLoja;
    }

    public Subtipo getSubtipo() {
        return subtipo;
    }

    public void setSubtipo(Subtipo subtipo) {
        this.subtipo = subtipo;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
