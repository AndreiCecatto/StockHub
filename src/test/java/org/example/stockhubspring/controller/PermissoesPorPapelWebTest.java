package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Permissões por papel (RN-62 a RN-65) na cadeia de segurança real. */
class PermissoesPorPapelWebTest extends ControllerTestBase {

    @org.junit.jupiter.api.BeforeEach
    void stubsDeListagem() {
        when(produtoService.listarPagina(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
    }

    @ParameterizedTest(name = "{0} não exclui produto nem categoria")
    @EnumSource(value = PapelMembro.class, names = {"OPERADOR", "VENDEDOR", "MECANICO"})
    @DisplayName("papéis operacionais recebem 403 ao excluir")
    void operacionaisNaoExcluem(PapelMembro papel) throws Exception {
        mvc.perform(post("/produtos/4/excluir").with(logadoComo(7, papel)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/categorias/12/excluir").with(logadoComo(7, papel)).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(categoriaService);
        verify(produtoService, never()).excluir(anyInt(), anyInt());
    }

    @ParameterizedTest(name = "{0} exclui")
    @EnumSource(value = PapelMembro.class, names = {"DONO", "GERENTE"})
    @DisplayName("dono e gerente excluem")
    void gestoresExcluem(PapelMembro papel) throws Exception {
        mvc.perform(post("/categorias/12/excluir").with(logadoComo(7, papel)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(categoriaService).excluir(12, 7);
    }

    @Test
    @DisplayName("operador não muda o tipo da loja; dono muda")
    void tipoDaLojaSoParaGestores() throws Exception {
        mvc.perform(post("/conta/tipo-loja").with(logadoComo(7, PapelMembro.OPERADOR)).with(csrf())
                        .param("tipoLoja", "COMERCIO"))
                .andExpect(status().isForbidden());
        verify(usuarioService, never()).alterarTipoLoja(anyInt(), any(), any());

        mvc.perform(post("/conta/tipo-loja").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("tipoLoja", "COMERCIO"))
                .andExpect(status().is3xxRedirection());
        verify(usuarioService).alterarTipoLoja(eq(7), any(), any());
    }

    @ParameterizedTest(name = "{0} não acessa a equipe")
    @EnumSource(value = PapelMembro.class, names = {"OPERADOR", "VENDEDOR", "MECANICO"})
    @DisplayName("equipe: 403 para papéis operacionais, em leitura e em escrita")
    void equipeBloqueadaParaOperacionais(PapelMembro papel) throws Exception {
        mvc.perform(get("/membros").with(logadoComo(7, papel))).andExpect(status().isForbidden());
        mvc.perform(post("/membros").with(logadoComo(7, papel)).with(csrf())
                        .param("nome", "X").param("email", "x@teste.invalid").param("senha", "senha-1234")
                        .param("papel", "OPERADOR"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/membros/5/desativar").with(logadoComo(7, papel)).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(membroService);
    }

    @Test
    @DisplayName("dono vê a equipe da PRÓPRIA loja e a tela oferece gerente; gerente não vê a opção gerente")
    void donoVeEquipeDaPropriaLoja() throws Exception {
        MembroLoja m = new MembroLoja();
        m.setId(1);
        m.setNome("Ana");
        m.setEmail("ana@teste.invalid");
        m.setPapel(PapelMembro.DONO);
        m.setAtivo(true);
        when(membroService.listar(7)).thenReturn(List.of(m));

        mvc.perform(get("/membros").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(view().name("membros"))
                .andExpect(model().attribute("papeisEscolhiveis", new PapelMembro[]{
                        PapelMembro.GERENTE, PapelMembro.OPERADOR, PapelMembro.VENDEDOR, PapelMembro.MECANICO}));
        verify(membroService).listar(7);
        verify(membroService, never()).listar(8);

        mvc.perform(get("/membros").with(logadoComo(7, PapelMembro.GERENTE)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("papeisEscolhiveis", new PapelMembro[]{
                        PapelMembro.OPERADOR, PapelMembro.VENDEDOR, PapelMembro.MECANICO}));
    }

    @Test
    @DisplayName("criar membro usa a loja e o papel da SESSÃO, não valores do formulário; exige CSRF")
    void criarUsaLojaDaSessao() throws Exception {
        mvc.perform(post("/membros").with(logadoComo(7, PapelMembro.DONO))
                        .param("nome", "Bia").param("email", "bia@teste.invalid").param("senha", "senha-1234")
                        .param("papel", "OPERADOR"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(membroService);

        mvc.perform(post("/membros").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("lojaCodigo", "99")
                        .param("nome", "Bia").param("email", "bia@teste.invalid").param("senha", "senha-1234")
                        .param("papel", "OPERADOR"))
                .andExpect(redirectedUrl("/membros"));
        verify(membroService).criar(7, PapelMembro.DONO, "Bia", "bia@teste.invalid", "senha-1234",
                PapelMembro.OPERADOR);
    }

    @ParameterizedTest(name = "POST /membros/5/{0} exige CSRF")
    @ValueSource(strings = {"desativar", "reativar"})
    @DisplayName("desativar e reativar exigem token CSRF")
    void desativarExigeCsrf(String acao) throws Exception {
        mvc.perform(post("/membros/5/" + acao).with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(membroService);
    }

    @Test
    @DisplayName("desativar passa a loja, o membro e o papel de quem pede")
    void desativarPassaContextoDaSessao() throws Exception {
        mvc.perform(post("/membros/5/desativar").with(logadoComo(7, PapelMembro.GERENTE)).with(csrf()))
                .andExpect(redirectedUrl("/membros"));
        verify(membroService).desativar(7, 107, PapelMembro.GERENTE, 5);
    }

    @ParameterizedTest(name = "{0} movimenta o estoque")
    @EnumSource(PapelMembro.class)
    @DisplayName("todos os papéis movimentam e a movimentação leva quem fez")
    void todosMovimentam(PapelMembro papel) throws Exception {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any(Usuario.class))).thenReturn(mov);

        mvc.perform(post("/estoque/movimentar").with(logadoComo(7, papel)).with(csrf())
                        .param("produtoId", "4").param("tipo", TipoMovimentacao.ENTRADA.name())
                        .param("quantidade", "5").param("observacao", "Compra"))
                .andExpect(redirectedUrl("/estoque"));

        org.mockito.ArgumentCaptor<Usuario> usuario = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(estoqueService).movimentar(eq(4), eq(TipoMovimentacao.ENTRADA), eq(5), eq("Compra"), usuario.capture());
        org.assertj.core.api.Assertions.assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
        org.assertj.core.api.Assertions.assertThat(usuario.getValue().getMembroId()).isEqualTo(107);
        org.assertj.core.api.Assertions.assertThat(usuario.getValue().getPapel()).isEqualTo(papel);
    }

    @Test
    @DisplayName("operador consulta produtos normalmente")
    void operadorConsulta() throws Exception {
        when(estoqueService.listarMovimentacoes(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/produtos").with(logadoComo(7, PapelMembro.OPERADOR))).andExpect(status().isOk());
        mvc.perform(get("/estoque").with(logadoComo(7, PapelMembro.OPERADOR))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("membro desativado perde a sessão no pedido seguinte")
    void membroDesativadoPerdeASessao() throws Exception {
        when(membroLojaRepository.existsByIdAndAtivoTrue(107)).thenReturn(false);

        mvc.perform(get("/produtos").with(logadoComo(7, PapelMembro.OPERADOR)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("membro ativo segue com a sessão")
    void membroAtivoSegue() throws Exception {
        when(estoqueService.listarMovimentacoes(anyInt(), any(), anyInt(), anyInt()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(membroLojaRepository.existsByIdAndAtivoTrue(107)).thenReturn(true);

        mvc.perform(get("/produtos").with(logadoComo(7, PapelMembro.OPERADOR)).session(new MockHttpSession()))
                .andExpect(status().isOk());
    }
}
