package org.example.stockhubspring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Rotas publicas: login, cadastro, pagina de erro e recursos estaticos. O resto exige login.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/index.jsp", "/cadastro", "/error",
                                "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico").permitAll()
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
                // CSRF ligado (padrao do Spring Security); as JSPs precisam enviar o token.
                .csrf(Customizer.withDefaults())
                // Protecao contra session fixation: nova sessao a cada login.
                .sessionManagement(sessao -> sessao.sessionFixation(fix -> fix.changeSessionId()));
        return http.build();
    }
}
