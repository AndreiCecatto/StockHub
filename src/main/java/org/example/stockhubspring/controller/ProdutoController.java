package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final UsuarioAtualService usuarioAtualService;

    public ProdutoController(ProdutoService produtoService,
                             CategoriaService categoriaService,
                             UsuarioAtualService usuarioAtualService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String acao,
                         @RequestParam(required = false) Integer id,
                         @RequestParam(required = false) String busca,
                         @RequestParam(required = false) Integer categoriaId,
                         @RequestParam(required = false) String nivelEstoque,
                         @RequestParam(defaultValue = "id") String ordenacao,
                         @RequestParam(defaultValue = "desc") String direcao,
                         @RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(defaultValue = "8") int tamanho,
                         Model model) {
        if ("novo".equals(acao)) {
            return "redirect:/produtos/novo";
        }
        if ("editar".equals(acao) && id != null) {
            return "redirect:/produtos/" + id + "/editar";
        }
        if ("visualizar".equals(acao) && id != null) {
            return "redirect:/produtos/" + id;
        }

        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        List<Produto> filtrados = produtoService.listarFiltrado(busca, categoriaId, nivelEstoque, ordenacao, direcao, usuario.getCodigo());
        int totalProdutos = filtrados.size();
        int totalPaginas = Math.max(1, (int) Math.ceil(totalProdutos / (double) tamanho));
        int paginaAtual = Math.max(1, Math.min(pagina, totalPaginas));
        int inicio = Math.min((paginaAtual - 1) * tamanho, totalProdutos);
        int fim = Math.min(inicio + tamanho, totalProdutos);

        model.addAttribute("produtos", filtrados.subList(inicio, fim));
        model.addAttribute("categorias", categoriaService.listar(null, usuario.getCodigo()));
        model.addAttribute("busca", busca);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("nivelEstoque", nivelEstoque);
        model.addAttribute("ordenacao", ordenacao);
        model.addAttribute("direcao", direcao);
        model.addAttribute("direcaoInversa", "asc".equals(direcao) ? "desc" : "asc");
        model.addAttribute("paginaAtual", paginaAtual);
        model.addAttribute("totalPaginas", totalPaginas);
        model.addAttribute("totalProdutosFiltro", totalProdutos);
        model.addAttribute("tamanho", tamanho);
        model.addAttribute("parametrosFiltros", montarParametros(busca, categoriaId, nivelEstoque, ordenacao, tamanho));
        return "produtos";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("produto", new Produto());
        model.addAttribute("categorias", categoriaService.listar(null, usuario.getCodigo()));
        return "produto-form";
    }

    @GetMapping("/{id}")
    public String visualizar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("produto", produtoService.buscarPorId(id, usuario.getCodigo()));
        return "produto-detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("produto", produtoService.buscarPorId(id, usuario.getCodigo()));
        model.addAttribute("categorias", categoriaService.listar(null, usuario.getCodigo()));
        return "produto-form";
    }

    @PostMapping
    public String salvar(@ModelAttribute Produto produto, @RequestParam Integer categoriaId) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        produtoService.salvar(produto, categoriaId, usuario);
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        produtoService.excluir(id, usuario.getCodigo());
        return "redirect:/produtos";
    }

    private String montarParametros(String busca, Integer categoriaId, String nivelEstoque, String ordenacao, int tamanho) {
        StringBuilder parametros = new StringBuilder();
        adicionarParametro(parametros, "busca", busca);
        adicionarParametro(parametros, "categoriaId", categoriaId == null ? null : categoriaId.toString());
        adicionarParametro(parametros, "nivelEstoque", nivelEstoque);
        adicionarParametro(parametros, "ordenacao", ordenacao);
        adicionarParametro(parametros, "tamanho", String.valueOf(tamanho));
        return parametros.toString();
    }

    private void adicionarParametro(StringBuilder parametros, String nome, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        parametros.append("&")
                .append(nome)
                .append("=")
                .append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
    }
}
