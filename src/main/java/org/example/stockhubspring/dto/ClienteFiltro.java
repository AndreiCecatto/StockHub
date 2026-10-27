package org.example.stockhubspring.dto;

import java.util.Locale;

/**
 * Busca, filtros, ordem e página da lista de clientes, já normalizados: valor desconhecido vira o padrão, a página
 * começa em 1 e o tamanho é limitado (RN-97).
 */
public record ClienteFiltro(String busca, Situacao situacao, ComOs comOs, Ordem ordem, int pagina, int tamanho) {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    public enum Situacao {
        ATIVOS, INATIVOS, TODOS;

        public static Situacao de(String texto) {
            return valor(Situacao.class, texto, ATIVOS);
        }
    }

    public enum ComOs {
        TODOS, COM, SEM;

        public static ComOs de(String texto) {
            return valor(ComOs.class, texto, TODOS);
        }
    }

    public enum Ordem {
        NOME, RECENTES;

        public static Ordem de(String texto) {
            return valor(Ordem.class, texto, NOME);
        }
    }

    public ClienteFiltro {
        busca = (busca == null || busca.isBlank()) ? null : busca.trim();
        if (busca != null && busca.length() > 120) {
            busca = busca.substring(0, 120);
        }
        situacao = situacao == null ? Situacao.ATIVOS : situacao;
        comOs = comOs == null ? ComOs.TODOS : comOs;
        ordem = ordem == null ? Ordem.NOME : ordem;
        pagina = Math.max(1, pagina);
        tamanho = tamanho <= 0 ? TAMANHO_PADRAO : Math.min(tamanho, TAMANHO_MAXIMO);
    }

    /** Monta o filtro a partir dos parâmetros crus da requisição. */
    public static ClienteFiltro de(String busca, String situacao, String comOs, String ordem, int pagina) {
        return new ClienteFiltro(busca, Situacao.de(situacao), ComOs.de(comOs), Ordem.de(ordem), pagina, TAMANHO_PADRAO);
    }

    /** Há algo além do padrão (ativos, por nome)? Serve para mostrar o botão "Limpar filtros". */
    public boolean temFiltros() {
        return busca != null || situacao != Situacao.ATIVOS || comOs != ComOs.TODOS;
    }

    private static <E extends Enum<E>> E valor(Class<E> tipo, String texto, E padrao) {
        if (texto == null || texto.isBlank()) {
            return padrao;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return padrao;
        }
    }
}
