package org.example.stockhubspring.service;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Entrega o usuario logado a partir do SecurityContext. O Usuario devolvido
 * e leve (codigo, nome, email) e NUNCA contem a senha/hash.
 */
@Service
public class UsuarioAtualService {

    public Usuario getUsuarioAtual() {
        Usuario usuario = getUsuarioAtualOuNull();
        if (usuario == null) {
            throw new IllegalStateException("Usuario nao autenticado.");
        }
        return usuario;
    }

    public Usuario getUsuarioAtualOuNull() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioPrincipal principal)) {
            return null;
        }
        Usuario usuario = new Usuario();
        usuario.setCodigo(principal.getCodigo());
        usuario.setNome(principal.getNome());
        usuario.setEmail(principal.getUsername());
        return usuario;
    }
}
