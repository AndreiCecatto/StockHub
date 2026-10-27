package org.example.stockhubspring.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class BuscaDifusaTest {

    @ParameterizedTest(name = "\"{0}\" acha \"{1}\"")
    @DisplayName("erros comuns de digitação ainda acham o produto")
    @CsvSource({
            "cenata,            Caneta esferográfica azul",
            "canta,             Caneta esferográfica azul",
            "caneat,            Caneta esferográfica azul",
            "canetta,           Caneta esferográfica azul",
            "esferografica,     Caneta esferográfica azul",
            "ESFEROGRAFICA,     Caneta esferográfica azul",
            "cane azl,          Caneta esferográfica azul",
            "detergnte,         Detergente neutro 500 ml",
            "cafe torado,       Café torrado 500 g",
            "mouze,             Mouse sem fio",
            "pen driv,          Pen drive 64 GB"
    })
    void acha(String digitado, String nome) {
        assertThat(BuscaDifusa.pontuar(digitado, nome)).isGreaterThan(0);
    }

    @ParameterizedTest(name = "\"{0}\" não acha \"{1}\"")
    @DisplayName("palavras sem parecido não casam, nem as curtas por acaso")
    @CsvSource({
            "xyz,               Caneta esferográfica azul",
            "caderno,           Caneta esferográfica azul",
            "ca,                Mouse sem fio",
            "mo ca,             Mouse sem fio",
            "teclado,           Mouse sem fio"
    })
    void naoAcha(String digitado, String nome) {
        assertThat(BuscaDifusa.pontuar(digitado, nome)).isEqualTo(BuscaDifusa.SEM_CORRESPONDENCIA);
    }

    @Test
    @DisplayName("o começo exato vale mais que o aproximado, e a palavra inteira mais que o começo")
    void ordemDasNotas() {
        double inteira = BuscaDifusa.pontuar("caneta", "Caneta azul");
        double comeco = BuscaDifusa.pontuar("cane", "Caneta azul");
        double erro = BuscaDifusa.pontuar("cenata", "Caneta azul");
        assertThat(inteira).isEqualTo(1);
        assertThat(inteira).isGreaterThan(comeco);
        assertThat(comeco).isGreaterThan(erro);
        assertThat(erro).isGreaterThan(0);
    }

    @Test
    @DisplayName("todas as palavras digitadas precisam casar")
    void todasAsPalavras() {
        assertThat(BuscaDifusa.pontuar("caneta azul", "Caneta azul")).isGreaterThan(0);
        assertThat(BuscaDifusa.pontuar("caneta verde", "Caneta azul")).isEqualTo(0);
    }

    @Test
    @DisplayName("entradas vazias ou só pontuação não casam com nada")
    void entradasVazias() {
        assertThat(BuscaDifusa.pontuar(null, "Caneta")).isEqualTo(0);
        assertThat(BuscaDifusa.pontuar("  ", "Caneta")).isEqualTo(0);
        assertThat(BuscaDifusa.pontuar("%%", "Caneta")).isEqualTo(0);
        assertThat(BuscaDifusa.pontuar("caneta", "")).isEqualTo(0);
    }

    @Test
    @DisplayName("distância conta a inversão de letras vizinhas como uma edição")
    void distancia() {
        assertThat(BuscaDifusa.distancia("caneta", "cnaeta")).isEqualTo(1);
        assertThat(BuscaDifusa.distancia("caneta", "cenata")).isEqualTo(2);
        assertThat(BuscaDifusa.distancia("caneta", "canteta")).isEqualTo(1);
        assertThat(BuscaDifusa.distancia("ab", "ba")).isEqualTo(1);
        assertThat(BuscaDifusa.distancia("", "abc")).isEqualTo(3);
    }
}
