package org.example.stockhubspring.nfe;

import org.example.stockhubspring.service.EstoqueService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Item de uma NF-e. {@code ean} já vem nulo quando o XML traz "SEM GTIN" ou um valor inválido;
 * {@code cfopEntrada} é o CFOP derivado (5xxx para 1xxx, 6xxx para 2xxx, 7xxx para 3xxx) ou nulo.
 */
public record ItemNfe(int numero, String codigoFornecedor, String ean, String descricao, String ncm,
                      String cfop, String cfopEntrada, String unidade, BigDecimal quantidade,
                      BigDecimal valorUnitario, BigDecimal valorBruto, BigDecimal desconto,
                      BigDecimal frete, BigDecimal seguro, BigDecimal outras) {

    /** Forma antiga (sem frete, seguro e outras despesas do item). */
    public ItemNfe(int numero, String codigoFornecedor, String ean, String descricao, String ncm,
                   String cfop, String cfopEntrada, String unidade, BigDecimal quantidade,
                   BigDecimal valorUnitario, BigDecimal valorBruto, BigDecimal desconto) {
        this(numero, codigoFornecedor, ean, descricao, ncm, cfop, cfopEntrada, unidade, quantidade,
                valorUnitario, valorBruto, desconto, null, null, null);
    }

    /** Maior custo unitário que cabe na coluna produto.custo (NUMERIC(14,4)). */
    public static final BigDecimal CUSTO_MAXIMO = new BigDecimal("9999999999.9999");
    /** CFOP (3 últimos dígitos) que normalmente não é mercadoria para revenda: bonificação, remessa, demonstração etc. */
    private static final Set<String> CFOP_SUFIXOS_SUSPEITOS = Set.of("910", "949", "915", "916", "920", "921", "927");

    /** Valor do item menos o desconto do próprio item (base do rateio; sem frete, seguro e outras despesas). */
    public BigDecimal valorLiquido() {
        return valorBruto.subtract(desconto == null ? BigDecimal.ZERO : desconto);
    }

    /** Parte de frete, seguro e outras despesas que cabe a este item (já rateada pelo {@link RateioNfe}). */
    public BigDecimal rateio() {
        BigDecimal soma = BigDecimal.ZERO;
        for (BigDecimal parte : new BigDecimal[]{frete, seguro, outras}) {
            if (parte != null) {
                soma = soma.add(parte);
            }
        }
        return soma;
    }

    /** Valor líquido mais a parte de frete, seguro e outras despesas: o que a loja pagou de fato pelo item. */
    public BigDecimal valorComRateio() {
        return valorLiquido().add(rateio());
    }

    /** Cópia com frete, seguro e outras despesas do item definidos (usada pelo rateio). */
    ItemNfe comDespesas(BigDecimal novoFrete, BigDecimal novoSeguro, BigDecimal novasOutras) {
        return new ItemNfe(numero, codigoFornecedor, ean, descricao, ncm, cfop, cfopEntrada, unidade, quantidade,
                valorUnitario, valorBruto, desconto, novoFrete, novoSeguro, novasOutras);
    }

    /** Custo unitário = (valor líquido + rateio) / quantidade, 4 casas, HALF_UP; nulo se a quantidade não for positiva. */
    public BigDecimal custoUnitario() {
        if (quantidade.signum() <= 0) {
            return null;
        }
        return valorComRateio().divide(quantidade, 4, RoundingMode.HALF_UP);
    }

    /** Quantidade com parte decimal (ex.: 2,5): o saldo do sistema é inteiro, então o item não entra no estoque. */
    public boolean fracionado() {
        return quantidade.signum() > 0 && quantidade.stripTrailingZeros().scale() > 0;
    }

    /** Quantidade inteira (12.0000 vale 12). Só chamar se não for fracionado e estiver dentro do limite. */
    public int quantidadeInteira() {
        return quantidade.intValueExact();
    }

    /** Por que o item não pode ser importado de jeito nenhum (só ignorado); nulo se pode. */
    public String motivoBloqueio() {
        if (quantidade.signum() <= 0) {
            return "Quantidade menor ou igual a zero.";
        }
        if (quantidade.compareTo(BigDecimal.valueOf(EstoqueService.QUANTIDADE_MAXIMA_MOVIMENTACAO)) > 0) {
            return "Quantidade acima do limite de entrada do sistema (1.000.000).";
        }
        if (valorLiquido().signum() < 0) {
            return "Desconto maior que o valor do item.";
        }
        if (!fracionado() && custoUnitario().compareTo(CUSTO_MAXIMO) > 0) {
            return "Custo unitário fora da faixa aceita.";
        }
        return null;
    }

    /** Avisos do item que não dependem do produto escolhido (o dono decide). */
    public List<String> avisos() {
        List<String> avisos = new ArrayList<>();
        if (cfop != null && cfop.length() == 4 && CFOP_SUFIXOS_SUSPEITOS.contains(cfop.substring(1))) {
            avisos.add("CFOP " + cfop + " normalmente não é mercadoria para revenda (bonificação, remessa, "
                    + "demonstração ou similar). Confira antes de dar entrada.");
        }
        if (cfopEntrada == null) {
            avisos.add("CFOP " + cfop + " não é de saída (5, 6 ou 7): não foi possível derivar o CFOP de entrada.");
        }
        if (fracionado()) {
            avisos.add("Quantidade fracionada (" + quantidade.stripTrailingZeros().toPlainString()
                    + " " + unidade + "): o saldo do sistema é inteiro, então este item NÃO entra no estoque. "
                    + "Lance a entrada manualmente.");
        }
        return avisos;
    }
}
