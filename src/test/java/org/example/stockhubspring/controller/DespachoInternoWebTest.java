package org.example.stockhubspring.controller;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Regressão: o MockMvc não renderiza JSP, então o teste simula o despacho FORWARD que o container faz
 * para a view. Sem liberar FORWARD/ERROR na SecurityConfig, o forward para a JSP caía em
 * anyRequest().authenticated() e /login redirecionava para si mesmo (laço no navegador).
 */
class DespachoInternoWebTest extends ControllerTestBase {

    @ParameterizedTest(name = "forward para {0} não é redirecionado ao login")
    @ValueSource(strings = {"/WEB-INF/pages/login.jsp", "/WEB-INF/pages/error.jsp"})
    @DisplayName("Forward interno para a view não exige autenticação de novo")
    void forwardInternoNaoRedirecionaAoLogin(String view) throws Exception {
        mvc.perform(get(view).with(requisicao -> {
                    requisicao.setDispatcherType(DispatcherType.FORWARD);
                    return requisicao;
                }))
                .andExpect(redirectedUrl(null));
    }

    @ParameterizedTest(name = "include de {0} não é redirecionado ao login")
    @ValueSource(strings = {"/WEB-INF/includes/sh/head.jsp", "/WEB-INF/includes/sh/alertas.jsp"})
    @DisplayName("Include de fragmento numa página pública não é bloqueado (senão o <head> sai vazio)")
    void includeInternoNaoRedirecionaAoLogin(String fragmento) throws Exception {
        mvc.perform(get(fragmento).with(requisicao -> {
                    requisicao.setDispatcherType(DispatcherType.INCLUDE);
                    return requisicao;
                }))
                .andExpect(redirectedUrl(null));
    }

    @ParameterizedTest(name = "requisição externa a {0} continua exigindo login")
    @ValueSource(strings = {"/WEB-INF/pages/dashboard.jsp", "/produtos"})
    @DisplayName("Requisição externa (REQUEST) a rota protegida continua redirecionando ao login")
    void requisicaoExternaContinuaProtegida(String rota) throws Exception {
        mvc.perform(get(rota))
                .andExpect(redirectedUrl("/login"));
    }
}
