package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.PainelAnalises;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.AnaliseEstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/** Análises de estoque (RN-45 a RN-51) contra o PostgreSQL, com dados criados no teste (rollback no fim). */
class AnaliseEstoqueIntegracaoTest extends IntegracaoBase {

    // Dia fixo no meio do mês: os meses do "caixa" ficam previsíveis
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 15, 12, 0);

    @Autowired
    private AnaliseEstoqueService analiseService;

    private Usuario loja;
    private Categoria bebidas;
    private Categoria limpeza;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        bebidas = novaCategoria(loja, "Bebidas");
        limpeza = novaCategoria(loja, "Limpeza");
    }

    private Produto produto(Usuario dono, Categoria categoria, String nome, int saldo, String custo) {
        Produto p = novoProduto(dono, categoria, nome, saldo, 0);
        p.setCusto(new BigDecimal(custo));
        return produtoRepository.saveAndFlush(p);
    }

    private void mov(Usuario dono, Produto p, TipoMovimentacao tipo, int qtd, String custo, LocalDateTime quando,
                     String observacao) {
        MovimentacaoEstoque m = new MovimentacaoEstoque();
        m.setProduto(p);
        m.setUsuario(dono);
        m.setTipo(tipo);
        m.setQuantidade(qtd);
        m.setQuantidadeAnterior(0);
        m.setQuantidadeNova(0);
        m.setCustoUnitario(new BigDecimal(custo));
        m.setObservacao(observacao);
        m.setCriadoEm(quando);
        movimentacaoRepository.saveAndFlush(m);
    }

    private PainelAnalises painel(Usuario dono, int periodo) {
        limparSessao();
        return analiseService.montar(dono.getCodigo(), periodo, AGORA);
    }

    @Test
    @DisplayName("valor do estoque = saldo x custo, por categoria; produto sem custo é contado à parte")
    void valorDoEstoque() {
        produto(loja, bebidas, "Refri", 10, "3.50");     // 35,00
        produto(loja, bebidas, "Suco", 4, "2.25");       // 9,00
        produto(loja, limpeza, "Sabão", 5, "0");         // sem custo
        produto(loja, limpeza, "Vazio", 0, "9.00");      // sem saldo: fora
        Produto inativo = produto(loja, limpeza, "Velho", 9, "100.00");
        inativo.setAtivo(false);
        produtoRepository.saveAndFlush(inativo);

        PainelAnalises p = painel(loja, 30);

        assertThat(p.valorEstoque()).isEqualByComparingTo("44.00");
        assertThat(p.produtosComSaldo()).isEqualTo(3);
        assertThat(p.produtosSemCusto()).isEqualTo(1);
        assertThat(p.valorPorCategoria()).extracting("categoria").containsExactly("Bebidas", "Limpeza");
        assertThat(p.valorPorCategoria().get(0).valor()).isEqualByComparingTo("44.00");
        assertThat(p.valorPorCategoria().get(1).valor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("loja sem nada devolve tudo vazio e zerado, com todos os dias e meses presentes")
    void lojaVazia() {
        PainelAnalises p = painel(loja, 30);

        assertThat(p.valorEstoque()).isEqualByComparingTo("0");
        assertThat(p.saidasPorDia()).hasSize(30);
        assertThat(p.saidasPorDia().get(29).dia()).isEqualTo(AGORA.toLocalDate());
        assertThat(p.topProdutos()).isEmpty();
        assertThat(p.caixa()).hasSize(3);
        assertThat(p.cobertura()).isEmpty();
        assertThat(p.parados()).isEmpty();
        assertThat(p.curvaAbc()).isEmpty();
    }

    @Test
    @DisplayName("saídas por dia, produto e categoria usam o custo gravado na movimentação")
    void saidas() {
        Produto refri = produto(loja, bebidas, "Refri", 100, "99.00"); // custo atual não pode interferir
        Produto sabao = produto(loja, limpeza, "Sabão", 100, "1.00");
        mov(loja, refri, TipoMovimentacao.SAIDA, 3, "2.00", AGORA.minusDays(1), "venda");
        mov(loja, refri, TipoMovimentacao.SAIDA, 2, "4.00", AGORA.minusDays(1).withHour(20), "venda");
        mov(loja, sabao, TipoMovimentacao.SAIDA, 10, "1.00", AGORA.minusDays(5), "venda");
        mov(loja, sabao, TipoMovimentacao.SAIDA, 7, "1.00", AGORA.minusDays(45), "fora dos 30 dias");
        mov(loja, refri, TipoMovimentacao.ENTRADA, 50, "2.00", AGORA.minusDays(2), "compra");

        PainelAnalises p = painel(loja, 30);

        assertThat(p.unidadesSaidas()).isEqualTo(15);
        assertThat(p.valorSaidas()).isEqualByComparingTo("24.00"); // 6 + 8 + 10
        assertThat(p.saidasPorDia()).hasSize(30);
        var ontem = p.saidasPorDia().stream().filter(d -> d.dia().equals(AGORA.toLocalDate().minusDays(1)))
                .findFirst().orElseThrow();
        assertThat(ontem.unidades()).isEqualTo(5);
        assertThat(ontem.valor()).isEqualByComparingTo("14.00");
        assertThat(p.topProdutos()).extracting("nome").containsExactly("Sabão", "Refri");
        assertThat(p.saidasPorCategoria()).extracting("categoria").containsExactly("Bebidas", "Limpeza"); // 14,00 x 10,00

        // Em 90 dias a saída antiga entra
        assertThat(painel(loja, 90).unidadesSaidas()).isEqualTo(22);
    }

    @Test
    @DisplayName("caixa: compras (sem estoque inicial) x saídas por mês, valor ao custo gravado, meses vazios com zero")
    void caixa() {
        Produto refri = produto(loja, bebidas, "Refri", 100, "5.00");
        mov(loja, refri, TipoMovimentacao.ENTRADA, 10, "2.00", AGORA.minusMonths(1), "Compra");              // 20
        mov(loja, refri, TipoMovimentacao.ENTRADA, 100, "2.00", AGORA.minusMonths(1), "Estoque inicial");   // fora
        mov(loja, refri, TipoMovimentacao.SAIDA, 4, "3.00", AGORA, "venda");                                // 12
        mov(loja, refri, TipoMovimentacao.AJUSTE, 8, "3.00", AGORA, "contagem");                            // fora

        PainelAnalises p = painel(loja, 30);

        assertThat(p.caixa()).extracting("mes").containsExactly(
                YearMonth.of(2026, 4), YearMonth.of(2026, 5), YearMonth.of(2026, 6));
        assertThat(p.caixa().get(0).compras()).isEqualByComparingTo("0");
        assertThat(p.caixa().get(1).compras()).isEqualByComparingTo("20.00");
        assertThat(p.caixa().get(1).saidas()).isEqualByComparingTo("0");
        assertThat(p.caixa().get(2).saidas()).isEqualByComparingTo("12.00");
        assertThat(painel(loja, 365).caixa()).hasSize(12);
    }

    @Test
    @DisplayName("cobertura = saldo / média diária dos últimos 30 dias; alerta abaixo de 7 dias; sem saída não entra")
    void cobertura() {
        Produto acabando = produto(loja, bebidas, "Acabando", 6, "1.00");   // 60 em 30 dias = 2/dia -> 3 dias
        Produto folgado = produto(loja, bebidas, "Folgado", 600, "1.00");   // 2/dia -> 300 dias
        produto(loja, bebidas, "Sem saída", 50, "1.00");
        mov(loja, acabando, TipoMovimentacao.SAIDA, 60, "1.00", AGORA.minusDays(3), "venda");
        mov(loja, folgado, TipoMovimentacao.SAIDA, 60, "1.00", AGORA.minusDays(3), "venda");

        PainelAnalises p = painel(loja, 30);

        assertThat(p.cobertura()).extracting("nome").containsExactly("Acabando", "Folgado");
        assertThat(p.cobertura().get(0).dias()).isEqualByComparingTo("3.0");
        assertThat(p.cobertura().get(0).alerta()).isTrue();
        assertThat(p.cobertura().get(1).alerta()).isFalse();
        assertThat(p.totalEmAlerta()).isEqualTo(1);
    }

    @Test
    @DisplayName("parados: com saldo e sem saída há 60 dias; produto novo ou com saída recente não entra")
    void parados() {
        Produto velho = produto(loja, bebidas, "Velho", 10, "5.00");
        Produto saiuAntes = produto(loja, bebidas, "Saiu antes", 4, "5.00");
        Produto saiuRecente = produto(loja, bebidas, "Saiu recente", 4, "5.00");
        Produto novo = produto(loja, bebidas, "Novo", 4, "5.00");
        Produto semHistorico = produto(loja, bebidas, "Sem histórico", 2, "1.00");
        Produto semSaldo = produto(loja, bebidas, "Sem saldo", 0, "5.00");
        mov(loja, velho, TipoMovimentacao.ENTRADA, 10, "5.00", AGORA.minusDays(100), "compra");
        mov(loja, saiuAntes, TipoMovimentacao.ENTRADA, 10, "5.00", AGORA.minusDays(200), "compra");
        mov(loja, saiuAntes, TipoMovimentacao.SAIDA, 6, "5.00", AGORA.minusDays(70), "venda");
        mov(loja, saiuRecente, TipoMovimentacao.ENTRADA, 10, "5.00", AGORA.minusDays(200), "compra");
        mov(loja, saiuRecente, TipoMovimentacao.SAIDA, 6, "5.00", AGORA.minusDays(10), "venda");
        mov(loja, novo, TipoMovimentacao.ENTRADA, 4, "5.00", AGORA.minusDays(5), "compra");
        mov(loja, semSaldo, TipoMovimentacao.ENTRADA, 4, "5.00", AGORA.minusDays(100), "compra");

        PainelAnalises p = painel(loja, 30);

        assertThat(p.parados()).extracting("nome").containsExactly("Velho", "Saiu antes", "Sem histórico");
        assertThat(p.totalParados()).isEqualTo(3);
        assertThat(p.valorParado()).isEqualByComparingTo("72.00"); // 50 + 20 + 2
        assertThat(p.parados().get(0).diasParado()).isEqualTo(100); // nunca saiu: conta da 1ª movimentação
        assertThat(p.parados().get(1).diasParado()).isEqualTo(70);
        assertThat(p.parados().get(2).diasParado()).isNull();
    }

    @Test
    @DisplayName("curva ABC por valor de saída em 90 dias, independente do período escolhido")
    void curvaAbc() {
        Produto a = produto(loja, bebidas, "A", 100, "1.00");
        Produto b = produto(loja, bebidas, "B", 100, "1.00");
        Produto c = produto(loja, bebidas, "C", 100, "1.00");
        mov(loja, a, TipoMovimentacao.SAIDA, 80, "1.00", AGORA.minusDays(40), "venda");  // 80 %
        mov(loja, b, TipoMovimentacao.SAIDA, 15, "1.00", AGORA.minusDays(40), "venda");  // 15 %
        mov(loja, c, TipoMovimentacao.SAIDA, 5, "1.00", AGORA.minusDays(40), "venda");   // 5 %

        PainelAnalises p = painel(loja, 30); // 40 dias atrás está fora dos 30, mas dentro dos 90 da curva

        assertThat(p.unidadesSaidas()).isZero();
        assertThat(p.curvaAbc()).extracting("nome").containsExactly("A", "B", "C");
        assertThat(p.curvaAbc()).extracting("classe").containsExactly("A", "B", "C");
        assertThat(p.curvaAbc().get(2).acumulado()).isEqualByComparingTo("100.00");
        assertThat(p.produtosClasseA()).isEqualTo(1);
        assertThat(p.percentualClasseA()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("isolamento: nada de outra loja aparece em nenhuma análise")
    void isolamentoPorLoja() {
        Usuario outra = novoUsuario();
        Categoria cat = novaCategoria(outra, "Alheia");
        Produto alheio = produto(outra, cat, "Alheio", 5, "100.00");
        mov(outra, alheio, TipoMovimentacao.ENTRADA, 50, "100.00", AGORA.minusDays(100), "compra");
        mov(outra, alheio, TipoMovimentacao.SAIDA, 50, "100.00", AGORA.minusDays(2), "venda");
        produto(outra, cat, "Parado alheio", 5, "100.00");

        PainelAnalises p = painel(loja, 365);

        assertThat(p.valorEstoque()).isEqualByComparingTo("0");
        assertThat(p.valorPorCategoria()).isEmpty();
        assertThat(p.unidadesSaidas()).isZero();
        assertThat(p.topProdutos()).isEmpty();
        assertThat(p.saidasPorCategoria()).isEmpty();
        assertThat(p.caixa()).allSatisfy(m -> {
            assertThat(m.compras()).isEqualByComparingTo("0");
            assertThat(m.saidas()).isEqualByComparingTo("0");
        });
        assertThat(p.cobertura()).isEmpty();
        assertThat(p.parados()).isEmpty();
        assertThat(p.curvaAbc()).isEmpty();

        // e a outra loja enxerga os próprios dados
        PainelAnalises dela = painel(outra, 365);
        assertThat(dela.unidadesSaidas()).isEqualTo(50);
        assertThat(dela.totalParados()).isEqualTo(1);
    }

    @Test
    @DisplayName("movimentar grava o custo informado, ou o custo atual do produto, na movimentação")
    void movimentarGravaCusto() {
        Produto p = produto(loja, bebidas, "Refri", 0, "3.50");
        MovimentacaoEstoque comCustoAtual = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5,
                "Compra", loja);
        MovimentacaoEstoque comCustoDaNota = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5,
                "Compra 2", loja, null, null, new BigDecimal("4.1234"));

        assertThat(comCustoAtual.getCustoUnitario()).isEqualByComparingTo("3.50");
        assertThat(comCustoDaNota.getCustoUnitario()).isEqualByComparingTo("4.1234");
        limparSessao();
        assertThat(jdbc.queryForObject("select custo_unitario from movimentacao_estoque where id = ?",
                BigDecimal.class, comCustoDaNota.getId())).isEqualByComparingTo("4.1234");
    }
}
