package org.example.stockhubspring.service;

import org.example.stockhubspring.model.Subtipo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapa de campos por ramo para o veículo da oficina (AD-18, RN-104). */
class CamposRamoVeiculoTest {

    @ParameterizedTest(name = "{0} tem placa e modelo")
    @EnumSource(value = Subtipo.class, names = {"MECANICA", "MOTO"})
    void mecanicaEMotoTemVeiculo(Subtipo subtipo) {
        var p = CamposRamo.permitidos(subtipo);
        assertThat(p.placa()).isTrue();
        assertThat(p.modelo()).isTrue();
        assertThat(p.temVeiculo()).isTrue();
        assertThat(p.isTemVeiculo()).isTrue();
        assertThat(p.permite(CamposRamo.Campo.PLACA)).isTrue();
        assertThat(p.permite(CamposRamo.Campo.MODELO)).isTrue();
        // placa e modelo não contam como campo de produto
        assertThat(p.temAlgum()).isFalse();
        assertThat(p.isTemAlgum()).isFalse();
    }

    @ParameterizedTest(name = "{0} não tem placa nem modelo")
    @EnumSource(value = Subtipo.class, names = {"MECANICA", "MOTO"}, mode = EnumSource.Mode.EXCLUDE)
    void outrosNaoTemVeiculo(Subtipo subtipo) {
        var p = CamposRamo.permitidos(subtipo);
        assertThat(p.placa()).isFalse();
        assertThat(p.modelo()).isFalse();
        assertThat(p.temVeiculo()).isFalse();
        assertThat(p.permite(CamposRamo.Campo.PLACA)).isFalse();
        assertThat(p.permite(CamposRamo.Campo.MODELO)).isFalse();
    }

    @Test
    @DisplayName("subtipo nulo e NENHUM não têm veículo")
    void nuloSemVeiculo() {
        assertThat(CamposRamo.permitidos(null).temVeiculo()).isFalse();
        assertThat(CamposRamo.Permitidos.NENHUM.temVeiculo()).isFalse();
    }

    @Test
    @DisplayName("só MECANICA e MOTO têm veículo, em todo o enum")
    void somenteDois() {
        for (Subtipo s : Subtipo.values()) {
            boolean esperado = s == Subtipo.MECANICA || s == Subtipo.MOTO;
            assertThat(CamposRamo.permitidos(s).temVeiculo()).as(s.name()).isEqualTo(esperado);
        }
    }
}
