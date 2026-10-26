package org.example.stockhubspring.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** RN-12: definição única do nível de estoque. */
class NivelEstoqueTest {

    @ParameterizedTest(name = "saldo {0} e mínimo {1} -> {2}")
    @CsvSource({
            "0, 0, SEM_ESTOQUE",
            "0, 5, SEM_ESTOQUE",
            "-3, 5, SEM_ESTOQUE",
            "1, 0, NORMAL",
            "1, 1, BAIXO",
            "4, 5, BAIXO",
            "5, 5, BAIXO",
            "6, 5, NORMAL",
            "100, 5, NORMAL"
    })
    @DisplayName("calcula o nível nos limites: 0, mínimo e mínimo+1")
    void calculaNivelNosLimites(int saldo, int minimo, NivelEstoque esperado) {
        assertThat(NivelEstoque.calcular(saldo, minimo)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("saldo igual ao mínimo é BAIXO e mínimo+1 já é NORMAL")
    void saldoIgualAoMinimoEBaixoEUmAcimaENormal() {
        assertThat(NivelEstoque.calcular(10, 10)).isEqualTo(NivelEstoque.BAIXO);
        assertThat(NivelEstoque.calcular(11, 10)).isEqualTo(NivelEstoque.NORMAL);
    }

    @Test
    @DisplayName("Produto.isEstoqueBaixo só é verdadeiro no nível BAIXO, não em SEM_ESTOQUE")
    void produtoEstoqueBaixoSoNoNivelBaixo() {
        assertThat(produto(0, 5).isEstoqueBaixo()).isFalse();
        assertThat(produto(5, 5).isEstoqueBaixo()).isTrue();
        assertThat(produto(6, 5).isEstoqueBaixo()).isFalse();
        assertThat(produto(0, 5).getNivelEstoque()).isEqualTo(NivelEstoque.SEM_ESTOQUE);
    }

    @Test
    @DisplayName("valor total do produto é preço vezes quantidade")
    void valorTotalDoProduto() {
        Produto produto = produto(3, 1);
        produto.setPreco(new BigDecimal("19.90"));
        assertThat(produto.getValorTotal()).isEqualByComparingTo("59.70");
    }

    @ParameterizedTest
    @CsvSource({"ok, NORMAL", "OK, NORMAL", "baixo, BAIXO", "BAIXO, BAIXO", "semEstoque, SEM_ESTOQUE",
            "SEM_ESTOQUE, SEM_ESTOQUE"})
    @DisplayName("aceita o código da tela ou o nome do enum como parâmetro")
    void interpretaParametroDaTela(String texto, NivelEstoque esperado) {
        assertThat(NivelEstoque.deParametro(texto)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("parâmetro com espaços nas pontas é aparado")
    void parametroComEspacosEAparado() {
        assertThat(NivelEstoque.deParametro("  baixo ")).isEqualTo(NivelEstoque.BAIXO);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "xyz", "alto", "normal; drop table produto"})
    @DisplayName("parâmetro vazio ou inválido vira null (sem filtro)")
    void parametroInvalidoViraNulo(String texto) {
        assertThat(NivelEstoque.deParametro(texto)).isNull();
    }

    private static Produto produto(int quantidade, int minimo) {
        Produto produto = new Produto();
        produto.setQuantidade(quantidade);
        produto.setEstoqueMinimo(minimo);
        return produto;
    }
}
