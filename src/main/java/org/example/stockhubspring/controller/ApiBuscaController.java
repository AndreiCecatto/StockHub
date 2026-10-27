package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ProdutoBusca;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/** Busca rápida de produtos (autocompletar da interface). Sempre escopada à loja do usuário logado. */
@RestController
@RequestMapping("/api")
public class ApiBuscaController {

    private final ProdutoService produtoService;
    private final UsuarioAtualService usuarioAtualService;

    public ApiBuscaController(ProdutoService produtoService, UsuarioAtualService usuarioAtualService) {
        this.produtoService = produtoService;
        this.usuarioAtualService = usuarioAtualService;
    }

    /**
     * {@code q} com menos de 2 caracteres devolve []. {@code limite} (padrão 8, máximo 20): valor
     * ausente, inválido ou fora da faixa nunca dá erro, apenas é ajustado.
     */
    @GetMapping(value = "/busca", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProdutoBusca> buscar(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String limite) {
        var usuario = usuarioAtualService.getUsuarioAtual();
        Integer loja = usuario.getCodigo();
        var campos = produtoService.camposPermitidos(usuario);
        var resultado = produtoService.buscarTolerante(q, lerInteiro(limite, ProdutoService.BUSCA_LIMITE_PADRAO), loja);
        List<ProdutoBusca> itens = new ArrayList<>();
        for (int i = 0; i < resultado.produtos().size(); i++) {
            var produto = resultado.produtos().get(i);
            itens.add(i < resultado.exatos() ? ProdutoBusca.de(produto, campos)
                    : ProdutoBusca.parecido(produto, campos));
        }
        return itens;
    }

    static int lerInteiro(String valor, int padrao) {
        if (valor == null || valor.isBlank()) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return padrao;
        }
    }
}
