package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Desfazer movimentação (RN-69, RN-70) na cadeia de segurança real. */
class EstornoEClientesWebTest extends ControllerTestBase {

    @BeforeEach
    void stubs() {
        when(estoqueService.listarMovimentacoes(anyInt(), any(MovimentacaoFiltro.class), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));
        when(produtoService.listarParaSelecao(anyInt())).thenReturn(List.of());
    }

    @ParameterizedTest(name = "{0} recebe 403 ao desfazer")
    @EnumSource(value = PapelMembro.class, names = {"OPERADOR", "VENDEDOR", "MECANICO"})
    @DisplayName("papéis operacionais não desfazem movimentação")
    void operacionaisNaoDesfazem(PapelMembro papel) throws Exception {
        mvc.perform(post("/estoque/5/estornar").with(logadoComo(7, papel)).with(csrf()).param("motivo", "erro"))
                .andExpect(status().isForbidden());
        verify(estoqueService, never()).estornar(anyInt(), anyString(), any(Usuario.class));
    }

    @ParameterizedTest(name = "{0} desfaz")
    @EnumSource(value = PapelMembro.class, names = {"DONO", "GERENTE"})
    @DisplayName("dono e gerente desfazem e voltam ao histórico com a mensagem")
    void gestoresDesfazem(PapelMembro papel) throws Exception {
        MovimentacaoEstoque estorno = new MovimentacaoEstoque();
        estorno.setQuantidadeAnterior(10);
        estorno.setQuantidadeNova(7);
        when(estoqueService.estornar(eq(5), eq("lancei errado"), any(Usuario.class))).thenReturn(estorno);

        mvc.perform(post("/estoque/5/estornar").with(logadoComo(7, papel)).with(csrf()).param("motivo", "lancei errado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estoque"))
                .andExpect(flash().attributeExists("mensagem"));
    }

    @Test
    @DisplayName("recusa de regra volta ao histórico com a mensagem de erro")
    void recusaDeRegra() throws Exception {
        when(estoqueService.estornar(eq(5), any(), any(Usuario.class)))
                .thenThrow(new RegraNegocioException("Esta movimentação já foi desfeita."));

        mvc.perform(post("/estoque/5/estornar").with(logadoComo(7, PapelMembro.DONO)).with(csrf()).param("motivo", "x"))
                .andExpect(redirectedUrl("/estoque"))
                .andExpect(flash().attribute("erro", "Esta movimentação já foi desfeita."));
    }

    @Test
    @DisplayName("sem token de segurança o pedido de desfazer é recusado")
    void semCsrf() throws Exception {
        mvc.perform(post("/estoque/5/estornar").with(logadoComo(7, PapelMembro.DONO)).param("motivo", "x"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(estoqueService);
    }

    @Test
    @DisplayName("o histórico recebe os conjuntos que decidem onde aparece o botão Desfazer")
    void historicoTrazConjuntos() throws Exception {
        when(estoqueService.estornaveis(any(), any(Usuario.class))).thenReturn(Set.of(1));
        when(estoqueService.jaDesfeitas(any())).thenReturn(Set.of(2));

        mvc.perform(get("/estoque").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("estornaveis", Set.of(1)))
                .andExpect(model().attribute("jaDesfeitas", Set.of(2)));
    }

}
