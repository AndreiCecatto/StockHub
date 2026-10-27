package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ClienteFiltro;
import org.example.stockhubspring.dto.ClienteForm;
import org.example.stockhubspring.dto.ClienteResumo;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

/** Telas de clientes (RN-76, RN-91 a RN-98) na cadeia de segurança real. */
class ClienteWebTest extends ControllerTestBase {

    private Parceiro cliente;

    @BeforeEach
    void stubs() {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(Optional.of(TipoLoja.MISTO));
        when(clienteService.listarPagina(eq(7), any(ClienteFiltro.class))).thenReturn(new PageImpl<>(List.of()));
        cliente = new Parceiro();
        cliente.setNome("Ana");
        cliente.setCliente(true);
    }

    @ParameterizedTest(name = "loja {0}: tela de clientes abre")
    @EnumSource(value = TipoLoja.class, names = {"COMERCIO", "OFICINA", "MISTO"})
    @DisplayName("módulo Vendas ou OS ligado mostra a lista de clientes")
    void listaComModulo(TipoLoja tipo) throws Exception {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(Optional.of(tipo));

        mvc.perform(get("/clientes").with(logadoComo(7, PapelMembro.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(view().name("clientes"))
                .andExpect(model().attributeExists("clientes", "filtro", "paginaAtual", "totalPaginas"))
                .andExpect(model().attribute("temOs", tipo.temModulo(org.example.stockhubspring.model.Modulo.OS)));
    }

    @Test
    @DisplayName("a lista repassa busca e filtros normalizados, da loja logada")
    void listaComFiltros() throws Exception {
        mvc.perform(get("/clientes").with(logadoComo(7, PapelMembro.DONO))
                        .param("busca", " 529.982 ").param("situacao", "inativos").param("os", "sem")
                        .param("ordem", "recentes").param("pagina", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("parametrosFiltros",
                        "&busca=529.982&situacao=inativos&os=sem&ordem=recentes"));

        ArgumentCaptor<ClienteFiltro> filtro = ArgumentCaptor.forClass(ClienteFiltro.class);
        verify(clienteService).listarPagina(eq(7), filtro.capture());
        assertThat(filtro.getValue()).isEqualTo(new ClienteFiltro("529.982", ClienteFiltro.Situacao.INATIVOS,
                ClienteFiltro.ComOs.SEM, ClienteFiltro.Ordem.RECENTES, 3, ClienteFiltro.TAMANHO_PADRAO));
    }

    @Test
    @DisplayName("valor de filtro desconhecido cai no padrão (ativos, todos, por nome)")
    void filtroDesconhecido() throws Exception {
        mvc.perform(get("/clientes").with(logadoComo(7, PapelMembro.DONO)).param("situacao", "xx").param("os", "?"))
                .andExpect(status().isOk());
        ArgumentCaptor<ClienteFiltro> filtro = ArgumentCaptor.forClass(ClienteFiltro.class);
        verify(clienteService).listarPagina(eq(7), filtro.capture());
        assertThat(filtro.getValue().temFiltros()).isFalse();
    }

    @Test
    @DisplayName("loja só de estoque não tem as telas de clientes (404) nem cadastra")
    void semModulo() throws Exception {
        when(usuarioRepository.findTipoLojaByCodigo(7)).thenReturn(Optional.of(TipoLoja.ESTOQUE));

        mvc.perform(get("/clientes").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(get("/clientes/novo").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(get("/clientes/3").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(post("/clientes").with(logadoComo(7, PapelMembro.DONO)).with(csrf()).param("nome", "Ana"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("sem login a tela manda para o login")
    void semLogin() throws Exception {
        mvc.perform(get("/clientes")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/clientes/3")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("formulário de novo cliente abre vazio")
    void novo() throws Exception {
        mvc.perform(get("/clientes/novo").with(logadoComo(7, PapelMembro.VENDEDOR)))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente-form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("editando", false));
    }

    @Test
    @DisplayName("cadastrar repassa todos os campos da loja logada e vai para o detalhe do cliente")
    void cadastra() throws Exception {
        Parceiro salvo = new Parceiro();
        salvo.setNome("Ana");
        org.springframework.test.util.ReflectionTestUtils.setField(salvo, "id", 42);
        when(clienteService.cadastrar(any(Usuario.class), any(ClienteForm.class))).thenReturn(salvo);

        mvc.perform(post("/clientes").with(logadoComo(7, PapelMembro.VENDEDOR)).with(csrf())
                        .param("nome", "Ana").param("documento", "529.982.247-25").param("telefone", "(55) 3222-1000")
                        .param("email", "ana@exemplo.com").param("cidade", "Santa Maria").param("uf", "RS")
                        .param("observacoes", "paga em dia"))
                .andExpect(redirectedUrl("/clientes/42"))
                .andExpect(flash().attributeExists("mensagem"));

        ArgumentCaptor<ClienteForm> form = ArgumentCaptor.forClass(ClienteForm.class);
        verify(clienteService).cadastrar(any(Usuario.class), form.capture());
        assertThat(form.getValue().nome()).isEqualTo("Ana");
        assertThat(form.getValue().documento()).isEqualTo("529.982.247-25");
        assertThat(form.getValue().telefone()).isEqualTo("(55) 3222-1000");
        assertThat(form.getValue().email()).isEqualTo("ana@exemplo.com");
        assertThat(form.getValue().observacoes()).isEqualTo("paga em dia");
        assertThat(form.getValue().cep()).isNull();
    }

    @Test
    @DisplayName("erro de cadastro volta ao formulário com a mensagem e o que foi digitado")
    void erroDeCadastro() throws Exception {
        when(clienteService.cadastrar(any(Usuario.class), any(ClienteForm.class)))
                .thenThrow(new RegraNegocioException("O CPF ou CNPJ informado não é válido."));

        mvc.perform(post("/clientes").with(logadoComo(7, PapelMembro.DONO)).with(csrf())
                        .param("documento", "123").param("nome", "Ana"))
                .andExpect(redirectedUrl("/clientes/novo"))
                .andExpect(flash().attribute("erro", "O CPF ou CNPJ informado não é válido."))
                .andExpect(flash().attributeExists("form"));
    }

    @Test
    @DisplayName("cadastrar sem o token de segurança é recusado")
    void semCsrf() throws Exception {
        mvc.perform(post("/clientes").with(logadoComo(7, PapelMembro.DONO)).param("nome", "Ana"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("detalhe traz o cliente e o resumo da loja logada")
    void detalhe() throws Exception {
        when(clienteService.buscar(7, 5)).thenReturn(cliente);
        when(clienteService.resumo(7, 5)).thenReturn(new ClienteResumo(2, 1, 1, new BigDecimal("150.00"), null, List.of()));

        mvc.perform(get("/clientes/5").with(logadoComo(7, PapelMembro.MECANICO)))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente-detalhe"))
                .andExpect(model().attribute("cliente", cliente))
                .andExpect(model().attributeExists("resumo"))
                .andExpect(model().attribute("temOs", true));
    }

    @Test
    @DisplayName("cliente de outra loja ou inexistente é 404 no detalhe e na edição")
    void naoEncontrado() throws Exception {
        when(clienteService.buscar(7, 9)).thenThrow(new RecursoNaoEncontradoException("Cliente não encontrado."));

        mvc.perform(get("/clientes/9").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
        mvc.perform(get("/clientes/9/editar").with(logadoComo(7, PapelMembro.DONO))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("edição abre com os dados atuais e trava o documento de quem também é fornecedor")
    void editar() throws Exception {
        cliente.setFornecedor(true);
        cliente.setDocumento("52998224725");
        when(clienteService.buscar(7, 5)).thenReturn(cliente);

        mvc.perform(get("/clientes/5/editar").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isOk())
                .andExpect(view().name("cliente-form"))
                .andExpect(model().attribute("editando", true))
                .andExpect(model().attribute("documentoTravado", true))
                .andExpect(model().attribute("form", ClienteForm.de(cliente)));
    }

    @Test
    @DisplayName("salvar a edição volta ao detalhe; erro volta à edição com a mensagem")
    void atualizar() throws Exception {
        mvc.perform(post("/clientes/5").with(logadoComo(7, PapelMembro.DONO)).with(csrf()).param("nome", "Ana Maria"))
                .andExpect(redirectedUrl("/clientes/5"))
                .andExpect(flash().attributeExists("mensagem"));
        verify(clienteService).atualizar(eq(7), eq(5), any(ClienteForm.class));

        when(clienteService.atualizar(eq(7), eq(5), any(ClienteForm.class)))
                .thenThrow(new RegraNegocioException("Já existe um cliente com este CPF ou CNPJ."));
        mvc.perform(post("/clientes/5").with(logadoComo(7, PapelMembro.DONO)).with(csrf()).param("nome", "Ana"))
                .andExpect(redirectedUrl("/clientes/5/editar"))
                .andExpect(flash().attribute("erro", "Já existe um cliente com este CPF ou CNPJ."));
    }

    @Test
    @DisplayName("inativar e reativar passam pela loja logada e exigem o token")
    void situacao() throws Exception {
        mvc.perform(post("/clientes/5/inativar").with(logadoComo(7, PapelMembro.DONO)).with(csrf()))
                .andExpect(redirectedUrl("/clientes/5"));
        verify(clienteService).alterarSituacao(7, 5, false);
        mvc.perform(post("/clientes/5/reativar").with(logadoComo(7, PapelMembro.DONO)).with(csrf()))
                .andExpect(redirectedUrl("/clientes/5"));
        verify(clienteService).alterarSituacao(7, 5, true);
        mvc.perform(post("/clientes/5/inativar").with(logadoComo(7, PapelMembro.DONO)))
                .andExpect(status().isForbidden());
    }
}
