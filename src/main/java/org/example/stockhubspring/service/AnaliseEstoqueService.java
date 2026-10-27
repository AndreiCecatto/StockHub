package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.CaixaMes;
import org.example.stockhubspring.dto.ItemCobertura;
import org.example.stockhubspring.dto.ItemCurvaAbc;
import org.example.stockhubspring.dto.PainelAnalises;
import org.example.stockhubspring.dto.ProdutoParado;
import org.example.stockhubspring.dto.SaidaCategoria;
import org.example.stockhubspring.dto.SaidaComSaldo;
import org.example.stockhubspring.dto.SaidaDia;
import org.example.stockhubspring.dto.SaidaProduto;
import org.example.stockhubspring.dto.ValorCategoria;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Análises de estoque (RN-45 a RN-51). A soma, a contagem e o agrupamento são feitos no banco, sempre
 * restritos à loja; aqui ficam só o preenchimento de dias e meses sem movimento, a cobertura, a curva ABC
 * e o recorte das listas.
 */
@Service
public class AnaliseEstoqueService {

    /** Períodos aceitos, em dias. Qualquer outro valor vira {@link #PERIODO_PADRAO} (RN-45). */
    public static final Set<Integer> PERIODOS = Set.of(30, 90, 365);
    public static final int PERIODO_PADRAO = 30;
    /** Janela da média de saída usada na cobertura (RN-49). */
    public static final int JANELA_COBERTURA_DIAS = 30;
    /** Cobertura menor que isto, em dias, gera alerta (RN-49). */
    public static final BigDecimal LIMITE_ALERTA_DIAS = BigDecimal.valueOf(7);
    /** Sem saída há tantos dias, com saldo, o produto é considerado parado (RN-50). */
    public static final int DIAS_PARADO = 60;
    /** Janela da curva ABC (RN-51). */
    public static final int JANELA_ABC_DIAS = 90;
    public static final int TOP_PRODUTOS = 10;
    public static final int MAX_COBERTURA = 20;
    public static final int MAX_PARADOS = 50;

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public AnaliseEstoqueService(ProdutoRepository produtoRepository,
                                 MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    /** Período válido: 30, 90 ou 365; qualquer outro (ou nulo) vira 30. */
    public static int normalizarPeriodo(Integer periodo) {
        return periodo != null && PERIODOS.contains(periodo) ? periodo : PERIODO_PADRAO;
    }

    /** Quantos meses o gráfico de compras x saídas mostra para cada período. */
    public static int mesesDoCaixa(int periodo) {
        return switch (periodo) {
            case 365 -> 12;
            case 90 -> 6;
            default -> 3;
        };
    }

    @Transactional(readOnly = true)
    public PainelAnalises montar(Integer usuarioCodigo, Integer periodoPedido) {
        return montar(usuarioCodigo, periodoPedido, LocalDateTime.now(DashboardService.FUSO));
    }

    /** Igual ao anterior, com o "agora" informado (permite testar sem depender do relógio). */
    @Transactional(readOnly = true)
    public PainelAnalises montar(Integer usuarioCodigo, Integer periodoPedido, LocalDateTime agora) {
        int periodo = normalizarPeriodo(periodoPedido);
        LocalDate hoje = agora.toLocalDate();
        LocalDateTime fim = hoje.plusDays(1).atStartOfDay();

        // 1. Dinheiro parado no estoque
        List<ValorCategoria> porCategoria = produtoRepository.valorPorCategoria(usuarioCodigo);
        BigDecimal valorEstoque = porCategoria.stream().map(ValorCategoria::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long produtosComSaldo = porCategoria.stream().mapToLong(ValorCategoria::produtos).sum();
        long semCusto = produtoRepository.contarComSaldoSemCusto(usuarioCodigo);

        // 2. O que mais sai
        LocalDateTime inicioPeriodo = hoje.minusDays(periodo - 1L).atStartOfDay();
        List<SaidaDia> saidasDia = saidasPorDia(usuarioCodigo, hoje, periodo, inicioPeriodo, fim);
        List<SaidaProduto> saidasProduto = movimentacaoRepository.saidasPorProduto(usuarioCodigo, inicioPeriodo, fim);
        long unidadesSaidas = saidasProduto.stream().mapToLong(SaidaProduto::unidades).sum();
        BigDecimal valorSaidas = saidasProduto.stream().map(SaidaProduto::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SaidaProduto> topProdutos = saidasProduto.stream().limit(TOP_PRODUTOS).toList();
        List<SaidaCategoria> saidasCategoria =
                movimentacaoRepository.saidasPorCategoria(usuarioCodigo, inicioPeriodo, fim);

        // 3. Compras x saídas por mês
        int meses = mesesDoCaixa(periodo);
        List<CaixaMes> caixa = caixaPorMes(usuarioCodigo, YearMonth.from(hoje), meses, fim);

        // 4. Vai acabar em breve
        List<ItemCobertura> cobertura = cobertura(usuarioCodigo, hoje, fim);
        long emAlerta = cobertura.stream().filter(ItemCobertura::alerta).count();

        // 5. Produtos parados
        List<ProdutoParado> paradosTodos = parados(usuarioCodigo, agora);
        BigDecimal valorParado = paradosTodos.stream().map(ProdutoParado::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6. Curva ABC (90 dias)
        List<SaidaProduto> base90 = periodo == JANELA_ABC_DIAS ? saidasProduto
                : movimentacaoRepository.saidasPorProduto(usuarioCodigo,
                        hoje.minusDays(JANELA_ABC_DIAS - 1L).atStartOfDay(), fim);
        List<ItemCurvaAbc> abc = curvaAbc(base90);
        long classeA = abc.stream().filter(i -> "A".equals(i.classe())).count();
        BigDecimal percentualA = abc.stream().filter(i -> "A".equals(i.classe()))
                .map(ItemCurvaAbc::percentual).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PainelAnalises(periodo, meses,
                valorEstoque, produtosComSaldo, semCusto, porCategoria,
                saidasDia, unidadesSaidas, valorSaidas, topProdutos, saidasCategoria,
                caixa,
                cobertura, emAlerta,
                paradosTodos.stream().limit(MAX_PARADOS).toList(), paradosTodos.size(), valorParado,
                abc, classeA, percentualA);
    }

    private List<SaidaDia> saidasPorDia(Integer usuarioCodigo, LocalDate hoje, int periodo,
                                        LocalDateTime inicio, LocalDateTime fim) {
        Map<LocalDate, Object[]> porDia = new HashMap<>();
        for (Object[] linha : movimentacaoRepository.saidasPorDia(usuarioCodigo, inicio, fim)) {
            porDia.put(paraData(linha[0]), linha);
        }
        List<SaidaDia> resultado = new ArrayList<>(periodo);
        for (int i = periodo - 1; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            Object[] linha = porDia.get(dia);
            resultado.add(linha == null ? new SaidaDia(dia, 0, BigDecimal.ZERO)
                    : new SaidaDia(dia, ((Number) linha[1]).longValue(), dinheiro(linha[2])));
        }
        return resultado;
    }

    private List<CaixaMes> caixaPorMes(Integer usuarioCodigo, YearMonth atual, int meses, LocalDateTime fim) {
        YearMonth primeiro = atual.minusMonths(meses - 1L);
        Map<YearMonth, Object[]> porMes = new HashMap<>();
        for (Object[] linha : movimentacaoRepository.caixaPorMes(usuarioCodigo,
                primeiro.atDay(1).atStartOfDay(), fim)) {
            porMes.put(YearMonth.parse(linha[0].toString()), linha);
        }
        List<CaixaMes> resultado = new ArrayList<>(meses);
        for (int i = 0; i < meses; i++) {
            YearMonth mes = primeiro.plusMonths(i);
            Object[] linha = porMes.get(mes);
            resultado.add(linha == null ? new CaixaMes(mes, BigDecimal.ZERO, BigDecimal.ZERO)
                    : new CaixaMes(mes, dinheiro(linha[1]), dinheiro(linha[2])));
        }
        return resultado;
    }

    /** Saldo dividido pela média diária de saída dos últimos 30 dias; só quem teve saída entra (RN-49). */
    private List<ItemCobertura> cobertura(Integer usuarioCodigo, LocalDate hoje, LocalDateTime fim) {
        LocalDateTime inicio = hoje.minusDays(JANELA_COBERTURA_DIAS - 1L).atStartOfDay();
        List<ItemCobertura> itens = new ArrayList<>();
        for (SaidaComSaldo linha : movimentacaoRepository.saidasComSaldo(usuarioCodigo, inicio, fim)) {
            BigDecimal media = BigDecimal.valueOf(linha.saidas())
                    .divide(BigDecimal.valueOf(JANELA_COBERTURA_DIAS), 4, RoundingMode.HALF_UP);
            if (media.signum() == 0) {
                continue;
            }
            BigDecimal dias = BigDecimal.valueOf(linha.saldo()).divide(media, 1, RoundingMode.HALF_UP);
            itens.add(new ItemCobertura(linha.produtoId(), linha.nome(), linha.saldo(),
                    media.setScale(2, RoundingMode.HALF_UP), dias, dias.compareTo(LIMITE_ALERTA_DIAS) < 0));
        }
        itens.sort(Comparator.comparing(ItemCobertura::dias).thenComparing(ItemCobertura::nome));
        return itens.stream().limit(MAX_COBERTURA).toList();
    }

    private List<ProdutoParado> parados(Integer usuarioCodigo, LocalDateTime agora) {
        List<ProdutoParado> lista = produtoRepository.listarParados(usuarioCodigo, agora.minusDays(DIAS_PARADO));
        if (lista.isEmpty()) {
            return lista;
        }
        List<Integer> semSaida = lista.stream().filter(p -> p.ultimaSaida() == null)
                .map(ProdutoParado::produtoId).toList();
        Map<Integer, LocalDateTime> primeira = new HashMap<>();
        if (!semSaida.isEmpty()) {
            for (Object[] linha : movimentacaoRepository.primeiraMovimentacao(usuarioCodigo, semSaida)) {
                primeira.put((Integer) linha[0], (LocalDateTime) linha[1]);
            }
        }
        LocalDate hoje = agora.toLocalDate();
        return lista.stream().map(p -> {
            LocalDateTime desde = p.ultimaSaida() != null ? p.ultimaSaida() : primeira.get(p.produtoId());
            return desde == null ? p : p.comDias(Math.max(0, ChronoUnit.DAYS.between(desde.toLocalDate(), hoje)));
        }).collect(Collectors.toList());
    }

    /**
     * Curva ABC por valor de saída: A cobre os produtos que somam até 80 % do valor, B os que levam a 95 %
     * e C o restante. O produto que cruza o limite fica na classe de cima. Produtos sem valor (custo zero) ficam de fora.
     */
    static List<ItemCurvaAbc> curvaAbc(List<SaidaProduto> saidas) {
        List<SaidaProduto> ordenados = saidas.stream()
                .filter(s -> s.valor() != null && s.valor().signum() > 0)
                .sorted(Comparator.comparing(SaidaProduto::valor).reversed().thenComparing(SaidaProduto::nome))
                .toList();
        BigDecimal total = ordenados.stream().map(SaidaProduto::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            return List.of();
        }
        List<ItemCurvaAbc> itens = new ArrayList<>(ordenados.size());
        BigDecimal acumulado = BigDecimal.ZERO;
        for (SaidaProduto s : ordenados) {
            BigDecimal antes = acumulado;
            acumulado = acumulado.add(s.valor());
            BigDecimal percentual = s.valor().multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
            BigDecimal pctAntes = antes.multiply(CEM).divide(total, 4, RoundingMode.HALF_UP);
            String classe = pctAntes.compareTo(BigDecimal.valueOf(80)) < 0 ? "A"
                    : pctAntes.compareTo(BigDecimal.valueOf(95)) < 0 ? "B" : "C";
            itens.add(new ItemCurvaAbc(s.produtoId(), s.nome(), dinheiro(s.valor()), percentual,
                    acumulado.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP), classe));
        }
        return itens;
    }

    private static BigDecimal dinheiro(Object valor) {
        BigDecimal v = valor instanceof BigDecimal b ? b : new BigDecimal(valor.toString());
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate paraData(Object valor) {
        if (valor instanceof LocalDate data) {
            return data;
        }
        if (valor instanceof java.sql.Date data) {
            return data.toLocalDate();
        }
        return LocalDate.parse(valor.toString().substring(0, 10));
    }
}
