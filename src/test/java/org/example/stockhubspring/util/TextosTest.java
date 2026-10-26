package org.example.stockhubspring.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextosTest {

    @Test
    @DisplayName("limpar remove espaços das pontas e texto vazio vira null")
    void limparRemoveEspacosEVazioViraNulo() {
        assertThat(Textos.limpar("  oi  ")).isEqualTo("oi");
        assertThat(Textos.limpar("   ")).isNull();
        assertThat(Textos.limpar("")).isNull();
        assertThat(Textos.limpar(null)).isNull();
    }

    @Test
    @DisplayName("normalizarNome ignora caixa e espaços nas pontas, mas não os do meio")
    void normalizarNomeIgnoraCaixaEEspacosNasPontas() {
        assertThat(Textos.normalizarNome("  CaMiSeTa  ")).isEqualTo("camiseta");
        assertThat(Textos.normalizarNome("Capa  Azul")).isEqualTo("capa  azul");
        assertThat(Textos.normalizarNome(null)).isEmpty();
    }

    @Test
    @DisplayName("padraoLike sem texto casa com tudo")
    void padraoLikeSemTextoCasaComTudo() {
        assertThat(Textos.padraoLike(null)).isEqualTo("%");
        assertThat(Textos.padraoLike("   ")).isEqualTo("%");
    }

    @Test
    @DisplayName("padraoLike põe em minúsculas e envolve com %")
    void padraoLikeMinusculasEEnvolve() {
        assertThat(Textos.padraoLike(" Caneta ")).isEqualTo("%caneta%");
    }

    @Test
    @DisplayName("padraoLike escapa os curingas % e _ e o próprio caractere de escape")
    void padraoLikeEscapaCuringas() {
        assertThat(Textos.padraoLike("50%")).isEqualTo("%50!%%");
        assertThat(Textos.padraoLike("a_b")).isEqualTo("%a!_b%");
        assertThat(Textos.padraoLike("oi!")).isEqualTo("%oi!!%");
        assertThat(Textos.padraoLike("%_!")).isEqualTo("%!%!_!!%");
    }
}
