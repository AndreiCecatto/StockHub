package org.example.stockhubspring.dto;

import org.example.stockhubspring.model.SituacaoItemNfe;
import org.example.stockhubspring.nfe.NotaNfe;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tudo o que a tela de revisão mostra, calculado pelo service a partir do XML guardado.
 * {@code avisoVerificacao} deve aparecer sempre: a assinatura digital não é validada e a SEFAZ não é consultada.
 */
public record RevisaoNfe(UUID token, LocalDateTime expiraEm, NotaNfe nota, String avisoVerificacao,
                         List<String> avisos, boolean lojaTemCnpj, Boolean destinatarioConfere,
                         boolean fornecedorConhecido, int totalImportaveis, List<ItemRevisao> itens) {

    /** Como o item foi relacionado a um produto da loja. {@code NENHUM} = sugere criar produto novo. */
    public enum MotivoSugestao {
        EAN, CODIGO_FORNECEDOR, NOME, NENHUM
    }

    /**
     * Item da nota para revisão. {@code situacao}: IMPORTADO = pode entrar no estoque; FRACIONADO = não entra
     * (lançar à mão); IGNORADO = recusado ({@code bloqueado}, com {@code motivoBloqueio}); só pode ser ignorado.
     */
    public record ItemRevisao(int numero, String codigoFornecedor, String ean, String descricao, String ncm,
                              String cfop, String cfopEntrada, String unidade, BigDecimal quantidade,
                              BigDecimal valorUnitario, BigDecimal valorTotal, BigDecimal desconto,
                              BigDecimal custoUnitario, SituacaoItemNfe situacao, boolean bloqueado,
                              String motivoBloqueio, List<String> avisos, Integer produtoSugeridoId,
                              String produtoSugeridoNome, String produtoSugeridoUnidade,
                              MotivoSugestao motivoSugestao, boolean unidadeDiferente, BigDecimal rateio,
                              boolean avisoEan, boolean avisoNcm) {
    }
}
