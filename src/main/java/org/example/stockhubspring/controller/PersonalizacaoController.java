package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.PersonalizacaoForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.PersonalizacaoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;

/** Tela de personalização do início e da lista de produtos (AD-19). A rota /conta/** já exige DONO ou GERENTE (RN-110). */
@Controller
public class PersonalizacaoController {

    private final PersonalizacaoService personalizacaoService;
    private final UsuarioAtualService usuarioAtualService;

    public PersonalizacaoController(PersonalizacaoService personalizacaoService,
                                    UsuarioAtualService usuarioAtualService) {
        this.personalizacaoService = personalizacaoService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping("/conta/personalizacao")
    public String formulario(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        Integer loja = usuario.getCodigo();
        model.addAttribute("blocosInicio", personalizacaoService.blocosAplicaveis(loja, TelaPersonalizavel.INICIO));
        model.addAttribute("estadoBlocos", personalizacaoService.blocosVisiveis(loja, TelaPersonalizavel.INICIO));
        model.addAttribute("blocosProdutos", personalizacaoService.blocosAplicaveis(loja, TelaPersonalizavel.PRODUTOS));
        model.addAttribute("estadoColunas", personalizacaoService.blocosVisiveis(loja, TelaPersonalizavel.PRODUTOS));
        return "personalizacao";
    }

    /** Salva uma tela por vez: /conta/personalizacao/inicio ou /conta/personalizacao/produtos. */
    @PostMapping("/conta/personalizacao/{tela}")
    public String salvar(@PathVariable String tela, @ModelAttribute PersonalizacaoForm form,
                         RedirectAttributes redirectAttributes) {
        TelaPersonalizavel alvo = TelaPersonalizavel.deSlug(tela);
        if (alvo == null) {
            redirectAttributes.addFlashAttribute("erro", "Tela inválida para personalizar.");
            return "redirect:/conta/personalizacao";
        }
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        try {
            personalizacaoService.salvar(usuario.getCodigo(), usuario.getPapel(), usuario.getMembroId(),
                    alvo, new HashSet<>(form.getBlocos()));
            redirectAttributes.addFlashAttribute("mensagem",
                    alvo == TelaPersonalizavel.PRODUTOS ? "Lista de produtos personalizada." : "Início personalizado.");
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/conta/personalizacao";
    }
}
