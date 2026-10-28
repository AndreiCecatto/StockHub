package org.example.stockhubspring.model;

import org.example.stockhubspring.service.CamposRamo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Colunas da lista de produtos em BlocoTela e slugs de TelaPersonalizavel (AD-20, RN-115 a RN-120). */
class BlocoTelaColunasTest {

    private static final List<BlocoTela> COLUNAS_FIXAS = List.of(BlocoTela.COL_CATEGORIA, BlocoTela.COL_PRECO,
            BlocoTela.COL_SITUACAO, BlocoTela.COL_DESCRICAO);

    @Test
    @DisplayName("a tela PRODUTOS tem exatamente as 9 colunas COL_*")
    void nove() {
        List<BlocoTela> daTela = java.util.Arrays.stream(BlocoTela.values())
                .filter(b -> b.getTela() == TelaPersonalizavel.PRODUTOS).toList();
        assertThat(daTela).hasSize(9).allSatisfy(b -> assertThat(b.name()).startsWith("COL_"));
    }

    @Test
    @DisplayName("campoExigido: só as colunas de ramo exigem campo")
    void campoExigido() {
        assertThat(BlocoTela.COL_TAMANHO.getCampoExigido()).isEqualTo(CamposRamo.Campo.TAMANHO);
        assertThat(BlocoTela.COL_COR.getCampoExigido()).isEqualTo(CamposRamo.Campo.COR);
        assertThat(BlocoTela.COL_LOTE.getCampoExigido()).isEqualTo(CamposRamo.Campo.LOTE);
        assertThat(BlocoTela.COL_VALIDADE.getCampoExigido()).isEqualTo(CamposRamo.Campo.VALIDADE);
        assertThat(BlocoTela.COL_NUMERO_SERIE.getCampoExigido()).isEqualTo(CamposRamo.Campo.NUMERO_SERIE);
        assertThat(COLUNAS_FIXAS).allSatisfy(b -> assertThat(b.getCampoExigido()).isNull());
        assertThat(BlocoTela.TOTAIS.getCampoExigido()).isNull();
    }

    @Test
    @DisplayName("aplicavel: coluna de ramo só com o campo permitido; sem informar permitidos não se aplica")
    void aplicavel() {
        CamposRamo.Permitidos farmacia = CamposRamo.permitidos(Subtipo.FARMACIA);
        assertThat(BlocoTela.COL_LOTE.aplicavel(TipoLoja.COMERCIO, farmacia)).isTrue();
        assertThat(BlocoTela.COL_TAMANHO.aplicavel(TipoLoja.COMERCIO, farmacia)).isFalse();
        assertThat(BlocoTela.COL_LOTE.aplicavel(TipoLoja.COMERCIO)).isFalse();
        assertThat(BlocoTela.COL_LOTE.aplicavel(TipoLoja.COMERCIO, null)).isFalse();
        assertThat(BlocoTela.COL_PRECO.aplicavel(TipoLoja.ESTOQUE, CamposRamo.Permitidos.NENHUM)).isTrue();
        assertThat(BlocoTela.COL_PRECO.aplicavel(null)).isTrue();
        // o módulo continua valendo
        assertThat(BlocoTela.OS_FLUXO.aplicavel(TipoLoja.ESTOQUE, farmacia)).isFalse();
    }

    @Test
    @DisplayName("daTela(PRODUTOS): padrão traz só as 4 colunas fixas; cada ramo soma as suas")
    void daTelaPorRamo() {
        assertThat(BlocoTela.daTela(TelaPersonalizavel.PRODUTOS, TipoLoja.ESTOQUE, CamposRamo.Permitidos.NENHUM))
                .containsExactlyElementsOf(COLUNAS_FIXAS);
        assertThat(BlocoTela.daTela(TelaPersonalizavel.PRODUTOS, TipoLoja.COMERCIO, CamposRamo.permitidos(Subtipo.VESTUARIO)))
                .containsExactly(BlocoTela.COL_CATEGORIA, BlocoTela.COL_PRECO, BlocoTela.COL_SITUACAO,
                        BlocoTela.COL_DESCRICAO, BlocoTela.COL_TAMANHO, BlocoTela.COL_COR);
        assertThat(BlocoTela.daTela(TelaPersonalizavel.PRODUTOS, TipoLoja.COMERCIO, CamposRamo.permitidos(Subtipo.INFORMATICA)))
                .contains(BlocoTela.COL_NUMERO_SERIE).doesNotContain(BlocoTela.COL_LOTE);
    }

    @Test
    @DisplayName("daTela(INICIO) não traz colunas, e daTela(PRODUTOS) não traz blocos do início")
    void telasNaoMisturam() {
        assertThat(BlocoTela.daTela(TelaPersonalizavel.INICIO, TipoLoja.COMERCIO, CamposRamo.permitidos(Subtipo.FARMACIA)))
                .allSatisfy(b -> assertThat(b.name()).doesNotStartWith("COL_"));
        assertThat(BlocoTela.daTela(TelaPersonalizavel.PRODUTOS, TipoLoja.COMERCIO, CamposRamo.permitidos(Subtipo.FARMACIA)))
                .allSatisfy(b -> assertThat(b.getTela()).isEqualTo(TelaPersonalizavel.PRODUTOS));
    }

    @Test
    @DisplayName("slug e deSlug fazem o caminho de ida e volta")
    void slugs() {
        assertThat(TelaPersonalizavel.INICIO.slug()).isEqualTo("inicio");
        assertThat(TelaPersonalizavel.PRODUTOS.slug()).isEqualTo("produtos");
        assertThat(TelaPersonalizavel.deSlug("inicio")).isEqualTo(TelaPersonalizavel.INICIO);
        assertThat(TelaPersonalizavel.deSlug("produtos")).isEqualTo(TelaPersonalizavel.PRODUTOS);
        assertThat(TelaPersonalizavel.deSlug(" produtos ")).isEqualTo(TelaPersonalizavel.PRODUTOS);
    }

    @Test
    @DisplayName("slug não depende do Locale padrão (tr-TR não vira 'ınıcıo')")
    void slugIndependeDoLocale() {
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertThat(TelaPersonalizavel.INICIO.slug()).isEqualTo("inicio");
            assertThat(TelaPersonalizavel.deSlug("inicio")).isEqualTo(TelaPersonalizavel.INICIO);
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "INICIO", "Produtos", "usuarios", "../etc", "<script>alert(1)</script>"})
    @DisplayName("deSlug devolve null para o que não é slug exato")
    void slugDesconhecido(String slug) {
        assertThat(TelaPersonalizavel.deSlug(slug)).isNull();
    }
}
