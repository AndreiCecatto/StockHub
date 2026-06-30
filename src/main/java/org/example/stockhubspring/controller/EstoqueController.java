package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;
    private final UsuarioAtualService usuarioAtualService;

    public EstoqueController(EstoqueService estoqueService,
                             ProdutoService produtoService,
                             UsuarioAtualService usuarioAtualService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping
    public String movimentacoes(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("produtos", produtoService.listar(null, usuario.getCodigo()));
        model.addAttribute("movimentacoes", estoqueService.listarMovimentacoes(usuario.getCodigo()));
        model.addAttribute("tipos", TipoMovimentacao.values());
        return "estoque";
    }

    @PostMapping("/movimentar")
    public String movimentar(@RequestParam Integer produtoId,
                             @RequestParam TipoMovimentacao tipo,
                             @RequestParam int quantidade,
                             @RequestParam(required = false) String observacao,
                             RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        try {
            estoqueService.movimentar(produtoId, tipo, quantidade, observacao, usuario);
            redirectAttributes.addFlashAttribute("mensagem", "Movimentacao registrada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/estoque";
    }
}
