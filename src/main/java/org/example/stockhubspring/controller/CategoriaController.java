package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final UsuarioAtualService usuarioAtualService;

    public CategoriaController(CategoriaService categoriaService, UsuarioAtualService usuarioAtualService) {
        this.categoriaService = categoriaService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("categorias", categoriaService.listar(busca, usuario.getCodigo()));
        model.addAttribute("busca", busca);
        return "categorias";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("categoria", new Categoria());
        return "categoria-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("categoria", categoriaService.buscarPorId(id, usuario.getCodigo()));
        return "categoria-form";
    }

    @PostMapping
    public String salvar(@ModelAttribute Categoria categoria) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        categoriaService.salvar(categoria, usuario);
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        categoriaService.excluir(id, usuario.getCodigo());
        return "redirect:/categorias";
    }
}
