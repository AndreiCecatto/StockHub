package org.example.stockhubspring.config;

import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Pessoa logada, guardada na sessão (AD-13). {@code codigo} é o código da LOJA (a conta {@link Usuario}),
 * a chave de isolamento de todos os dados (RN-01); {@code membroId} e {@code papel} dizem QUEM está operando.
 * O hash da senha existe só durante a autenticação e é apagado em seguida (CredentialsContainer),
 * então nunca fica na sessão.
 */
public class UsuarioPrincipal implements UserDetails, CredentialsContainer, Serializable {

    private static final long serialVersionUID = 2L;

    private final Integer codigo;
    private final String nome;
    private final String email;
    private final boolean ativo;
    private final Integer membroId;
    private final String membroNome;
    private final PapelMembro papel;
    private String senhaHash;

    /** Conta sem membro associado: age como DONO da loja (compatível com o modelo anterior). */
    public UsuarioPrincipal(Usuario loja) {
        this.codigo = loja.getCodigo();
        this.nome = loja.getNome();
        this.email = loja.getEmail();
        this.ativo = loja.isAtivo();
        this.membroId = null;
        this.membroNome = loja.getNome();
        this.papel = PapelMembro.DONO;
        this.senhaHash = loja.getSenha();
    }

    /** Membro logado na loja dele: e-mail e senha são os do membro; o código é o da loja. */
    public UsuarioPrincipal(Usuario loja, MembroLoja membro) {
        this.codigo = loja.getCodigo();
        this.nome = loja.getNome();
        this.email = membro.getEmail();
        this.ativo = membro.isAtivo() && loja.isAtivo();
        this.membroId = membro.getId();
        this.membroNome = membro.getNome();
        this.papel = membro.getPapel();
        this.senhaHash = membro.getSenha();
    }

    /** Código da loja. */
    public Integer getCodigo() {
        return codigo;
    }

    /** Nome da loja. */
    public String getNome() {
        return nome;
    }

    public Integer getMembroId() {
        return membroId;
    }

    public String getMembroNome() {
        return membroNome;
    }

    public PapelMembro getPapel() {
        return papel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    @Override
    public void eraseCredentials() {
        this.senhaHash = null;
    }
}
