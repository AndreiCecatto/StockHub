package org.example.stockhubspring.model;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.util.Locale;

/**
 * Nível de estoque de um produto (RN-12). É a ÚNICA definição no sistema:
 * dashboard, filtros e alertas usam {@link #calcular(int, int)} (em memória) ou
 * {@link #predicado} (no banco), que descrevem exatamente a mesma regra.
 * <ul>
 *   <li>SEM_ESTOQUE: saldo = 0;</li>
 *   <li>BAIXO: 0 &lt; saldo &le; estoque mínimo;</li>
 *   <li>NORMAL: saldo acima do estoque mínimo.</li>
 * </ul>
 */
public enum NivelEstoque {

    // codigo = valor usado hoje no parâmetro "nivelEstoque" das telas
    SEM_ESTOQUE("semEstoque", "Sem estoque"),
    BAIXO("baixo", "Estoque baixo"),
    NORMAL("ok", "Estoque normal");

    private final String codigo;
    private final String descricao;

    NivelEstoque(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Calcula o nível a partir do saldo e do estoque mínimo. */
    public static NivelEstoque calcular(int quantidade, int estoqueMinimo) {
        if (quantidade <= 0) {
            return SEM_ESTOQUE;
        }
        return quantidade <= estoqueMinimo ? BAIXO : NORMAL;
    }

    /** Mesma regra de {@link #calcular}, para uso em consultas (Specification) no banco. */
    public Predicate predicado(CriteriaBuilder cb, Expression<Integer> quantidade, Expression<Integer> estoqueMinimo) {
        return switch (this) {
            case SEM_ESTOQUE -> cb.le(quantidade, 0);
            case BAIXO -> cb.and(cb.gt(quantidade, 0), cb.le(quantidade, estoqueMinimo));
            case NORMAL -> cb.and(cb.gt(quantidade, 0), cb.gt(quantidade, estoqueMinimo));
        };
    }

    /** Aceita o código da tela ("ok", "baixo", "semEstoque") ou o nome do enum; vazio/inválido vira null. */
    public static NivelEstoque deParametro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String texto = valor.trim();
        for (NivelEstoque nivel : values()) {
            if (nivel.codigo.equalsIgnoreCase(texto) || nivel.name().equals(texto.toUpperCase(Locale.ROOT))) {
                return nivel;
            }
        }
        return null;
    }
}
