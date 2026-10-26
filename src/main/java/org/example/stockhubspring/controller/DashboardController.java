package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.DashboardService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {

    private static final int LIMITE_PAINEL = 6;
    private static final int TAMANHO_LISTAS = 100;

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
        model.addAttribute("produtosRecentes", produtoService.listarRecentes(LIMITE_PAINEL, usuario.getCodigo()));
        model.addAttribute("produtosEstoqueBaixo",
                produtoService.listarEstoqueBaixo(usuario.getCodigo(), 1, LIMITE_PAINEL).getContent());
        return "dashboard";
    }

    @GetMapping("/estoque-baixo")
    public String estoqueBaixo(@RequestParam(defaultValue = "1") int pagina, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        publicarPagina(model, produtoService.listarEstoqueBaixo(usuario.getCodigo(), pagina, TAMANHO_LISTAS));
        return "estoque-baixo";
    }

    @GetMapping("/unidades")
    public String unidades(@RequestParam(defaultValue = "1") int pagina, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("resumo", dashboardService.buscarResumo(usuario.getCodigo()));
        publicarPagina(model, listarPorNome(pagina, usuario));
        return "unidades";
    }

    @GetMapping("/valor-estoque")
    public String valorEstoque(@RequestParam(defaultValue = "1") int pagina, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("resumo", dashboardService.buscarResumo(usuario.getCodigo()));
        publicarPagina(model, listarPorNome(pagina, usuario));
        return "valor-estoque";
    }

    private Page<Produto> listarPorNome(int pagina, Usuario usuario) {
        ProdutoFiltro filtro = ProdutoFiltro.de(null, null, null, "nome", "asc", pagina, TAMANHO_LISTAS);
        return produtoService.listarPagina(filtro, usuario.getCodigo());
    }

    /** Atributo "produtos" continua sendo a lista; os demais permitem paginar na tela. */
    private void publicarPagina(Model model, Page<Produto> pagina) {
        model.addAttribute("produtos", pagina.getContent());
        model.addAttribute("paginaAtual", pagina.getNumber() + 1);
        model.addAttribute("totalPaginas", Math.max(1, pagina.getTotalPages()));
        model.addAttribute("totalProdutosFiltro", pagina.getTotalElements());
    }
}
