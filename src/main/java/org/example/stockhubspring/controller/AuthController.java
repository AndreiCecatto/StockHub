package org.example.stockhubspring.controller;

import org.example.stockhubspring.service.UsuarioAtualService;
import org.example.stockhubspring.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas de login e cadastro. O POST /login e o POST /logout sao tratados
 * pelo Spring Security (ver SecurityConfig), nao por este controller.
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioAtualService usuarioAtualService;

    public AuthController(UsuarioService usuarioService, UsuarioAtualService usuarioAtualService) {
        this.usuarioService = usuarioService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping({"/login", "/index.jsp"})
    public String login(@RequestParam(required = false) String erro,
                        @RequestParam(required = false) String saiu,
                        @RequestParam(required = false) String cadastrado,
                        Model model) {
        if (usuarioAtualService.getUsuarioAtualOuNull() != null) {
            return "redirect:/dashboard";
        }
        if (erro != null) {
            model.addAttribute("erro", "E-mail ou senha incorretos.");
        }
        if (saiu != null) {
            model.addAttribute("sucesso", "Voce saiu do sistema.");
        }
        if (cadastrado != null) {
            model.addAttribute("sucesso", "Cadastro concluido. Entre com seu e-mail e senha.");
        }
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("modoCadastro", true);
        return "login";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@RequestParam String nome,
                            @RequestParam String email,
                            @RequestParam String senha,
                            Model model) {
        try {
            usuarioService.cadastrarLoja(nome, email, senha);
            return "redirect:/login?cadastrado";
        } catch (IllegalArgumentException e) {
            model.addAttribute("modoCadastro", true);
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("nome", nome);
            model.addAttribute("email", email);
            return "login";
        }
    }
}
