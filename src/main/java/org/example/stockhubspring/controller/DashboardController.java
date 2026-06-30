package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.DashboardService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UsuarioAtualService usuarioAtualService;
    private final ProdutoService produtoService;
    private final DashboardService dashboardService;

    public DashboardController(UsuarioAtualService usuarioAtualService,
                               ProdutoService produtoService,
                               DashboardService dashboardService) {
        this.usuarioAtualService = usuarioAtualService;
        this.produtoService = produtoService;
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("resumo", dashboardService.buscarResumo(usuario.getCodigo()));
        model.addAttribute("produtosRecentes", produtoService.listarRecentes(6, usuario.getCodigo()));
        model.addAttribute("produtosEstoqueBaixo", produtoService.listarEstoqueBaixo(usuario.getCodigo()).stream().limit(6).toList());
        return "dashboard";
    }

    @GetMapping("/estoque-baixo")
    public String estoqueBaixo(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("produtos", produtoService.listarEstoqueBaixo(usuario.getCodigo()));
        return "estoque-baixo";
    }

    @GetMapping("/unidades")
    public String unidades(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("resumo", dashboardService.buscarResumo(usuario.getCodigo()));
        model.addAttribute("produtos", produtoService.listarFiltrado(null, null, null, "nome", "asc", usuario.getCodigo()));
        return "unidades";
    }

    @GetMapping("/valor-estoque")
    public String valorEstoque(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("resumo", dashboardService.buscarResumo(usuario.getCodigo()));
        model.addAttribute("produtos", produtoService.listarFiltrado(null, null, null, "nome", "asc", usuario.getCodigo()));
        return "valor-estoque";
    }
}
