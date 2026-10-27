package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.OrdemServicoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Rotas /os/**: módulo por tipo de loja (404), papéis, CSRF, redirecionamentos e telas (RN-77 a RN-90). */
class OrdemServicoWebTest extends ControllerTestBase {

    private void tipo(TipoLoja tipo) {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(Optional.of(tipo));
    }

    private OrdemServico os(int id, int numero) {
        OrdemServico os = new OrdemServico();
        org.springframework.test.util.ReflectionTestUtils.setField(os, "id", id);
        os.setNumero(numero);
        os.setStatus(StatusOs.APROVADA);
        return os;
    }

    // ---------- módulo por tipo de loja ----------

    @ParameterizedTest(name = "loja {0}: /os responde 404")
    @EnumSource(value = TipoLoja.class, names = {"ESTOQUE", "COMERCIO"})
    @DisplayName("lojas sem o módulo OS não têm nenhuma rota /os")
    void semModulo(TipoLoja tipo) throws Exception {
        tipo(tipo);
        mvc.perform(get("/os").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(get("/os/nova").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(get("/os/3").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(get("/os/3/imprimir").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(post("/os/3/avancar").with(logadoComo(7, PapelMembro.DONO)).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/os").with(logadoComo(7, PapelMembro.DONO)).with(csrf()).param("defeito", "x"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(ordemServicoService);
    }

    @ParameterizedTest(name = "loja {0}: lista abre")
    @EnumSource(value = TipoLoja.class, names = {"OFICINA", "MISTO"})
    @DisplayName("oficina e misto abrem a lista, com filtro por status")
    void listaComModulo(TipoLoja tipo) throws Exception {
        tipo(tipo);
        when(ordemServicoService.listar(7, StatusOs.APROVADA)).thenReturn(List.of());
        when(ordemServicoService.contarPorStatus(7)).thenReturn(Map.of());

        mvc.perform(get("/os").param("status", "APROVADA").with(logadoComo(7, PapelMembro.MECANICO)))
                .andExpect(status().isOk())
                .andExpect(view().name("os-lista"))
                .andExpect(model().attribute("filtro", StatusOs.APROVADA));
        verify(ordemServicoService).listar(7, StatusOs.APROVADA);
    }

    @Test
    @DisplayName("status desconhecido no filtro vira 'todas'")
    void filtroInvalido() throws Exception {
        tipo(TipoLoja.OFICINA);
        mvc.perform(get("/os").param("status", "XYZ").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk());
        verify(ordemServicoService).listar(7, null);
    }

    @Test
    @DisplayName("sem login, /os manda para a entrada")
    void semLogin() throws Exception {
        mvc.perform(get("/os")).andExpect(status().is3xxRedirection());
    }

    // ---------- ações ----------

    @ParameterizedTest(name = "{0} cria OS")
    @EnumSource(value = PapelMembro.class, names = {"MECANICO", "OPERADOR", "DONO", "GERENTE"})
    @DisplayName("quem conduz OS cria e vai para o detalhe")
    void cria(PapelMembro papel) throws Exception {
        tipo(TipoLoja.OFICINA);
        when(ordemServicoService.criar(any(Usuario.class), eq(4), eq("Uno"), eq("ABC1D23"), eq("Não liga"),
                any(), any(), any())).thenReturn(os(9, 12));

        mvc.perform(post("/os").with(logadoComo(7, papel)).with(csrf()).param("clienteId", "4")
                        .param("equipamento", "Uno").param("identificador", "ABC1D23").param("defeito", "Não liga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/os/9"))
                .andExpect(flash().attributeExists("mensagem"));
    }

    @Test
    @DisplayName("recusa de regra ao criar volta ao formulário com o que foi digitado")
    void criaRecusa() throws Exception {
        tipo(TipoLoja.OFICINA);
        when(ordemServicoService.criar(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RegraNegocioException("Descreva o defeito relatado pelo cliente."));

        mvc.perform(post("/os").with(logadoComo(7, PapelMembro.MECANICO)).with(csrf()).param("equipamento", "Uno"))
                .andExpect(redirectedUrl("/os/nova"))
                .andExpect(flash().attribute("erro", "Descreva o defeito relatado pelo cliente."))
                .andExpect(flash().attribute("equipamentoDigitado", "Uno"));
    }

    @ParameterizedTest(name = "{0}: avançar chama o service e volta ao detalhe")
    @EnumSource(value = PapelMembro.class, names = {"MECANICO", "OPERADOR", "VENDEDOR", "DONO", "GERENTE"})
    void avanca(PapelMembro papel) throws Exception {
        tipo(TipoLoja.MISTO);
        when(ordemServicoService.avancar(any(Usuario.class), eq(9))).thenReturn(os(9, 12));

        mvc.perform(post("/os/9/avancar").with(logadoComo(7, papel)).with(csrf()))
                .andExpect(redirectedUrl("/os/9"))
                .andExpect(flash().attributeExists("mensagem"));
    }

    @Test
    @DisplayName("o service recebe a loja e o papel de quem agiu (para decidir cancelar e desconto)")
    void servicoRecebePapel() throws Exception {
        tipo(TipoLoja.OFICINA);
        when(ordemServicoService.cancelar(any(Usuario.class), eq(9))).thenThrow(
                new RegraNegocioException("Só o dono ou o gerente cancela uma OS já concluída."));

        mvc.perform(post("/os/9/cancelar").with(logadoComo(7, PapelMembro.MECANICO)).with(csrf())
                        .header("Referer", "http://localhost/os/9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/os/9"))
                .andExpect(flash().attribute("erro", "Só o dono ou o gerente cancela uma OS já concluída."));
        verify(ordemServicoService).cancelar(org.mockito.ArgumentMatchers.argThat(
                u -> u.getCodigo() == 7 && u.getPapel() == PapelMembro.MECANICO), eq(9));
    }

    @Test
    @DisplayName("valor digitado com vírgula vira decimal ao salvar dados e incluir peça")
    void valoresComVirgula() throws Exception {
        tipo(TipoLoja.OFICINA);
        mvc.perform(post("/os/9/dados").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("defeito", "x").param("desconto", "1.250,50"))
                .andExpect(redirectedUrl("/os/9"));
        verify(ordemServicoService).atualizarDados(any(), eq(9), eq("x"), any(), any(), any(),
                eq(new BigDecimal("1250.50")));

        mvc.perform(post("/os/9/pecas").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("produtoId", "3").param("quantidade", "2").param("preco", "12,5"))
                .andExpect(redirectedUrl("/os/9"));
        verify(ordemServicoService).adicionarPeca(any(), eq(9), eq(3), eq(2), eq(new BigDecimal("12.5")));
    }

    @Test
    @DisplayName("valor que não é número é recusado com mensagem")
    void valorInvalido() throws Exception {
        tipo(TipoLoja.OFICINA);
        mvc.perform(post("/os/9/servicos").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("descricao", "Troca").param("valor", "abc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("erro"));
        verify(ordemServicoService, never()).adicionarServico(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("toda ação por POST sem token de segurança é recusada")
    void semCsrf() throws Exception {
        tipo(TipoLoja.OFICINA);
        for (String caminho : List.of("/os", "/os/9/dados", "/os/9/pecas", "/os/9/pecas/1/remover",
                "/os/9/servicos", "/os/9/servicos/1/remover", "/os/9/avancar", "/os/9/cancelar")) {
            mvc.perform(post(caminho).with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(ordemServicoService);
    }

    @Test
    @DisplayName("detalhe e impressão carregam a OS da loja de quem pediu")
    void detalheEImpressao() throws Exception {
        tipo(TipoLoja.OFICINA);
        OrdemServico os = os(9, 12);
        os.setStatus(StatusOs.EM_EXECUCAO);
        var detalhe = new OrdemServicoService.Detalhe(os, List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        when(ordemServicoService.detalhe(7, 9)).thenReturn(detalhe);

        mvc.perform(get("/os/9/imprimir").with(logadoComo(7, PapelMembro.MECANICO)))
                .andExpect(status().isOk()).andExpect(view().name("os-imprimir"))
                .andExpect(model().attribute("detalhe", detalhe));
        verify(ordemServicoService).detalhe(7, 9);
    }
}
