package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.service.DashboardService;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.OrdemServicoService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

@Controller
public class DashboardController {

    private static final int LIMITE_PAINEL = 6;
    private static final int TAMANHO_LISTAS = 100;
    /** Quantas movimentações aparecem na linha do tempo do dia. */
    private static final int LIMITE_LINHA_TEMPO = 8;
    /** Quantas OS em andamento aparecem no início das oficinas. */
    private static final int LIMITE_OS = 5;
    private static final Set<StatusOs> OS_EM_ANDAMENTO =
            Set.of(StatusOs.APROVADA, StatusOs.EM_EXECUCAO, StatusOs.CONCLUIDA);

    private final UsuarioAtualService usuarioAtualService;
    private final ProdutoService produtoService;
    private final DashboardService dashboardService;
    private final EstoqueService estoqueService;
    private final OrdemServicoService ordemServicoService;

    public DashboardController(UsuarioAtualService usuarioAtualService,
                               ProdutoService produtoService,
                               DashboardService dashboardService,
                               EstoqueService estoqueService,
                               OrdemServicoService ordemServicoService) {
        this.usuarioAtualService = usuarioAtualService;
        this.produtoService = produtoService;
        this.dashboardService = dashboardService;
        this.estoqueService = estoqueService;
        this.ordemServicoService = ordemServicoService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        model.addAttribute("resumo", dashboardService.buscarResumo(usuario.getCodigo()));
        model.addAttribute("produtosRecentes", produtoService.listarRecentes(LIMITE_PAINEL, usuario.getCodigo()));
        model.addAttribute("produtosEstoqueBaixo",
                produtoService.listarEstoqueBaixo(usuario.getCodigo(), 1, LIMITE_PAINEL).getContent());
        // Linha do tempo do dia: últimas movimentações da loja (o produto já vem junto na consulta)
        model.addAttribute("movimentacoesRecentes",
                estoqueService.listarMovimentacoes(usuario.getCodigo(), 1, LIMITE_LINHA_TEMPO).getContent());
        // Só lojas com o módulo OS (o mapa "modulos" vem do GlobalModelAdvice): fluxo por situação e OS em andamento
        if (moduloLigado(model, "OS")) {
            model.addAttribute("osPorStatus", ordemServicoService.contarPorStatus(usuario.getCodigo()));
            model.addAttribute("osAndamento", ordemServicoService.listar(usuario.getCodigo(), null).stream()
                    .filter(os -> OS_EM_ANDAMENTO.contains(os.getStatus()))
                    .limit(LIMITE_OS)
                    .toList());
        }
        return "dashboard";
    }

    private static boolean moduloLigado(Model model, String nome) {
        return model.asMap().get("modulos") instanceof Map<?, ?> mapa && Boolean.TRUE.equals(mapa.get(nome));
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
