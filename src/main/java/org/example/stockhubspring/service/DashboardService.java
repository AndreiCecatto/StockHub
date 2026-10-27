package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.MovimentoDiario;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.ResumoEstoque;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.ProdutoSpecs;
import org.example.stockhubspring.repository.ResumoProdutos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Totais do painel, todos calculados no banco por consultas agregadas (RN-13), só de produtos ativos. */
@Service
public class DashboardService {

    public static final int DIAS_PADRAO = 14;
    public static final int DIAS_MINIMO = 1;
    public static final int DIAS_MAXIMO = 60;
    /** Fuso que define o "dia" de uma movimentação (criado_em é gravado no horário de parede de São Paulo). */
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public DashboardService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository,
                            MovimentacaoEstoqueRepository movimentacaoRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @Transactional(readOnly = true)
    public ResumoEstoque buscarResumo(Integer usuarioCodigo) {
        ResumoProdutos totais = produtoRepository.resumir(usuarioCodigo);

        ResumoEstoque resumo = new ResumoEstoque();
        resumo.setTotalProdutos(valorOuZero(totais.getTotalProdutos()));
        resumo.setQuantidadeTotalItens(valorOuZero(totais.getTotalUnidades()));
        resumo.setValorTotalEstoque(totais.getValorTotal() == null ? BigDecimal.ZERO : totais.getValorTotal());
        resumo.setTotalCategorias(categoriaRepository.countByUsuarioCodigo(usuarioCodigo));

        // Níveis pela definição única de NivelEstoque (RN-12)
        long semEstoque = produtoRepository.count(ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and(ProdutoSpecs.comNivel(NivelEstoque.SEM_ESTOQUE)));
        long baixo = produtoRepository.count(ProdutoSpecs.ativosDoUsuario(usuarioCodigo)
                .and(ProdutoSpecs.comNivel(NivelEstoque.BAIXO)));
        resumo.setProdutosSemEstoque(semEstoque);
        resumo.setProdutosEstoqueBaixo(semEstoque + baixo);
        return resumo;
    }

    /**
     * Movimentos dos últimos {@code dias} dias (inclusive hoje, no fuso de São Paulo), um item por dia
     * do mais antigo ao mais recente, com zeros nos dias sem movimento. Soma UNIDADES de ENTRADA e SAIDA;
     * AJUSTE só conta em {@code ajustes}. Agregação numa única consulta no banco. {@code dias} fica entre
     * {@link #DIAS_MINIMO} e {@link #DIAS_MAXIMO}.
     */
    @Transactional(readOnly = true)
    public List<MovimentoDiario> movimentosPorDia(Integer usuarioCodigo, int dias) {
        return movimentosPorDia(usuarioCodigo, dias, LocalDate.now(FUSO));
    }

    /** Igual ao anterior, com o "hoje" informado (permite testar sem depender do relógio). */
    @Transactional(readOnly = true)
    public List<MovimentoDiario> movimentosPorDia(Integer usuarioCodigo, int dias, LocalDate hoje) {
        int total = Math.max(DIAS_MINIMO, Math.min(dias, DIAS_MAXIMO));
        LocalDate primeiro = hoje.minusDays(total - 1L);
        List<Object[]> linhas = movimentacaoRepository.somarPorDia(usuarioCodigo,
                primeiro.atStartOfDay(), hoje.plusDays(1).atStartOfDay());

        Map<LocalDate, long[]> porDia = new HashMap<>();
        for (Object[] linha : linhas) {
            porDia.put(paraData(linha[0]), new long[]{
                    ((Number) linha[1]).longValue(), ((Number) linha[2]).longValue(), ((Number) linha[3]).longValue()});
        }
        List<MovimentoDiario> resultado = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            LocalDate dia = primeiro.plusDays(i);
            long[] valores = porDia.getOrDefault(dia, new long[3]);
            resultado.add(new MovimentoDiario(dia.toString(), valores[0], valores[1], valores[2]));
        }
        return resultado;
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

    private static long valorOuZero(Long valor) {
        return valor == null ? 0L : valor;
    }
}
