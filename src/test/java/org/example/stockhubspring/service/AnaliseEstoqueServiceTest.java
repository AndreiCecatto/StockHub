package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.ItemCurvaAbc;
import org.example.stockhubspring.dto.PainelAnalises;
import org.example.stockhubspring.dto.SaidaComSaldo;
import org.example.stockhubspring.dto.SaidaProduto;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de AnaliseEstoqueService (RN-45 a RN-51) com repositórios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnaliseEstoqueServiceTest {

    private static final int LOJA = 7;
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 6, 15, 12, 0);

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    private AnaliseEstoqueService service;

    @BeforeEach
    void montar() {
        service = new AnaliseEstoqueService(produtoRepository, movimentacaoRepository);
        when(produtoRepository.valorPorCategoria(anyInt())).thenReturn(List.of());
        when(produtoRepository.listarParados(anyInt(), any())).thenReturn(List.of());
        when(movimentacaoRepository.saidasPorProduto(anyInt(), any(), any())).thenReturn(List.of());
        when(movimentacaoRepository.saidasPorCategoria(anyInt(), any(), any())).thenReturn(List.of());
        when(movimentacaoRepository.saidasPorDia(anyInt(), any(), any())).thenReturn(List.of());
        when(movimentacaoRepository.caixaPorMes(anyInt(), any(), any())).thenReturn(List.of());
        when(movimentacaoRepository.saidasComSaldo(anyInt(), any(), any())).thenReturn(List.of());
    }

    private static SaidaProduto saida(int id, String nome, long unidades, String valor) {
        return new SaidaProduto(id, nome, unidades, new BigDecimal(valor));
    }

    @Test
    @DisplayName("período aceita só 30, 90 e 365; qualquer outro valor vira 30")
    void periodoValidado() {
        assertThat(AnaliseEstoqueService.normalizarPeriodo(30)).isEqualTo(30);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(90)).isEqualTo(90);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(365)).isEqualTo(365);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(null)).isEqualTo(30);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(0)).isEqualTo(30);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(7)).isEqualTo(30);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(-90)).isEqualTo(30);
        assertThat(AnaliseEstoqueService.normalizarPeriodo(Integer.MAX_VALUE)).isEqualTo(30);
    }

    @Test
    @DisplayName("todas as consultas recebem a loja logada e o intervalo termina amanhã à meia-noite")
    void todasAsConsultasSaoDaLoja() {
        service.montar(LOJA, 90, AGORA);

        verify(produtoRepository).valorPorCategoria(LOJA);
        verify(produtoRepository).contarComSaldoSemCusto(LOJA);
        verify(produtoRepository).listarParados(LOJA, AGORA.minusDays(60));
        ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(movimentacaoRepository, atLeastOnce()).saidasPorProduto(anyInt(), inicio.capture(), fim.capture());
        assertThat(fim.getValue()).isEqualTo(LocalDateTime.of(2026, 6, 16, 0, 0));
        assertThat(inicio.getAllValues()).contains(LocalDateTime.of(2026, 3, 18, 0, 0)); // 90 dias incluindo hoje
        verify(movimentacaoRepository).saidasPorCategoria(org.mockito.ArgumentMatchers.eq(LOJA), any(), any());
        verify(movimentacaoRepository).saidasComSaldo(org.mockito.ArgumentMatchers.eq(LOJA), any(), any());
    }

    @Test
    @DisplayName("sem movimento, o painel traz todos os dias do período e os meses do caixa, com zero")
    void preencheDiasEMeses() {
        PainelAnalises p30 = service.montar(LOJA, 30, AGORA);
        PainelAnalises p365 = service.montar(LOJA, 365, AGORA);

        assertThat(p30.saidasPorDia()).hasSize(30);
        assertThat(p30.saidasPorDia().get(0).dia()).isEqualTo(AGORA.toLocalDate().minusDays(29));
        assertThat(p30.caixa()).hasSize(3);
        assertThat(p365.saidasPorDia()).hasSize(365);
        assertThat(p365.caixa()).hasSize(12);
        assertThat(p30.valorEstoque()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("cobertura: saldo / (saídas em 30 dias / 30); alerta só abaixo de 7 dias; ordena do mais urgente")
    void cobertura() {
        when(movimentacaoRepository.saidasComSaldo(anyInt(), any(), any())).thenReturn(List.of(
                new SaidaComSaldo(1, "Folgado", 300, 30L),   // 1/dia -> 300 dias
                new SaidaComSaldo(2, "Limite", 7, 30L),      // 1/dia -> 7 dias: não alerta
                new SaidaComSaldo(3, "Urgente", 6, 30L)));   // 1/dia -> 6 dias: alerta

        PainelAnalises p = service.montar(LOJA, 30, AGORA);

        assertThat(p.cobertura()).extracting("nome").containsExactly("Urgente", "Limite", "Folgado");
        assertThat(p.cobertura().get(0).dias()).isEqualByComparingTo("6.0");
        assertThat(p.cobertura().get(0).alerta()).isTrue();
        assertThat(p.cobertura().get(1).alerta()).isFalse();
        assertThat(p.totalEmAlerta()).isEqualTo(1);
    }

    @Test
    @DisplayName("cobertura mostra no máximo 20 produtos")
    void coberturaLimitada() {
        List<SaidaComSaldo> muitos = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            muitos.add(new SaidaComSaldo(i, "P" + i, i, 30L));
        }
        when(movimentacaoRepository.saidasComSaldo(anyInt(), any(), any())).thenReturn(muitos);

        assertThat(service.montar(LOJA, 30, AGORA).cobertura()).hasSize(AnaliseEstoqueService.MAX_COBERTURA);
    }

    @Test
    @DisplayName("top de produtos tem no máximo 10, na ordem que o banco devolveu")
    void topDez() {
        List<SaidaProduto> doze = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            doze.add(saida(i, "P" + i, 100 - i, "1.00"));
        }
        when(movimentacaoRepository.saidasPorProduto(anyInt(), any(), any())).thenReturn(doze);

        PainelAnalises p = service.montar(LOJA, 30, AGORA);

        assertThat(p.topProdutos()).hasSize(10);
        assertThat(p.topProdutos().get(0).nome()).isEqualTo("P1");
        assertThat(p.unidadesSaidas()).isEqualTo(doze.stream().mapToLong(SaidaProduto::unidades).sum());
    }

    @Test
    @DisplayName("curva ABC: A até 80 %, B até 95 %, C o resto; o produto que cruza o limite fica na classe de cima")
    void curvaAbc() {
        List<ItemCurvaAbc> abc = AnaliseEstoqueService.curvaAbc(List.of(
                saida(3, "C", 1, "5.00"),
                saida(1, "A", 1, "70.00"),
                saida(2, "B", 1, "20.00"),
                saida(4, "SemCusto", 9, "0.00")));

        assertThat(abc).extracting("nome").containsExactly("A", "B", "C");
        // A: 0 % antes -> A. B: 70 % antes -> A (cruza os 80 %). C: 90 % antes -> B.
        assertThat(abc).extracting("classe").containsExactly("A", "A", "B");
        assertThat(abc.get(0).percentual()).isEqualByComparingTo("73.68");
        assertThat(abc.get(2).acumulado()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("curva ABC sem valor de saída é vazia (sem divisão por zero)")
    void curvaAbcVazia() {
        assertThat(AnaliseEstoqueService.curvaAbc(Collections.emptyList())).isEmpty();
        assertThat(AnaliseEstoqueService.curvaAbc(List.of(saida(1, "X", 5, "0.00")))).isEmpty();
    }

    @Test
    @DisplayName("com período de 90 dias a curva ABC reaproveita a consulta de produtos (uma só ida ao banco)")
    void abcReaproveitaConsulta90() {
        service.montar(LOJA, 90, AGORA);
        verify(movimentacaoRepository, org.mockito.Mockito.times(1)).saidasPorProduto(anyInt(), any(), any());

        org.mockito.Mockito.clearInvocations(movimentacaoRepository);
        service.montar(LOJA, 30, AGORA);
        verify(movimentacaoRepository, org.mockito.Mockito.times(2)).saidasPorProduto(anyInt(), any(), any());
    }
}
