package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.NivelEstoque;

import java.util.Locale;
import java.util.Map;

/**
 * Filtros, ordenação e paginação da listagem de produtos, já normalizados:
 * a ordenação só aceita campos da lista branca, o tamanho de página é limitado e a página começa em 1.
 *
 * @param ordenacao nome do campo como a tela usa ("id", "nome", "preco", "quantidade", "categoria")
 * @param direcao   "asc" ou "desc"
 */
public record ProdutoFiltro(String busca, Integer categoriaId, NivelEstoque nivel,
                            String ordenacao, String direcao, int pagina, int tamanho) {

    public static final int TAMANHO_PADRAO = 8;
    public static final int TAMANHO_MAXIMO = 100;

    // Lista branca: campo da tela -> propriedade JPA. Qualquer outro valor cai em "id".
    private static final Map<String, String> PROPRIEDADES = Map.of(
            "id", "id",
            "nome", "nome",
            "preco", "preco",
            "quantidade", "quantidade",
            "categoria", "categoria.nome");

    public ProdutoFiltro {
        busca = (busca == null || busca.isBlank()) ? null : busca.trim();
        if (categoriaId != null && categoriaId <= 0) {
            categoriaId = null;
        }
        ordenacao = (ordenacao != null && PROPRIEDADES.containsKey(ordenacao)) ? ordenacao : "id";
        direcao = "asc".equals(direcao == null ? null : direcao.toLowerCase(Locale.ROOT)) ? "asc" : "desc";
        pagina = Math.max(1, pagina);
        tamanho = tamanho <= 0 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
    }

    /** Monta o filtro a partir dos parâmetros crus da requisição (nivelEstoque como texto da tela). */
    public static ProdutoFiltro de(String busca, Integer categoriaId, String nivelEstoque,
                                   String ordenacao, String direcao, int pagina, int tamanho) {
        return new ProdutoFiltro(busca, categoriaId, NivelEstoque.deParametro(nivelEstoque),
                ordenacao, direcao, pagina, tamanho);
    }

    /** Propriedade JPA correspondente à ordenação escolhida. */
    public String propriedadeOrdenacao() {
        return PROPRIEDADES.get(ordenacao);
    }

    /** Código do nível para devolver à tela (null se não há filtro de nível). */
    public String nivelEstoqueParametro() {
        return nivel == null ? null : nivel.getCodigo();
    }
}
