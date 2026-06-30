package org.example.stockhubspring.service;

import jakarta.servlet.http.HttpSession;
import org.example.stockhubspring.model.Usuario;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAtualService {

    private final HttpSession session;

    public UsuarioAtualService(HttpSession session) {
        this.session = session;
    }

    public Usuario getUsuarioAtual() {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            throw new IllegalStateException("Usuario nao autenticado.");
        }
        return usuario;
    }

    public Usuario getUsuarioAtualOuNull() {
        return (Usuario) session.getAttribute("usuarioLogado");
    }
}