package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.CamposRamo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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

/** Placa e modelo no formulário de nova OS (AD-18, RN-104 a RN-108). */
class OrdemServicoVeiculoWebTest extends ControllerTestBase {

    @BeforeEach
    void oficina() {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(Optional.of(TipoLoja.OFICINA));
    }

    private OrdemServico os() {
        OrdemServico os = new OrdemServico();
        org.springframework.test.util.ReflectionTestUtils.setField(os, "id", 9);
        os.setNumero(12);
        return os;
    }

    @Test
    @DisplayName("placa e modelo chegam ao service")
    void parametrosChegamAoService() throws Exception {
        when(ordemServicoService.criar(any(Usuario.class), eq(4), eq("Uno"), eq("SN1"), eq("abc-1d23"),
                eq("Uno Mille"), eq("Não liga"), any(), any(), any())).thenReturn(os());

        mvc.perform(post("/os").with(logadoComo(7, PapelMembro.MECANICO)).with(csrf()).param("clienteId", "4")
                        .param("equipamento", "Uno").param("identificador", "SN1").param("placa", "abc-1d23")
                        .param("modelo", "Uno Mille").param("defeito", "Não liga"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/os/9"))
                .andExpect(flash().attributeExists("mensagem"));
        verify(ordemServicoService).criar(any(Usuario.class), eq(4), eq("Uno"), eq("SN1"), eq("abc-1d23"),
                eq("Uno Mille"), eq("Não liga"), any(), any(), any());
    }

    @Test
    @DisplayName("sem placa e modelo no formulário, o service recebe null")
    void semParametros() throws Exception {
        when(ordemServicoService.criar(any(Usuario.class), eq(4), eq("Uno"), any(), eq(null), eq(null),
                eq("Não liga"), any(), any(), any())).thenReturn(os());

        mvc.perform(post("/os").with(logadoComo(7, PapelMembro.DONO)).with(csrf()).param("clienteId", "4")
                        .param("equipamento", "Uno").param("defeito", "Não liga"))
                .andExpect(redirectedUrl("/os/9"));
    }

    @Test
    @DisplayName("recusa de placa devolve o que foi digitado (placa e modelo) no flash")
    void erroDevolvePlacaEModelo() throws Exception {
        when(ordemServicoService.criar(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RegraNegocioException("Placa inválida. Use o formato ABC-1234 ou ABC1D23."));

        mvc.perform(post("/os").with(logadoComo(7, PapelMembro.MECANICO)).with(csrf())
                        .param("equipamento", "Uno").param("placa", "XX-1").param("modelo", "Uno <script>"))
                .andExpect(redirectedUrl("/os/nova"))
                .andExpect(flash().attribute("erro", "Placa inválida. Use o formato ABC-1234 ou ABC1D23."))
                .andExpect(flash().attribute("placaDigitada", "XX-1"))
                .andExpect(flash().attribute("modeloDigitado", "Uno <script>"))
                .andExpect(flash().attribute("equipamentoDigitado", "Uno"));
    }

    @Test
    @DisplayName("GET /os/nova expõe camposRamo com o veículo da oficina")
    void novaTemCamposRamo() throws Exception {
        CamposRamo.Permitidos mecanica = CamposRamo.permitidos(org.example.stockhubspring.model.Subtipo.MECANICA);
        when(clienteService.listar(7)).thenReturn(List.of());
        when(ordemServicoService.responsaveis(7)).thenReturn(List.of());
        when(ordemServicoService.camposPermitidos(any(Usuario.class))).thenReturn(mecanica);

        mvc.perform(get("/os/nova").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(view().name("os-form"))
                .andExpect(model().attribute("camposRamo", mecanica));
    }

    @Test
    @DisplayName("POST /os sem token CSRF é 403 e não chega ao service")
    void semCsrf() throws Exception {
        mvc.perform(post("/os").with(logadoComo(7, PapelMembro.DONO)).param("clienteId", "4")
                        .param("equipamento", "Uno").param("placa", "ABC1D23").param("defeito", "x"))
                .andExpect(status().isForbidden());
        verify(ordemServicoService, never()).criar(any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any());
    }
}
