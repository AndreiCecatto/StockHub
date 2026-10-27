package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Modulo;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Lista e cadastro simples de clientes (RN-76). A tela só existe para lojas com o módulo VENDAS ou OS ligado
 * (RN-43): nas demais, a rota responde 404, como se não existisse.
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
    public String lista(Model model) {
        Usuario loja = lojaComModulo();
        model.addAttribute("clientes", clienteService.listar(loja.getCodigo()));
        return "clientes";
    }

    @PostMapping
    public String cadastrar(@RequestParam(required = false) String documento,
                            @RequestParam(required = false) String nome,
                            @RequestParam(required = false) String uf,
                            RedirectAttributes redirectAttributes) {
        Usuario loja = lojaComModulo();
        try {
            clienteService.cadastrar(loja, documento, nome, uf);
            redirectAttributes.addFlashAttribute("mensagem", "Cliente cadastrado.");
        } catch (RegraNegocioException e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            redirectAttributes.addFlashAttribute("documentoDigitado", documento);
            redirectAttributes.addFlashAttribute("nomeDigitado", nome);
            redirectAttributes.addFlashAttribute("ufDigitada", uf);
        }
        return "redirect:/clientes";
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
}
