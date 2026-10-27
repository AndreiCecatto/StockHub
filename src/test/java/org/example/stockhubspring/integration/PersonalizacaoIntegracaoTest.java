package org.example.stockhubspring.integration;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.PreferenciaBloco;
import org.example.stockhubspring.model.TelaPersonalizavel;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.PreferenciaBlocoRepository;
import org.example.stockhubspring.service.PersonalizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migration V15 e personalização (AD-19) no PostgreSQL real; a transação do teste é sempre revertida. */
class PersonalizacaoIntegracaoTest extends IntegracaoBase {

    @Autowired
    private PreferenciaBlocoRepository repository;
    @Autowired
    private PersonalizacaoService service;

    private long linhas(Integer loja) {
        Long n = jdbc.queryForObject("select count(*) from preferencia_bloco where usuario_codigo = ?", Long.class, loja);
        return n == null ? 0 : n;
    }

    private Boolean valorNoBanco(Integer loja, String bloco) {
        return jdbc.queryForObject(
                "select visivel from preferencia_bloco where usuario_codigo = ? and tela = 'INICIO' and bloco = ?",
                Boolean.class, loja, bloco);
    }

    @Test
    @DisplayName("V15: a tabela existe com as colunas esperadas e loja nova não tem linha (padrão = visível)")
    void tabelaSobeVazia() {
        List<String> colunas = jdbc.queryForList(
                "select column_name from information_schema.columns where table_name = 'preferencia_bloco'",
                String.class);
        assertThat(colunas).containsExactlyInAnyOrder("usuario_codigo", "tela", "bloco", "visivel",
                "atualizado_em", "atualizado_por");

        Usuario loja = novoUsuario();
        assertThat(linhas(loja.getCodigo())).isZero();
        assertThat(repository.findByUsuarioCodigoAndTela(loja.getCodigo(), "INICIO")).isEmpty();
        assertThat(service.blocosVisiveis(loja.getCodigo(), TelaPersonalizavel.INICIO))
                .isNotEmpty().allSatisfy((k, v) -> assertThat(v).isTrue());
    }

    @Test
    @DisplayName("ON CONFLICT real: gravar duas vezes o mesmo bloco deixa 1 linha com o valor atualizado")
    void upsertIdempotente() {
        Usuario loja = novoUsuario();

        repository.gravar(loja.getCodigo(), "INICIO", "TOTAIS", true, 5);
        repository.gravar(loja.getCodigo(), "INICIO", "TOTAIS", false, 6);

        assertThat(linhas(loja.getCodigo())).isEqualTo(1);
        assertThat(valorNoBanco(loja.getCodigo(), "TOTAIS")).isFalse();
        Integer por = jdbc.queryForObject("select atualizado_por from preferencia_bloco where usuario_codigo = ?",
                Integer.class, loja.getCodigo());
        assertThat(por).isEqualTo(6);

        repository.gravar(loja.getCodigo(), "INICIO", "TOTAIS", false, 6);
        assertThat(linhas(loja.getCodigo())).isEqualTo(1);
    }

    @Test
    @DisplayName("upsert: mesma loja, blocos diferentes viram linhas diferentes (e membro nulo é aceito)")
    void blocosDiferentes() {
        Usuario loja = novoUsuario();
        repository.gravar(loja.getCodigo(), "INICIO", "TOTAIS", false, null);
        repository.gravar(loja.getCodigo(), "INICIO", "ATALHOS", true, null);

        assertThat(linhas(loja.getCodigo())).isEqualTo(2);
        Long nulos = jdbc.queryForObject(
                "select count(*) from preferencia_bloco where usuario_codigo = ? and atualizado_por is null",
                Long.class, loja.getCodigo());
        assertThat(nulos).isEqualTo(2);
    }

    @Test
    @DisplayName("isolamento: preferência de uma loja não aparece nem altera a de outra")
    void isolamentoEntreLojas() {
        Usuario a = novoUsuario();
        Usuario b = novoUsuario();

        repository.gravar(a.getCodigo(), "INICIO", "TOTAIS", false, 1);
        repository.gravar(b.getCodigo(), "INICIO", "TOTAIS", true, 2);
        repository.gravar(a.getCodigo(), "INICIO", "TOTAIS", true, 1);
        limparSessao();

        assertThat(valorNoBanco(a.getCodigo(), "TOTAIS")).isTrue();
        assertThat(valorNoBanco(b.getCodigo(), "TOTAIS")).isTrue();
        repository.gravar(b.getCodigo(), "INICIO", "TOTAIS", false, 2);
        assertThat(valorNoBanco(a.getCodigo(), "TOTAIS")).isTrue();
        assertThat(valorNoBanco(b.getCodigo(), "TOTAIS")).isFalse();
        assertThat(repository.findByUsuarioCodigoAndTela(a.getCodigo(), "INICIO")).hasSize(1);
    }

