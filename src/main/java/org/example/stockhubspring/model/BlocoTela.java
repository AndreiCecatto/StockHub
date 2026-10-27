package org.example.stockhubspring.model;

import java.util.Arrays;
import java.util.List;

/**
 * Lista fechada de blocos que o dono pode ocultar (AD-19, RN-111). O servidor decide; a tela só mostra caixas.
 * A capa do Início e os "primeiros passos" da loja vazia não entram aqui porque não podem ser ocultados.
 */
public enum BlocoTela {
    ATALHOS(TelaPersonalizavel.INICIO, "Atalhos", "Botões de ação rápida no topo.", null),
    PRATELEIRA(TelaPersonalizavel.INICIO, "Prateleira", "Marcas de produtos em dia, abaixo do mínimo e sem estoque.", null),
    TOTAIS(TelaPersonalizavel.INICIO, "Totais da loja", "Valor em estoque, unidades e produtos.", null),
    FAZER_AGORA(TelaPersonalizavel.INICIO, "Fazer agora", "Produtos que pedem reposição.", null),
    HOJE_NA_LOJA(TelaPersonalizavel.INICIO, "Hoje na loja", "Últimas movimentações de estoque.", null),
    OS_FLUXO(TelaPersonalizavel.INICIO, "Ordens de serviço", "Fluxo por situação e OS em andamento.", Modulo.OS),
    GRAFICO_14_DIAS(TelaPersonalizavel.INICIO, "Ritmo dos últimos 14 dias", "Gráfico de entradas, saídas e ajustes.", null),
    CADASTRADOS_ULTIMO(TelaPersonalizavel.INICIO, "Cadastrados por último", "Os produtos mais recentes.", null);

    private final TelaPersonalizavel tela;
    private final String rotulo;
    private final String descricao;
    private final Modulo moduloExigido;

    BlocoTela(TelaPersonalizavel tela, String rotulo, String descricao, Modulo moduloExigido) {
        this.tela = tela;
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.moduloExigido = moduloExigido;
    }

    public TelaPersonalizavel getTela() { return tela; }
    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }
    /** Módulo que a loja precisa ter para o bloco existir; null quando vale para todas. */
    public Modulo getModuloExigido() { return moduloExigido; }

    public boolean aplicavel(TipoLoja tipo) {
        return moduloExigido == null || (tipo != null && tipo.temModulo(moduloExigido));
    }

    /** Blocos da tela que se aplicam ao tipo da loja, na ordem de declaração. */
    public static List<BlocoTela> daTela(TelaPersonalizavel tela, TipoLoja tipo) {
        return Arrays.stream(values()).filter(b -> b.tela == tela && b.aplicavel(tipo)).toList();
    }

    /** Converte o código gravado ou enviado; devolve null se desconhecido. */
    public static BlocoTela de(String codigo) {
        if (codigo == null) {
            return null;
        }
        try {
            return valueOf(codigo.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
