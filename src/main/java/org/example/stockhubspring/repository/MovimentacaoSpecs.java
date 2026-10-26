package org.example.stockhubspring.repository;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/** Filtros dinâmicos do histórico de movimentações, montados no banco. */
public final class MovimentacaoSpecs {

    private MovimentacaoSpecs() {
    }

    /** Movimentações da loja (RN-01): base de toda consulta do histórico. */
    public static Specification<MovimentacaoEstoque> doUsuario(Integer usuarioCodigo) {
        return (root, query, cb) -> cb.equal(root.get("usuario").get("codigo"), usuarioCodigo);
    }

    /** Escopo da loja mais os filtros opcionais da tela (cada um só entra se foi informado). */
    public static Specification<MovimentacaoEstoque> comFiltro(Integer usuarioCodigo, MovimentacaoFiltro filtro) {
        Specification<MovimentacaoEstoque> spec = doUsuario(usuarioCodigo);
        if (filtro.produtoId() != null) {
            spec = spec.and(doProduto(filtro.produtoId()));
        }
        if (filtro.tipo() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tipo"), filtro.tipo()));
        }
        if (filtro.de() != null) {
            spec = spec.and(desde(filtro.de()));
        }
        if (filtro.ate() != null) {
            spec = spec.and(ate(filtro.ate()));
        }
        return spec;
    }

    public static Specification<MovimentacaoEstoque> doProduto(Integer produtoId) {
        return (root, query, cb) -> cb.equal(root.get("produto").get("id"), produtoId);
    }

    /** A partir das 00:00 do dia dado (inclusivo). */
    public static Specification<MovimentacaoEstoque> desde(LocalDate dia) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("criadoEm"), dia.atStartOfDay());
    }

    /** Até o fim do dia dado (inclusivo): antes das 00:00 do dia seguinte. */
    public static Specification<MovimentacaoEstoque> ate(LocalDate dia) {
        return (root, query, cb) -> cb.lessThan(root.get("criadoEm"), dia.plusDays(1).atStartOfDay());
    }
}
