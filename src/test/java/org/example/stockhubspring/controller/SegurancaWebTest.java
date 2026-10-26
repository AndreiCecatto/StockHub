package org.example.stockhubspring.controller;

import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Cadeia de segurança real (SecurityConfig): login obrigatório, CSRF, rotas públicas, logout por POST. */
class SegurancaWebTest extends ControllerTestBase {

    @Nested
    @DisplayName("sem login")
    class SemLogin {

        @ParameterizedTest(name = "GET {0} redireciona para /login")
        @ValueSource(strings = {"/", "/dashboard", "/produtos", "/produtos/novo", "/produtos/1", "/produtos/1/editar",
                "/categorias", "/categorias/novo", "/categorias/1/editar", "/estoque", "/estoque-baixo",
                "/unidades", "/valor-estoque"})
        @DisplayName("páginas internas exigem login")
        void paginasInternasRedirecionamParaLogin(String caminho) throws Exception {
            mvc.perform(get(caminho))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }

        @ParameterizedTest(name = "POST {0} sem login e com CSRF redireciona para /login")
        @ValueSource(strings = {"/produtos", "/produtos/1/excluir", "/categorias", "/categorias/1/excluir",
                "/estoque/movimentar"})
        @DisplayName("ações de escrita exigem login mesmo com token CSRF válido")
        void escritaExigeLogin(String caminho) throws Exception {
            mvc.perform(post(caminho).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
            verifyNoInteractions(produtoService, categoriaService, estoqueService);
        }

        @ParameterizedTest(name = "POST {0} sem login e sem CSRF é 403")
        @ValueSource(strings = {"/produtos", "/categorias", "/estoque/movimentar", "/cadastro"})
        @DisplayName("POST sem token CSRF é negado")
        void postSemCsrfNegado(String caminho) throws Exception {
            mvc.perform(post(caminho))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(produtoService, categoriaService, estoqueService, usuarioService);
        }
    }

    @Nested
    @DisplayName("rotas públicas")
    class RotasPublicas {

