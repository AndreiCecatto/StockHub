package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Modulo;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.OrdemServicoService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * Ordens de serviço (RN-77 a RN-90). Só existe para lojas com o módulo OS ligado (OFICINA e MISTO): nas demais,
 * toda rota /os/** responde 404. Quem decide o que cada papel pode é o {@link OrdemServicoService}.
 */
@Controller
@RequestMapping("/os")
public class OrdemServicoController {

    private final OrdemServicoService osService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final UsuarioAtualService usuarioAtualService;
    private final UsuarioRepository usuarioRepository;

    public OrdemServicoController(OrdemServicoService osService, ClienteService clienteService,
                                  ProdutoService produtoService, UsuarioAtualService usuarioAtualService,
                                  UsuarioRepository usuarioRepository) {
        this.osService = osService;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.usuarioAtualService = usuarioAtualService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String status, Model model) {
        Usuario loja = lojaComModulo();
        StatusOs filtro = StatusOs.de(status);
        model.addAttribute("ordens", osService.listar(loja.getCodigo(), filtro));
        model.addAttribute("contagens", osService.contarPorStatus(loja.getCodigo()));
        model.addAttribute("filtro", filtro);
        model.addAttribute("todosStatus", StatusOs.values());
        return "os-lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        Usuario loja = lojaComModulo();
        model.addAttribute("clientes", clienteService.listar(loja.getCodigo()));
        model.addAttribute("responsaveis", osService.responsaveis(loja.getCodigo()));
        return "os-form";
    }

    @PostMapping
    public String criar(@RequestParam(required = false) Integer clienteId,
                        @RequestParam(required = false) String equipamento,
                        @RequestParam(required = false) String identificador,
                        @RequestParam(required = false) String defeito,
                        @RequestParam(required = false) Integer responsavelId,
                        @RequestParam(required = false) Integer garantiaDias,
                        @RequestParam(required = false) String observacoes,
                        RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        try {
            OrdemServico os = osService.criar(loja, clienteId, equipamento, identificador, defeito, responsavelId,
                    garantiaDias, observacoes);
            redirect.addFlashAttribute("mensagem", "Ordem de serviço " + os.getNumero() + " criada.");
            return "redirect:/os/" + os.getId();
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            redirect.addFlashAttribute("clienteDigitado", clienteId);
            redirect.addFlashAttribute("equipamentoDigitado", equipamento);
            redirect.addFlashAttribute("identificadorDigitado", identificador);
            redirect.addFlashAttribute("defeitoDigitado", defeito);
            redirect.addFlashAttribute("garantiaDigitada", garantiaDias);
            redirect.addFlashAttribute("observacoesDigitadas", observacoes);
            return "redirect:/os/nova";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Integer id, Model model) {
        Usuario loja = lojaComModulo();
        carregarDetalhe(loja, id, model);
        model.addAttribute("responsaveis", osService.responsaveis(loja.getCodigo()));
        OrdemServicoService.Detalhe detalhe = (OrdemServicoService.Detalhe) model.getAttribute("detalhe");
        if (detalhe != null && detalhe.os().getStatus().podeTrocarPecas()) {
            model.addAttribute("produtos", produtoService.listarParaSelecao(loja.getCodigo()));
        }
        return "os-detalhe";
    }

    /** Página própria da impressão: só a OS, sem menus (RN-90). */
    @GetMapping("/{id}/imprimir")
    public String imprimir(@PathVariable Integer id, Model model) {
        Usuario loja = lojaComModulo();
        carregarDetalhe(loja, id, model);
        return "os-imprimir";
    }

    @PostMapping("/{id}/dados")
    public String dados(@PathVariable Integer id,
                        @RequestParam(required = false) String defeito,
                        @RequestParam(required = false) Integer responsavelId,
                        @RequestParam(required = false) Integer garantiaDias,
                        @RequestParam(required = false) String observacoes,
                        @RequestParam(required = false) String desconto,
                        RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        osService.atualizarDados(loja, id, defeito, responsavelId, garantiaDias, observacoes, valor(desconto));
        redirect.addFlashAttribute("mensagem", "Dados da OS salvos.");
        return voltar(id);
    }

    @PostMapping("/{id}/pecas")
    public String adicionarPeca(@PathVariable Integer id,
                                @RequestParam(required = false) Integer produtoId,
                                @RequestParam(defaultValue = "1") int quantidade,
                                @RequestParam(required = false) String preco,
                                RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        osService.adicionarPeca(loja, id, produtoId, quantidade, valor(preco));
        redirect.addFlashAttribute("mensagem", "Peça incluída.");
        return voltar(id);
    }

    @PostMapping("/{id}/pecas/{pecaId}/remover")
    public String removerPeca(@PathVariable Integer id, @PathVariable Integer pecaId, RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        osService.removerPeca(loja, id, pecaId);
        redirect.addFlashAttribute("mensagem", "Peça retirada.");
        return voltar(id);
    }

    @PostMapping("/{id}/servicos")
    public String adicionarServico(@PathVariable Integer id,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam(required = false) String valor,
                                   RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        osService.adicionarServico(loja, id, descricao, valor(valor));
        redirect.addFlashAttribute("mensagem", "Serviço incluído.");
        return voltar(id);
    }

    @PostMapping("/{id}/servicos/{servicoId}/remover")
    public String removerServico(@PathVariable Integer id, @PathVariable Integer servicoId,
                                 RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        osService.removerServico(loja, id, servicoId);
        redirect.addFlashAttribute("mensagem", "Serviço retirado.");
        return voltar(id);
    }

    @PostMapping("/{id}/avancar")
    public String avancar(@PathVariable Integer id, RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        OrdemServico os = osService.avancar(loja, id);
        redirect.addFlashAttribute("mensagem", "OS " + os.getNumero() + ": " + os.getStatus().getRotulo() + ".");
        return voltar(id);
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, RedirectAttributes redirect) {
        Usuario loja = lojaComModulo();
        OrdemServico os = osService.cancelar(loja, id);
        redirect.addFlashAttribute("mensagem", "OS " + os.getNumero() + " cancelada.");
        return voltar(id);
    }

    // ------------------------------------------------------------------------------------------

    private void carregarDetalhe(Usuario loja, Integer id, Model model) {
        model.addAttribute("detalhe", osService.detalhe(loja.getCodigo(), id));
    }

    private static String voltar(Integer id) {
        return "redirect:/os/" + id;
    }

    /** Valor em reais digitado como 12,50 ou 1.250,00 (ou 12.50); vazio vira null. */
    static BigDecimal valor(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.trim().replace("R$", "").replace(" ", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(',', '.');
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Valor inválido. Use números, por exemplo 150,00.");
        }
    }

    /** A loja logada, desde que o tipo dela tenha o módulo OS; senão 404 (RN-77). */
    private Usuario lojaComModulo() {
        Usuario loja = usuarioAtualService.getUsuarioAtual();
        TipoLoja tipo = usuarioRepository.findTipoLojaByCodigo(loja.getCodigo()).orElse(TipoLoja.ESTOQUE);
        if (!tipo.temModulo(Modulo.OS)) {
            throw new RecursoNaoEncontradoException("Página não encontrada.");
        }
        return loja;
    }
}
