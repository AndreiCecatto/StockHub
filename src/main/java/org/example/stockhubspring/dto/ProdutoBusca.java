package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.service.CamposRamo.Permitidos;

import java.time.LocalDate;

/**
 * Item do resultado da busca rápida (GET /api/busca). Os campos por ramo (tamanho, cor, lote, validade,
 * numeroSerie) vêm preenchidos só quando o ramo da loja os usa (RN-100/RN-103); senão ficam null.
 */
public record ProdutoBusca(Integer id, String nome, String categoria, int saldo, int minimo,
                           NivelEstoque nivel, String url, boolean aproximado,
                           String tamanho, String cor, String lote, LocalDate validade, String numeroSerie) {

    /** Resultado de nome parecido com o digitado (não contém o texto exato). */
    public static ProdutoBusca parecido(Produto produto) {
        return parecido(produto, Permitidos.NENHUM);
    }

    public static ProdutoBusca parecido(Produto produto, Permitidos campos) {
        ProdutoBusca b = de(produto, campos);
        return new ProdutoBusca(b.id, b.nome, b.categoria, b.saldo, b.minimo, b.nivel, b.url, true,
                b.tamanho, b.cor, b.lote, b.validade, b.numeroSerie);
    }

    public static ProdutoBusca de(Produto produto) {
        return de(produto, Permitidos.NENHUM);
    }

    public static ProdutoBusca de(Produto produto, Permitidos campos) {
        return new ProdutoBusca(produto.getId(), produto.getNome(), produto.getCategoria().getNome(),
                produto.getQuantidade(), produto.getEstoqueMinimo(), produto.getNivelEstoque(),
                "/produtos/" + produto.getId(), false,
                campos.tamanho() ? produto.getTamanho() : null,
                campos.cor() ? produto.getCor() : null,
                campos.lote() ? produto.getLote() : null,
                campos.validade() ? produto.getDataValidade() : null,
                campos.numeroSerie() ? produto.getNumeroSerie() : null);
    }
}