        @Test
        @DisplayName("GET /login é público e mostra a tela de login")
        void loginPublico() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @DisplayName("GET /index.jsp é público e mostra a tela de login")
        void indexJspPublico() throws Exception {
            mvc.perform(get("/index.jsp"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @DisplayName("GET /cadastro é público e mostra o formulário de cadastro")
        void cadastroPublico() throws Exception {
            mvc.perform(get("/cadastro"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("login"));
        }

        @Test
        @DisplayName("recursos estáticos não pedem login (nunca redirecionam ao login nem dão 401/403)")
        void estaticosNaoPedemLogin() throws Exception {
            for (String caminho : new String[]{"/css/style.css", "/js/app.js", "/img/logo.png", "/favicon.ico"}) {
                mvc.perform(get(caminho))
                        .andExpect(status().is(not(anyOf(is(302), is(401), is(403)))));
            }
        }

        @Test
        @DisplayName("usuário já logado que abre /login vai direto para o painel")
        void logadoEmLoginVaiParaOPainel() throws Exception {
            mvc.perform(get("/login").with(logadoComo(7)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"));
        }
    }

    @Nested
    @DisplayName("CSRF")
    class Csrf {

        @ParameterizedTest(name = "POST {0} logado e sem token CSRF é 403")
        @ValueSource(strings = {"/produtos", "/produtos/1/excluir", "/categorias", "/categorias/1/excluir",
                "/estoque/movimentar", "/logout"})
        @DisplayName("POST autenticado sem token CSRF é negado e nada é executado")
        void postLogadoSemCsrfNegado(String caminho) throws Exception {
            mvc.perform(post(caminho).with(logadoComo(7)))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(produtoService, categoriaService, estoqueService);
        }

        @Test
        @DisplayName("token CSRF inválido também é 403")
        void tokenInvalidoNegado() throws Exception {
            mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf().useInvalidToken())
                            .param("nome", "Livros"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(categoriaService);
        }

        @Test
        @DisplayName("POST com token CSRF e usuário logado passa (categoria)")
        void postComCsrfELogadoPassaCategoria() throws Exception {
            mvc.perform(post("/categorias").with(logadoComo(7)).with(csrf()).param("nome", "Livros"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/categorias"));
            verify(categoriaService).salvar(any(), any());
        }

        @Test
        @DisplayName("POST com token CSRF e usuário logado passa (produto)")
        void postComCsrfELogadoPassaProduto() throws Exception {
            mvc.perform(post("/produtos").with(logadoComo(7)).with(csrf())
                            .param("nome", "Caneta").param("preco", "5.00").param("estoqueMinimo", "1")
                            .param("quantidade", "0").param("categoriaId", "3"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/produtos"));
            verify(produtoService).salvar(any(), any());
        }

        @Test
        @DisplayName("POST com token CSRF e usuário logado passa (movimentação)")
        void postComCsrfELogadoPassaMovimentacao() throws Exception {
            when(estoqueService.movimentar(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any(), any()))
                    .thenReturn(new org.example.stockhubspring.model.MovimentacaoEstoque());
            mvc.perform(post("/estoque/movimentar").with(logadoComo(7)).with(csrf())
                            .param("produtoId", "1").param("tipo", "ENTRADA").param("quantidade", "2"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/estoque"));
        }

        @Test
        @DisplayName("GET nunca altera dados: excluir por GET não é aceito")
        void excluirPorGetNaoEAceito() throws Exception {
            mvc.perform(get("/produtos/1/excluir").with(logadoComo(7)))
                    .andExpect(status().is4xxClientError());
            mvc.perform(get("/categorias/1/excluir").with(logadoComo(7)))
                    .andExpect(status().is4xxClientError());
            verify(produtoService, never()).excluir(any(), any());
            verify(categoriaService, never()).excluir(any(), any());
        }

        @Test
        @DisplayName("o token CSRF vai para a view como atributo _csrf (as JSPs o enviam nos formulários)")
        void tokenCsrfDisponivelParaAsViews() throws Exception {
            mvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        Object token = result.getRequest().getAttribute("_csrf");
                        if (!(token instanceof CsrfToken)) {
                            throw new AssertionError("Atributo _csrf ausente na requisição: " + token);
                        }
                    });
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("POST /logout com CSRF desloga e vai para /login?saiu")
        void logoutPorPostDesloga() throws Exception {
            mvc.perform(post("/logout").with(logadoComo(7)).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?saiu"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("GET /logout não desloga (logout só por POST)")
        void logoutPorGetNaoDesloga() throws Exception {
            mvc.perform(get("/logout").with(logadoComo(7)))
                    .andExpect(result -> {
                        String destino = result.getResponse().getRedirectedUrl();
                        if ("/login?saiu".equals(destino)) {
                            throw new AssertionError("GET /logout deslogou o usuário (esperado: só POST)");
                        }
                    })
                    .andExpect(authenticated());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

        private Usuario usuarioComSenha(String senha) {
            Usuario usuario = Fabrica.usuario(7);
            usuario.setSenha(encoder.encode(senha));
            return usuario;
        }

        @Test
        @DisplayName("credenciais corretas autenticam e levam ao painel")
        void credenciaisCorretasAutenticam() throws Exception {
            when(usuarioRepository.findByEmailAndAtivoTrue("loja7@teste.invalid"))
                    .thenReturn(Optional.of(usuarioComSenha("senha-correta-1")));

            mvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                            .user("email", "  LOJA7@teste.invalid ").password("senha", "senha-correta-1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/dashboard"))
                    .andExpect(authenticated());
        }

        @Test
        @DisplayName("senha errada não autentica e volta para /login?erro")
        void senhaErradaNaoAutentica() throws Exception {
            when(usuarioRepository.findByEmailAndAtivoTrue("loja7@teste.invalid"))
                    .thenReturn(Optional.of(usuarioComSenha("senha-correta-1")));

            mvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                            .user("email", "loja7@teste.invalid").password("senha", "senha-errada-9"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?erro"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("usuário inexistente ou inativo não autentica e a resposta é igual à de senha errada")
        void usuarioInexistenteOuInativoNaoAutentica() throws Exception {
            when(usuarioRepository.findByEmailAndAtivoTrue(any())).thenReturn(Optional.empty());

            mvc.perform(SecurityMockMvcRequestBuilders.formLogin("/login")
                            .user("email", "ninguem@teste.invalid").password("senha", "qualquer-senha-1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?erro"))
                    .andExpect(unauthenticated());
        }

        @Test
        @DisplayName("POST /login sem token CSRF é 403")
        void loginSemCsrfNegado() throws Exception {
            MockHttpServletRequestBuilder requisicao = post("/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("email", "loja7@teste.invalid").param("senha", "senha-correta-1");
            mvc.perform(requisicao).andExpect(status().isForbidden());
            verifyNoInteractions(usuarioRepository);
        }
    }
}
