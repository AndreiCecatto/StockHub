package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.ExportacaoService;
import org.example.stockhubspring.service.ExportacaoService.ArquivoCsv;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Filtros do histórico (B2), histórico no detalhe (B3) e exportação CSV (B4) contra o PostgreSQL real,
 * com dados sintéticos e transação revertida.
 */
class HistoricoExportacaoIntegracaoTest extends IntegracaoBase {

    @Autowired
    private ExportacaoService exportacaoService;

    private Usuario loja;
    private Categoria categoria;
    private Produto caneta;
    private Produto lapis;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Papelaria");
        caneta = novoProduto(loja, categoria, "Caneta", 100, 5);
        lapis = novoProduto(loja, categoria, "Lápis", 100, 5);
    }

    /** Registra a movimentação e fixa a data dela (para testar os limites do período). */
    private MovimentacaoEstoque mover(Produto produto, TipoMovimentacao tipo, int quantidade, String motivo,
                                      LocalDateTime quando) {
        MovimentacaoEstoque mov = estoqueService.movimentar(produto.getId(), tipo, quantidade, motivo, loja);
        jdbc.update("update movimentacao_estoque set criado_em = ? where id = ?", Timestamp.valueOf(quando), mov.getId());
        limparSessao();
        return mov;
    }

    private static MovimentacaoFiltro filtro(Integer produtoId, TipoMovimentacao tipo, LocalDate de, LocalDate ate) {
        return new MovimentacaoFiltro(produtoId, tipo, de, ate);
    }

    private List<String> motivos(Page<MovimentacaoEstoque> pagina) {
        return pagina.getContent().stream().map(MovimentacaoEstoque::getObservacao).toList();
    }

    // ----- B2: filtros do histórico -----

    @Test
    @DisplayName("filtro por produto, por tipo e pelos dois juntos, recentes primeiro")
    void filtraPorProdutoETipo() {
        mover(caneta, TipoMovimentacao.ENTRADA, 5, "c-entrada", LocalDateTime.of(2026, 3, 1, 10, 0));
        mover(caneta, TipoMovimentacao.SAIDA, 2, "c-saida", LocalDateTime.of(2026, 3, 2, 10, 0));
        mover(lapis, TipoMovimentacao.SAIDA, 1, "l-saida", LocalDateTime.of(2026, 3, 3, 10, 0));

        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(), MovimentacaoFiltro.VAZIO, 1, 50)))
                .containsExactly("l-saida", "c-saida", "c-entrada");
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(caneta.getId(), null, null, null), 1, 50))).containsExactly("c-saida", "c-entrada");
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(null, TipoMovimentacao.SAIDA, null, null), 1, 50))).containsExactly("l-saida", "c-saida");
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(caneta.getId(), TipoMovimentacao.SAIDA, null, null), 1, 50))).containsExactly("c-saida");
        assertThat(estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(caneta.getId(), TipoMovimentacao.AJUSTE, null, null), 1, 50).getContent()).isEmpty();
    }

    @Test
    @DisplayName("período é inclusivo nos dois extremos: 'de' a partir das 00:00 e 'ate' até 23:59:59 do dia")
    void periodoInclusivo() {
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "antes", LocalDateTime.of(2026, 3, 9, 23, 59, 59));
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "inicio", LocalDateTime.of(2026, 3, 10, 0, 0, 0));
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "meio", LocalDateTime.of(2026, 3, 11, 12, 0, 0));
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "fim", LocalDateTime.of(2026, 3, 12, 23, 59, 59));
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "depois", LocalDateTime.of(2026, 3, 13, 0, 0, 0));

        LocalDate de = LocalDate.of(2026, 3, 10);
        LocalDate ate = LocalDate.of(2026, 3, 12);
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(), filtro(null, null, de, ate), 1, 50)))
                .containsExactly("fim", "meio", "inicio");
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(), filtro(null, null, de, null), 1, 50)))
                .containsExactly("depois", "fim", "meio", "inicio");
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(), filtro(null, null, null, ate), 1, 50)))
                .containsExactly("fim", "meio", "inicio", "antes");
        // um único dia: de = ate
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(null, null, ate, ate), 1, 50))).containsExactly("fim");
    }

    @Test
    @DisplayName("RN-01: o filtro nunca alcança movimentações de outra loja, nem filtrando pelo id do produto dela")
    void filtroEscopadoPelaLoja() {
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "minha", LocalDateTime.of(2026, 3, 1, 10, 0));

        Usuario outra = novoUsuario();
        Produto alheio = novoProduto(outra, novaCategoria(outra, "X"), "Alheio", 10, 1);
        estoqueService.movimentar(alheio.getId(), TipoMovimentacao.ENTRADA, 1, "alheia", outra);
        limparSessao();

        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(), MovimentacaoFiltro.VAZIO, 1, 50)))
                .containsExactly("minha");
        assertThat(estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(alheio.getId(), null, null, null), 1, 50).getContent()).isEmpty();
        assertThat(estoqueService.listarParaExportar(loja.getCodigo(),
                filtro(alheio.getId(), null, null, null)).getContent()).isEmpty();
    }

    @Test
    @DisplayName("paginação preservada com filtro: total, páginas e página além da última voltam à última")
    void paginacaoComFiltro() {
        for (int i = 1; i <= 5; i++) {
            mover(caneta, TipoMovimentacao.ENTRADA, 1, "e" + i, LocalDateTime.of(2026, 3, i, 10, 0));
        }
        mover(lapis, TipoMovimentacao.ENTRADA, 1, "outro", LocalDateTime.of(2026, 3, 6, 10, 0));

        MovimentacaoFiltro soCaneta = filtro(caneta.getId(), null, null, null);
        Page<MovimentacaoEstoque> pagina2 = estoqueService.listarMovimentacoes(loja.getCodigo(), soCaneta, 2, 2);
        assertThat(pagina2.getTotalElements()).isEqualTo(5);
        assertThat(pagina2.getTotalPages()).isEqualTo(3);
        assertThat(motivos(pagina2)).containsExactly("e3", "e2");
        assertThat(motivos(estoqueService.listarMovimentacoes(loja.getCodigo(), soCaneta, 99, 2)))
                .containsExactly("e1");
    }

    @Test
    @DisplayName("sem N+1: produto e categoria vêm na mesma consulta (no máximo select + count) e estão carregados")
    void semNMaisUm() {
        for (int i = 1; i <= 6; i++) {
            Produto p = novoProduto(loja, novaCategoria(loja, "Cat " + i), "Prod " + i, 5, 1);
            estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, "m" + i, loja);
        }
        limparSessao();
        Statistics estatisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        estatisticas.clear();

        Page<MovimentacaoEstoque> pagina = estoqueService.listarMovimentacoes(loja.getCodigo(),
                filtro(null, TipoMovimentacao.ENTRADA, null, null), 1, 3);
        em.clear(); // se algo estivesse LAZY, o acesso abaixo falharia
        List<String> nomes = pagina.getContent().stream()
                .map(m -> m.getProduto().getNome() + "/" + m.getProduto().getCategoria().getNome()).toList();

        assertThat(nomes).hasSize(3).allMatch(n -> n.contains("Prod ") && n.contains("Cat "));
        assertThat(estatisticas.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    // ----- B3: histórico no detalhe do produto -----

    @Test
    @DisplayName("últimas do produto: só dele, recentes primeiro, limitadas, com saldo anterior e novo")
    void ultimasDoProduto() {
        for (int i = 1; i <= 25; i++) {
            mover(caneta, TipoMovimentacao.ENTRADA, 1, "c" + i, LocalDateTime.of(2026, 3, 1, 8, 0).plusMinutes(i));
        }
        mover(lapis, TipoMovimentacao.ENTRADA, 1, "do-lapis", LocalDateTime.of(2026, 3, 1, 20, 0));

        List<MovimentacaoEstoque> ultimas = estoqueService.ultimasDoProduto(caneta.getId(), loja.getCodigo(), 20);
        em.clear();

        assertThat(ultimas).hasSize(20);
        assertThat(ultimas.get(0).getObservacao()).isEqualTo("c25");
        assertThat(ultimas.get(19).getObservacao()).isEqualTo("c6");
        assertThat(ultimas.get(0).getQuantidadeAnterior()).isEqualTo(124);
        assertThat(ultimas.get(0).getQuantidadeNova()).isEqualTo(125);
        assertThat(ultimas).allMatch(m -> m.getProduto().getId().equals(caneta.getId()));
        // produto e categoria já carregados
        assertThat(ultimas.get(0).getProduto().getCategoria().getNome()).isEqualTo("Papelaria");
    }

    @Test
    @DisplayName("últimas do produto: produto de outra loja não devolve nada (RN-01)")
    void ultimasDoProdutoDeOutraLoja() {
        mover(caneta, TipoMovimentacao.ENTRADA, 1, "minha", LocalDateTime.of(2026, 3, 1, 10, 0));
        Usuario outra = novoUsuario();
        assertThat(estoqueService.ultimasDoProduto(caneta.getId(), outra.getCodigo(), 20)).isEmpty();
    }

    // ----- B4: exportação -----

    @Test
    @DisplayName("exportação de produtos: sem paginação (mais de 100 linhas), só ativos da loja, mesmos filtros e ordem")
    void exportaProdutosSemPaginacao() {
        for (int i = 1; i <= 110; i++) {
            novoProduto(loja, categoria, String.format("Item %03d", i), 1, 0);
        }
        Produto inativo = novoProduto(loja, categoria, "Inativo", 1, 0);
        inativo.setAtivo(false);
        produtoRepository.saveAndFlush(inativo);
        Usuario outra = novoUsuario();
        novoProduto(outra, novaCategoria(outra, "X"), "Alheio", 1, 0);
        limparSessao();

        ProdutoFiltro todos = new ProdutoFiltro(null, null, null, "nome", "asc", 1, 8);
        Page<Produto> exportados = produtoService.listarParaExportar(todos, loja.getCodigo());
        em.clear();

        // 2 produtos do @BeforeEach + 110; o tamanho de página (8) é ignorado
        assertThat(exportados.getContent()).hasSize(112);
        assertThat(exportados.getContent()).extracting(Produto::getNome)
                .doesNotContain("Inativo", "Alheio").first().isEqualTo("Caneta");
        assertThat(exportados.getContent().get(0).getCategoria().getNome()).isEqualTo("Papelaria");

        ProdutoFiltro soItens = new ProdutoFiltro("item 05", null, null, "nome", "desc", 1, 8);
        assertThat(produtoService.listarParaExportar(soItens, loja.getCodigo()).getContent())
                .extracting(Produto::getNome).containsExactly("Item 059", "Item 058", "Item 057", "Item 056",
                        "Item 055", "Item 054", "Item 053", "Item 052", "Item 051", "Item 050");
    }

    @Test
    @DisplayName("CSV de produtos do banco: nome com fórmula e aspas sai protegido e escapado")
    void csvDeProdutosProtegido() {
        novoProduto(loja, categoria, "=CMD|' /C calc'!A0", 3, 1);
        novoProduto(loja, categoria, "Fita \"larga\"; azul", 3, 1);
        limparSessao();

        ArquivoCsv arquivo = exportacaoService.exportarProdutos(
                new ProdutoFiltro("a", null, null, "id", "asc", 1, 8), loja.getCodigo());
        String texto = new String(arquivo.conteudo(), 3, arquivo.conteudo().length - 3, StandardCharsets.UTF_8);

        assertThat(arquivo.conteudo()).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(arquivo.nome()).matches("produtos-\\d{4}-\\d{2}-\\d{2}\\.csv");
        assertThat(arquivo.truncado()).isFalse();
        assertThat(texto).startsWith("Código;Nome;Descrição;Categoria;");
        assertThat(texto).contains(";'=CMD|' /C calc'!A0;");
        assertThat(texto).contains(";\"Fita \"\"larga\"\"; azul\";");
    }

    @Test
    @DisplayName("CSV só dos marcados: filtra pelos ids e ignora id de outra loja ou produto inativo")
    void csvSoDosMarcados() {
        Produto a = novoProduto(loja, categoria, "Alfa", 3, 1);
        Produto b = novoProduto(loja, categoria, "Beta", 3, 1);
        Produto inativo = novoProduto(loja, categoria, "Inativo", 1, 0);
        inativo.setAtivo(false);
        produtoRepository.saveAndFlush(inativo);
        Usuario outra = novoUsuario();
        Produto alheio = novoProduto(outra, novaCategoria(outra, "X"), "Alheio", 1, 0);
        limparSessao();

        ProdutoFiltro todos = new ProdutoFiltro(null, null, null, "nome", "asc", 1, 8);
        Page<Produto> marcados = produtoService.listarParaExportar(todos, loja.getCodigo(),
                List.of(a.getId(), inativo.getId(), alheio.getId()));
        em.clear();

        assertThat(marcados.getContent()).extracting(Produto::getNome).containsExactly("Alfa");
        assertThat(produtoService.listarParaExportar(todos, loja.getCodigo(), List.of(b.getId())).getContent())
                .extracting(Produto::getNome).containsExactly("Beta");
        // lista vazia = filtro inteiro, como antes
        assertThat(produtoService.listarParaExportar(todos, loja.getCodigo(), List.of()).getContent())
                .extracting(Produto::getNome).contains("Alfa", "Beta", "Caneta");
    }

    @Test
    @DisplayName("CSV do histórico do banco: filtros aplicados, só da loja, com saldo anterior e novo")
    void csvDoHistorico() {
        mover(caneta, TipoMovimentacao.SAIDA, 4, "=SOMA(1;2)", LocalDateTime.of(2026, 3, 2, 9, 30, 15));
        mover(lapis, TipoMovimentacao.ENTRADA, 1, "outro produto", LocalDateTime.of(2026, 3, 2, 10, 0));
        Usuario outra = novoUsuario();
        Produto alheio = novoProduto(outra, novaCategoria(outra, "X"), "Alheio", 10, 1);
        estoqueService.movimentar(alheio.getId(), TipoMovimentacao.SAIDA, 1, "alheia", outra);
        limparSessao();

        ArquivoCsv arquivo = exportacaoService.exportarMovimentacoes(
                filtro(null, TipoMovimentacao.SAIDA, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2)),
                loja.getCodigo());
        String texto = new String(arquivo.conteudo(), 3, arquivo.conteudo().length - 3, StandardCharsets.UTF_8);

        assertThat(arquivo.nome()).matches("movimentacoes-\\d{4}-\\d{2}-\\d{2}\\.csv");
        assertThat(texto.split("\r\n")).containsExactly(
                "Data e hora;Código do produto;Produto;Categoria;Tipo;Quantidade;Saldo anterior;Saldo novo;Observação",
                "02/03/2026 09:30:15;" + caneta.getId() + ";Caneta;Papelaria;Saída;4;100;96;\"'=SOMA(1;2)\"");
    }

    @Test
    @DisplayName("exportação do histórico não é paginada: passa de 100 linhas")
    void exportaHistoricoSemPaginacao() {
        for (int i = 0; i < 120; i++) {
            estoqueService.movimentar(caneta.getId(), TipoMovimentacao.ENTRADA, 1, "m" + i, loja);
        }
        limparSessao();

        Page<MovimentacaoEstoque> exportadas = estoqueService.listarParaExportar(loja.getCodigo(), MovimentacaoFiltro.VAZIO);
        assertThat(exportadas.getContent()).hasSize(120);
        assertThat(exportadas.getTotalElements()).isEqualTo(120);
    }

    // ----- B7: seletor de produtos -----

    @Test
    @DisplayName("listarParaSelecao traz a categoria junto (sem LazyInitializationException na tela do seletor)")
    void selecaoComCategoria() {
        em.clear();
        List<Produto> produtos = produtoService.listarParaSelecao(loja.getCodigo());
        em.clear();
        assertThat(produtos).extracting(p -> p.getCategoria().getNome()).containsOnly("Papelaria");
    }
}
