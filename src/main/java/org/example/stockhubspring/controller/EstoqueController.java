package org.example.stockhubspring.controller;

import jakarta.validation.Valid;
import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.dto.MovimentacaoForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ExportacaoService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Só recebe, valida e redireciona; as regras de saldo ficam no {@link EstoqueService}. */
@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private static final int TAMANHO_PAGINA = 50;

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;
    private final ExportacaoService exportacaoService;

    public EstoqueController(EstoqueService estoqueService,
                             ProdutoService produtoService,
                             UsuarioAtualService usuarioAtualService,
                             ErrosFormulario errosFormulario,
                             ExportacaoService exportacaoService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
        this.exportacaoService = exportacaoService;
    }

    /**
     * Histórico com filtros opcionais: {@code produto} (código), {@code tipo} (ENTRADA, SAIDA ou AJUSTE),
     * {@code de} e {@code ate} (aaaa-MM-dd, inclusivos). Valor inválido é ignorado.
     */
    @GetMapping
    public String movimentacoes(@RequestParam(defaultValue = "1") int pagina,
                                @RequestParam(required = false) String produto,
                                @RequestParam(required = false) String tipo,
                                @RequestParam(required = false) String de,
                                @RequestParam(required = false) String ate,
                                Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        carregarTela(model, usuario, MovimentacaoFiltro.de(produto, tipo, de, ate), pagina);
        return "estoque";
    }

    /** Exporta o histórico da loja em CSV, com os mesmos filtros da tela (sem paginação, com limite de linhas). */
    @GetMapping("/exportar.csv")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) String produto,
                                              @RequestParam(required = false) String tipo,
                                              @RequestParam(required = false) String de,
                                              @RequestParam(required = false) String ate) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        MovimentacaoFiltro filtro = MovimentacaoFiltro.de(produto, tipo, de, ate);
        return RespostaCsv.de(exportacaoService.exportarMovimentacoes(filtro, usuario.getCodigo()));
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
            carregarTela(model, usuario, MovimentacaoFiltro.VAZIO, 1);
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
            carregarTela(model, usuario, MovimentacaoFiltro.VAZIO, 1);
            return "estoque";
        }
    }

    private void carregarTela(Model model, Usuario usuario, MovimentacaoFiltro filtro, int pagina) {
        Page<MovimentacaoEstoque> historico =
                estoqueService.listarMovimentacoes(usuario.getCodigo(), filtro, pagina, TAMANHO_PAGINA);
        model.addAttribute("produtos", produtoService.listarParaSelecao(usuario.getCodigo()));
        model.addAttribute("movimentacoes", historico.getContent());
        model.addAttribute("paginaAtual", historico.getNumber() + 1);
        model.addAttribute("totalPaginas", Math.max(1, historico.getTotalPages()));
        model.addAttribute("totalMovimentacoes", historico.getTotalElements());
        model.addAttribute("tipos", TipoMovimentacao.values());
        // Filtros aplicados (já validados), para repor nos campos e montar os links de página/exportação
        model.addAttribute("filtroProduto", filtro.produtoId());
        model.addAttribute("filtroTipo", filtro.tipoParametro());
        model.addAttribute("filtroDe", filtro.deParametro());
        model.addAttribute("filtroAte", filtro.ateParametro());
        model.addAttribute("filtrosAtivos", !filtro.vazio());
        model.addAttribute("parametrosFiltros", montarParametros(filtro));
    }

    /** Filtros como "&produto=3&tipo=SAIDA..." (valores codificados), para anexar a links de página e exportação. */
    private String montarParametros(MovimentacaoFiltro filtro) {
        StringBuilder parametros = new StringBuilder();
        adicionarParametro(parametros, "produto", filtro.produtoId() == null ? null : filtro.produtoId().toString());
        adicionarParametro(parametros, "tipo", filtro.tipoParametro());
        adicionarParametro(parametros, "de", filtro.deParametro());
        adicionarParametro(parametros, "ate", filtro.ateParametro());
        return parametros.toString();
    }

    private void adicionarParametro(StringBuilder parametros, String nome, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        parametros.append("&").append(nome).append("=").append(URLEncoder.encode(valor, StandardCharsets.UTF_8));
    }
}
