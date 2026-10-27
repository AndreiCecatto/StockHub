package org.example.stockhubspring.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RN-43 e RN-44: módulos por tipo e combinações válidas de tipo e subtipo. */
class TipoLojaTest {

    @Test
    @DisplayName("ESTOQUE está sempre ligado; VENDAS só em comércio e misto; OS só em oficina e misto")
    void modulosPorTipo() {
        for (TipoLoja t : TipoLoja.values()) {
            assertThat(t.temModulo(Modulo.ESTOQUE)).isTrue();
        }
        assertThat(TipoLoja.ESTOQUE.temModulo(Modulo.VENDAS)).isFalse();
        assertThat(TipoLoja.ESTOQUE.temModulo(Modulo.OS)).isFalse();
        assertThat(TipoLoja.COMERCIO.temModulo(Modulo.VENDAS)).isTrue();
        assertThat(TipoLoja.COMERCIO.temModulo(Modulo.OS)).isFalse();
        assertThat(TipoLoja.OFICINA.temModulo(Modulo.VENDAS)).isFalse();
        assertThat(TipoLoja.OFICINA.temModulo(Modulo.OS)).isTrue();
        assertThat(TipoLoja.MISTO.temModulo(Modulo.VENDAS)).isTrue();
        assertThat(TipoLoja.MISTO.temModulo(Modulo.OS)).isTrue();
    }

    @Test
    @DisplayName("subtipo pertence ao seu tipo; misto aceita comércio e serviços, mas não os de estoque")
    void combinacoes() {
        assertThat(Subtipo.ALMOXARIFADO.pertenceA(TipoLoja.ESTOQUE)).isTrue();
        assertThat(Subtipo.ALMOXARIFADO.pertenceA(TipoLoja.COMERCIO)).isFalse();
        assertThat(Subtipo.FARMACIA.pertenceA(TipoLoja.COMERCIO)).isTrue();
        assertThat(Subtipo.FARMACIA.pertenceA(TipoLoja.OFICINA)).isFalse();
        assertThat(Subtipo.MECANICA.pertenceA(TipoLoja.OFICINA)).isTrue();
        assertThat(Subtipo.MECANICA.pertenceA(TipoLoja.MISTO)).isTrue();
        assertThat(Subtipo.MERCADO.pertenceA(TipoLoja.MISTO)).isTrue();
        assertThat(Subtipo.DEPOSITO.pertenceA(TipoLoja.MISTO)).isFalse();
    }

    @Test
    @DisplayName("conversão de texto: vazio vira ESTOQUE, desconhecido vira null")
    void conversao() {
        assertThat(TipoLoja.de(null)).isEqualTo(TipoLoja.ESTOQUE);
        assertThat(TipoLoja.de("  ")).isEqualTo(TipoLoja.ESTOQUE);
        assertThat(TipoLoja.de("misto")).isEqualTo(TipoLoja.MISTO);
        assertThat(TipoLoja.de("outro")).isNull();
        assertThat(Subtipo.de("")).isNull();
    }
}
