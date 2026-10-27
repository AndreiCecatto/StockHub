package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.example.stockhubspring.service.UsuarioService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Dados da conta da loja logada. Por enquanto só o tipo e o ramo (RN-43/RN-44). */
@Controller
public class ContaController {

    private final UsuarioService usuarioService;
    private final UsuarioAtualService usuarioAtualService;

    public ContaController(UsuarioService usuarioService, UsuarioAtualService usuarioAtualService) {
        this.usuarioService = usuarioService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @PostMapping("/conta/tipo-loja")
    public String alterarTipoLoja(@RequestParam(required = false) String tipoLoja,
                                  @RequestParam(required = false) String subtipo,
                                  RedirectAttributes redirectAttributes) {
        TipoLoja tipo = TipoLoja.de(tipoLoja);
        try {
            if (tipo == null) {
                throw new RegraNegocioException("Escolha um tipo de loja válido.");
            }
            Subtipo ramo;
            try {
                ramo = Subtipo.de(subtipo);
            } catch (IllegalArgumentException e) {
                throw new RegraNegocioException("Escolha um ramo válido para o tipo de loja.");
            }
            usuarioService.alterarTipoLoja(usuarioAtualService.getUsuarioAtual().getCodigo(), tipo, ramo);
            redirectAttributes.addFlashAttribute("mensagem", "Tipo de loja atualizado.");
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/dashboard";
    }
}
