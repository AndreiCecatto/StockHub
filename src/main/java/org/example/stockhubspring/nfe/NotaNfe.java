package org.example.stockhubspring.nfe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * NF-e lida do XML (só o que o sistema usa), já validada e sanitizada pelo {@link NfeParser}.
 * {@code assinaturaPresente} e {@code protocoloPresente} só dizem se o XML TEM esses blocos:
 * a assinatura NÃO é validada e a SEFAZ NÃO é consultada (ver {@link #AVISO_NAO_VERIFICADA}).
 */
public record NotaNfe(String chaveAcesso, int modelo, int serie, long numero, String naturezaOperacao,
                      LocalDateTime emitidaEm, Pessoa emitente, Pessoa destinatario,
                      BigDecimal valorProdutos, BigDecimal valorFrete, BigDecimal valorSeguro,
                      BigDecimal valorOutras, BigDecimal valorDesconto,
                      BigDecimal valorTotal, boolean assinaturaPresente, boolean protocoloPresente,
                      List<ItemNfe> itens, List<Pagamento> pagamentos, List<Parcela> parcelas) {

    /** Texto fixo que a tela de revisão deve mostrar: o sistema nunca afirma que a nota é autêntica. */
    public static final String AVISO_NAO_VERIFICADA =
            "A assinatura digital do arquivo não foi verificada e a situação da nota não foi consultada na SEFAZ. "
                    + "Confirme a nota no portal da SEFAZ ou com o fornecedor antes de dar entrada no estoque.";

    /** Emitente ou destinatário. {@code documento}: CNPJ (14) ou CPF (11) só com dígitos; pode ser nulo no destinatário. */
    public record Pessoa(String documento, String nome, String fantasia, String uf) {
    }

    /** Forma de pagamento da nota (pag/detPag); {@code descricao} já em linguagem simples (ver {@link FormaPagamento}). */
    public record Pagamento(String forma, String descricao, BigDecimal valor) {
    }

    /** Parcela da cobrança (cobr/dup); o vencimento pode faltar na nota. */
    public record Parcela(int numero, LocalDate vencimento, BigDecimal valor) {
    }

    public NotaNfe {
        itens = List.copyOf(itens);
        pagamentos = pagamentos == null ? List.of() : List.copyOf(pagamentos);
        parcelas = parcelas == null ? List.of() : List.copyOf(parcelas);
    }

    /** Forma antiga (sem seguro nem outras despesas), mantida para quem não precisa deles. */
    public NotaNfe(String chaveAcesso, int modelo, int serie, long numero, String naturezaOperacao,
                   LocalDateTime emitidaEm, Pessoa emitente, Pessoa destinatario, BigDecimal valorProdutos,
                   BigDecimal valorFrete, BigDecimal valorDesconto, BigDecimal valorTotal,
                   boolean assinaturaPresente, boolean protocoloPresente, List<ItemNfe> itens) {
        this(chaveAcesso, modelo, serie, numero, naturezaOperacao, emitidaEm, emitente, destinatario,
                valorProdutos, valorFrete, null, null, valorDesconto, valorTotal, assinaturaPresente,
                protocoloPresente, itens, List.of(), List.of());
    }
}
