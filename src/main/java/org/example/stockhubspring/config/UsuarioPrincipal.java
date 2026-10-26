package org.example.stockhubspring.config;

import org.example.stockhubspring.model.Usuario;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * Usuario guardado na sessao. Mantem so o necessario (codigo, nome e email);
 * o hash da senha existe apenas durante a autenticacao e e apagado em seguida
 * (CredentialsContainer), entao nunca fica na sessao.
 */
public class UsuarioPrincipal implements UserDetails, CredentialsContainer, Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer codigo;
    private final String nome;
    private final String email;
    private final boolean ativo;
    private String senhaHash;

    public UsuarioPrincipal(Usuario usuario) {
        this.codigo = usuario.getCodigo();
        this.nome = usuario.getNome();
        this.email = usuario.getEmail();
        this.ativo = usuario.isAtivo();
        this.senhaHash = usuario.getSenha();
    }

    public Integer getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
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
