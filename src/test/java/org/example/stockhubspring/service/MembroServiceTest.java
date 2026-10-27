package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RN-61 a RN-65: equipe da loja. */
class MembroServiceTest {

    private static final String SENHA = "senha-inicial-1";

    private MembroLojaRepository repositorio;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private MembroService service;

    @BeforeEach
    void preparar() {
        repositorio = mock(MembroLojaRepository.class);
        when(repositorio.saveAndFlush(any(MembroLoja.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repositorio.save(any(MembroLoja.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new MembroService(repositorio, encoder);
    }

    private static MembroLoja membro(int id, int loja, PapelMembro papel) {
        MembroLoja m = new MembroLoja();
        m.setId(id);
        m.setLojaCodigo(loja);
        m.setNome("Pessoa " + id);
        m.setEmail("p" + id + "@teste.invalid");
        m.setSenha("hash");
        m.setPapel(papel);
        m.setAtivo(true);
        return m;
    }

    @Test
    @DisplayName("cria a pessoa na loja de quem convida, com e-mail normalizado e senha em BCrypt")
    void criaComBCrypt() {
        MembroLoja criado = service.criar(7, PapelMembro.DONO, "  Bia ", "  BIA@Teste.Invalid ", SENHA,
                PapelMembro.OPERADOR);

        assertThat(criado.getLojaCodigo()).isEqualTo(7);
        assertThat(criado.getNome()).isEqualTo("Bia");
        assertThat(criado.getEmail()).isEqualTo("bia@teste.invalid");
        assertThat(criado.getPapel()).isEqualTo(PapelMembro.OPERADOR);
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.getSenha()).isNotEqualTo(SENHA).startsWith("$2");
        assertThat(encoder.matches(SENHA, criado.getSenha())).isTrue();
    }

    @Test
    @DisplayName("senha inicial com menos de 8 caracteres é recusada")
    void senhaCurta() {
        assertThatThrownBy(() -> service.criar(7, PapelMembro.DONO, "Bia", "bia@teste.invalid", "1234567",
                PapelMembro.OPERADOR)).isInstanceOf(RegraNegocioException.class).hasMessageContaining("8 caracteres");
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("e-mail repetido responde com mensagem genérica")
    void emailRepetido() {
        when(repositorio.existsByEmail("bia@teste.invalid")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(7, PapelMembro.DONO, "Bia", "bia@teste.invalid", SENHA,
                PapelMembro.OPERADOR)).isInstanceOf(RegraNegocioException.class)
                .hasMessage(MembroService.MSG_CRIACAO_GENERICA);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("não cria DONO, nem papel vazio; gerente não cria gerente")
    void papeisInvalidos() {
        assertThatThrownBy(() -> service.criar(7, PapelMembro.DONO, "Bia", "bia@teste.invalid", SENHA,
                PapelMembro.DONO)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.criar(7, PapelMembro.DONO, "Bia", "bia@teste.invalid", SENHA, null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.criar(7, PapelMembro.GERENTE, "Bia", "bia@teste.invalid", SENHA,
                PapelMembro.GERENTE)).isInstanceOf(RegraNegocioException.class);
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("operador, vendedor e mecânico não criam nem desativam, mesmo chamando o service direto")
    void operacionaisNaoGerenciam() {
        for (PapelMembro papel : new PapelMembro[]{PapelMembro.OPERADOR, PapelMembro.VENDEDOR, PapelMembro.MECANICO}) {
            assertThatThrownBy(() -> service.criar(7, papel, "Bia", "bia@teste.invalid", SENHA, PapelMembro.OPERADOR))
                    .isInstanceOf(RegraNegocioException.class);
            assertThatThrownBy(() -> service.desativar(7, 1, papel, 5)).isInstanceOf(RegraNegocioException.class);
        }
        verify(repositorio, never()).saveAndFlush(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("desativa (não exclui) uma pessoa da loja")
    void desativa() {
        when(repositorio.findByIdAndLojaCodigo(5, 7)).thenReturn(Optional.of(membro(5, 7, PapelMembro.OPERADOR)));

        service.desativar(7, 1, PapelMembro.DONO, 5);

        ArgumentCaptor<MembroLoja> salvo = ArgumentCaptor.forClass(MembroLoja.class);
        verify(repositorio).save(salvo.capture());
        assertThat(salvo.getValue().isAtivo()).isFalse();
        verify(repositorio, never()).delete(any());
        verify(repositorio, never()).deleteById(any());
    }

    @Test
    @DisplayName("membro de OUTRA loja é tratado como inexistente")
    void outraLojaNaoExiste() {
        when(repositorio.findByIdAndLojaCodigo(5, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desativar(7, 1, PapelMembro.DONO, 5))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.reativar(7, PapelMembro.DONO, 5))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("ninguém desativa a si mesmo nem o dono; gerente não mexe em gerente")
    void protecoes() {
        when(repositorio.findByIdAndLojaCodigo(1, 7)).thenReturn(Optional.of(membro(1, 7, PapelMembro.DONO)));
        when(repositorio.findByIdAndLojaCodigo(2, 7)).thenReturn(Optional.of(membro(2, 7, PapelMembro.GERENTE)));
        when(repositorio.findByIdAndLojaCodigo(3, 7)).thenReturn(Optional.of(membro(3, 7, PapelMembro.OPERADOR)));

        assertThatThrownBy(() -> service.desativar(7, 3, PapelMembro.GERENTE, 3))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("a si mesmo");
        assertThatThrownBy(() -> service.desativar(7, 2, PapelMembro.GERENTE, 1))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("dono");
        assertThatThrownBy(() -> service.desativar(7, 9, PapelMembro.GERENTE, 2))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("gerente");
        verify(repositorio, never()).save(any());

        service.desativar(7, 1, PapelMembro.DONO, 2); // o dono pode desativar o gerente
        verify(repositorio).save(any(MembroLoja.class));
    }
}
