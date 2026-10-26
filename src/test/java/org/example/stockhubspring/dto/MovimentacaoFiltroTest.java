package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.TipoMovimentacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentacaoFiltroTest {

    @Test
    @DisplayName("valores válidos são lidos (tipo sem diferenciar caixa, espaços aparados)")
    void valoresValidos() {
        MovimentacaoFiltro filtro = MovimentacaoFiltro.de(" 12 ", "ajuste", "2026-01-31", " 2026-02-01 ");
        assertThat(filtro.produtoId()).isEqualTo(12);
        assertThat(filtro.tipo()).isEqualTo(TipoMovimentacao.AJUSTE);
        assertThat(filtro.de()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(filtro.ate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(filtro.vazio()).isFalse();
        assertThat(filtro.tipoParametro()).isEqualTo("AJUSTE");
        assertThat(filtro.deParametro()).isEqualTo("2026-01-31");
        assertThat(filtro.ateParametro()).isEqualTo("2026-02-01");
    }

    @Test
    @DisplayName("nulos e vazios dão filtro vazio")
    void vazios() {
        assertThat(MovimentacaoFiltro.de(null, null, null, null)).isEqualTo(MovimentacaoFiltro.VAZIO);
        assertThat(MovimentacaoFiltro.de("", " ", "", " ").vazio()).isTrue();
        assertThat(MovimentacaoFiltro.VAZIO.tipoParametro()).isEmpty();
        assertThat(MovimentacaoFiltro.VAZIO.deParametro()).isEmpty();
    }

    @ParameterizedTest(name = "produto \"{0}\" é ignorado")
    @ValueSource(strings = {"abc", "0", "-4", "1.5", "99999999999", "1; drop table produto"})
    @DisplayName("produto inválido é ignorado")
    void produtoInvalido(String valor) {
        assertThat(MovimentacaoFiltro.de(valor, null, null, null).produtoId()).isNull();
    }

    @ParameterizedTest(name = "tipo \"{0}\" é ignorado")
    @ValueSource(strings = {"TRANSFERENCIA", "entrada ou saida", "1", "'"})
    @DisplayName("tipo inválido é ignorado")
    void tipoInvalido(String valor) {
        assertThat(MovimentacaoFiltro.de(null, valor, null, null).tipo()).isNull();
    }

    @ParameterizedTest(name = "data \"{0}\" é ignorada")
    @ValueSource(strings = {"31/12/2026", "2026-13-01", "2026-02-30", "26-1-1", "ontem", "2026-1-5",
            "+999999999-12-31", "0000-01-01", "-0001-05-05"})
    @DisplayName("data fora do formato aaaa-MM-dd, inexistente ou de ano absurdo é ignorada")
    void dataInvalida(String valor) {
        MovimentacaoFiltro filtro = MovimentacaoFiltro.de(null, null, valor, valor);
        assertThat(filtro.de()).isNull();
        assertThat(filtro.ate()).isNull();
    }
}
