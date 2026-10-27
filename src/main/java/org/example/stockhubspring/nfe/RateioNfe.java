package org.example.stockhubspring.nfe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rateio de frete, seguro e outras despesas da nota entre os itens (RN-54).
 * Cada despesa é dividida proporcionalmente ao valor líquido do item (valor do produto menos o desconto do
 * item), em centavos, pelo método do maior resto: a soma das partes é sempre igual ao total da nota (nenhum
 * centavo some nem sobra). Se o emitente já informou a despesa dentro dos itens, esses valores são mantidos como
 * estão (o emitente já fez o rateio dele).
 */
final class RateioNfe {

    private static final BigDecimal CENTAVO = new BigDecimal("0.01");

    private RateioNfe() {
    }

    /** Devolve os itens com frete, seguro e outras despesas preenchidos (rateados ou os do próprio item). */
    static List<ItemNfe> aplicar(BigDecimal totalFrete, BigDecimal totalSeguro, BigDecimal totalOutras,
                                 List<ItemNfe> itens) {
        List<BigDecimal> bases = new ArrayList<>();
        for (ItemNfe item : itens) {
            bases.add(item.valorLiquido().max(BigDecimal.ZERO));
        }
        List<BigDecimal> frete = componente(totalFrete, itens.stream().map(ItemNfe::frete).toList(), bases);
        List<BigDecimal> seguro = componente(totalSeguro, itens.stream().map(ItemNfe::seguro).toList(), bases);
        List<BigDecimal> outras = componente(totalOutras, itens.stream().map(ItemNfe::outras).toList(), bases);
        List<ItemNfe> resultado = new ArrayList<>();
        for (int i = 0; i < itens.size(); i++) {
            resultado.add(itens.get(i).comDespesas(frete.get(i), seguro.get(i), outras.get(i)));
        }
        return resultado;
    }

    /** Valores do item quando o emitente os informou; senão o total da nota rateado; senão como veio. */
    private static List<BigDecimal> componente(BigDecimal totalNota, List<BigDecimal> doItem, List<BigDecimal> bases) {
        BigDecimal somaItens = BigDecimal.ZERO;
        for (BigDecimal valor : doItem) {
            if (valor != null) {
                somaItens = somaItens.add(valor);
            }
        }
        if (somaItens.signum() > 0 || totalNota == null || totalNota.signum() <= 0) {
            return doItem;
        }
        return distribuir(totalNota, bases);
    }

    /**
     * Divide {@code total} em centavos proporcionalmente a {@code bases} (maior resto; empate vai para o item de
     * menor número). Sem base positiva não há como dividir: devolve nulos (nada é rateado).
     */
    static List<BigDecimal> distribuir(BigDecimal total, List<BigDecimal> bases) {
        BigDecimal somaBases = bases.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> partes = new ArrayList<>();
        if (somaBases.signum() <= 0) {
            for (int i = 0; i < bases.size(); i++) {
                partes.add(null);
            }
            return partes;
        }
        List<BigDecimal> restos = new ArrayList<>();
        BigDecimal somaPartes = BigDecimal.ZERO;
        for (BigDecimal base : bases) {
            BigDecimal exato = total.multiply(base).divide(somaBases, 12, RoundingMode.DOWN);
            BigDecimal parte = exato.setScale(2, RoundingMode.DOWN);
            partes.add(parte);
            restos.add(exato.subtract(parte));
            somaPartes = somaPartes.add(parte);
        }
        int centavosQueFaltam = total.subtract(somaPartes).movePointRight(2).intValueExact();
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < bases.size(); i++) {
            ordem.add(i);
        }
        ordem.sort(Comparator.comparing((Integer i) -> restos.get(i)).reversed().thenComparing(i -> i));
        for (int k = 0; k < centavosQueFaltam; k++) {
            int i = ordem.get(k);
            partes.set(i, partes.get(i).add(CENTAVO));
        }
        return partes;
    }
}
