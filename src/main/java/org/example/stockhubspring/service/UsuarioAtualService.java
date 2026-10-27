package org.example.stockhubspring.service;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.model.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Entrega a LOJA logada (com o membro e o papel de quem opera) a partir do SecurityContext.
 * O Usuario devolvido é leve (código da loja, nome da loja, e-mail, membroId, papel) e NUNCA
 * contém a senha/hash.
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
        usuario.setMembroId(principal.getMembroId());
        usuario.setPapel(principal.getPapel());
        return usuario;
    }
}
