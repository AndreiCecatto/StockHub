package org.example.stockhubspring.util;

import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentosTest {

    @Test
    @DisplayName("CNPJ de teste calculado é válido; trocar um dígito invalida")
    void cnpj() {
        assertThat(Documentos.cnpjValido(NfeXml.CNPJ_FORNECEDOR)).isTrue();
        assertThat(Documentos.cnpjValido(NfeXml.CNPJ_LOJA)).isTrue();
        String errado = NfeXml.CNPJ_LOJA.substring(0, 13) + ((NfeXml.CNPJ_LOJA.charAt(13) - '0' + 1) % 10);
        assertThat(Documentos.cnpjValido(errado)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "00000000000000", "11111111111111", "1234567800019a", "123456780001999"})
    @DisplayName("CNPJ malformado ou de dígitos repetidos é inválido")
    void cnpjInvalido(String valor) {
        assertThat(Documentos.cnpjValido(valor)).isFalse();
    }

    @Test
    @DisplayName("CNPJ nulo é inválido")
    void cnpjNulo() {
        assertThat(Documentos.cnpjValido(null)).isFalse();
    }

    @Test
    @DisplayName("CPF com DV correto é válido (CPF sintético 529.982.247-25)")
    void cpf() {
        assertThat(Documentos.cpfValido("52998224725")).isTrue();
        assertThat(Documentos.cpfValido("52998224726")).isFalse();
        assertThat(Documentos.cpfValido("11111111111")).isFalse();
    }

    @Test
    @DisplayName("chave de acesso: DV módulo 11")
    void chave() {
        String chave = NfeXml.nota().item(NfeXml.item()).chave();
        assertThat(Documentos.chaveAcessoValida(chave)).isTrue();
        String errada = chave.substring(0, 43) + ((chave.charAt(43) - '0' + 1) % 10);
        assertThat(Documentos.chaveAcessoValida(errada)).isFalse();
        assertThat(Documentos.chaveAcessoValida("123")).isFalse();
        assertThat(Documentos.chaveAcessoValida(null)).isFalse();
    }

    @Test
    @DisplayName("soDigitos remove tudo que não é dígito")
    void soDigitos() {
        assertThat(Documentos.soDigitos("12.345.678/0001-95")).isEqualTo("12345678000195");
        assertThat(Documentos.soDigitos(null)).isEmpty();
    }
}
