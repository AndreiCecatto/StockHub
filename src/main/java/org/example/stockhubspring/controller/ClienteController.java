package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ClienteFiltro;
import org.example.stockhubspring.dto.ClienteForm;
import org.example.stockhubspring.dto.ClienteLinha;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Modulo;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Clientes (RN-76, RN-91 a RN-98): lista com busca e filtros, cadastro, detalhe com resumo, edição e inativação.
 * A tela só existe para lojas com o módulo VENDAS ou OS ligado (RN-43): nas demais, a rota responde 404.
 */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final UsuarioAtualService usuarioAtualService;
    private final UsuarioRepository usuarioRepository;

    public ClienteController(ClienteService clienteService, UsuarioAtualService usuarioAtualService,
                             UsuarioRepository usuarioRepository) {
        this.clienteService = clienteService;
        this.usuarioAtualService = usuarioAtualService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String busca,
                        @RequestParam(required = false) String situacao,
                        @RequestParam(name = "os", required = false) String comOs,
                        @RequestParam(required = false) String ordem,
                        @RequestParam(defaultValue = "1") int pagina,
                        Model model) {
        Usuario loja = lojaComModulo();
        ClienteFiltro filtro = ClienteFiltro.de(busca, situacao, comOs, ordem, pagina);
        Page<ClienteLinha> resultado = clienteService.listarPagina(loja.getCodigo(), filtro);
        model.addAttribute("temOs", temModulo(loja, Modulo.OS));
        model.addAttribute("clientes", resultado.getContent());
        model.addAttribute("filtro", filtro);
        model.addAttribute("paginaAtual", resultado.getNumber() + 1);
        model.addAttribute("totalPaginas", Math.max(1, resultado.getTotalPages()));
        model.addAttribute("totalClientes", resultado.getTotalElements());
        model.addAttribute("tamanho", filtro.tamanho());
        model.addAttribute("parametrosFiltros", parametros(filtro));
        return "clientes";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        lojaComModulo();
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ClienteForm.basico(null, null, null));
        }
        model.addAttribute("editando", false);
        return "cliente-form";
    }

    @PostMapping
    public String cadastrar(@ModelAttribute ClienteForm form, RedirectAttributes redirectAttributes) {
        Usuario loja = lojaComModulo();
        try {
            Parceiro cliente = clienteService.cadastrar(loja, form);
            redirectAttributes.addFlashAttribute("mensagem", "Cliente cadastrado.");
            return "redirect:/clientes/" + cliente.getId();
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/clientes/novo";
        }
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Integer id, Model model) {
        Usuario loja = lojaComModulo();
        Parceiro cliente = clienteService.buscar(loja.getCodigo(), id);
        model.addAttribute("cliente", cliente);
        model.addAttribute("resumo", clienteService.resumo(loja.getCodigo(), id));
        model.addAttribute("temOs", temModulo(loja, Modulo.OS));
        return "cliente-detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario loja = lojaComModulo();
        Parceiro cliente = clienteService.buscar(loja.getCodigo(), id);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ClienteForm.de(cliente));
        }
        model.addAttribute("cliente", cliente);
        model.addAttribute("editando", true);
        model.addAttribute("documentoTravado", cliente.isFornecedor() && cliente.getDocumento() != null);
        return "cliente-form";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Integer id, @ModelAttribute ClienteForm form,
                            RedirectAttributes redirectAttributes) {
        Usuario loja = lojaComModulo();
        try {
            clienteService.atualizar(loja.getCodigo(), id, form);
            redirectAttributes.addFlashAttribute("mensagem", "Cadastro atualizado.");
            return "redirect:/clientes/" + id;
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/clientes/" + id + "/editar";
        }
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario loja = lojaComModulo();
        clienteService.alterarSituacao(loja.getCodigo(), id, false);
        redirectAttributes.addFlashAttribute("mensagem", "Cliente inativado. O histórico continua guardado.");
        return "redirect:/clientes/" + id;
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Usuario loja = lojaComModulo();
        clienteService.alterarSituacao(loja.getCodigo(), id, true);
        redirectAttributes.addFlashAttribute("mensagem", "Cliente reativado.");
        return "redirect:/clientes/" + id;
    }

    /** A loja logada, desde que o tipo dela tenha vendas ou ordens de serviço; senão 404. */
    private Usuario lojaComModulo() {
        Usuario loja = usuarioAtualService.getUsuarioAtual();
        TipoLoja tipo = usuarioRepository.findTipoLojaByCodigo(loja.getCodigo()).orElse(TipoLoja.ESTOQUE);
        if (!tipo.temModulo(Modulo.VENDAS) && !tipo.temModulo(Modulo.OS)) {
            throw new RecursoNaoEncontradoException("Página não encontrada.");
        }
        return loja;
    }

    private boolean temModulo(Usuario loja, Modulo modulo) {
        return usuarioRepository.findTipoLojaByCodigo(loja.getCodigo()).orElse(TipoLoja.ESTOQUE).temModulo(modulo);
    }

    /** Filtros já codificados para os links de paginação (começa com "&"). */
    private String parametros(ClienteFiltro filtro) {
        StringBuilder p = new StringBuilder();
        if (filtro.busca() != null) {
            p.append("&busca=").append(URLEncoder.encode(filtro.busca(), StandardCharsets.UTF_8));
        }
        p.append("&situacao=").append(filtro.situacao().name().toLowerCase());
        p.append("&os=").append(filtro.comOs().name().toLowerCase());
        p.append("&ordem=").append(filtro.ordem().name().toLowerCase());
        return p.toString();
    }
}
