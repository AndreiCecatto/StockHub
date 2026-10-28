package org.example.stockhubspring.model;

import org.example.stockhubspring.service.CamposRamo;

import java.util.Arrays;
import java.util.List;

/**
 * Lista fechada de blocos (início) e colunas (lista de produtos) que o dono pode ocultar (AD-19, AD-20, RN-111, RN-116).
 * O servidor decide; a tela só mostra caixas. A capa do Início e os "primeiros passos" da loja vazia não entram aqui
 * porque não podem ser ocultados; nome, saldo e ações da lista de produtos também não.
 */
public enum BlocoTela {
    ATALHOS(TelaPersonalizavel.INICIO, "Atalhos", "Botões de ação rápida no topo.", null, null),
    PRATELEIRA(TelaPersonalizavel.INICIO, "Prateleira", "Marcas de produtos em dia, abaixo do mínimo e sem estoque.", null, null),
    TOTAIS(TelaPersonalizavel.INICIO, "Totais da loja", "Valor em estoque, unidades e produtos.", null, null),
    FAZER_AGORA(TelaPersonalizavel.INICIO, "Fazer agora", "Produtos que pedem reposição.", null, null),
    HOJE_NA_LOJA(TelaPersonalizavel.INICIO, "Hoje na loja", "Últimas movimentações de estoque.", null, null),
    OS_FLUXO(TelaPersonalizavel.INICIO, "Ordens de serviço", "Fluxo por situação e OS em andamento.", Modulo.OS, null),
    GRAFICO_14_DIAS(TelaPersonalizavel.INICIO, "Ritmo dos últimos 14 dias", "Gráfico de entradas, saídas e ajustes.", null, null),
    CADASTRADOS_ULTIMO(TelaPersonalizavel.INICIO, "Cadastrados por último", "Os produtos mais recentes.", null, null),

    // Colunas opcionais da lista de produtos
    COL_CATEGORIA(TelaPersonalizavel.PRODUTOS, "Categoria", "Categoria do produto.", null, null),
    COL_PRECO(TelaPersonalizavel.PRODUTOS, "Preço", "Preço de venda.", null, null),
    COL_SITUACAO(TelaPersonalizavel.PRODUTOS, "Situação", "Em dia, abaixo do mínimo ou sem estoque.", null, null),
    COL_DESCRICAO(TelaPersonalizavel.PRODUTOS, "Descrição", "Descrição curta do produto.", null, null),
    COL_TAMANHO(TelaPersonalizavel.PRODUTOS, "Tamanho", "Tamanho da peça.", null, CamposRamo.Campo.TAMANHO),
    COL_COR(TelaPersonalizavel.PRODUTOS, "Cor", "Cor da peça.", null, CamposRamo.Campo.COR),
    COL_LOTE(TelaPersonalizavel.PRODUTOS, "Lote", "Lote do medicamento.", null, CamposRamo.Campo.LOTE),
    COL_VALIDADE(TelaPersonalizavel.PRODUTOS, "Validade", "Data de validade.", null, CamposRamo.Campo.VALIDADE),
    COL_NUMERO_SERIE(TelaPersonalizavel.PRODUTOS, "Número de série", "Número de série do equipamento.", null,
            CamposRamo.Campo.NUMERO_SERIE);

    private final TelaPersonalizavel tela;
    private final String rotulo;
    private final String descricao;
    private final Modulo moduloExigido;
    private final CamposRamo.Campo campoExigido;

    BlocoTela(TelaPersonalizavel tela, String rotulo, String descricao, Modulo moduloExigido,
              CamposRamo.Campo campoExigido) {
        this.tela = tela;
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.moduloExigido = moduloExigido;
        this.campoExigido = campoExigido;
    }

    public TelaPersonalizavel getTela() { return tela; }
    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }
    /** Módulo que a loja precisa ter para o bloco existir; null quando vale para todas. */
    public Modulo getModuloExigido() { return moduloExigido; }
    /** Campo de ramo que o subtipo precisa permitir (CamposRamo); null quando vale para todas. */
    public CamposRamo.Campo getCampoExigido() { return campoExigido; }

    /** Considera só o módulo; um bloco que exige campo de ramo não se aplica sem informar os permitidos. */
    public boolean aplicavel(TipoLoja tipo) {
        return aplicavel(tipo, CamposRamo.Permitidos.NENHUM);
    }

    public boolean aplicavel(TipoLoja tipo, CamposRamo.Permitidos permitidos) {
        boolean modulo = moduloExigido == null || (tipo != null && tipo.temModulo(moduloExigido));
        boolean campo = campoExigido == null || (permitidos != null && permitidos.permite(campoExigido));
        return modulo && campo;
    }

    /** Blocos da tela que se aplicam ao tipo da loja (sem campos de ramo), na ordem de declaração. */
    public static List<BlocoTela> daTela(TelaPersonalizavel tela, TipoLoja tipo) {
        return daTela(tela, tipo, CamposRamo.Permitidos.NENHUM);
    }

    /** Blocos da tela que se aplicam ao tipo e ao subtipo da loja, na ordem de declaração. */
    public static List<BlocoTela> daTela(TelaPersonalizavel tela, TipoLoja tipo, CamposRamo.Permitidos permitidos) {
        return Arrays.stream(values()).filter(b -> b.tela == tela && b.aplicavel(tipo, permitidos)).toList();
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
