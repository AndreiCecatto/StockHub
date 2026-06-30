package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final UsuarioAtualService usuarioAtualService;

    public GlobalModelAdvice(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @ModelAttribute("usuarioLogado")
    public Usuario usuarioLogado() {
        return usuarioAtualService.getUsuarioAtualOuNull();
    }
}