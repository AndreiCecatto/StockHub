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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;

/** Tela de personalização do início (AD-19). A rota /conta/** já exige DONO ou GERENTE (RN-110). */
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
        model.addAttribute("blocosInicio",
                personalizacaoService.blocosAplicaveis(usuario.getCodigo(), TelaPersonalizavel.INICIO));
        model.addAttribute("estadoBlocos",
                personalizacaoService.blocosVisiveis(usuario.getCodigo(), TelaPersonalizavel.INICIO));
        return "personalizacao";
    }

    @PostMapping("/conta/personalizacao/inicio")
    public String salvarInicio(@ModelAttribute PersonalizacaoForm form, RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        try {
            personalizacaoService.salvar(usuario.getCodigo(), usuario.getPapel(), usuario.getMembroId(),
                    TelaPersonalizavel.INICIO, new HashSet<>(form.getBlocos()));
            redirectAttributes.addFlashAttribute("mensagem", "Início personalizado.");
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/conta/personalizacao";
    }
}
