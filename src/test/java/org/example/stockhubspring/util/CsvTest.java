package org.example.stockhubspring.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Formato, escape e proteção contra injeção de fórmula do CSV. */
class CsvTest {

    @ParameterizedTest(name = "texto \"{0}\" ganha apóstrofo na frente")
    @ValueSource(strings = {"=1+1", "+1", "-1", "@SUM(A1)", "\t=1", "\r=1", "  =1+1", "=HYPERLINK(\"http://x\")"})
    @DisplayName("texto que começa com = + - @ tab ou CR é neutralizado com apóstrofo")
    void protegeFormula(String valor) {
        assertThat(Csv.texto(valor)).contains("'" + valor.replace("\"", "\"\""));
        assertThat(Csv.protegerFormula(valor)).startsWith("'");
    }

    @ParameterizedTest(name = "texto \"{0}\" não é alterado")
    @ValueSource(strings = {"Caneta", "a=b", "x-y", "1+1", "e@mail.com", "123", "'=já tem apóstrofo", "Café com leite"})
    @DisplayName("texto comum, ou com esses símbolos fora do começo, passa intacto")
    void textoComumNaoMuda(String valor) {
        assertThat(Csv.texto(valor)).isEqualTo(valor);
    }

    @Test
    @DisplayName("nulo vira célula vazia")
    void nuloViraVazio() {
        assertThat(Csv.texto(null)).isEmpty();
        assertThat(Csv.decimal(null)).isEmpty();
    }

    @Test
    @DisplayName("aspas, separador e quebras de linha põem a célula entre aspas, com as aspas internas dobradas")
    void escapaAspasSeparadorEQuebras() {
        assertThat(Csv.texto("a;b")).isEqualTo("\"a;b\"");
        assertThat(Csv.texto("diz \"oi\"")).isEqualTo("\"diz \"\"oi\"\"\"");
        assertThat(Csv.texto("linha1\nlinha2")).isEqualTo("\"linha1\nlinha2\"");
        assertThat(Csv.texto("linha1\r\nlinha2")).isEqualTo("\"linha1\r\nlinha2\"");
    }

    @Test
    @DisplayName("proteção e escape se combinam: apóstrofo primeiro, depois aspas")
    void protecaoEEscapeJuntos() {
        assertThat(Csv.texto("=A1;\"x\"")).isEqualTo("\"'=A1;\"\"x\"\"\"");
    }

    @Test
    @DisplayName("decimal usa vírgula, 2 casas e nenhum separador de milhar")
    void decimalComVirgula() {
        assertThat(Csv.decimal(new BigDecimal("1234.5"))).isEqualTo("1234,50");
        assertThat(Csv.decimal(new BigDecimal("0"))).isEqualTo("0,00");
        assertThat(Csv.decimal(new BigDecimal("9999999.99"))).isEqualTo("9999999,99");
    }

    @Test
    @DisplayName("números inteiros não recebem apóstrofo, mesmo negativos")
    void inteirosSemProtecao() {
        assertThat(Csv.inteiro(-3)).isEqualTo("-3");
        assertThat(Csv.inteiro(42)).isEqualTo("42");
    }

    @Test
    @DisplayName("linha junta com ';' e termina em CRLF; arquivo começa com BOM UTF-8 e guarda os acentos")
    void linhaEArquivo() {
        String linha = Csv.linha("a", "b", "c");
        assertThat(linha).isEqualTo("a;b;c\r\n");

        byte[] arquivo = Csv.bytes(List.of(Csv.linha("Código", "Descrição"), linha));
        assertThat(arquivo).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(arquivo, 3, arquivo.length - 3, StandardCharsets.UTF_8))
                .isEqualTo("Código;Descrição\r\na;b;c\r\n");
    }
}
