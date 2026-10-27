package org.example.stockhubspring.integration;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migration V6 e RN-43/RN-44 no PostgreSQL real (a transação do teste é sempre revertida). */
class TipoLojaIntegracaoTest extends IntegracaoBase {

    private String emailNovo() {
        return "tl-" + UUID.randomUUID() + "@teste.invalid";
    }

    @Test
    @DisplayName("V6: a loja nova nasce como ESTOQUE, sem subtipo (padrão do banco e da entidade)")
    void padraoEstoque() {
        Usuario u = novoUsuario();
        limparSessao();
        assertThat(usuarioRepository.findTipoLojaByCodigo(u.getCodigo())).contains(TipoLoja.ESTOQUE);
        String subtipo = jdbc.queryForObject("select subtipo from usuario where codigo = ?", String.class, u.getCodigo());
        assertThat(subtipo).isNull();
    }

    @Test
    @DisplayName("V6: coluna tem DEFAULT 'ESTOQUE' para linhas inseridas sem o campo")
    void padraoDoBanco() {
        jdbc.update("insert into usuario (nome, email, senha, ativo) values ('L', ?, 'x', true)", emailNovo());
        String tipo = jdbc.queryForObject("select tipo_loja from usuario order by codigo desc limit 1", String.class);
        assertThat(tipo).isEqualTo("ESTOQUE");
    }

    @Test
    @DisplayName("V6: o CHECK recusa tipo e subtipo fora da lista")
    void checkRecusaValoresInvalidos() {
        Usuario u = novoUsuario();
        assertThatThrownBy(() -> jdbc.update("update usuario set tipo_loja = 'BANCO' where codigo = ?", u.getCodigo()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V6: o CHECK recusa subtipo desconhecido")
    void checkRecusaSubtipoInvalido() {
        Usuario u = novoUsuario();
        assertThatThrownBy(() -> jdbc.update("update usuario set subtipo = 'XYZ' where codigo = ?", u.getCodigo()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("cadastro grava tipo e subtipo; sem tipo vira ESTOQUE")
    void cadastroGravaTipo() {
        Usuario misto = usuarioService.cadastrarLoja("Loja Mista", emailNovo(), "senha-forte-123",
                TipoLoja.MISTO, Subtipo.INFORMATICA);
        Usuario padrao = usuarioService.cadastrarLoja("Loja Padrão", emailNovo(), "senha-forte-123");
        limparSessao();
        assertThat(usuarioRepository.findTipoLojaByCodigo(misto.getCodigo())).contains(TipoLoja.MISTO);
        assertThat(usuarioRepository.findById(misto.getCodigo()).orElseThrow().getSubtipo()).isEqualTo(Subtipo.INFORMATICA);
        assertThat(usuarioRepository.findTipoLojaByCodigo(padrao.getCodigo())).contains(TipoLoja.ESTOQUE);
    }

    @Test
    @DisplayName("alterar o tipo troca só o tipo e recusa combinação inválida sem alterar nada")
    void alterarTipo() {
        Usuario u = usuarioService.cadastrarLoja("Loja", emailNovo(), "senha-forte-123");
        usuarioService.alterarTipoLoja(u.getCodigo(), TipoLoja.OFICINA, Subtipo.REFRIGERACAO);
        limparSessao();
        assertThat(usuarioRepository.findTipoLojaByCodigo(u.getCodigo())).contains(TipoLoja.OFICINA);

        assertThatThrownBy(() -> usuarioService.alterarTipoLoja(u.getCodigo(), TipoLoja.COMERCIO, Subtipo.REFRIGERACAO))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(usuarioRepository.findTipoLojaByCodigo(u.getCodigo())).contains(TipoLoja.OFICINA);
    }
}
