package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Tela de personalização do início (AD-19, RN-110) na cadeia de segurança real. */
class PersonalizacaoWebTest extends ControllerTestBase {

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"VENDEDOR", "OPERADOR", "MECANICO"})
    @DisplayName("papéis operacionais recebem 403 no GET e no POST")
    void operacionaisBarrados(PapelMembro papel) throws Exception {
        mvc.perform(get("/conta/personalizacao").with(logadoComo(7, papel)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/conta/personalizacao/inicio").with(logadoComo(7, papel)).with(csrf())
                        .param("blocos", "ATALHOS"))
                .andExpect(status().isForbidden());
        verify(personalizacaoService, never()).salvar(anyInt(), any(), any(), any(), any());
        verify(personalizacaoService, never()).blocosAplicaveis(anyInt(), any());
    }

    @Test
    @DisplayName("sem login, o GET redireciona ao login")
    void semLogin() throws Exception {
        mvc.perform(get("/conta/personalizacao"))
                .andExpect(status().is3xxRedirection());
        verify(personalizacaoService, never()).blocosAplicaveis(anyInt(), any());
    }

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"DONO", "GERENTE"})
    @DisplayName("dono e gerente abrem o formulário com os blocos e o estado")
    void gestoresAbrem(PapelMembro papel) throws Exception {
        mvc.perform(get("/conta/personalizacao").with(logadoComo(7, papel)))
                .andExpect(status().isOk())
                .andExpect(view().name("personalizacao"))
                .andExpect(model().attributeExists("blocosInicio", "estadoBlocos"));
        verify(personalizacaoService).blocosAplicaveis(7, TelaPersonalizavel.INICIO);
        verify(personalizacaoService, Mockito.atLeastOnce()).blocosVisiveis(7, TelaPersonalizavel.INICIO);
    }

    @Test
    @DisplayName("POST sem CSRF é recusado e nada é salvo")
    void semCsrf() throws Exception {
        mvc.perform(post("/conta/personalizacao/inicio").with(logadoComo(7, PapelMembro.DONO))
                        .param("blocos", "ATALHOS"))
                .andExpect(status().isForbidden());
        verify(personalizacaoService, never()).salvar(anyInt(), any(), any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"DONO", "GERENTE"})
    @DisplayName("dono e gerente salvam os blocos marcados e voltam ao formulário")
    void gestoresSalvam(PapelMembro papel) throws Exception {
        mvc.perform(post("/conta/personalizacao/inicio").with(logadoComo(7, papel)).with(csrf())
                        .param("blocos", "ATALHOS", "TOTAIS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conta/personalizacao"))
                .andExpect(flash().attributeExists("mensagem"));
        verify(personalizacaoService).salvar(eq(7), eq(papel), eq(107), eq(TelaPersonalizavel.INICIO),
                eq(Set.of("ATALHOS", "TOTAIS")));
    }

    @Test
    @DisplayName("nada marcado envia conjunto vazio (tudo oculto)")
    void nadaMarcado() throws Exception {
        mvc.perform(post("/conta/personalizacao/inicio").with(logadoComo(7, PapelMembro.DONO)).with(csrf()))
                .andExpect(redirectedUrl("/conta/personalizacao"));
        verify(personalizacaoService).salvar(eq(7), eq(PapelMembro.DONO), eq(107), eq(TelaPersonalizavel.INICIO),
                eq(Set.of()));
    }

    @Test
    @DisplayName("erro de regra de negócio vira mensagem flash e volta ao formulário")
    void erroViraFlash() throws Exception {
        doThrow(new RegraNegocioException("Bloco inválido para esta loja."))
                .when(personalizacaoService).salvar(anyInt(), any(), any(), any(), any());

        mvc.perform(post("/conta/personalizacao/inicio").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("blocos", "<script>alert(1)</script>"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conta/personalizacao"))
                .andExpect(flash().attribute("erro", "Bloco inválido para esta loja."))
                .andExpect(flash().attributeCount(1));
    }
}
