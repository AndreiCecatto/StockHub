package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.NivelEstoque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoFiltroTest {

    @Test
    @DisplayName("ordenação fora da lista branca cai em id (sem injeção por ORDER BY)")
    void ordenacaoForaDaListaBrancaViraId() {
        ProdutoFiltro filtro = ProdutoFiltro.de(null, null, null, "nome; drop table produto", "asc", 1, 8);
        assertThat(filtro.ordenacao()).isEqualTo("id");
        assertThat(filtro.propriedadeOrdenacao()).isEqualTo("id");
    }

    @Test
    @DisplayName("ordenar por categoria usa a propriedade categoria.nome")
    void ordenarPorCategoria() {
        ProdutoFiltro filtro = ProdutoFiltro.de(null, null, null, "categoria", "asc", 1, 8);
        assertThat(filtro.propriedadeOrdenacao()).isEqualTo("categoria.nome");
    }

    @Test
    @DisplayName("direção inválida vira desc, página mínima é 1 e tamanho é limitado a 100")
    void normalizaDirecaoPaginaETamanho() {
        ProdutoFiltro filtro = ProdutoFiltro.de("  ", -5, "ok", "nome", "sideways", -2, 100000);
        assertThat(filtro.direcao()).isEqualTo("desc");
        assertThat(filtro.pagina()).isEqualTo(1);
        assertThat(filtro.tamanho()).isEqualTo(ProdutoFiltro.TAMANHO_MAXIMO);
        assertThat(filtro.busca()).isNull();
        assertThat(filtro.categoriaId()).isNull();
        assertThat(filtro.nivel()).isEqualTo(NivelEstoque.NORMAL);
    }

    @Test
    @DisplayName("tamanho zero ou negativo usa o tamanho padrão")
    void tamanhoZeroUsaPadrao() {
        assertThat(ProdutoFiltro.de(null, null, null, "id", "asc", 1, 0).tamanho())
                .isEqualTo(ProdutoFiltro.TAMANHO_PADRAO);
    }

    @Test
    @DisplayName("busca é aparada e o código do nível volta para a tela")
    void buscaAparadaECodigoDoNivel() {
        ProdutoFiltro filtro = ProdutoFiltro.de("  caneta ", 3, "baixo", "preco", "ASC", 2, 20);
        assertThat(filtro.busca()).isEqualTo("caneta");
        assertThat(filtro.categoriaId()).isEqualTo(3);
        assertThat(filtro.nivelEstoqueParametro()).isEqualTo("baixo");
        assertThat(filtro.direcao()).isEqualTo("asc");
    }
}
