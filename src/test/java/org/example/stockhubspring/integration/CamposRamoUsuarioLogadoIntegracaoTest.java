package org.example.stockhubspring.integration;

import org.example.stockhubspring.config.UsuarioPrincipal;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.CamposRamo;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

/** Caminho real, sem mock: usuário logado (SecurityContext) de uma loja com subtipo -> campos por ramo (AD-17). */
class CamposRamoUsuarioLogadoIntegracaoTest extends IntegracaoBase {

    @Autowired
    private UsuarioAtualService usuarioAtualService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void logar(Usuario loja) {
        UsuarioPrincipal principal = new UsuarioPrincipal(loja);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("loja FARMACIA logada: camposPermitidos devolve lote e validade (subtipo lido do banco)")
    void farmaciaLogada() {
        Usuario loja = novoUsuario();
        loja.setTipoLoja(TipoLoja.COMERCIO);
        loja.setSubtipo(Subtipo.FARMACIA);
        usuarioRepository.saveAndFlush(loja);
        limparSessao();
        logar(loja);

        var campos = produtoService.camposPermitidos(usuarioAtualService.getUsuarioAtual());

        assertThat(campos.lote()).isTrue();
        assertThat(campos.validade()).isTrue();
        assertThat(campos.tamanho()).isFalse();
        assertThat(campos.numeroSerie()).isFalse();
        assertThat(campos.isTemAlgum()).isTrue();
    }

    @Test
    @DisplayName("troca de subtipo no banco vale na hora, sem depender da sessão")
    void subtipoTrocadoValeNaHora() {
        Usuario loja = novoUsuario();
        loja.setTipoLoja(TipoLoja.COMERCIO);
        loja.setSubtipo(Subtipo.FARMACIA);
        usuarioRepository.saveAndFlush(loja);
        logar(loja);
        assertThat(produtoService.camposPermitidos(usuarioAtualService.getUsuarioAtual()).lote()).isTrue();

        jdbc.update("update usuario set subtipo = 'VESTUARIO' where codigo = ?", loja.getCodigo());
        limparSessao();

        var campos = produtoService.camposPermitidos(usuarioAtualService.getUsuarioAtual());
        assertThat(campos.lote()).isFalse();
        assertThat(campos.tamanho()).isTrue();
        assertThat(campos.cor()).isTrue();
    }

    @Test
    @DisplayName("loja sem subtipo: nenhum campo extra")
    void semSubtipo() {
        Usuario loja = novoUsuario();
        limparSessao();
        logar(loja);

        assertThat(produtoService.camposPermitidos(usuarioAtualService.getUsuarioAtual()))
                .isEqualTo(CamposRamo.Permitidos.NENHUM);
    }
}
