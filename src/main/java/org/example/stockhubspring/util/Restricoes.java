package org.example.stockhubspring.util;

import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;

/** Identifica qual restrição do banco foi violada, sem expor detalhes ao usuário. */
public final class Restricoes {

    /** Índice único parcial de produto ativo por loja e nome (migration V2). */
    public static final String UX_PRODUTO_NOME_ATIVO = "ux_produto_usuario_nome_ativo";
    /** Índice único de categoria por loja e nome (migration V2). */
    public static final String UX_CATEGORIA_NOME = "ux_categoria_usuario_nome";

    /** Mensagem para violações que não são de nome duplicado (ex.: chave estrangeira). */
    public static final String MENSAGEM_GENERICA =
            "Não foi possível salvar: o registro relacionado foi alterado ou removido. Tente de novo.";

    private Restricoes() {
    }

    /**
     * A exceção (ou alguma causa dela) veio da restrição de nome dado? Confere o nome da constraint
     * informado pelo Hibernate e, como reserva, o texto da mensagem do driver (que cita o índice).
     */
    public static boolean violou(Throwable erro, String nomeRestricao) {
        String procurado = nomeRestricao.toLowerCase(Locale.ROOT);
        for (Throwable atual = erro; atual != null; atual = atual.getCause() == atual ? null : atual.getCause()) {
            if (atual instanceof ConstraintViolationException hibernate && hibernate.getConstraintName() != null
                    && hibernate.getConstraintName().toLowerCase(Locale.ROOT).contains(procurado)) {
                return true;
            }
            String mensagem = atual.getMessage();
            if (mensagem != null && mensagem.toLowerCase(Locale.ROOT).contains(procurado)) {
                return true;
            }
        }
        return false;
    }
}
