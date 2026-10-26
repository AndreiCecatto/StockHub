package org.example.stockhubspring.controller;

import jakarta.validation.Valid;
import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ExportacaoService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.ProdutoService.ResultadoExclusao;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Só recebe, valida e redireciona; as regras ficam no {@link ProdutoService}. */
@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;
    private final EstoqueService estoqueService;
    private final ExportacaoService exportacaoService;

    public ProdutoController(ProdutoService produtoService,
                             CategoriaService categoriaService,
                             UsuarioAtualService usuarioAtualService,
                             ErrosFormulario errosFormulario,
                             EstoqueService estoqueService,
                             ExportacaoService exportacaoService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
        this.estoqueService = estoqueService;
        this.exportacaoService = exportacaoService;
    }

    /** Quantas movimentações recentes o detalhe do produto mostra. */
    static final int MOVIMENTACOES_NO_DETALHE = 20;

    @GetMapping
    public String listar(@RequestParam(required = false) String acao,
                         @RequestParam(required = false) Integer id,
                         @RequestParam(required = false) String busca,
                         @RequestParam(required = false) Integer categoriaId,
                         @RequestParam(required = false) String nivelEstoque,
                         @RequestParam(defaultValue = "id") String ordenacao,
                         @RequestParam(defaultValue = "desc") String direcao,
                         @RequestParam(defaultValue = "1") int pagina,
                         @RequestParam(defaultValue = "25") int tamanho,
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
        // O filtro normaliza tudo: ordenação só da lista branca, tamanho de página limitado
        ProdutoFiltro filtro = ProdutoFiltro.de(busca, categoriaId, nivelEstoque, ordenacao, direcao, pagina, tamanho);
        Page<Produto> resultado = produtoService.listarPagina(filtro, usuario.getCodigo());

        model.addAttribute("produtos", resultado.getContent());
        model.addAttribute("categorias", categoriaService.listarParaSelecao(usuario.getCodigo()));
        model.addAttribute("busca", filtro.busca());
        model.addAttribute("categoriaId", filtro.categoriaId());
        model.addAttribute("nivelEstoque", filtro.nivelEstoqueParametro());
        model.addAttribute("ordenacao", filtro.ordenacao());
        model.addAttribute("direcao", filtro.direcao());
        model.addAttribute("direcaoInversa", "asc".equals(filtro.direcao()) ? "desc" : "asc");
        model.addAttribute("paginaAtual", resultado.getNumber() + 1);
        model.addAttribute("totalPaginas", Math.max(1, resultado.getTotalPages()));
        model.addAttribute("totalProdutosFiltro", resultado.getTotalElements());
        model.addAttribute("tamanho", filtro.tamanho());
        model.addAttribute("parametrosFiltros", montarParametros(filtro));
        return "produtos";
    }

    /**
     * Exporta os produtos da loja em CSV, com os mesmos filtros e ordenação da listagem
     * (sem paginação; há um limite de segurança de linhas).
     */
    @GetMapping("/exportar.csv")
    public ResponseEntity<byte[]> exportarCsv(@RequestParam(required = false) String busca,
                                              @RequestParam(required = false) Integer categoriaId,
                                              @RequestParam(required = false) String nivelEstoque,
                                              @RequestParam(defaultValue = "id") String ordenacao,
                                              @RequestParam(defaultValue = "desc") String direcao) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        ProdutoFiltro filtro = ProdutoFiltro.de(busca, categoriaId, nivelEstoque, ordenacao, direcao, 1,
                ProdutoFiltro.TAMANHO_PADRAO);
        return RespostaCsv.de(exportacaoService.exportarProdutos(filtro, usuario.getCodigo()));
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("produto", new ProdutoForm());
        model.addAttribute("categorias", categoriaService.listarParaSelecao(usuario.getCodigo()));
        return "produto-form";
    }

    @GetMapping("/{id}")
    public String visualizar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        // Primeiro confirma que o produto é da loja (senão 404); só então busca o histórico dele
        model.addAttribute("produto", produtoService.buscarPorId(id, usuario.getCodigo()));
        model.addAttribute("movimentacoesProduto",
                estoqueService.ultimasDoProduto(id, usuario.getCodigo(), MOVIMENTACOES_NO_DETALHE));
        return "produto-detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        Produto produto = produtoService.buscarPorId(id, usuario.getCodigo());
        model.addAttribute("produto", ProdutoForm.de(produto));
        model.addAttribute("categorias", categoriaService.listarParaSelecao(usuario.getCodigo()));
        return "produto-form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("produto") ProdutoForm produto,
                         BindingResult resultado,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        if (resultado.hasErrors()) {
            // Volta ao MESMO formulário, com o que o usuário digitou e os erros
            errosFormulario.publicar(resultado, model);
            model.addAttribute("categorias", categoriaService.listarParaSelecao(usuario.getCodigo()));
            return "produto-form";
        }
        boolean novo = produto.getId() == null;
        try {
            produtoService.salvar(produto, usuario);
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("categorias", categoriaService.listarParaSelecao(usuario.getCodigo()));
            return "produto-form";
        }
        redirectAttributes.addFlashAttribute("mensagem", novo ? "Produto cadastrado." : "Produto atualizado.");
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        ResultadoExclusao resultado = produtoService.excluir(id, usuario.getCodigo());
        redirectAttributes.addFlashAttribute("mensagem", resultado == ResultadoExclusao.EXCLUIDO
                ? "Produto excluído."
                : "Produto inativado: ele tem histórico de movimentações, que foi preservado.");
        return "redirect:/produtos";
    }

    private String montarParametros(ProdutoFiltro filtro) {
        StringBuilder parametros = new StringBuilder();
        adicionarParametro(parametros, "busca", filtro.busca());
        adicionarParametro(parametros, "categoriaId", filtro.categoriaId() == null ? null : filtro.categoriaId().toString());
        adicionarParametro(parametros, "nivelEstoque", filtro.nivelEstoqueParametro());
        adicionarParametro(parametros, "ordenacao", filtro.ordenacao());
        adicionarParametro(parametros, "tamanho", String.valueOf(filtro.tamanho()));
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
