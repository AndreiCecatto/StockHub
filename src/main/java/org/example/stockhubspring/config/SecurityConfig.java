package org.example.stockhubspring.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    /** Pedido de API: caminho /api/** ou cliente que pede JSON explicitamente (o Accept de navegador não conta). */
    private static final RequestMatcher PEDIDO_JSON = request -> {
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        if (accept == null || accept.isBlank()) {
            return false;
        }
        try {
            return MediaType.parseMediaTypes(accept).stream()
                    .anyMatch(tipo -> tipo.equalsTypeAndSubtype(MediaType.APPLICATION_JSON));
        } catch (InvalidMediaTypeException e) {
            return false;
        }
    };

    private static final AuthenticationEntryPoint ENTRADA_JSON = (request, response, ex) ->
            escreverJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Sessão expirada ou ausente. Entre novamente.");

    private static final AccessDeniedHandler NEGADO_JSON = (request, response, ex) ->
            escreverJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "Requisição recusada (token de segurança ausente ou inválido). Recarregue a página e tente de novo.");

    /** Só recebe mensagens fixas (sem dados do usuário), por isso não precisa escapar. */
    private static void escreverJson(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                ("{\"ok\":false,\"erro\":\"" + mensagem + "\"}").getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, MembroLojaRepository membroRepository)
            throws Exception {
        http
                // Quem foi desativado perde a sessão no pedido seguinte (RN-65)
                .addFilterBefore(new MembroAtivoFilter(membroRepository), AuthorizationFilter.class)
                // Rotas publicas: login, cadastro, pagina de erro e recursos estaticos. O resto exige login.
                .authorizeHttpRequests(auth -> auth
                        // O forward interno para as JSPs (e para /error) e os includes de fragmentos (head.jsp etc.)
                        // acontecem depois de a requisicao original ja ter sido autorizada. Sem isso o login
                        // redireciona para si mesmo (FORWARD) e a pagina publica sai sem <head>/CSS (INCLUDE).
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/", "/login", "/cadastro", "/error",
                                "/css/**", "/js/**", "/vendor/**", "/img/**", "/images/**", "/favicon.ico").permitAll()
                        // Permissões por papel (RN-62): só dono e gerente excluem, mudam o tipo da loja
                        // e cuidam da equipe. Operador e demais papéis consultam e movimentam.
                        .requestMatchers(HttpMethod.POST, "/produtos/*/excluir", "/categorias/*/excluir")
                                .hasAnyRole("DONO", "GERENTE")
                        .requestMatchers("/conta/**", "/membros", "/membros/**").hasAnyRole("DONO", "GERENTE")
                        .anyRequest().authenticated())
                // Login pelo formulario existente (campos "email" e "senha"). O UserDetailsService
                // e o PasswordEncoder sao detectados automaticamente como beans.
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("senha")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?erro")
                        .permitAll())
                // Logout somente por POST em /logout (com token CSRF); invalida a sessao.
                .logout(logout -> logout
                        // Com CSRF ligado, logoutUrl so aceita POST.
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                // Pedidos JSON (/api/** ou Accept: application/json): 401 e 403 em JSON, sem redirect para o login.
                .exceptionHandling(erros -> erros
                        .defaultAuthenticationEntryPointFor(ENTRADA_JSON, PEDIDO_JSON)
                        // Demais pedidos: o redirecionamento ao login de sempre (o primeiro registrado seria o padrao)
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), AnyRequestMatcher.INSTANCE)
                        .defaultAccessDeniedHandlerFor(NEGADO_JSON, PEDIDO_JSON))
                // CSRF ligado (padrao do Spring Security); as JSPs precisam enviar o token.
                .csrf(Customizer.withDefaults())
                // Protecao contra session fixation: nova sessao a cada login.
                .sessionManagement(sessao -> sessao.sessionFixation(fix -> fix.changeSessionId()));
        return http.build();
    }
}
