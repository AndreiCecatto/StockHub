package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.AnaliseEstoqueService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela de análises do estoque. Módulo ESTOQUE (sempre ligado, RN-43). Os dados vêm só da loja logada
 * (RN-45); o período é validado pelo service e qualquer valor fora de 30, 90 e 365 vira 30.
 */
@Controller
public class AnaliseController {

    private final UsuarioAtualService usuarioAtualService;
    private final AnaliseEstoqueService analiseService;

    public AnaliseController(UsuarioAtualService usuarioAtualService, AnaliseEstoqueService analiseService) {
        this.usuarioAtualService = usuarioAtualService;
        this.analiseService = analiseService;
    }

    @GetMapping("/analises")
    public String analises(@RequestParam(required = false) String periodo, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("painel", analiseService.montar(usuario.getCodigo(), lerPeriodo(periodo)));
        return "analises";
    }

    /** Texto que não é número (ex.: "abc") vira nulo e cai no período padrão, sem erro 400. */
    private static Integer lerPeriodo(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
