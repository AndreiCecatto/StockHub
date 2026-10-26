package org.example.stockhubspring.integration;

import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.ResumoEstoque;
import org.example.stockhubspring.model.Usuario;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Resumo do painel (RN-13) e listagem de categorias com total de produtos, no PostgreSQL. */
class DashboardCategoriasIntegracaoTest extends IntegracaoBase {

    private Usuario loja;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
    }

    private void inativar(Produto produto) {
        produto.setAtivo(false);
        produtoRepository.saveAndFlush(produto);
    }

    @Test
    @DisplayName("loja sem nada tem resumo zerado, sem NullPointerException (somas nulas viram zero)")
    void lojaVaziaTemResumoZerado() {
        ResumoEstoque resumo = dashboardService.buscarResumo(loja.getCodigo());

        assertThat(resumo.getTotalProdutos()).isZero();
        assertThat(resumo.getTotalCategorias()).isZero();
        assertThat(resumo.getQuantidadeTotalItens()).isZero();
        assertThat(resumo.getProdutosEstoqueBaixo()).isZero();
        assertThat(resumo.getProdutosSemEstoque()).isZero();
        assertThat(resumo.getValorTotalEstoque()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("totais somam só produtos ativos da própria loja; valor = soma de preço x quantidade, calculada no banco")
    void totaisSomamSoAtivosDaLoja() {
        Categoria categoria = novaCategoria(loja, "Geral");
        novaCategoria(loja, "Vazia");
        novoProduto(loja, categoria, "p1", 3, 1, new BigDecimal("10.50"));   // 31,50
        novoProduto(loja, categoria, "p2", 100, 1, new BigDecimal("0.99"));  // 99,00
        inativar(novoProduto(loja, categoria, "inativo", 5, 1, new BigDecimal("1000.00")));

        Usuario outra = novoUsuario();
        novoProduto(outra, novaCategoria(outra, "X"), "alheio", 7, 1, new BigDecimal("500.00"));
        limparSessao();

        ResumoEstoque resumo = dashboardService.buscarResumo(loja.getCodigo());

        assertThat(resumo.getTotalProdutos()).isEqualTo(2);
        assertThat(resumo.getQuantidadeTotalItens()).isEqualTo(103);
        assertThat(resumo.getValorTotalEstoque()).isEqualByComparingTo("130.50");
        assertThat(resumo.getTotalCategorias()).isEqualTo(2);
    }

    @Test
    @DisplayName("valor do estoque com centavos não perde precisão")
    void valorComCentavosSemPerderPrecisao() {
        Categoria categoria = novaCategoria(loja, "Geral");
        novoProduto(loja, categoria, "a", 3, 0, new BigDecimal("0.10"));
        novoProduto(loja, categoria, "b", 7, 0, new BigDecimal("0.07"));
        limparSessao();

        assertThat(dashboardService.buscarResumo(loja.getCodigo()).getValorTotalEstoque())
                .isEqualByComparingTo("0.79"); // 0,30 + 0,49
    }

    @Test
    @DisplayName("contadores de nível: sem estoque conta só saldo 0; 'estoque baixo' soma baixo + sem estoque")
    void contadoresDeNivel() {
        Categoria categoria = novaCategoria(loja, "Geral");
        novoProduto(loja, categoria, "zero-min0", 0, 0);   // SEM
        novoProduto(loja, categoria, "zero-min5", 0, 5);   // SEM
        novoProduto(loja, categoria, "um-min1", 1, 1);     // BAIXO
        novoProduto(loja, categoria, "cinco-min5", 5, 5);  // BAIXO
        novoProduto(loja, categoria, "seis-min5", 6, 5);   // NORMAL
        novoProduto(loja, categoria, "um-min0", 1, 0);     // NORMAL
        inativar(novoProduto(loja, categoria, "inativo", 0, 0));
        limparSessao();

        ResumoEstoque resumo = dashboardService.buscarResumo(loja.getCodigo());

        assertThat(resumo.getProdutosSemEstoque()).isEqualTo(2);
        assertThat(resumo.getProdutosEstoqueBaixo()).isEqualTo(4);
        assertThat(resumo.getTotalProdutos()).isEqualTo(6);
    }

    @Test
    @DisplayName("categorias da loja vêm com o total de produtos ATIVOS de cada uma, inclusive zero")
    void categoriasComTotalDeProdutosAtivos() {
        Categoria alfa = novaCategoria(loja, "Alfa");
        Categoria beta = novaCategoria(loja, "Beta");
        novoProduto(loja, alfa, "a1", 1, 0);
        novoProduto(loja, alfa, "a2", 1, 0);
        inativar(novoProduto(loja, alfa, "a3-inativo", 1, 0));

        Usuario outra = novoUsuario();
        Categoria alheia = novaCategoria(outra, "Alheia");
        novoProduto(outra, alheia, "x", 1, 0);
        limparSessao();

        List<Categoria> categorias = categoriaService.listar(null, loja.getCodigo());

        Map<String, Long> totais = categorias.stream()
                .collect(Collectors.toMap(Categoria::getNome, Categoria::getTotalProdutos));
        assertThat(totais).containsOnlyKeys("Alfa", "Beta").containsEntry("Alfa", 2L).containsEntry("Beta", 0L);
        assertThat(beta.getNome()).isEqualTo("Beta");
    }

    @Test
    @DisplayName("categorias saem em ordem alfabética sem diferenciar maiúsculas e minúsculas")
    void categoriasOrdenadasPorNome() {
        novaCategoria(loja, "gama");
        novaCategoria(loja, "Alfa");
        novaCategoria(loja, "beta");
        limparSessao();

        assertThat(categoriaService.listar(null, loja.getCodigo())).extracting(Categoria::getNome)
                .containsExactly("Alfa", "beta", "gama");
    }

    @Test
    @DisplayName("busca de categoria ignora caixa e trata % e _ como texto literal")
    void buscaDeCategoria() {
        novaCategoria(loja, "50% off");
        novaCategoria(loja, "Alfa");
        novaCategoria(loja, "a_b");
        novaCategoria(loja, "axb");
        limparSessao();

        assertThat(categoriaService.listar("ALF", loja.getCodigo())).extracting(Categoria::getNome).containsExactly("Alfa");
        assertThat(categoriaService.listar("%", loja.getCodigo())).extracting(Categoria::getNome).containsExactly("50% off");
        assertThat(categoriaService.listar("_", loja.getCodigo())).extracting(Categoria::getNome).containsExactly("a_b");
        assertThat(categoriaService.listar("' OR 1=1 --", loja.getCodigo())).isEmpty();
    }

    @Test
    @DisplayName("listar categorias com totais usa UMA única consulta SQL (sem N+1)")
    void listarCategoriasEmUmaConsulta() {
        for (int i = 1; i <= 5; i++) {
            Categoria categoria = novaCategoria(loja, "Categoria " + i);
            novoProduto(loja, categoria, "produto " + i, 1, 0);
        }
        limparSessao();

        Statistics estatisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean estavaLigada = estatisticas.isStatisticsEnabled();
        estatisticas.setStatisticsEnabled(true);
        try {
            long antes = estatisticas.getPrepareStatementCount();

            List<Categoria> categorias = categoriaService.listar(null, loja.getCodigo());
            categorias.forEach(Categoria::getTotalProdutos);

            assertThat(categorias).hasSize(5);
            assertThat(estatisticas.getPrepareStatementCount() - antes)
                    .as("consultas SQL preparadas para listar 5 categorias com totais").isEqualTo(1);
        } finally {
            estatisticas.setStatisticsEnabled(estavaLigada);
        }
    }

    @Test
    @DisplayName("o resumo do painel usa poucas consultas agregadas (não carrega os produtos em memória)")
    void resumoUsaPoucasConsultas() {
        Categoria categoria = novaCategoria(loja, "Geral");
        for (int i = 1; i <= 20; i++) {
            novoProduto(loja, categoria, "p" + i, i % 3, 1);
        }
        limparSessao();

        Statistics estatisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean estavaLigada = estatisticas.isStatisticsEnabled();
        estatisticas.setStatisticsEnabled(true);
        try {
            long antes = estatisticas.getPrepareStatementCount();
            long entidadesAntes = estatisticas.getEntityLoadCount();

            dashboardService.buscarResumo(loja.getCodigo());

            // resumir + countCategorias + 2 contagens de nível = 4, independente do número de produtos
            assertThat(estatisticas.getPrepareStatementCount() - antes).isLessThanOrEqualTo(4);
            assertThat(estatisticas.getEntityLoadCount() - entidadesAntes).isZero();
        } finally {
            estatisticas.setStatisticsEnabled(estavaLigada);
        }
    }
}
