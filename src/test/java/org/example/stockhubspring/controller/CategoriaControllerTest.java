package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.CategoriaForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
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

class CategoriaControllerTest extends ControllerTestBase {

    @Test
    @DisplayName("listagem usa a loja da sessão e devolve a busca digitada")
    void listagem() throws Exception {
        when(categoriaService.listar("livro", 7)).thenReturn(List.of());

        mvc.perform(get("/categorias").with(logadoComo(7)).param("busca", "livro"))
                .andExpect(status().isOk())
                .andExpect(view().name("categorias"))
                .andExpect(model().attribute("busca", "livro"))
                .andExpect(model().attributeExists("categorias"));
        verify(categoriaService).listar("livro", 7);
    }

    @Test
    @DisplayName("formulário novo mostra um CategoriaForm vazio")
    void formularioNovo() throws Exception {
        mvc.perform(get("/categorias/novo").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("categoria-form"))
                .andExpect(model().attributeExists("categoria"));
    }

    @Test
    @DisplayName("edição preenche o formulário com a categoria da loja")
    void edicaoPreencheFormulario() throws Exception {
        Usuario loja = Fabrica.usuario(7);
        Categoria categoria = Fabrica.categoria(12, "Livros", loja);
        when(categoriaService.buscarPorId(12, 7)).thenReturn(categoria);

        mvc.perform(get("/categorias/12/editar").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("categoria-form"))
                .andExpect(model().attributeExists("categoria"));
    }

    @Test
    @DisplayName("editar categoria de outra loja (IDOR) responde 404")
    void edicaoDeOutraLojaDa404() throws Exception {
        when(categoriaService.buscarPorId(12, 7)).thenThrow(new RecursoNaoEncontradoException("Categoria não encontrada."));
        mvc.perform(get("/categorias/12/editar").with(logadoComo(7)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cadastro válido chama o service com a loja da sessão e redireciona com mensagem")
    void cadastroValido() throws Exception {
        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "  Livros ").param("descricao", "Leitura"))
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("mensagem", "Categoria cadastrada."));

        ArgumentCaptor<CategoriaForm> form = ArgumentCaptor.forClass(CategoriaForm.class);
        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(categoriaService).salvar(form.capture(), usuario.capture());
        org.assertj.core.api.Assertions.assertThat(form.getValue().getNome()).isEqualTo("Livros");
        org.assertj.core.api.Assertions.assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
    }

    @Test
    @DisplayName("edição (com id) mostra a mensagem de atualização")
    void edicaoMensagem() throws Exception {
        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("id", "12").param("nome", "Livros"))
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("mensagem", "Categoria atualizada."));
    }

    @Test
    @DisplayName("nome vazio ou só com espaços volta ao mesmo formulário com erro e não chama o service")
    void nomeVazioVoltaComErro() throws Exception {
        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("categoria-form"))
                .andExpect(model().attributeHasFieldErrors("categoria", "nome"))
                .andExpect(model().attribute("errosCampos", hasKey("nome")));
        verify(categoriaService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("nome com 1 ou 121 caracteres e descrição com 501 caracteres são recusados")
    void faixasDeTamanho() throws Exception {
        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "a"))
                .andExpect(model().attributeHasFieldErrors("categoria", "nome"));
        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "a".repeat(121)))
                .andExpect(model().attributeHasFieldErrors("categoria", "nome"));
        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "ok")
                        .param("descricao", "d".repeat(501)))
                .andExpect(model().attributeHasFieldErrors("categoria", "descricao"));
        verify(categoriaService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("nome duplicado: a mensagem do service volta no formulário")
    void nomeDuplicadoVoltaNoFormulario() throws Exception {
        when(categoriaService.salvar(any(), any()))
                .thenThrow(new RegraNegocioException("Já existe uma categoria chamada \"Livros\" na sua loja."));

        mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "Livros"))
                .andExpect(status().isOk())
                .andExpect(view().name("categoria-form"))
                .andExpect(model().attribute("erro", "Já existe uma categoria chamada \"Livros\" na sua loja."));
    }

    @Test
    @DisplayName("excluir categoria sem produtos mostra 'excluída'")
    void excluirComSucesso() throws Exception {
        mvc.perform(post("/categorias/12/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("mensagem", "Categoria excluída."));
        verify(categoriaService).excluir(12, 7);
    }

    @Test
    @DisplayName("excluir categoria em uso mostra a mensagem do service com quantos produtos a usam")
    void excluirEmUsoMostraErro() throws Exception {
        org.mockito.Mockito.doThrow(new RegraNegocioException("Não é possível excluir a categoria \"Livros\": 3 produtos a usam."))
                .when(categoriaService).excluir(12, 7);

        mvc.perform(post("/categorias/12/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(redirectedUrl("/categorias"))
                .andExpect(flash().attribute("erro",
                        "Não é possível excluir a categoria \"Livros\": 3 produtos a usam."))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @DisplayName("excluir categoria de outra loja (IDOR) responde 404")
    void excluirDeOutraLojaDa404() throws Exception {
        org.mockito.Mockito.doThrow(new RecursoNaoEncontradoException("Categoria não encontrada."))
                .when(categoriaService).excluir(12, 7);

        mvc.perform(post("/categorias/12/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
