package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.BlocoTela;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.ResumoEstoque;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.model.TipoLoja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O Início só consulta os dados dos blocos visíveis (AD-19, RN-109 e RN-112). */
class DashboardPersonalizadoWebTest extends ControllerTestBase {

    @BeforeEach
    void stubs() {
        when(dashboardService.buscarResumo(anyInt())).thenReturn(new ResumoEstoque());
        when(produtoService.listarEstoqueBaixo(anyInt(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(estoqueService.listarMovimentacoes(anyInt(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
    }

    private static Map<String, Boolean> todos(boolean valor) {
        Map<String, Boolean> mapa = new LinkedHashMap<>();
        for (BlocoTela b : BlocoTela.values()) {
            mapa.put(b.name(), valor);
        }
        return mapa;
    }

    @Test
    @DisplayName("CADASTRADOS_ULTIMO e HOJE_NA_LOJA ocultos: não consulta recentes nem movimentações; resumo e estoque baixo seguem")
    void ocultosNaoConsultam() throws Exception {
        Map<String, Boolean> mapa = todos(true);
        mapa.put("CADASTRADOS_ULTIMO", false);
        mapa.put("HOJE_NA_LOJA", false);
        when(personalizacaoService.blocosVisiveis(7, TelaPersonalizavel.INICIO)).thenReturn(mapa);

        mvc.perform(get("/dashboard").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("blocos", mapa))
                .andExpect(model().attributeDoesNotExist("produtosRecentes", "movimentacoesRecentes"))
                .andExpect(model().attributeExists("resumo", "produtosEstoqueBaixo"));
        verify(produtoService, never()).listarRecentes(anyInt(), anyInt());
        verify(estoqueService, never()).listarMovimentacoes(anyInt(), anyInt(), anyInt());
        verify(dashboardService).buscarResumo(7);
        verify(produtoService).listarEstoqueBaixo(7, 1, 6);
    }

    @Test
    @DisplayName("tudo oculto: ainda carrega a capa (resumo e estoque baixo) e não toca em OS")
    void tudoOcultoMantemCapa() throws Exception {
        when(personalizacaoService.blocosVisiveis(7, TelaPersonalizavel.INICIO)).thenReturn(todos(false));

        mvc.perform(get("/dashboard").with(logadoComo(7, PapelMembro.GERENTE)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("resumo", "produtosEstoqueBaixo"))
                .andExpect(model().attributeDoesNotExist("produtosRecentes", "movimentacoesRecentes",
                        "osPorStatus", "osAndamento"));
        verifyNoInteractions(ordemServicoService);
        verify(produtoService, never()).listarRecentes(anyInt(), anyInt());
    }

    @Test
    @DisplayName("OS_FLUXO oculto em oficina: não consulta contagem nem lista de OS")
    void osOcultaNaoConsulta() throws Exception {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(java.util.Optional.of(TipoLoja.OFICINA));
        Map<String, Boolean> mapa = todos(true);
        mapa.put("OS_FLUXO", false);
        when(personalizacaoService.blocosVisiveis(7, TelaPersonalizavel.INICIO)).thenReturn(mapa);

        mvc.perform(get("/dashboard").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("osPorStatus", "osAndamento"));
        verifyNoInteractions(ordemServicoService);
    }

    @Test
    @DisplayName("podeGerenciar: verdadeiro para dono e gerente, falso para papéis operacionais")
    void podeGerenciarPorPapel() throws Exception {
        for (PapelMembro papel : PapelMembro.values()) {
            mvc.perform(get("/dashboard").with(logadoComo(7, papel)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("podeGerenciar", papel.gerenciaLoja()));
        }
    }

    @Test
    @DisplayName("mapa vazio (nenhum bloco aplicável) não quebra o painel")
    void mapaVazio() throws Exception {
        when(personalizacaoService.blocosVisiveis(any(), any())).thenReturn(Map.of());

        mvc.perform(get("/dashboard").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("produtosRecentes", "movimentacoesRecentes"));
    }
}
