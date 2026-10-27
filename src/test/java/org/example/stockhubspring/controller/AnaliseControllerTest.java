package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.PainelAnalises;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Tela /analises: loja da sessão, período vindo do parâmetro e exigência de login. */
class AnaliseControllerTest extends ControllerTestBase {

    private static PainelAnalises painelVazio(int periodo) {
        return new PainelAnalises(periodo, 3, BigDecimal.ZERO, 0, 0, List.of(), List.of(), 0, BigDecimal.ZERO,
                List.of(), List.of(), List.of(), List.of(), 0, List.of(), 0, BigDecimal.ZERO, List.of(), 0,
                BigDecimal.ZERO);
    }

    @Test
    @DisplayName("GET /analises usa a loja da sessão e o período pedido")
    void usaLojaEPeriodo() throws Exception {
        when(analiseEstoqueService.montar(eq(7), eq(90))).thenReturn(painelVazio(90));

        mvc.perform(get("/analises").with(logadoComo(7)).param("periodo", "90"))
                .andExpect(status().isOk())
                .andExpect(view().name("analises"))
                .andExpect(model().attributeExists("painel"));
        verify(analiseEstoqueService).montar(7, 90);
    }

    @Test
    @DisplayName("sem parâmetro, ou com texto que não é número, o período chega nulo (o service usa 30)")
    void periodoInvalidoNaoQuebra() throws Exception {
        when(analiseEstoqueService.montar(eq(7), any())).thenReturn(painelVazio(30));

        mvc.perform(get("/analises").with(logadoComo(7))).andExpect(status().isOk());
        mvc.perform(get("/analises").with(logadoComo(7)).param("periodo", "abc")).andExpect(status().isOk());
        mvc.perform(get("/analises").with(logadoComo(7)).param("periodo", "999999999999")).andExpect(status().isOk());

        verify(analiseEstoqueService, org.mockito.Mockito.times(3)).montar(eq(7), eq((Integer) null));
    }

    @Test
    @DisplayName("sem login redireciona para /login e não consulta nada")
    void exigeLogin() throws Exception {
        mvc.perform(get("/analises"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
        verify(analiseEstoqueService, never()).montar(any(), any());
    }
}
