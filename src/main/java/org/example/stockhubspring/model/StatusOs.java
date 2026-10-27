package org.example.stockhubspring.model;

import java.util.Locale;

/** Situação de uma ordem de serviço e para onde ela pode ir (RN-77). */
public enum StatusOs {
    ORCAMENTO("Orçamento", "Aguardando o cliente aprovar"),
    APROVADA("Aprovada", "Peças reservadas, pronta para começar"),
    EM_EXECUCAO("Em execução", "O serviço está sendo feito"),
    CONCLUIDA("Concluída", "Serviço pronto, aguardando retirada"),
    ENTREGUE("Entregue", "Entregue ao cliente"),
    CANCELADA("Cancelada", "Cancelada");

    private final String rotulo;
    private final String descricao;

    StatusOs(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }

    /** O passo seguinte do fluxo normal (null em ENTREGUE e CANCELADA). */
    public StatusOs proximo() {
        return switch (this) {
            case ORCAMENTO -> APROVADA;
            case APROVADA -> EM_EXECUCAO;
            case EM_EXECUCAO -> CONCLUIDA;
            case CONCLUIDA -> ENTREGUE;
            default -> null;
        };
    }

    /** Rótulo do botão que leva ao próximo passo. */
    public String getAcaoProximo() {
        return switch (this) {
            case ORCAMENTO -> "Aprovar orçamento";
            case APROVADA -> "Iniciar serviço";
            case EM_EXECUCAO -> "Concluir serviço";
            case CONCLUIDA -> "Registrar entrega";
            default -> null;
        };
    }

    /** Cancelável até a entrega; só dono e gerente cancelam depois de concluída (RN-82). */
    public boolean cancelavel() {
        return this != ENTREGUE && this != CANCELADA;
    }

    public boolean podeTrocarPecas() {
        return this == ORCAMENTO || this == APROVADA;
    }

    /** Serviços, desconto e dados ainda podem mudar (antes de concluir). */
    public boolean editavel() {
        return this == ORCAMENTO || this == APROVADA || this == EM_EXECUCAO;
    }

    public boolean transicaoValida(StatusOs destino) {
        if (destino == CANCELADA) {
            return cancelavel();
        }
        return destino != null && destino == proximo();
    }

    public static StatusOs de(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
