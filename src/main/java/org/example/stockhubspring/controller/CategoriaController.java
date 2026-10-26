package org.example.stockhubspring.controller;

import jakarta.validation.Valid;
import org.example.stockhubspring.dto.CategoriaForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Só recebe, valida e redireciona; as regras ficam no {@link CategoriaService}. */
@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;

    public CategoriaController(CategoriaService categoriaService,
                               UsuarioAtualService usuarioAtualService,
                               ErrosFormulario errosFormulario) {
        this.categoriaService = categoriaService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
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
        model.addAttribute("categoria", new CategoriaForm());
        return "categoria-form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("categoria", CategoriaForm.de(categoriaService.buscarPorId(id, usuario.getCodigo())));
        return "categoria-form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("categoria") CategoriaForm categoria,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            // Volta ao MESMO formulário, com o que o usuário digitou e os erros
            errosFormulario.publicar(resultado, model);
            return "categoria-form";
        }
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        boolean nova = categoria.getId() == null;
        try {
            categoriaService.salvar(categoria, usuario);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "categoria-form";
        }
        redirectAttributes.addFlashAttribute("mensagem", nova ? "Categoria cadastrada." : "Categoria atualizada.");
        return "redirect:/categorias";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        try {
            categoriaService.excluir(id, usuario.getCodigo());
            redirectAttributes.addFlashAttribute("mensagem", "Categoria excluída.");
        } catch (RegraNegocioException e) {
            // RN-09: mensagem diz quantos produtos usam a categoria
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/categorias";
    }
}
