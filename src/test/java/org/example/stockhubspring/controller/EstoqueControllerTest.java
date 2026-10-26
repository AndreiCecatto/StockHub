package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

class EstoqueControllerTest extends ControllerTestBase {

    @BeforeEach
    void stubs() {
        when(estoqueService.listarMovimentacoes(anyInt(), anyInt(), anyInt())).thenReturn(new PageImpl<>(List.of()));
    }

    private MockHttpServletRequestBuilder movimentar() {
        return post("/estoque/movimentar").with(logadoComo(7)).with(csrf());
    }

    @Test
    @DisplayName("tela de estoque lista só o histórico e os produtos da loja da sessão")
    void telaDeEstoque() throws Exception {
        mvc.perform(get("/estoque").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeExists("produtos", "movimentacoes", "tipos", "paginaAtual", "totalPaginas"));
        verify(estoqueService).listarMovimentacoes(eq(7), eq(1), anyInt());
        verify(produtoService).listarParaSelecao(7);
    }

    @Test
    @DisplayName("movimentação válida chama o service com a loja da sessão e informa saldo anterior e novo")
    void movimentacaoValida() throws Exception {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setQuantidadeAnterior(10);
        mov.setQuantidadeNova(15);
        when(estoqueService.movimentar(eq(4), eq(TipoMovimentacao.ENTRADA), eq(5), eq("Compra"), any(Usuario.class)))
                .thenReturn(mov);

        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "ENTRADA")
                        .param("quantidade", "5").param("observacao", " Compra "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estoque"))
                .andExpect(flash().attribute("mensagem", containsString("passou de 10 para 15")));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(estoqueService).movimentar(eq(4), eq(TipoMovimentacao.ENTRADA), eq(5), eq("Compra"), usuario.capture());
        org.assertj.core.api.Assertions.assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
    }

    @Test
    @DisplayName("formulário vazio volta à mesma tela com erros por campo e não chama o service")
    void formularioVazioVoltaComErros() throws Exception {
        mvc.perform(movimentar())
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "produtoId", "tipo", "quantidade"))
                .andExpect(model().attribute("errosCampos", hasKey("quantidade")))
                .andExpect(model().attributeExists("produtos", "movimentacoes", "erro"));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("quantidade negativa é recusada pela validação do formulário")
    void quantidadeNegativaRecusada() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "-3"))
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "quantidade"));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("quantidade com letras ou gigante dá erro de formulário em português, não 500")
    void quantidadeInvalidaDaErroDeFormulario() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "quantidade"));
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "99999999999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "quantidade"));
    }

    @Test
    @DisplayName("tipo de movimentação inexistente dá erro de formulário")
    void tipoInexistenteDaErro() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "TRANSFERENCIA").param("quantidade", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "tipo"))
                .andExpect(model().attribute("erro", containsString("tipo de movimentação")));
    }

    @Test
    @DisplayName("observação com mais de 255 caracteres é recusada")
    void observacaoLongaRecusada() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "ENTRADA").param("quantidade", "1")
                        .param("observacao", "o".repeat(256)))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "observacao"));
    }

    @Test
    @DisplayName("regra de negócio violada (saída acima do saldo) volta à tela de estoque com a mensagem")
    void saidaAcimaDoSaldoVoltaComMensagem() throws Exception {
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RegraNegocioException("Saída de 50 unidade(s) maior que o saldo atual (10). Nada foi registrado."));

        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attribute("erro",
                        "Saída de 50 unidade(s) maior que o saldo atual (10). Nada foi registrado."))
                .andExpect(model().attributeExists("produtos", "movimentacoes"));
    }

    @Test
    @DisplayName("produto de outra loja (IDOR) na movimentação responde 404")
    void produtoDeOutraLojaDa404() throws Exception {
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));

        mvc.perform(movimentar().param("produtoId", "999").param("tipo", "ENTRADA").param("quantidade", "1"))
                .andExpect(status().isNotFound());
    }
}
