package org.example.stockhubspring.controller;

import jakarta.validation.Valid;
import org.example.stockhubspring.dto.CadastroForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.example.stockhubspring.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Telas de login e cadastro. O POST /login e o POST /logout são tratados
 * pelo Spring Security (ver SecurityConfig), não por este controller.
 */
@Controller
public class AuthController {

    private final UsuarioService usuarioService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;

    public AuthController(UsuarioService usuarioService,
                          UsuarioAtualService usuarioAtualService,
                          ErrosFormulario errosFormulario) {
        this.usuarioService = usuarioService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
    }

    @GetMapping("/login")
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
            model.addAttribute("sucesso", "Você saiu do sistema.");
        }
        if (cadastrado != null) {
            model.addAttribute("sucesso", "Cadastro concluído. Entre com seu e-mail e senha.");
        }
        return "login";
    }

    @GetMapping("/cadastro")
    public String cadastro(Model model) {
        model.addAttribute("modoCadastro", true);
        return "login";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastroForm") CadastroForm form,
                            BindingResult resultado,
                            Model model) {
        if (!resultado.hasErrors()) {
            try {
                usuarioService.cadastrarLoja(form.getNome(), form.getEmail(), form.getSenha(),
                        form.tipoConvertido(), form.subtipoConvertido());
                return "redirect:/login?cadastrado";
            } catch (RegraNegocioException e) {
                model.addAttribute("erro", e.getMessage());
            }
        } else {
            errosFormulario.publicar(resultado, model);
        }
        // Volta ao mesmo formulário, sem devolver a senha digitada
        form.setSenha(null);
        model.addAttribute("modoCadastro", true);
        model.addAttribute("nome", form.getNome());
        model.addAttribute("email", form.getEmail());
        model.addAttribute("tipoEscolhido", form.getTipoLoja());
        model.addAttribute("subtipoEscolhido", form.getSubtipo());
        return "login";
    }
}
