package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.MovimentoDiario;
import org.example.stockhubspring.service.DashboardService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dados do painel em JSON, da loja do usuário logado. */
@RestController
@RequestMapping("/api/dashboard")
public class ApiDashboardController {

    private final DashboardService dashboardService;
    private final UsuarioAtualService usuarioAtualService;

    public ApiDashboardController(DashboardService dashboardService, UsuarioAtualService usuarioAtualService) {
        this.dashboardService = dashboardService;
        this.usuarioAtualService = usuarioAtualService;
    }

    /** Um item por dia (do mais antigo ao mais recente). {@code dias}: padrão 14, ajustado para 1..60 sem erro. */
    @GetMapping(value = "/movimentos", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MovimentoDiario> movimentos(@RequestParam(required = false) String dias) {
        Integer loja = usuarioAtualService.getUsuarioAtual().getCodigo();
        return dashboardService.movimentosPorDia(loja,
                ApiBuscaController.lerInteiro(dias, DashboardService.DIAS_PADRAO));
    }
}
