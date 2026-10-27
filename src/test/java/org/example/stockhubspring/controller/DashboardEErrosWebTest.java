package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.ResumoEstoque;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.model.TipoLoja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Telas do painel e o tratamento global de erros (GlobalExceptionHandler). */
class DashboardEErrosWebTest extends ControllerTestBase {

    @BeforeEach
    void stubs() {
        when(dashboardService.buscarResumo(anyInt())).thenReturn(new ResumoEstoque());
        when(produtoService.listarEstoqueBaixo(anyInt(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(produtoService.listarPagina(any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(estoqueService.listarMovimentacoes(anyInt(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    @DisplayName("GET /dashboard traz a linha do tempo e não consulta OS em loja de estoque")
    void painelSemOs() throws Exception {
        mvc.perform(get("/dashboard").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("movimentacoesRecentes"))
                .andExpect(model().attributeDoesNotExist("osPorStatus", "osAndamento"));
        verify(estoqueService).listarMovimentacoes(7, 1, 8);
        org.mockito.Mockito.verifyNoInteractions(ordemServicoService);
    }

    @Test
    @DisplayName("GET /dashboard de oficina traz o fluxo de OS e só as OS em andamento")
    void painelDeOficinaTrazOs() throws Exception {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(java.util.Optional.of(TipoLoja.OFICINA));
        when(ordemServicoService.contarPorStatus(7)).thenReturn(new java.util.EnumMap<>(StatusOs.class));
        OrdemServico emExecucao = new OrdemServico();
        emExecucao.setStatus(StatusOs.EM_EXECUCAO);
        OrdemServico orcamento = new OrdemServico();
        orcamento.setStatus(StatusOs.ORCAMENTO);
        when(ordemServicoService.listar(7, null)).thenReturn(List.of(orcamento, emExecucao));

        mvc.perform(get("/dashboard").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("osPorStatus"))
                .andExpect(model().attribute("osAndamento", List.of(emExecucao)));
    }

    @Test
    @DisplayName("GET /dashboard mostra o painel da loja da sessão")
    void painel() throws Exception {
        mvc.perform(get("/dashboard").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("resumo", "produtosRecentes", "produtosEstoqueBaixo"));
        verify(dashboardService).buscarResumo(7);
    }

    @Test
    @DisplayName("GET / logado redireciona ao painel sem consultar dados da loja")
    void raizLogadoVaiParaOPainel() throws Exception {
        mvc.perform(get("/").with(logadoComo(7)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        org.mockito.Mockito.verifyNoInteractions(dashboardService);
    }

    @Test
    @DisplayName("telas de estoque baixo, unidades e valor do estoque abrem com paginação")
    void telasDoPainel() throws Exception {
        mvc.perform(get("/estoque-baixo").with(logadoComo(7)).param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque-baixo"))
                .andExpect(model().attributeExists("produtos", "paginaAtual", "totalPaginas"));
        mvc.perform(get("/unidades").with(logadoComo(7)))
                .andExpect(view().name("unidades"))
                .andExpect(model().attributeExists("resumo", "produtos"));
        mvc.perform(get("/valor-estoque").with(logadoComo(7)))
                .andExpect(view().name("valor-estoque"))
                .andExpect(model().attributeExists("resumo", "produtos"));
        verify(produtoService).listarEstoqueBaixo(eq(7), eq(2), anyInt());
    }

    @Test
    @DisplayName("RegraNegocioException fora de um formulário vira mensagem flash e volta para a página de origem (Referer do mesmo site)")
    void regraDeNegocioVoltaParaOReferer() throws Exception {
        when(produtoService.excluir(5, 7)).thenThrow(new RegraNegocioException("Regra violada."));

        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf())
                        .header("Referer", "http://localhost/produtos?pagina=2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("erro", "Regra violada."));
    }

    @Test
    @DisplayName("sem Referer volta para o painel")
    void semRefererVoltaParaOPainel() throws Exception {
        when(produtoService.excluir(5, 7)).thenThrow(new RegraNegocioException("Regra violada."));

        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("erro", "Regra violada."));
    }

    @Test
    @DisplayName("Referer de outro site nunca vira destino (sem open redirect)")
    void refererDeOutroSiteIgnorado() throws Exception {
        when(produtoService.excluir(5, 7)).thenThrow(new RegraNegocioException("Regra violada."));

        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf())
                        .header("Referer", "http://site-malicioso.invalid/produtos"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Referer com caminho '//host' ou com caracteres estranhos é ignorado")
    void refererComCaminhoSuspeitoIgnorado() throws Exception {
        when(produtoService.excluir(5, 7)).thenThrow(new RegraNegocioException("Regra violada."));

        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf())
                        .header("Referer", "http://localhost//site-malicioso.invalid"))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf())
                        .header("Referer", "http://localhost/%7Bvariavel%7D"))
                .andExpect(redirectedUrl("/dashboard"));
        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf())
                        .header("Referer", "isto não é uma URL"))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("violação de integridade do banco vira mensagem amigável, sem stack trace")
    void integridadeViraMensagemAmigavel() throws Exception {
        when(produtoService.excluir(5, 7)).thenThrow(new DataIntegrityViolationException("duplicate key value violates ..."));

        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf())
                        .header("Referer", "http://localhost/produtos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("erro",
                        "Não foi possível concluir a operação: o registro entra em conflito com outro já existente ou em uso."));
    }
}
