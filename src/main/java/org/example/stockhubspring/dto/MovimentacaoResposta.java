package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.model.TipoMovimentacao;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Resposta JSON de sucesso do POST /estoque/movimentar (Accept: application/json). */
public record MovimentacaoResposta(boolean ok, Integer movimentacaoId, Integer produtoId, String nome,
                                   TipoMovimentacao tipo, int quantidade, int saldoAnterior, int saldoNovo,
                                   NivelEstoque nivel, String mensagem, LocalDateTime criadoEm,
                                   String observacao) {

    public static MovimentacaoResposta de(MovimentacaoEstoque mov) {
        var produto = mov.getProduto();
        return new MovimentacaoResposta(true, mov.getId(), produto.getId(), produto.getNome(), mov.getTipo(),
                mov.getQuantidade(), mov.getQuantidadeAnterior(), mov.getQuantidadeNova(),
                NivelEstoque.calcular(mov.getQuantidadeNova(), produto.getEstoqueMinimo()), mensagem(mov),
                mov.getCriadoEm() == null ? null : mov.getCriadoEm().truncatedTo(ChronoUnit.SECONDS),
                mov.getObservacao());
    }

    /** Mesmo texto do flash da tela (fluxo sem JavaScript). */
    public static String mensagem(MovimentacaoEstoque mov) {
        return "Movimentação registrada: o saldo passou de " + mov.getQuantidadeAnterior()
                + " para " + mov.getQuantidadeNova() + " un.";
    }
}
