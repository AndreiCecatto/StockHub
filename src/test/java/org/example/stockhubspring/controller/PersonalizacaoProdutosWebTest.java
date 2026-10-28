package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.BlocoTela;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.service.CamposRamo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

/** Personalização da lista de produtos (AD-20, RN-115 a RN-120) na cadeia de segurança real. */
class PersonalizacaoProdutosWebTest extends ControllerTestBase {

    @Test
    @DisplayName("GET traz as quatro listas do modelo, inclusive blocosProdutos e estadoColunas")
    void getTrazColunas() throws Exception {
        List<BlocoTela> colunas = List.of(BlocoTela.COL_PRECO, BlocoTela.COL_LOTE);
        Map<String, Boolean> estado = new LinkedHashMap<>();
        estado.put("COL_PRECO", false);
        estado.put("COL_LOTE", true);
        when(personalizacaoService.blocosAplicaveis(7, TelaPersonalizavel.PRODUTOS)).thenReturn(colunas);
        when(personalizacaoService.blocosVisiveis(7, TelaPersonalizavel.PRODUTOS)).thenReturn(estado);

        mvc.perform(get("/conta/personalizacao").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(view().name("personalizacao"))
                .andExpect(model().attributeExists("blocosInicio", "estadoBlocos"))
                .andExpect(model().attribute("blocosProdutos", colunas))
                .andExpect(model().attribute("estadoColunas", estado));
        verify(personalizacaoService).blocosAplicaveis(7, TelaPersonalizavel.PRODUTOS);
        verify(personalizacaoService).blocosVisiveis(7, TelaPersonalizavel.PRODUTOS);
    }

    @Test
    @DisplayName("POST produtos salva a tela PRODUTOS e diz 'Lista de produtos personalizada.'")
    void postProdutos() throws Exception {
        mvc.perform(post("/conta/personalizacao/produtos").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("blocos", "COL_PRECO", "COL_CATEGORIA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conta/personalizacao"))
                .andExpect(flash().attribute("mensagem", "Lista de produtos personalizada."));
        verify(personalizacaoService).salvar(eq(7), eq(PapelMembro.DONO), eq(107), eq(TelaPersonalizavel.PRODUTOS),
                eq(Set.of("COL_PRECO", "COL_CATEGORIA")));
    }

    @Test
    @DisplayName("POST inicio salva a tela INICIO e diz 'Início personalizado.'")
    void postInicio() throws Exception {
        mvc.perform(post("/conta/personalizacao/inicio").with(logadoComo(7, PapelMembro.GERENTE)).with(csrf())
                        .param("blocos", "TOTAIS"))
                .andExpect(redirectedUrl("/conta/personalizacao"))
                .andExpect(flash().attribute("mensagem", "Início personalizado."));
        verify(personalizacaoService).salvar(eq(7), eq(PapelMembro.GERENTE), eq(107), eq(TelaPersonalizavel.INICIO),
                eq(Set.of("TOTAIS")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuarios", "INICIO", "outra"})
    @DisplayName("slug inválido gera flash de erro e nada é salvo")
    void slugInvalido(String slug) throws Exception {
        mvc.perform(post("/conta/personalizacao/" + slug).with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("blocos", "ATALHOS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/conta/personalizacao"))
                .andExpect(flash().attribute("erro", "Tela inválida para personalizar."))
                .andExpect(flash().attributeCount(1));
        verify(personalizacaoService, never()).salvar(anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("coluna recusada pelo serviço (outro ramo) volta como flash de erro")
    void colunaRecusada() throws Exception {
        doThrow(new RegraNegocioException("Bloco inválido para esta loja."))
                .when(personalizacaoService).salvar(anyInt(), any(), any(), eq(TelaPersonalizavel.PRODUTOS), any());

        mvc.perform(post("/conta/personalizacao/produtos").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("blocos", "COL_LOTE"))
                .andExpect(redirectedUrl("/conta/personalizacao"))
                .andExpect(flash().attribute("erro", "Bloco inválido para esta loja."))
                .andExpect(flash().attributeCount(1));
    }

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"VENDEDOR", "OPERADOR", "MECANICO"})
    @DisplayName("papel sem permissão recebe 403 no POST de produtos")
    void papelSemPermissao(PapelMembro papel) throws Exception {
        mvc.perform(post("/conta/personalizacao/produtos").with(logadoComo(7, papel)).with(csrf())
                        .param("blocos", "COL_PRECO"))
                .andExpect(status().isForbidden());
        verify(personalizacaoService, never()).salvar(anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST de produtos sem CSRF é recusado e nada é salvo")
    void semCsrf() throws Exception {
        mvc.perform(post("/conta/personalizacao/produtos").with(logadoComo(7, PapelMembro.DONO))
                        .param("blocos", "COL_PRECO"))
                .andExpect(status().isForbidden());
        verify(personalizacaoService, never()).salvar(anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST de produtos sem login não salva")
    void semLogin() throws Exception {
        mvc.perform(post("/conta/personalizacao/produtos").with(csrf()).param("blocos", "COL_PRECO"))
                .andExpect(status().is3xxRedirection());
        verify(personalizacaoService, never()).salvar(anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /produtos publica colunas da loja, sem camposRamo")
    void listaPublicaCamposEColunas() throws Exception {
        CamposRamo.Permitidos farmacia = CamposRamo.permitidos(Subtipo.FARMACIA);
        Map<String, Boolean> colunas = new LinkedHashMap<>();
        colunas.put("COL_PRECO", false);
        colunas.put("COL_LOTE", true);
        when(produtoService.listarPagina(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(produtoService.camposPermitidos(any())).thenReturn(farmacia);
        when(personalizacaoService.blocosVisiveis(7, TelaPersonalizavel.PRODUTOS)).thenReturn(colunas);

        mvc.perform(get("/produtos").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos"))
                .andExpect(model().attributeDoesNotExist("camposRamo"))
                .andExpect(model().attribute("colunas", colunas));
        verify(personalizacaoService).blocosVisiveis(7, TelaPersonalizavel.PRODUTOS);
    }
}
