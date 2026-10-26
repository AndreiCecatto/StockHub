package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Filtros, busca, ordenação e paginação de produtos executados de verdade no PostgreSQL. */
class ProdutoConsultasIntegracaoTest extends IntegracaoBase {

    private Usuario loja;
    private Categoria papelaria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        papelaria = novaCategoria(loja, "Papelaria");
    }

    private ProdutoFiltro filtro(String busca, Integer categoriaId, NivelEstoque nivel, String ordenacao,
                                 String direcao, int pagina, int tamanho) {
        return new ProdutoFiltro(busca, categoriaId, nivel, ordenacao, direcao, pagina, tamanho);
    }

    private List<String> nomes(Page<Produto> pagina) {
        return pagina.getContent().stream().map(Produto::getNome).toList();
    }

    @Test
    @DisplayName("lista só os produtos ativos da própria loja (isolamento RN-01 e RN-08)")
    void listaSoAtivosDaPropriaLoja() {
        Usuario outra = novoUsuario();
        Categoria outraCategoria = novaCategoria(outra, "Outra");
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        Produto inativo = novoProduto(loja, papelaria, "Lápis antigo", 5, 1);
        inativo.setAtivo(false);
        produtoRepository.saveAndFlush(inativo);
        novoProduto(outra, outraCategoria, "Produto da outra loja", 5, 1);
        limparSessao();

        Page<Produto> pagina = produtoService.listarPagina(ProdutoFiltro.de(null, null, null, "nome", "asc", 1, 10),
                loja.getCodigo());

        assertThat(nomes(pagina)).containsExactly("Caneta");
        assertThat(pagina.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("busca por nome ignora maiúsculas e minúsculas")
    void buscaPorNomeIgnoraCaixa() {
        novoProduto(loja, papelaria, "Caneta Azul", 5, 1);
        novoProduto(loja, papelaria, "Caderno", 5, 1);
        limparSessao();

        assertThat(nomes(produtoService.listarPagina(ProdutoFiltro.de("CANETA", null, null, "nome", "asc", 1, 10),
                loja.getCodigo()))).containsExactly("Caneta Azul");
        assertThat(nomes(produtoService.listarPagina(ProdutoFiltro.de("  ca  ", null, null, "nome", "asc", 1, 10),
                loja.getCodigo()))).containsExactly("Caderno", "Caneta Azul");
    }

    @Test
    @DisplayName("busca também encontra pelo nome da categoria")
    void buscaPorNomeDaCategoria() {
        Categoria informatica = novaCategoria(loja, "Informática");
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        novoProduto(loja, informatica, "Mouse", 5, 1);
        limparSessao();

        assertThat(nomes(produtoService.listarPagina(ProdutoFiltro.de("inform", null, null, "nome", "asc", 1, 10),
                loja.getCodigo()))).containsExactly("Mouse");
    }

    @Test
    @DisplayName("busca por número encontra o produto pelo código exato")
    void buscaPorCodigoExato() {
        Produto alvo = novoProduto(loja, papelaria, "Caneta", 5, 1);
        novoProduto(loja, papelaria, "Lápis", 5, 1);
        limparSessao();

        Page<Produto> pagina = produtoService.listarPagina(
                ProdutoFiltro.de(String.valueOf(alvo.getId()), null, null, "nome", "asc", 1, 10), loja.getCodigo());

        assertThat(nomes(pagina)).containsExactly("Caneta");
    }

    @Test
    @DisplayName("busca por código de outra loja não vaza o produto")
    void buscaPorCodigoDeOutraLojaNaoVaza() {
        Usuario outra = novoUsuario();
        Produto alheio = novoProduto(outra, novaCategoria(outra, "X"), "Segredo", 5, 1);
        limparSessao();

        assertThat(produtoService.listarPagina(
                ProdutoFiltro.de(String.valueOf(alheio.getId()), null, null, "nome", "asc", 1, 10),
                loja.getCodigo()).getContent()).isEmpty();
    }

    @Test
    @DisplayName("curingas % e _ digitados na busca valem como texto literal, não como curinga do LIKE")
    void curingasDoLikeSaoLiterais() {
        novoProduto(loja, papelaria, "Desconto 50%", 5, 1);
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        novoProduto(loja, papelaria, "a_b", 5, 1);
        novoProduto(loja, papelaria, "axb", 5, 1);
        novoProduto(loja, papelaria, "oi!", 5, 1);
        novoProduto(loja, papelaria, "c:\\temp", 5, 1);
        limparSessao();

        assertThat(buscar("%")).containsExactly("Desconto 50%");
        assertThat(buscar("50%")).containsExactly("Desconto 50%");
        assertThat(buscar("_")).containsExactly("a_b");
        assertThat(buscar("a_b")).containsExactly("a_b");
        assertThat(buscar("!")).containsExactly("oi!");
        assertThat(buscar("\\")).containsExactly("c:\\temp");
    }

    @Test
    @DisplayName("busca com aspas e SQL malicioso não quebra e não devolve nada indevido")
    void buscaComSqlMaliciosoNaoQuebra() {
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        limparSessao();

        assertThat(buscar("' OR '1'='1")).isEmpty();
        assertThat(buscar("'; DROP TABLE produto; --")).isEmpty();
        // a tabela continua lá
        assertThat(buscar("caneta")).containsExactly("Caneta");
    }

    @Test
    @DisplayName("busca numérica enorme (maior que o int) e busca gigante não estouram")
    void buscaNumericaEnormeNaoEstoura() {
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        limparSessao();

        assertThat(buscar("99999999999999999999")).isEmpty();
        assertThat(buscar("x".repeat(5000))).isEmpty();
    }

    private List<String> buscar(String texto) {
        return nomes(produtoService.listarPagina(ProdutoFiltro.de(texto, null, null, "nome", "asc", 1, 50),
                loja.getCodigo()));
    }

    @Test
    @DisplayName("filtro por categoria devolve só os produtos dela")
    void filtroPorCategoria() {
        Categoria informatica = novaCategoria(loja, "Informática");
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        novoProduto(loja, informatica, "Mouse", 5, 1);
        limparSessao();

        Page<Produto> pagina = produtoService.listarPagina(
                ProdutoFiltro.de(null, informatica.getId(), null, "nome", "asc", 1, 10), loja.getCodigo());

        assertThat(nomes(pagina)).containsExactly("Mouse");
    }

    @Test
    @DisplayName("filtro por categoria de outra loja não devolve nada")
    void filtroPorCategoriaDeOutraLoja() {
        Usuario outra = novoUsuario();
        Categoria alheia = novaCategoria(outra, "Alheia");
        novoProduto(outra, alheia, "Segredo", 5, 1);
        novoProduto(loja, papelaria, "Caneta", 5, 1);
        limparSessao();

        assertThat(produtoService.listarPagina(ProdutoFiltro.de(null, alheia.getId(), null, "nome", "asc", 1, 10),
                loja.getCodigo()).getContent()).isEmpty();
    }

    // ----- Níveis de estoque (RN-12) -----

    private List<Produto> produtosDeTodosOsNiveis() {
        List<Produto> produtos = new ArrayList<>();
        produtos.add(novoProduto(loja, papelaria, "zero-min0", 0, 0));
        produtos.add(novoProduto(loja, papelaria, "zero-min5", 0, 5));
        produtos.add(novoProduto(loja, papelaria, "um-min0", 1, 0));
        produtos.add(novoProduto(loja, papelaria, "um-min1", 1, 1));
        produtos.add(novoProduto(loja, papelaria, "cinco-min5", 5, 5));
        produtos.add(novoProduto(loja, papelaria, "seis-min5", 6, 5));
        produtos.add(novoProduto(loja, papelaria, "cem-min5", 100, 5));
        Produto inativo = novoProduto(loja, papelaria, "inativo-zero", 0, 0);
        inativo.setAtivo(false);
        produtoRepository.saveAndFlush(inativo);
        limparSessao();
        return produtos;
    }

    @ParameterizedTest(name = "filtro {0} no banco bate com NivelEstoque.calcular em memória")
    @EnumSource(NivelEstoque.class)
    @DisplayName("o filtro de nível no banco dá o mesmo resultado que o cálculo em memória (definição única)")
    void filtroDeNivelBateComCalculoEmMemoria(NivelEstoque nivel) {
        List<Produto> produtos = produtosDeTodosOsNiveis();

        Page<Produto> pagina = produtoService.listarPagina(
                filtro(null, null, nivel, "nome", "asc", 1, 100), loja.getCodigo());

        List<String> esperados = produtos.stream()
                .filter(p -> NivelEstoque.calcular(p.getQuantidade(), p.getEstoqueMinimo()) == nivel)
                .map(Produto::getNome).sorted().toList();
        assertThat(nomes(pagina)).containsExactlyElementsOf(esperados);
    }

    @Test
    @DisplayName("níveis nos limites: sem estoque (0), baixo (mínimo) e normal (mínimo+1)")
    void niveisNosLimites() {
        produtosDeTodosOsNiveis();

        assertThat(porNivel(NivelEstoque.SEM_ESTOQUE)).containsExactly("zero-min0", "zero-min5");
        assertThat(porNivel(NivelEstoque.BAIXO)).containsExactly("cinco-min5", "um-min1");
        assertThat(porNivel(NivelEstoque.NORMAL)).containsExactly("cem-min5", "seis-min5", "um-min0");
    }

    private List<String> porNivel(NivelEstoque nivel) {
        return nomes(produtoService.listarPagina(filtro(null, null, nivel, "nome", "asc", 1, 100), loja.getCodigo()));
    }

    @Test
    @DisplayName("alerta de estoque baixo traz SEM_ESTOQUE e BAIXO, do menor saldo para o maior, e exclui NORMAL e inativos")
    void estoqueBaixoOrdenadoPeloMenorSaldo() {
        novoProduto(loja, papelaria, "Zeta", 2, 5);
        novoProduto(loja, papelaria, "B", 0, 3);
        novoProduto(loja, papelaria, "a", 0, 3);
        novoProduto(loja, papelaria, "Normal", 10, 5);
        limparSessao();

        Page<Produto> pagina = produtoService.listarEstoqueBaixo(loja.getCodigo(), 1, 10);

        assertThat(nomes(pagina)).containsExactly("a", "B", "Zeta");
    }

    // ----- Ordenação -----

    @Test
    @DisplayName("ordena por nome sem diferenciar maiúsculas e minúsculas")
    void ordenaPorNomeIgnorandoCaixa() {
        novoProduto(loja, papelaria, "banana", 1, 0);
        novoProduto(loja, papelaria, "Abacaxi", 1, 0);
        novoProduto(loja, papelaria, "cereja", 1, 0);
        limparSessao();

        assertThat(nomes(produtoService.listarPagina(filtro(null, null, null, "nome", "asc", 1, 10), loja.getCodigo())))
                .containsExactly("Abacaxi", "banana", "cereja");
        assertThat(nomes(produtoService.listarPagina(filtro(null, null, null, "nome", "desc", 1, 10), loja.getCodigo())))
                .containsExactly("cereja", "banana", "Abacaxi");
    }

    @Test
    @DisplayName("ordena por preço e por quantidade")
    void ordenaPorPrecoEQuantidade() {
        novoProduto(loja, papelaria, "barato", 30, 0, new BigDecimal("5.00"));
        novoProduto(loja, papelaria, "caro", 10, 0, new BigDecimal("20.00"));
        novoProduto(loja, papelaria, "medio", 20, 0, new BigDecimal("10.00"));
        limparSessao();

        assertThat(nomes(produtoService.listarPagina(filtro(null, null, null, "preco", "desc", 1, 10), loja.getCodigo())))
                .containsExactly("caro", "medio", "barato");
        assertThat(nomes(produtoService.listarPagina(filtro(null, null, null, "quantidade", "asc", 1, 10), loja.getCodigo())))
                .containsExactly("caro", "medio", "barato");
    }

    @Test
    @DisplayName("ordena pelo nome da categoria sem diferenciar maiúsculas e minúsculas")
    void ordenaPorCategoria() {
        novoProduto(loja, novaCategoria(loja, "Zeta"), "p-zeta", 1, 0);
        novoProduto(loja, novaCategoria(loja, "alfa"), "p-alfa", 1, 0);
        novoProduto(loja, novaCategoria(loja, "Beta"), "p-beta", 1, 0);
        limparSessao();

        assertThat(nomes(produtoService.listarPagina(filtro(null, null, null, "categoria", "asc", 1, 10), loja.getCodigo())))
                .containsExactly("p-alfa", "p-beta", "p-zeta");
    }

    @Test
    @DisplayName("ordenação padrão por id decrescente mostra o mais novo primeiro")
    void ordenacaoPadraoMaisNovoPrimeiro() {
        novoProduto(loja, papelaria, "primeiro", 1, 0);
        novoProduto(loja, papelaria, "segundo", 1, 0);
        novoProduto(loja, papelaria, "terceiro", 1, 0);
        limparSessao();

        assertThat(nomes(produtoService.listarPagina(ProdutoFiltro.de(null, null, null, null, null, 1, 10), loja.getCodigo())))
                .containsExactly("terceiro", "segundo", "primeiro");
    }

    // ----- Paginação -----

    @Test
    @DisplayName("pagina com total correto, sem repetir nem perder itens, mesmo com valores empatados")
    void paginacaoEstavelComEmpates() {
        for (int i = 1; i <= 5; i++) {
            novoProduto(loja, papelaria, "item-" + i, 3, 0); // todos com a mesma quantidade
        }
        limparSessao();

        List<String> vistos = new ArrayList<>();
        for (int pagina = 1; pagina <= 3; pagina++) {
            Page<Produto> resultado = produtoService.listarPagina(
                    filtro(null, null, null, "quantidade", "asc", pagina, 2), loja.getCodigo());
            assertThat(resultado.getTotalElements()).isEqualTo(5);
            assertThat(resultado.getTotalPages()).isEqualTo(3);
            vistos.addAll(nomes(resultado));
        }

        assertThat(vistos).containsExactly("item-1", "item-2", "item-3", "item-4", "item-5");
    }

    @Test
    @DisplayName("página além da última devolve a última página")
    void paginaAlemDaUltimaDevolveAUltima() {
        for (int i = 1; i <= 5; i++) {
            novoProduto(loja, papelaria, "item-" + i, 1, 0);
        }
        limparSessao();

        Page<Produto> resultado = produtoService.listarPagina(filtro(null, null, null, "id", "asc", 99, 2), loja.getCodigo());

        assertThat(resultado.getNumber()).isEqualTo(2);
        assertThat(nomes(resultado)).containsExactly("item-5");
    }

    @Test
    @DisplayName("loja sem produtos devolve página vazia com zero total")
    void lojaSemProdutos() {
        Page<Produto> resultado = produtoService.listarPagina(ProdutoFiltro.de(null, null, null, "nome", "asc", 1, 10),
                loja.getCodigo());

        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("a listagem traz a categoria junto (sem LazyInitializationException fora da sessão)")
    void listagemTrazACategoria() {
        novoProduto(loja, papelaria, "Caneta", 1, 0);
        limparSessao();

        Page<Produto> resultado = produtoService.listarPagina(ProdutoFiltro.de(null, null, null, "nome", "asc", 1, 10),
                loja.getCodigo());
        em.clear();
        // depois de esvaziar a sessão, a categoria carregada pela consulta ainda é acessível
        assertThat(resultado.getContent().get(0).getCategoria().getNome()).isEqualTo("Papelaria");
    }

    @Test
    @DisplayName("tamanho de página gigante é limitado a 100")
    void tamanhoDePaginaLimitado() {
        for (int i = 1; i <= 3; i++) {
            novoProduto(loja, papelaria, "item-" + i, 1, 0);
        }
        limparSessao();

        Page<Produto> resultado = produtoService.listarPagina(
                ProdutoFiltro.de(null, null, null, "id", "asc", 1, 1_000_000), loja.getCodigo());

        assertThat(resultado.getSize()).isEqualTo(100);
    }
}
