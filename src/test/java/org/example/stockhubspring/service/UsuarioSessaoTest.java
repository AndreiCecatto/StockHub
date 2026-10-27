package org.example.stockhubspring.service;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MembroLojaRepository;
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

    private static MembroLoja membro(Integer id, int loja, PapelMembro papel, boolean ativo) {
        MembroLoja m = new MembroLoja();
        m.setId(id);
        m.setLojaCodigo(loja);
        m.setNome("Pessoa " + id);
        m.setEmail("pessoa" + id + "@teste.invalid");
        m.setSenha("$2a$10$hash");
        m.setPapel(papel);
        m.setAtivo(ativo);
        return m;
    }

    @Test
    @DisplayName("login autentica o MEMBRO pelo e-mail normalizado e carrega o código da LOJA dele e o papel")
    void detailsServiceCarregaMembroELoja() {
        MembroLojaRepository membros = mock(MembroLojaRepository.class);
        UsuarioRepository lojas = mock(UsuarioRepository.class);
        when(membros.findByEmailAndAtivoTrue("pessoa5@teste.invalid"))
                .thenReturn(Optional.of(membro(5, 7, PapelMembro.OPERADOR, true)));
        when(lojas.findById(7)).thenReturn(Optional.of(Fabrica.usuario(7)));

        UserDetails detalhes = new UsuarioDetailsService(membros, lojas).loadUserByUsername("  PESSOA5@Teste.Invalid ");

        UsuarioPrincipal principal = (UsuarioPrincipal) detalhes;
        assertThat(principal.getUsername()).isEqualTo("pessoa5@teste.invalid");
        assertThat(principal.getCodigo()).isEqualTo(7);
        assertThat(principal.getMembroId()).isEqualTo(5);
        assertThat(principal.getPapel()).isEqualTo(PapelMembro.OPERADOR);
        assertThat(principal.getAuthorities()).extracting("authority").contains("ROLE_USER", "ROLE_OPERADOR");
        assertThat(principal.isEnabled()).isTrue();
        verify(membros).findByEmailAndAtivoTrue("pessoa5@teste.invalid");
    }

    @Test
    @DisplayName("o usuário da sessão leva o membro e o papel, além do código da loja")
    void usuarioAtualLevaMembroEPapel() {
        MembroLojaRepository membros = mock(MembroLojaRepository.class);
        UsuarioRepository lojas = mock(UsuarioRepository.class);
        when(membros.findByEmailAndAtivoTrue("pessoa5@teste.invalid"))
                .thenReturn(Optional.of(membro(5, 7, PapelMembro.GERENTE, true)));
        when(lojas.findById(7)).thenReturn(Optional.of(Fabrica.usuario(7)));
        UsuarioPrincipal principal = (UsuarioPrincipal) new UsuarioDetailsService(membros, lojas)
                .loadUserByUsername("pessoa5@teste.invalid");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        Usuario atual = new UsuarioAtualService().getUsuarioAtual();

        assertThat(atual.getCodigo()).isEqualTo(7);
        assertThat(atual.getMembroId()).isEqualTo(5);
        assertThat(atual.getPapel()).isEqualTo(PapelMembro.GERENTE);
    }

    @Test
    @DisplayName("conta sem membro (principal antigo) age como DONO da loja")
    void principalSemMembroEDono() {
        UsuarioPrincipal principal = new UsuarioPrincipal(Fabrica.usuario(3));

        assertThat(principal.getPapel()).isEqualTo(PapelMembro.DONO);
        assertThat(principal.getMembroId()).isNull();
        assertThat(principal.getCodigo()).isEqualTo(3);
    }

    @Test
    @DisplayName("membro inexistente, membro inativo ou loja inativa não são carregados")
    void naoCarregaInexistenteNemInativo() {
        MembroLojaRepository membros = mock(MembroLojaRepository.class);
        UsuarioRepository lojas = mock(UsuarioRepository.class);
        when(membros.findByEmailAndAtivoTrue("x@teste.invalid")).thenReturn(Optional.empty());
        when(membros.findByEmailAndAtivoTrue("pessoa5@teste.invalid"))
                .thenReturn(Optional.of(membro(5, 7, PapelMembro.DONO, true)));
        Usuario lojaInativa = Fabrica.usuario(7);
        lojaInativa.setAtivo(false);
        when(lojas.findById(7)).thenReturn(Optional.of(lojaInativa));

        UsuarioDetailsService servico = new UsuarioDetailsService(membros, lojas);

        assertThatThrownBy(() -> servico.loadUserByUsername("x@teste.invalid"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> servico.loadUserByUsername("pessoa5@teste.invalid"))
                .isInstanceOf(UsernameNotFoundException.class);
        assertThatThrownBy(() -> servico.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
