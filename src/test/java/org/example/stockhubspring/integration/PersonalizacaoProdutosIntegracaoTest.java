package org.example.stockhubspring.integration;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.model.TipoLoja;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.PersonalizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Personalização da lista de produtos (AD-20) no PostgreSQL real; a transação do teste é sempre revertida. */
class PersonalizacaoProdutosIntegracaoTest extends IntegracaoBase {

    @Autowired
    private PersonalizacaoService service;

    private Usuario loja(TipoLoja tipo, Subtipo subtipo) {
        Usuario loja = novoUsuario();
        loja.setTipoLoja(tipo);
        loja.setSubtipo(subtipo);
        usuarioRepository.saveAndFlush(loja);
        limparSessao();
        return loja;
    }

    private long linhas(Integer loja, String tela) {
        Long n = jdbc.queryForObject("select count(*) from preferencia_bloco where usuario_codigo = ? and tela = ?",
                Long.class, loja, tela);
        return n == null ? 0 : n;
    }

    @Test
    @DisplayName("salvar COL_PRECO oculta a coluna e a leitura devolve o estado gravado")
    void ocultarPrecoELerDeVolta() {
        Usuario l = loja(TipoLoja.COMERCIO, null);

        service.salvar(l.getCodigo(), PapelMembro.DONO, null, TelaPersonalizavel.PRODUTOS,
                Set.of("COL_CATEGORIA", "COL_SITUACAO", "COL_DESCRICAO"));
        limparSessao();

        Map<String, Boolean> estado = service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.PRODUTOS);
        assertThat(estado.get("COL_PRECO")).isFalse();
        assertThat(estado.get("COL_CATEGORIA")).isTrue();
        assertThat(estado.get("COL_SITUACAO")).isTrue();
        Boolean noBanco = jdbc.queryForObject(
                "select visivel from preferencia_bloco where usuario_codigo = ? and tela = 'PRODUTOS' and bloco = 'COL_PRECO'",
                Boolean.class, l.getCodigo());
        assertThat(noBanco).isFalse();
    }

    @Test
    @DisplayName("loja nova não tem linha e vê todas as colunas fixas")
    void padraoSemLinhas() {
        Usuario l = loja(TipoLoja.COMERCIO, null);

        assertThat(linhas(l.getCodigo(), "PRODUTOS")).isZero();
        assertThat(service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.PRODUTOS))
                .containsOnlyKeys("COL_CATEGORIA", "COL_PRECO", "COL_SITUACAO", "COL_DESCRICAO")
                .allSatisfy((k, v) -> assertThat(v).isTrue());
    }

    @Test
    @DisplayName("COL_VALIDADE aparece visível numa loja de mercado e não existe numa de vestuário")
    void validadePorRamo() {
        Usuario mercado = loja(TipoLoja.COMERCIO, Subtipo.MERCADO);
        Usuario vestuario = loja(TipoLoja.COMERCIO, Subtipo.VESTUARIO);

        assertThat(service.blocosVisiveis(mercado.getCodigo(), TelaPersonalizavel.PRODUTOS))
                .containsEntry("COL_VALIDADE", true).doesNotContainKey("COL_TAMANHO");
        assertThat(service.blocosVisiveis(vestuario.getCodigo(), TelaPersonalizavel.PRODUTOS))
                .doesNotContainKey("COL_VALIDADE").containsEntry("COL_COR", true);
    }

    @Test
    @DisplayName("COL_LOTE numa loja de vestuário é recusada e nada é gravado")
    void loteEmVestuarioRecusado() {
        Usuario l = loja(TipoLoja.COMERCIO, Subtipo.VESTUARIO);

        assertThatThrownBy(() -> service.salvar(l.getCodigo(), PapelMembro.DONO, null, TelaPersonalizavel.PRODUTOS,
                Set.of("COL_LOTE")))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(linhas(l.getCodigo(), "PRODUTOS")).isZero();
    }

    @Test
    @DisplayName("salvar PRODUTOS não toca as linhas de INICIO, e vice-versa")
    void telasIndependentes() {
        Usuario l = loja(TipoLoja.COMERCIO, Subtipo.FARMACIA);
        service.salvar(l.getCodigo(), PapelMembro.DONO, null, TelaPersonalizavel.INICIO, Set.of("TOTAIS"));
        limparSessao();
        long inicio = linhas(l.getCodigo(), "INICIO");
        assertThat(inicio).isPositive();

        service.salvar(l.getCodigo(), PapelMembro.GERENTE, null, TelaPersonalizavel.PRODUTOS, Set.of("COL_LOTE"));
        limparSessao();

        assertThat(linhas(l.getCodigo(), "INICIO")).isEqualTo(inicio);
        assertThat(service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.INICIO))
                .containsEntry("TOTAIS", true).containsEntry("ATALHOS", false);
        assertThat(service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.PRODUTOS))
                .containsEntry("COL_LOTE", true).containsEntry("COL_VALIDADE", false);

        service.salvar(l.getCodigo(), PapelMembro.DONO, null, TelaPersonalizavel.INICIO, Set.of());
        limparSessao();
        assertThat(service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.PRODUTOS))
                .containsEntry("COL_LOTE", true);
    }

    @Test
    @DisplayName("troca de subtipo no banco: linha gravada de coluna que não se aplica mais é ignorada")
    void trocaDeSubtipoIgnoraLinhaAntiga() {
        Usuario l = loja(TipoLoja.COMERCIO, Subtipo.FARMACIA);
        service.salvar(l.getCodigo(), PapelMembro.DONO, null, TelaPersonalizavel.PRODUTOS, Set.of("COL_PRECO"));
        limparSessao();
        assertThat(service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.PRODUTOS)).containsEntry("COL_LOTE", false);

        jdbc.update("update usuario set subtipo = 'VESTUARIO' where codigo = ?", l.getCodigo());
        limparSessao();

        Map<String, Boolean> estado = service.blocosVisiveis(l.getCodigo(), TelaPersonalizavel.PRODUTOS);
        assertThat(estado).doesNotContainKeys("COL_LOTE", "COL_VALIDADE").containsKeys("COL_TAMANHO", "COL_COR");
        assertThat(estado.get("COL_TAMANHO")).isTrue();
    }
}
