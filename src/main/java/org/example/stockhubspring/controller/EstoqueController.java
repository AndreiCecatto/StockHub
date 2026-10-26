package org.example.stockhubspring.controller;

import jakarta.validation.Valid;
import org.example.stockhubspring.dto.MovimentacaoForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Só recebe, valida e redireciona; as regras de saldo ficam no {@link EstoqueService}. */
@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private static final int TAMANHO_PAGINA = 50;

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;

    public EstoqueController(EstoqueService estoqueService,
                             ProdutoService produtoService,
                             UsuarioAtualService usuarioAtualService,
                             ErrosFormulario errosFormulario) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
    }

    @GetMapping
    public String movimentacoes(@RequestParam(defaultValue = "1") int pagina, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        carregarTela(model, usuario, pagina);
        return "estoque";
    }

    @PostMapping("/movimentar")
    public String movimentar(@Valid @ModelAttribute("movimentacaoForm") MovimentacaoForm form,
                             BindingResult resultado,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        if (resultado.hasErrors()) {
            // Volta à MESMA tela, com os erros
            errosFormulario.publicar(resultado, model);
            carregarTela(model, usuario, 1);
            return "estoque";
        }
        try {
            MovimentacaoEstoque movimentacao = estoqueService.movimentar(
                    form.getProdutoId(), form.getTipo(), form.getQuantidade(), form.getObservacao(), usuario);
            redirectAttributes.addFlashAttribute("mensagem", "Movimentação registrada: o saldo passou de "
                    + movimentacao.getQuantidadeAnterior() + " para " + movimentacao.getQuantidadeNova() + " un.");
            return "redirect:/estoque";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            carregarTela(model, usuario, 1);
            return "estoque";
        }
    }

    private void carregarTela(Model model, Usuario usuario, int pagina) {
        Page<MovimentacaoEstoque> historico = estoqueService.listarMovimentacoes(usuario.getCodigo(), pagina, TAMANHO_PAGINA);
        model.addAttribute("produtos", produtoService.listarParaSelecao(usuario.getCodigo()));
        model.addAttribute("movimentacoes", historico.getContent());
        model.addAttribute("paginaAtual", historico.getNumber() + 1);
        model.addAttribute("totalPaginas", Math.max(1, historico.getTotalPages()));
        model.addAttribute("totalMovimentacoes", historico.getTotalElements());
        model.addAttribute("tipos", TipoMovimentacao.values());
    }
}
