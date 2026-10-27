package org.example.stockhubspring.nfe;

import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** RN-54: rateio de frete, seguro e outras despesas no custo do item. */
class RateioNfeTest {

    private static NotaNfe ler(NfeXml nota) {
        return NfeParser.ler(nota.bytes());
    }

    private static BigDecimal soma(List<BigDecimal> partes) {
        return partes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("divide em proporção ao valor do item")
    void proporcional() {
        NotaNfe nota = ler(NfeXml.nota().frete("8.00")
                .item(NfeXml.item().codigo("A").quantidade("10.0000").unitario("5.0000000000"))   // 50,00
                .item(NfeXml.item().codigo("B").quantidade("3.0000").unitario("10.0000000000"))); // 30,00

        assertThat(nota.itens().get(0).frete()).isEqualByComparingTo("5.00");
        assertThat(nota.itens().get(1).frete()).isEqualByComparingTo("3.00");
        // custo = (50 + 5) / 10 e (30 + 3) / 3
        assertThat(nota.itens().get(0).custoUnitario()).isEqualByComparingTo("5.5000");
        assertThat(nota.itens().get(1).custoUnitario()).isEqualByComparingTo("11.0000");
    }

    @Test
    @DisplayName("a soma das partes fecha com o total da nota (centavo que sobra vai para o maior resto)")
    void fechaComOTotal() {
        NotaNfe nota = ler(NfeXml.nota().frete("10.00").seguro("0.01").outras("0.02")
                .item(NfeXml.item().codigo("A").quantidade("1.0000").unitario("10.0000000000"))
                .item(NfeXml.item().codigo("B").quantidade("1.0000").unitario("10.0000000000"))
                .item(NfeXml.item().codigo("C").quantidade("1.0000").unitario("10.0000000000")));

        assertThat(soma(nota.itens().stream().map(ItemNfe::frete).toList())).isEqualByComparingTo("10.00");
        assertThat(soma(nota.itens().stream().map(ItemNfe::seguro).toList())).isEqualByComparingTo("0.01");
        assertThat(soma(nota.itens().stream().map(ItemNfe::outras).toList())).isEqualByComparingTo("0.02");
        assertThat(nota.itens().get(0).frete()).isEqualByComparingTo("3.34");
        assertThat(nota.itens().get(1).frete()).isEqualByComparingTo("3.33");
        assertThat(nota.itens().get(0).seguro()).isEqualByComparingTo("0.01");
        assertThat(nota.itens().get(1).seguro()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a base é o valor líquido: o desconto do item reduz a sua parte")
    void descontoNaBase() {
        NotaNfe nota = ler(NfeXml.nota().frete("9.00")
                .item(NfeXml.item().codigo("A").quantidade("1.0000").unitario("100.0000000000").desconto("50.00"))
                .item(NfeXml.item().codigo("B").quantidade("1.0000").unitario("50.0000000000")));

        assertThat(nota.itens().get(0).frete()).isEqualByComparingTo("4.50");
        assertThat(nota.itens().get(1).frete()).isEqualByComparingTo("4.50");
    }

    @Test
    @DisplayName("valores que o emitente já pôs dentro dos itens são mantidos, sem ratear de novo")
    void valoresDoItemSaoMantidos() {
        NotaNfe nota = ler(NfeXml.nota()
                .item(NfeXml.item().codigo("A").quantidade("1.0000").unitario("10.0000000000").frete("1.00"))
                .item(NfeXml.item().codigo("B").quantidade("1.0000").unitario("10.0000000000").frete("2.00")));

        assertThat(nota.itens().get(0).frete()).isEqualByComparingTo("1.00");
        assertThat(nota.itens().get(1).frete()).isEqualByComparingTo("2.00");
        assertThat(nota.itens().get(1).custoUnitario()).isEqualByComparingTo("12.0000");
    }

    @Test
    @DisplayName("sem despesas na nota o custo continua sendo só valor líquido / quantidade")
    void semDespesas() {
        ItemNfe item = ler(NfeXml.nota().item(NfeXml.item().quantidade("4.0000").unitario("2.5000000000")
                .desconto("2.00"))).itens().get(0);

        assertThat(item.rateio()).isEqualByComparingTo("0");
        assertThat(item.custoUnitario()).isEqualByComparingTo("2.0000");
    }

    @Test
    @DisplayName("custo unitário com 4 casas e arredondamento HALF_UP depois do rateio")
    void quatroCasas() {
        ItemNfe item = ler(NfeXml.nota().frete("1.00")
                .item(NfeXml.item().quantidade("3.0000").unitario("1.0000000000"))).itens().get(0);

        // (3,00 + 1,00) / 3 = 1,33333...
        assertThat(item.custoUnitario()).isEqualByComparingTo("1.3333");
    }

    @Test
    @DisplayName("distribuir: sem base positiva não há como ratear (devolve nulos)")
    void semBase() {
        List<BigDecimal> partes = RateioNfe.distribuir(new BigDecimal("5.00"),
                List.of(BigDecimal.ZERO, BigDecimal.ZERO));

        assertThat(partes).containsOnlyNulls();
    }

    @Test
    @DisplayName("distribuir: 1 centavo entre 3 itens iguais vai para o primeiro")
    void umCentavo() {
        List<BigDecimal> partes = RateioNfe.distribuir(new BigDecimal("0.01"),
                List.of(BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN));

        assertThat(partes.get(0)).isEqualByComparingTo("0.01");
        assertThat(soma(partes)).isEqualByComparingTo("0.01");
    }
}
