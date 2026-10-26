package org.example.stockhubspring.controller;

import org.example.stockhubspring.config.SecurityConfig;
import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.DashboardService;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.example.stockhubspring.service.UsuarioDetailsService;
import org.example.stockhubspring.service.UsuarioService;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base dos testes de controller: carrega todos os controllers e a SecurityConfig REAL do projeto
 * (só os services/repositórios são simulados). Assim os testes provam o comportamento de verdade
 * da cadeia de segurança: redirecionamento ao login, CSRF e logout por POST.
 */
@WebMvcTest
@Import({ControllerTestBase.HabilitaSeguranca.class, SecurityConfig.class, UsuarioAtualService.class,
        ErrosFormulario.class, UsuarioDetailsService.class})
abstract class ControllerTestBase {

    /** O slice @WebMvcTest não liga a segurança sozinho; habilita como a aplicação faz. */
    @TestConfiguration
    @EnableWebSecurity
    static class HabilitaSeguranca {
    }

    @Autowired
    private WebApplicationContext contexto;

    protected MockMvc mvc;

    @MockitoBean
    protected ProdutoService produtoService;
    @MockitoBean
    protected CategoriaService categoriaService;
    @MockitoBean
    protected EstoqueService estoqueService;
    @MockitoBean
    protected DashboardService dashboardService;
    @MockitoBean
    protected UsuarioService usuarioService;
    @MockitoBean
    protected UsuarioRepository usuarioRepository;

    @BeforeEach
    void montarMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    /** Autentica a requisição como a loja de código dado (principal real da aplicação). */
    protected static RequestPostProcessor logadoComo(int codigo) {
        UsuarioPrincipal principal = new UsuarioPrincipal(Fabrica.usuario(codigo));
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
