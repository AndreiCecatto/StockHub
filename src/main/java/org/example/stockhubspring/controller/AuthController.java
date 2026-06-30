package org.example.stockhubspring.controller;

import jakarta.servlet.http.HttpSession;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.LoginService;
import org.example.stockhubspring.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final LoginService loginService;
    private final UsuarioService usuarioService;

    public AuthController(LoginService loginService, UsuarioService usuarioService) {
        this.loginService = loginService;
        this.usuarioService = usuarioService;
    }

    @GetMapping({"/login", "/index.jsp"})
    public String login(HttpSession session) {
        if (session.getAttribute("usuarioLogado") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String autenticar(@RequestParam String email,
                             @RequestParam String senha,
                             HttpSession session,
                             Model model) {
        Usuario usuario = loginService.autenticar(email, senha);
        if (usuario == null) {
            model.addAttribute("erro", "USUARIO OU SENHA INCORRETOS");
            model.addAttribute("email", email);
            return "login";
        }
        session.setAttribute("usuarioLogado", usuario);
        return "redirect:/dashboard";
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
                            HttpSession session,
                            Model model) {
        try {
            Usuario usuario = usuarioService.cadastrarLoja(nome, email, senha);
            session.setAttribute("usuarioLogado", usuario);
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            model.addAttribute("modoCadastro", true);
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("nome", nome);
            model.addAttribute("email", email);
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}