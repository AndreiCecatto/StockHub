package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TipoLoja;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

class AuthControllerTest extends ControllerTestBase {

    @Test
    @DisplayName("GET /login mostra a tela de login sem mensagens")
    void telaDeLogin() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("erro", "sucesso", "modoCadastro"));
    }

    @Test
    @DisplayName("GET /login?erro mostra mensagem única de credenciais incorretas")
    void loginComErro() throws Exception {
        mvc.perform(get("/login").param("erro", ""))
                .andExpect(view().name("login"))
                .andExpect(model().attribute("erro", "E-mail ou senha incorretos."));
    }

    @Test
    @DisplayName("GET /login?saiu e ?cadastrado mostram mensagem de sucesso")
    void loginComSucesso() throws Exception {
        mvc.perform(get("/login").param("saiu", ""))
                .andExpect(model().attribute("sucesso", "Você saiu do sistema."));
        mvc.perform(get("/login").param("cadastrado", ""))
                .andExpect(model().attribute("sucesso", "Cadastro concluído. Entre com seu e-mail e senha."));
    }

    @Test
    @DisplayName("GET /cadastro mostra o formulário em modo cadastro")
    void telaDeCadastro() throws Exception {
        mvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("modoCadastro", true));
    }

    @Test
    @DisplayName("cadastro válido normaliza o e-mail, chama o service e redireciona para /login?cadastrado")
    void cadastroValido() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "  Minha Loja ").param("email", "  LOJA@Email.Teste ")
                        .param("senha", "senha-segura-1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?cadastrado"));

        verify(usuarioService).cadastrarLoja("Minha Loja", "loja@email.teste", "senha-segura-1", TipoLoja.ESTOQUE, null);
    }

    @Test
    @DisplayName("cadastro inválido volta ao mesmo formulário com erros por campo e não chama o service")
    void cadastroInvalidoVoltaComErros() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "").param("email", "isto-nao-e-email").param("senha", "curta"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "nome", "email", "senha"))
                .andExpect(model().attribute("modoCadastro", true))
                .andExpect(model().attributeExists("erro"))
                .andExpect(model().attribute("errosCampos", hasKey("senha")));

        verify(usuarioService, never()).cadastrarLoja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("cadastro inválido nunca devolve a senha digitada")
    void cadastroInvalidoNaoDevolveSenha() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Loja").param("email", "ruim").param("senha", "curta"))
                .andExpect(model().attribute("cadastroForm", hasProperty("senha", nullValue())));
    }

    @Test
    @DisplayName("senha com mais de 72 bytes é recusada pela validação do formulário")
    void senhaGrandeDemaisRecusada() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Loja").param("email", "a@b.com").param("senha", "a".repeat(73)))
                .andExpect(view().name("login"))
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "senhaDentroDoLimite"));
        verify(usuarioService, never()).cadastrarLoja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("e-mail duplicado: o service responde com mensagem genérica e a tela a mostra, sem a senha")
    void emailDuplicadoMostraMensagemGenerica() throws Exception {
        when(usuarioService.cadastrarLoja(anyString(), anyString(), anyString(), any(), any()))
                .thenThrow(new RegraNegocioException("Não foi possível concluir o cadastro. Verifique os dados e tente novamente."));

        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Loja").param("email", "dup@email.teste").param("senha", "senha-segura-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("erro",
                        "Não foi possível concluir o cadastro. Verifique os dados e tente novamente."))
                .andExpect(model().attribute("email", "dup@email.teste"))
                .andExpect(model().attribute("modoCadastro", true))
                .andExpect(model().attribute("cadastroForm", hasProperty("senha", is((String) null))));
    }

    @Test
    @DisplayName("nome com <script> chega ao service como texto puro (o escape é responsabilidade da view, via c:out)")
    void nomeComScriptChegaComoTextoPuro() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "<script>alert(1)</script>").param("email", "x@email.teste")
                        .param("senha", "senha-segura-1"))
                .andExpect(redirectedUrl("/login?cadastrado"));
        verify(usuarioService).cadastrarLoja("<script>alert(1)</script>", "x@email.teste", "senha-segura-1", TipoLoja.ESTOQUE, null);
    }

    @Test
    @DisplayName("cadastro com tipo e ramo válidos repassa os dois ao service")
    void cadastroComTipoERamo() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Oficina").param("email", "o@email.teste").param("senha", "senha-segura-1")
                        .param("tipoLoja", "oficina").param("subtipo", "MOTO"))
                .andExpect(redirectedUrl("/login?cadastrado"));
        verify(usuarioService).cadastrarLoja("Oficina", "o@email.teste", "senha-segura-1", TipoLoja.OFICINA, Subtipo.MOTO);
    }

    @Test
    @DisplayName("ramo que não combina com o tipo volta ao formulário com erro e não chama o service")
    void ramoIncompativelRecusado() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Loja").param("email", "a@b.com").param("senha", "senha-segura-1")
                        .param("tipoLoja", "ESTOQUE").param("subtipo", "FARMACIA"))
                .andExpect(view().name("login"))
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "subtipoValido"))
                .andExpect(model().attribute("tipoEscolhido", "ESTOQUE"));
        verify(usuarioService, never()).cadastrarLoja(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("tipo ou ramo inventados viram erro de validação, não erro 400")
    void tipoInventadoRecusado() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Loja").param("email", "a@b.com").param("senha", "senha-segura-1")
                        .param("tipoLoja", "BANCO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "tipoLojaValido"));
        mvc.perform(post("/cadastro").with(csrf())
                        .param("nome", "Loja").param("email", "a@b.com").param("senha", "senha-segura-1")
                        .param("subtipo", "XYZ"))
                .andExpect(model().attributeHasFieldErrors("cadastroForm", "subtipoValido"));
    }

    @Test
    @DisplayName("POST /conta/tipo-loja altera o tipo da loja logada e exige CSRF")
    void alterarTipoLoja() throws Exception {
        mvc.perform(post("/conta/tipo-loja").with(logadoComo(7)).with(csrf())
                        .param("tipoLoja", "MISTO").param("subtipo", "MECANICA"))
                .andExpect(redirectedUrl("/dashboard"));
        verify(usuarioService).alterarTipoLoja(7, TipoLoja.MISTO, Subtipo.MECANICA);

        mvc.perform(post("/conta/tipo-loja").with(logadoComo(7)).param("tipoLoja", "MISTO"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tipo inválido em /conta/tipo-loja não chama o service")
    void alterarTipoInvalido() throws Exception {
        mvc.perform(post("/conta/tipo-loja").with(logadoComo(7)).with(csrf()).param("tipoLoja", "X"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attributeExists("erro"));
        verify(usuarioService, never()).alterarTipoLoja(any(), any(), any());
    }
}
