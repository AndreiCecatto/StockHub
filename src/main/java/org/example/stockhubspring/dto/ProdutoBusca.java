package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.Produto;

/** Item do resultado da busca rápida (GET /api/busca). */
public record ProdutoBusca(Integer id, String nome, String categoria, int saldo, int minimo,
                           NivelEstoque nivel, String url, boolean aproximado) {

    /** Resultado de nome parecido com o digitado (não contém o texto exato). */
    public static ProdutoBusca parecido(Produto produto) {
        ProdutoBusca exato = de(produto);
        return new ProdutoBusca(exato.id(), exato.nome(), exato.categoria(), exato.saldo(), exato.minimo(), exato.nivel(),
                exato.url(), true);
    }

    public static ProdutoBusca de(Produto produto) {
        return new ProdutoBusca(produto.getId(), produto.getNome(), produto.getCategoria().getNome(),
                produto.getQuantidade(), produto.getEstoqueMinimo(), produto.getNivelEstoque(),
                "/produtos/" + produto.getId(), false);
    }
}
