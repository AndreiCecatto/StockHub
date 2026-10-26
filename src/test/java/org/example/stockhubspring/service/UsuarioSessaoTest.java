package org.example.stockhubspring.service;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** UsuarioAtualService, UsuarioDetailsService e UsuarioPrincipal. */
class UsuarioSessaoTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sem login, getUsuarioAtualOuNull devolve null e getUsuarioAtual lança exceção")
    void semLoginNaoHaUsuario() {
        UsuarioAtualService atual = new UsuarioAtualService();

        assertThat(atual.getUsuarioAtualOuNull()).isNull();
        assertThatThrownBy(atual::getUsuarioAtual).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("com login devolve usuário leve (código, nome, e-mail) e nunca a senha")
    void comLoginDevolveUsuarioSemSenha() {
        Usuario original = Fabrica.usuario(7);
        original.setSenha("$2a$10$hashSecreto");
        UsuarioPrincipal principal = new UsuarioPrincipal(original);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Usuario atual = new UsuarioAtualService().getUsuarioAtual();

        assertThat(atual.getCodigo()).isEqualTo(7);
        assertThat(atual.getEmail()).isEqualTo(original.getEmail());
        assertThat(atual.getNome()).isEqualTo(original.getNome());
        assertThat(atual.getSenha()).isNull();
    }

    @Test
    @DisplayName("principal que não é UsuarioPrincipal (ex.: anônimo) não vira usuário")
    void principalDeOutroTipoNaoViraUsuario() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null));
        assertThat(new UsuarioAtualService().getUsuarioAtualOuNull()).isNull();
    }

    @Test
    @DisplayName("eraseCredentials apaga o hash da senha do principal guardado na sessão")
    void eraseCredentialsApagaOHash() {
        UsuarioPrincipal principal = new UsuarioPrincipal(Fabrica.usuario(7));
        assertThat(principal.getPassword()).isNotNull();

        principal.eraseCredentials();

        assertThat(principal.getPassword()).isNull();
    }

    @Test
    @DisplayName("UserDetailsService busca pelo e-mail normalizado e só entre usuários ativos")
    void detailsServiceBuscaEmailNormalizadoEAtivo() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        Usuario ativo = Fabrica.usuario(7);
        when(repositorio.findByEmailAndAtivoTrue("loja7@teste.invalid")).thenReturn(Optional.of(ativo));

        UserDetails detalhes = new UsuarioDetailsService(repositorio).loadUserByUsername("  LOJA7@Teste.Invalid ");

        assertThat(detalhes.getUsername()).isEqualTo("loja7@teste.invalid");
        assertThat(detalhes.isEnabled()).isTrue();
        verify(repositorio).findByEmailAndAtivoTrue("loja7@teste.invalid");
    }

    @Test
    @DisplayName("usuário inexistente ou inativo não é carregado")
    void usuarioInexistenteOuInativoNaoCarrega() {
        UsuarioRepository repositorio = mock(UsuarioRepository.class);
        when(repositorio.findByEmailAndAtivoTrue("x@teste.invalid")).thenReturn(Optional.empty());

        UsuarioDetailsService servico = new UsuarioDetailsService(repositorio);

        assertThatThrownBy(() -> servico.loadUserByUsername("x@teste.invalid"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> servico.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
