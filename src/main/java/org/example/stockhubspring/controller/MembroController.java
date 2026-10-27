package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.MembroService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Equipe da loja (RN-61 a RN-65). O acesso é só de dono e gerente (SecurityConfig); o service confere de novo.
 * A senha digitada nunca volta para a tela.
 */
@Controller
@RequestMapping("/membros")
public class MembroController {

    private final MembroService membroService;
    private final UsuarioAtualService usuarioAtualService;

    public MembroController(MembroService membroService, UsuarioAtualService usuarioAtualService) {
        this.membroService = membroService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping
    public String lista(Model model) {
        Usuario atual = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("membros", membroService.listar(atual.getCodigo()));
        model.addAttribute("membroAtualId", atual.getMembroId());
        model.addAttribute("papeisEscolhiveis", papeisEscolhiveis(atual.getPapel()));
        return "membros";
    }

    @PostMapping
    public String criar(@RequestParam(required = false) String nome,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String senha,
                        @RequestParam(required = false) String papel,
                        RedirectAttributes redirectAttributes) {
        Usuario atual = usuarioAtualService.getUsuarioAtual();
        try {
            membroService.criar(atual.getCodigo(), atual.getPapel(), nome, email, senha, PapelMembro.de(papel));
            redirectAttributes.addFlashAttribute("mensagem", "Pessoa adicionada. Ela já pode entrar com o e-mail e a senha inicial.");
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            // Devolve o que foi digitado, menos a senha
            redirectAttributes.addFlashAttribute("nomeDigitado", nome);
            redirectAttributes.addFlashAttribute("emailDigitado", email);
            redirectAttributes.addFlashAttribute("papelDigitado", papel);
        }
        return "redirect:/membros";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario atual = usuarioAtualService.getUsuarioAtual();
        try {
            membroService.desativar(atual.getCodigo(), atual.getMembroId(), atual.getPapel(), id);
            redirectAttributes.addFlashAttribute("mensagem", "Pessoa desativada. Ela não consegue mais entrar.");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/membros";
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario atual = usuarioAtualService.getUsuarioAtual();
        try {
            membroService.reativar(atual.getCodigo(), atual.getPapel(), id);
            redirectAttributes.addFlashAttribute("mensagem", "Pessoa ativada de novo.");
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/membros";
    }

    private static PapelMembro[] papeisEscolhiveis(PapelMembro papelDoAtor) {
        return java.util.Arrays.stream(PapelMembro.values())
                .filter(PapelMembro::escolhivel)
                .filter(p -> p != PapelMembro.GERENTE || papelDoAtor == PapelMembro.DONO)
                .toArray(PapelMembro[]::new);
    }
}
