package org.example.stockhubspring.controller;

import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de apresentação pública do produto (rota "/"). É estática: não coloca dados da loja
 * nem do usuário no model. Quem já está logado vai direto para o painel.
 */
@Controller
public class ApresentacaoController {

    private final UsuarioAtualService usuarioAtualService;

    public ApresentacaoController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping("/")
    public String apresentacao() {
        if (usuarioAtualService.getUsuarioAtualOuNull() != null) {
            return "redirect:/dashboard";
        }
        return "apresentacao";
    }
}
