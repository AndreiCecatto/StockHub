package org.example.stockhubspring.controller;

import jakarta.validation.Valid;
import org.example.stockhubspring.dto.MovimentacaoForm;
import org.example.stockhubspring.dto.MovimentacaoResposta;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mesma URL do formulário (POST /estoque/movimentar), atendida só quando o cliente pede
 * "Accept: application/json". Sem esse cabeçalho (ou com o Accept de um navegador, que lista
 * text/html antes do curinga) continua valendo o {@link EstoqueController}, com redirect e flash.
 * Erros de regra (422), produto inexistente (404) etc. saem em {@link ApiExcecoes}.
 */
@RestController
@RequestMapping("/estoque")
public class EstoqueJsonController {

    private final EstoqueService estoqueService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;

    public EstoqueJsonController(EstoqueService estoqueService,
                                 UsuarioAtualService usuarioAtualService,
                                 ErrosFormulario errosFormulario) {
        this.estoqueService = estoqueService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
    }

    @PostMapping(value = "/movimentar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> movimentar(@Valid @ModelAttribute("movimentacaoForm") MovimentacaoForm form,
                                        BindingResult resultado) {
        if (resultado.hasErrors()) {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("ok", false);
            corpo.put("erro", errosFormulario.resumir(resultado));
            corpo.put("campos", errosFormulario.porCampo(resultado));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpo);
        }
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        MovimentacaoEstoque movimentacao = estoqueService.movimentar(
                form.getProdutoId(), form.getTipo(), form.getQuantidade(), form.getObservacao(), usuario);
        return ResponseEntity.ok(MovimentacaoResposta.de(movimentacao));
    }
}
