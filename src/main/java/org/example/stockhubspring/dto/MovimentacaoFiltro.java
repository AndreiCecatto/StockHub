package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.TipoMovimentacao;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Filtros do histórico de movimentações, já normalizados. Qualquer valor inválido vira "sem filtro"
 * (nunca causa erro): o histórico é sempre escopado à loja pelo service (RN-01).
 *
 * @param produtoId código do produto (maior que zero) ou null
 * @param tipo      tipo da movimentação ou null
 * @param de        data inicial, inclusiva, ou null
 * @param ate       data final, inclusiva, ou null
 */
public record MovimentacaoFiltro(Integer produtoId, TipoMovimentacao tipo, LocalDate de, LocalDate ate) {

    public static final MovimentacaoFiltro VAZIO = new MovimentacaoFiltro(null, null, null, null);

    public MovimentacaoFiltro {
        if (produtoId != null && produtoId <= 0) {
            produtoId = null;
        }
    }

    /** Monta o filtro a partir dos parâmetros crus da requisição; o que não for válido é ignorado. */
    public static MovimentacaoFiltro de(String produto, String tipo, String de, String ate) {
        return new MovimentacaoFiltro(lerProduto(produto), lerTipo(tipo), lerData(de), lerData(ate));
    }

    public boolean vazio() {
        return produtoId == null && tipo == null && de == null && ate == null;
    }

    /** Data no formato aaaa-MM-dd para repor o campo da tela (string vazia se não há filtro). */
    public String deParametro() {
        return de == null ? "" : de.toString();
    }

    public String ateParametro() {
        return ate == null ? "" : ate.toString();
    }

    public String tipoParametro() {
        return tipo == null ? "" : tipo.name();
    }

    private static Integer lerProduto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            int id = Integer.parseInt(valor.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static TipoMovimentacao lerTipo(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return TipoMovimentacao.valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate lerData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            // ISO_LOCAL_DATE é estrito: "2026-02-30" e "26-1-1" são recusados
            LocalDate data = LocalDate.parse(valor.trim());
            // Anos absurdos ("+999999999-12-31") estouram o cálculo do dia seguinte e o timestamp do banco
            return data.getYear() < 1 || data.getYear() > 9999 ? null : data;
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
