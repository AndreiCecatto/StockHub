package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.TipoMovimentacao;

/** Resposta JSON de sucesso do POST /estoque/movimentar (Accept: application/json). */
public record MovimentacaoResposta(boolean ok, Integer movimentacaoId, Integer produtoId, String nome,
                                   TipoMovimentacao tipo, int quantidade, int saldoAnterior, int saldoNovo,
                                   NivelEstoque nivel, String mensagem) {

    public static MovimentacaoResposta de(MovimentacaoEstoque mov) {
        var produto = mov.getProduto();
        return new MovimentacaoResposta(true, mov.getId(), produto.getId(), produto.getNome(), mov.getTipo(),
                mov.getQuantidade(), mov.getQuantidadeAnterior(), mov.getQuantidadeNova(),
                NivelEstoque.calcular(mov.getQuantidadeNova(), produto.getEstoqueMinimo()), mensagem(mov));
    }

    /** Mesmo texto do flash da tela (fluxo sem JavaScript). */
    public static String mensagem(MovimentacaoEstoque mov) {
        return "Movimentação registrada: o saldo passou de " + mov.getQuantidadeAnterior()
                + " para " + mov.getQuantidadeNova() + " un.";
    }
}
