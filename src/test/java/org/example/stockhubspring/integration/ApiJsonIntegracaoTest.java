package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.MovimentacaoResposta;
import org.example.stockhubspring.dto.MovimentoDiario;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.DashboardService;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Busca rápida, movimentos por dia e movimentação ponta a ponta no PostgreSQL (transação revertida). */
class ApiJsonIntegracaoTest extends IntegracaoBase {

    private Usuario loja;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
    }

    private List<String> nomesDaBusca(String termo, int limite) {
        limparSessao();
        return produtoService.buscarRapido(termo, limite, loja.getCodigo()).stream().map(Produto::getNome).toList();
    }

    private void marcarCriadoEm(MovimentacaoEstoque mov, LocalDateTime quando) {
        limparSessao();
        jdbc.update("update movimentacao_estoque set criado_em = ? where id = ?", Timestamp.valueOf(quando), mov.getId());
    }

    // ----- busca -----

    @Test
    @DisplayName("busca ignora caixa e acento nos dois lados e ordena por nome")
    void buscaIgnoraCaixaEAcento() {
        novoProduto(loja, categoria, "Café Torrado", 5, 1);
        novoProduto(loja, categoria, "CAFEZINHO", 5, 1);
        novoProduto(loja, categoria, "Chá Mate", 5, 1);
        novoProduto(loja, categoria, "Ação Épica", 5, 1);

        assertThat(nomesDaBusca("cafe", 8)).containsExactly("Café Torrado", "CAFEZINHO");
        assertThat(nomesDaBusca("CAFÉ", 8)).containsExactly("Café Torrado", "CAFEZINHO");
        assertThat(nomesDaBusca("acao epica", 8)).containsExactly("Ação Épica");
        assertThat(nomesDaBusca("ÇHA", 8)).containsExactly("Chá Mate");
        assertThat(nomesDaBusca("XHA", 8)).isEmpty();
        assertThat(nomesDaBusca("cha", 8)).containsExactly("Chá Mate");
    }

    @Test
    @DisplayName("% e _ digitados são literais, não curingas")
    void buscaCuringasLiterais() {
        novoProduto(loja, categoria, "Desconto 100% algodão", 5, 1);
        novoProduto(loja, categoria, "Desconto 1000 geral", 5, 1);
        novoProduto(loja, categoria, "Cabo a_b longo", 5, 1);
        novoProduto(loja, categoria, "Cabo axb curto", 5, 1);

        assertThat(nomesDaBusca("100%", 8)).containsExactly("Desconto 100% algodão");
        assertThat(nomesDaBusca("a_b", 8)).containsExactly("Cabo a_b longo");
        assertThat(nomesDaBusca("%%", 8)).isEmpty();
    }

    @Test
    @DisplayName("busca traz só produtos ATIVOS da própria loja")
    void buscaSoAtivosDaLoja() {
        novoProduto(loja, categoria, "Caneta ativa", 5, 1);
        Produto inativo = novoProduto(loja, categoria, "Caneta inativa", 5, 1);
        inativo.setAtivo(false);
        produtoRepository.saveAndFlush(inativo);
        Usuario outra = novoUsuario();
        novoProduto(outra, novaCategoria(outra, "X"), "Caneta alheia", 5, 1);

        assertThat(nomesDaBusca("caneta", 8)).containsExactly("Caneta ativa");
    }

    @Test
    @DisplayName("busca: termo com menos de 2 caracteres (após trim) devolve vazio; limite entre 1 e 20")
    void buscaTermoMinimoELimite() {
        for (int i = 10; i < 35; i++) {
            novoProduto(loja, categoria, "Item " + i, 5, 1);
        }
        assertThat(nomesDaBusca("i", 8)).isEmpty();
        assertThat(nomesDaBusca("  i  ", 8)).isEmpty();
        assertThat(nomesDaBusca(null, 8)).isEmpty();
        assertThat(nomesDaBusca("item", 8)).hasSize(8);
        assertThat(nomesDaBusca("item", 1000)).hasSize(20);
        assertThat(nomesDaBusca("item", 0)).hasSize(1);
        assertThat(nomesDaBusca("item", 3)).containsExactly("Item 10", "Item 11", "Item 12");
    }

    @Test
    @DisplayName("busca carrega a categoria junto: uma consulta, sem N+1")
    void buscaSemNMaisUm() {
        for (int i = 0; i < 6; i++) {
            novoProduto(loja, novaCategoria(loja, "Cat " + i), "Lapis " + i, 5, 1);
        }
        limparSessao();

        Statistics estatisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean estavaLigada = estatisticas.isStatisticsEnabled();
        estatisticas.setStatisticsEnabled(true);
        try {
            long antes = estatisticas.getPrepareStatementCount();
            List<Produto> achados = produtoService.buscarRapido("lapis", 8, loja.getCodigo());
            achados.forEach(p -> p.getCategoria().getNome());

            assertThat(achados).hasSize(6);
            assertThat(estatisticas.getPrepareStatementCount() - antes).isEqualTo(1);
        } finally {
            estatisticas.setStatisticsEnabled(estavaLigada);
        }
    }

    // ----- movimentos por dia -----

    @Test
    @DisplayName("movimentos: um item por dia (mais antigo primeiro), zeros nos dias vazios, unidades somadas por tipo")
    void movimentosPorDiaComZeros() {
        LocalDate hoje = LocalDate.of(2026, 10, 26);
        Produto p = novoProduto(loja, categoria, "Caneta", 0, 1);

        MovimentacaoEstoque e1 = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 10, null, loja);
        MovimentacaoEstoque e2 = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, null, loja);
        MovimentacaoEstoque s1 = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 4, null, loja);
        MovimentacaoEstoque aj = estoqueService.movimentar(p.getId(), TipoMovimentacao.AJUSTE, 50, "Contagem", loja);
        MovimentacaoEstoque s2 = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 7, null, loja);
        em.flush();
        marcarCriadoEm(e1, LocalDateTime.of(2026, 10, 24, 9, 0));
        marcarCriadoEm(e2, LocalDateTime.of(2026, 10, 24, 23, 59, 59));
        marcarCriadoEm(s1, LocalDateTime.of(2026, 10, 24, 12, 0));
        marcarCriadoEm(aj, LocalDateTime.of(2026, 10, 24, 15, 0));
        marcarCriadoEm(s2, LocalDateTime.of(2026, 10, 26, 0, 0));

        List<MovimentoDiario> dias = dashboardService.movimentosPorDia(loja.getCodigo(), 4, hoje);

        assertThat(dias).extracting(MovimentoDiario::dia)
                .containsExactly("2026-10-23", "2026-10-24", "2026-10-25", "2026-10-26");
        assertThat(dias.get(0)).isEqualTo(new MovimentoDiario("2026-10-23", 0, 0, 0));
        assertThat(dias.get(1)).isEqualTo(new MovimentoDiario("2026-10-24", 15, 4, 1));
        assertThat(dias.get(2)).isEqualTo(new MovimentoDiario("2026-10-25", 0, 0, 0));
        assertThat(dias.get(3)).isEqualTo(new MovimentoDiario("2026-10-26", 0, 7, 0));
    }

    @Test
    @DisplayName("movimentos: fora da janela (antes do primeiro dia ou amanhã) e de outra loja não entram")
    void movimentosForaDaJanelaEOutraLoja() {
        LocalDate hoje = LocalDate.of(2026, 10, 26);
        Produto p = novoProduto(loja, categoria, "Caneta", 100, 1);
        MovimentacaoEstoque antigo = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, null, loja);
        MovimentacaoEstoque amanha = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 2, null, loja);
        MovimentacaoEstoque dentro = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 3, null, loja);
        Usuario outra = novoUsuario();
        Produto alheio = novoProduto(outra, novaCategoria(outra, "Y"), "Alheio", 100, 1);
        MovimentacaoEstoque deOutra = estoqueService.movimentar(alheio.getId(), TipoMovimentacao.ENTRADA, 99, null, outra);
        em.flush();
        marcarCriadoEm(antigo, LocalDateTime.of(2026, 10, 24, 23, 59, 59)); // dias=2 começa em 25/10
        marcarCriadoEm(amanha, LocalDateTime.of(2026, 10, 27, 0, 0));
        marcarCriadoEm(dentro, LocalDateTime.of(2026, 10, 25, 0, 0));
        marcarCriadoEm(deOutra, LocalDateTime.of(2026, 10, 25, 10, 0));

        List<MovimentoDiario> dias = dashboardService.movimentosPorDia(loja.getCodigo(), 2, hoje);

        assertThat(dias).containsExactly(
                new MovimentoDiario("2026-10-25", 3, 0, 0), new MovimentoDiario("2026-10-26", 0, 0, 0));
    }

    @Test
    @DisplayName("movimentos: dias é ajustado a 1..60 (sem erro) e o dia de hoje segue o fuso de São Paulo")
    void movimentosLimitesEFuso() {
        Produto p = novoProduto(loja, categoria, "Caneta", 0, 1);
        estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 8, null, loja);
        em.flush();

        assertThat(dashboardService.movimentosPorDia(loja.getCodigo(), 0)).hasSize(DashboardService.DIAS_MINIMO);
        assertThat(dashboardService.movimentosPorDia(loja.getCodigo(), -5)).hasSize(1);
        assertThat(dashboardService.movimentosPorDia(loja.getCodigo(), 1000)).hasSize(DashboardService.DIAS_MAXIMO);

        List<MovimentoDiario> padrao = dashboardService.movimentosPorDia(loja.getCodigo(), 14);
        LocalDate hojeSp = LocalDate.now(DashboardService.FUSO);
        assertThat(padrao).hasSize(14);
        assertThat(padrao.get(13).dia()).isEqualTo(hojeSp.toString());
        assertThat(padrao.get(0).dia()).isEqualTo(hojeSp.minusDays(13).toString());
        // a movimentação recém-criada (criado_em gravado no horário de São Paulo) cai em "hoje"
        assertThat(padrao.get(13).entradas()).isEqualTo(8);
    }

    // ----- movimentação ponta a ponta (a mesma montagem que o endpoint JSON usa) -----

    @Test
    @DisplayName("fluxo JSON: resposta traz saldoAnterior/saldoNovo/nível corretos e o banco confere")
    void fluxoMovimentarPontaAPonta() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 5);

        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 6, "Venda", loja);
        MovimentacaoResposta resposta = MovimentacaoResposta.de(saida);

        assertThat(resposta.ok()).isTrue();
        assertThat(resposta.movimentacaoId()).isEqualTo(saida.getId());
        assertThat(resposta.produtoId()).isEqualTo(p.getId());
        assertThat(resposta.nome()).isEqualTo("Caneta");
        assertThat(resposta.tipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(resposta.quantidade()).isEqualTo(6);
        assertThat(resposta.saldoAnterior()).isEqualTo(10);
        assertThat(resposta.saldoNovo()).isEqualTo(4);
        assertThat(resposta.nivel()).isEqualTo(NivelEstoque.BAIXO);
        assertThat(resposta.mensagem()).contains("passou de 10 para 4");
        limparSessao();
        assertThat(saldoNoBanco(p.getId())).isEqualTo(4);

        MovimentacaoResposta zerado = MovimentacaoResposta.de(
                estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 4, null, loja));
        assertThat(zerado.saldoAnterior()).isEqualTo(4);
        assertThat(zerado.saldoNovo()).isZero();
        assertThat(zerado.nivel()).isEqualTo(NivelEstoque.SEM_ESTOQUE);
    }

    @Test
    @DisplayName("erro de regra (saída maior que o saldo, motivo reservado) não grava nada")
    void erroDeRegraNaoGrava() {
        Produto p = novoProduto(loja, categoria, "Caneta", 3, 1);
        limparSessao();

        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 9, null, loja))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("maior que o saldo atual (3)");
        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, "Estoque inicial", loja))
                .isInstanceOf(RegraNegocioException.class);

        limparSessao();
        assertThat(saldoNoBanco(p.getId())).isEqualTo(3);
        assertThat(contarMovimentacoesDoProduto(p.getId())).isZero();
    }
}