    @Test
    @DisplayName("findBy lê as linhas gravadas pelo upsert nativo (mapeamento da entidade)")
    void leituraAposUpsert() {
        Usuario loja = novoUsuario();
        repository.gravar(loja.getCodigo(), "INICIO", "FAZER_AGORA", false, 3);
        limparSessao();

        List<PreferenciaBloco> lidas = repository.findByUsuarioCodigoAndTela(loja.getCodigo(), "INICIO");

        assertThat(lidas).hasSize(1);
        assertThat(lidas.get(0).getBloco()).isEqualTo("FAZER_AGORA");
        assertThat(lidas.get(0).isVisivel()).isFalse();
        assertThat(lidas.get(0).getAtualizadoEm()).isNotNull();
        assertThat(lidas.get(0).getAtualizadoPor()).isEqualTo(3L);
    }

    @Test
    @DisplayName("fluxo real do service: salvar, reler e salvar de novo sem duplicar nem apagar linhas")
    void servicoPontaAPonta() {
        Usuario loja = novoUsuario();
        int cod = loja.getCodigo();

        service.salvar(cod, PapelMembro.DONO, 1, TelaPersonalizavel.INICIO, Set.of("ATALHOS", "TOTAIS"));
        limparSessao();
        Map<String, Boolean> primeiro = service.blocosVisiveis(cod, TelaPersonalizavel.INICIO);
        assertThat(primeiro.get("ATALHOS")).isTrue();
        assertThat(primeiro.get("TOTAIS")).isTrue();
        assertThat(primeiro.get("PRATELEIRA")).isFalse();
        long total = linhas(cod);
        assertThat(total).isEqualTo(primeiro.size());

        service.salvar(cod, PapelMembro.GERENTE, 2, TelaPersonalizavel.INICIO, Set.of("PRATELEIRA"));
        limparSessao();
        Map<String, Boolean> segundo = service.blocosVisiveis(cod, TelaPersonalizavel.INICIO);
        assertThat(linhas(cod)).isEqualTo(total);
        assertThat(segundo.get("PRATELEIRA")).isTrue();
        assertThat(segundo.get("ATALHOS")).isFalse();
    }

    @Test
    @DisplayName("service real recusa bloco de OS em loja de estoque e papel sem permissão, sem gravar")
    void servicoRecusaSemGravar() {
        Usuario loja = novoUsuario();

        assertThatThrownBy(() -> service.salvar(loja.getCodigo(), PapelMembro.DONO, 1, TelaPersonalizavel.INICIO,
                Set.of("OS_FLUXO"))).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.salvar(loja.getCodigo(), PapelMembro.VENDEDOR, 1, TelaPersonalizavel.INICIO,
                Set.of("ATALHOS"))).isInstanceOf(RegraNegocioException.class);
        assertThat(linhas(loja.getCodigo())).isZero();
    }

    @ParameterizedTest(name = "CHECK recusa tela={0}")
    @ValueSource(strings = {"XYZ", "inicio", ""})
    @DisplayName("CHECK: tela fora de INICIO/PRODUTOS é rejeitada pelo banco")
    void checkTela(String tela) {
        Usuario loja = novoUsuario();
        assertThatThrownBy(() -> repository.gravar(loja.getCodigo(), tela, "TOTAIS", true, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest(name = "CHECK recusa bloco={0}")
    @ValueSource(strings = {"totais", "TOTAIS-X", "TOTAIS X", "", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    @DisplayName("CHECK: bloco fora do padrão ^[A-Z0-9_]{1,40}$ é rejeitado pelo banco")
    void checkBloco(String bloco) {
        Usuario loja = novoUsuario();
        assertThatThrownBy(() -> repository.gravar(loja.getCodigo(), "INICIO", bloco, true, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FK: loja inexistente é rejeitada pelo banco")
    void fkLoja() {
        assertThatThrownBy(() -> repository.gravar(2_000_000_000, "INICIO", "TOTAIS", true, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
