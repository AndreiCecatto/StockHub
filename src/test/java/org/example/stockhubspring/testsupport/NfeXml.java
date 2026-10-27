package org.example.stockhubspring.testsupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta NF-e 4.00 (modelo 55) SINTÉTICAS para os testes, com CNPJ matematicamente válido e chave de acesso com
 * dígito verificador correto (módulo 11). Nunca use dados reais de nota.
 */
public final class NfeXml {

    public static final String NS = "http://www.portalfiscal.inf.br/nfe";

    /** CNPJ de teste do emitente (válido). */
    public static final String CNPJ_FORNECEDOR = cnpj("123456780001");
    /** CNPJ de teste de uma loja destinatária (válido). */
    public static final String CNPJ_LOJA = cnpj("987654320001");
    /** Outro CNPJ de teste (válido). */
    public static final String CNPJ_OUTRO = cnpj("555666770001");

    /** Item da nota. Os valores padrão formam um item coerente (quantidade 10 de R$ 5,00). */
    public static final class Item {
        String codigo = "P001";
        String ean = "7891000100103";
        String descricao = "PRODUTO DE TESTE";
        String ncm = "22021000";
        String cfop = "5102";
        String unidade = "UN";
        String quantidade = "10.0000";
        String unitario = "5.0000000000";
        String bruto;
        String desconto;

        public Item codigo(String v) {
            codigo = v;
            return this;
        }

        public Item ean(String v) {
            ean = v;
            return this;
        }

        public Item descricao(String v) {
            descricao = v;
            return this;
        }

        public Item ncm(String v) {
            ncm = v;
            return this;
        }

        public Item cfop(String v) {
            cfop = v;
            return this;
        }

        public Item unidade(String v) {
            unidade = v;
            return this;
        }

        public Item quantidade(String v) {
            quantidade = v;
            return this;
        }

        public Item unitario(String v) {
            unitario = v;
            return this;
        }

        public Item bruto(String v) {
            bruto = v;
            return this;
        }

        public Item desconto(String v) {
            desconto = v;
            return this;
        }

        String valorBruto() {
            if (bruto != null) {
                return bruto;
            }
            return new BigDecimal(quantidade).multiply(new BigDecimal(unitario))
                    .setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
    }

    private String emitente = CNPJ_FORNECEDOR;
    private String destinatario = CNPJ_LOJA;
    private String nomeEmitente = "FORNECEDOR DE TESTE LTDA";
    private String fantasia = "FORNECEDOR TESTE";
    private String modelo = "55";
    private String finalidade = "1";
    private String tipoNota = "1";
    private String versao = "4.00";
    private String dataEmissao = "2026-03-10T10:30:00-03:00";
    private int serie = 1;
    private long numero = 1234;
    private int cNf = 0;
    private boolean protocolo = true;
    private boolean assinatura = true;
    private boolean raizNfeProc = true;
    private String idChave;
    private String chaveProtocolo;
    private String prefixoXml = "";
    private final List<Item> itens = new ArrayList<>();

    public static NfeXml nota() {
        return new NfeXml();
    }

    public static Item item() {
        return new Item();
    }

    /** Nota válida com um item padrão (para quem só precisa de "uma nota"). */
    public static NfeXml notaComUmItem() {
        return nota().item(item());
    }

    public NfeXml item(Item item) {
        itens.add(item);
        return this;
    }

    public NfeXml emitente(String cnpj) {
        emitente = cnpj;
        return this;
    }

    public NfeXml destinatario(String cnpj) {
        destinatario = cnpj;
        return this;
    }

    public NfeXml nomeEmitente(String nome) {
        nomeEmitente = nome;
        return this;
    }

    public NfeXml modelo(String v) {
        modelo = v;
        return this;
    }

    public NfeXml finalidade(String v) {
        finalidade = v;
        return this;
    }

    public NfeXml tipoNota(String v) {
        tipoNota = v;
        return this;
    }

    public NfeXml versao(String v) {
        versao = v;
        return this;
    }

    public NfeXml numero(long v) {
        numero = v;
        return this;
    }

    public NfeXml serie(int v) {
        serie = v;
        return this;
    }

    /** Data de emissão ISO com fuso (ex.: 2024-01-10T10:00:00-03:00). */
    public NfeXml emissao(String iso) {
        dataEmissao = iso;
        return this;
    }

    /** Código numérico (cNF) da chave: muda a chave sem mudar emitente, modelo, série e número. */
    public NfeXml codigoNumerico(int codigo) {
        cNf = codigo;
        return this;
    }

    public NfeXml semProtocolo() {
        protocolo = false;
        return this;
    }

    public NfeXml semAssinatura() {
        assinatura = false;
        return this;
    }

    public NfeXml semNfeProc() {
        raizNfeProc = false;
        protocolo = false;
        return this;
    }

    /** Força o atributo Id (chave de 44 dígitos) diferente da calculada. */
    public NfeXml idChave(String chave) {
        idChave = chave;
        return this;
    }

    /** Força o chNFe do protocolo diferente da chave. */
    public NfeXml chaveProtocolo(String chave) {
        chaveProtocolo = chave;
        return this;
    }

    /** Texto colocado entre a declaração XML e a raiz (ex.: um DOCTYPE malicioso). */
    public NfeXml antesDaRaiz(String texto) {
        prefixoXml = texto;
        return this;
    }

    /** A chave de acesso coerente com os dados da nota (44 dígitos, DV correto). */
    public String chave() {
        return chave(emitente, modelo, serie, numero, cNf == 0 ? (int) (numero % 100_000_000L) : cNf);
    }

    public static String chave(String cnpjEmitente, String modelo, int serie, long numero, int codigoNumerico) {
        String base = "43" + "2603" + cnpjEmitente + pad(modelo, 2) + pad(String.valueOf(serie), 3)
                + pad(String.valueOf(numero), 9) + "1" + pad(String.valueOf(codigoNumerico), 8);
        return base + dv(base);
    }

    /** Dígito verificador (módulo 11) de uma chave de 43 dígitos. */
    public static int dv(String base43) {
        int soma = 0;
        int peso = 2;
        for (int i = base43.length() - 1; i >= 0; i--) {
            soma += (base43.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Completa 12 dígitos-base com os dois dígitos verificadores de CNPJ. */
    public static String cnpj(String base12) {
        int[] p1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] p2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        String parcial = base12 + digito(base12, p1);
        return parcial + digito(parcial, p2);
    }

    private static int digito(String numero, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (numero.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static String pad(String valor, int tamanho) {
        return "0".repeat(Math.max(0, tamanho - valor.length())) + valor;
    }

    public String xml() {
        String chave = chave();
        String id = idChave != null ? idChave : chave;
        StringBuilder x = new StringBuilder();
        x.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(prefixoXml);
        if (raizNfeProc) {
            x.append("<nfeProc xmlns=\"").append(NS).append("\" versao=\"").append(versao).append("\">");
        }
        x.append("<NFe").append(raizNfeProc ? "" : " xmlns=\"" + NS + "\"").append("><infNFe versao=\"")
                .append(versao).append("\" Id=\"NFe").append(id).append("\">");
        x.append("<ide><cUF>43</cUF><natOp>VENDA DE MERCADORIA</natOp><mod>").append(modelo).append("</mod><serie>")
                .append(serie).append("</serie><nNF>").append(numero).append("</nNF><dhEmi>").append(dataEmissao)
                .append("</dhEmi><tpNF>").append(tipoNota).append("</tpNF><finNFe>").append(finalidade)
                .append("</finNFe></ide>");
        x.append("<emit><CNPJ>").append(emitente).append("</CNPJ><xNome>").append(esc(nomeEmitente))
                .append("</xNome><xFant>").append(esc(fantasia))
                .append("</xFant><enderEmit><xLgr>RUA TESTE</xLgr><UF>RS</UF></enderEmit></emit>");
        if (destinatario != null) {
            x.append("<dest><CNPJ>").append(destinatario).append("</CNPJ><xNome>LOJA DE TESTE</xNome></dest>");
        }
        BigDecimal totalProdutos = BigDecimal.ZERO;
        BigDecimal totalDesconto = BigDecimal.ZERO;
        int n = 1;
        for (Item i : itens) {
            x.append("<det nItem=\"").append(n++).append("\"><prod><cProd>").append(esc(i.codigo))
                    .append("</cProd><cEAN>").append(i.ean == null ? "SEM GTIN" : i.ean).append("</cEAN><xProd>")
                    .append(esc(i.descricao)).append("</xProd><NCM>").append(i.ncm).append("</NCM><CFOP>")
                    .append(i.cfop).append("</CFOP><uCom>").append(i.unidade).append("</uCom><qCom>")
                    .append(i.quantidade).append("</qCom><vUnCom>").append(i.unitario).append("</vUnCom><vProd>")
                    .append(i.valorBruto()).append("</vProd>");
            totalProdutos = totalProdutos.add(new BigDecimal(i.valorBruto()));
            if (i.desconto != null) {
                x.append("<vDesc>").append(i.desconto).append("</vDesc>");
                totalDesconto = totalDesconto.add(new BigDecimal(i.desconto));
            }
            x.append("</prod></det>");
        }
        x.append("<total><ICMSTot><vProd>").append(totalProdutos.toPlainString()).append("</vProd><vFrete>0.00</vFrete><vDesc>")
                .append(totalDesconto.setScale(2, RoundingMode.HALF_UP).toPlainString()).append("</vDesc><vNF>")
                .append(totalProdutos.subtract(totalDesconto).toPlainString()).append("</vNF></ICMSTot></total>");
        x.append("</infNFe>");
        if (assinatura) {
            x.append("<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><SignedInfo/></Signature>");
        }
        x.append("</NFe>");
        if (raizNfeProc) {
            if (protocolo) {
                x.append("<protNFe versao=\"4.00\"><infProt><chNFe>")
                        .append(chaveProtocolo != null ? chaveProtocolo : chave).append("</chNFe></infProt></protNFe>");
            }
            x.append("</nfeProc>");
        }
        return x.toString();
    }

    public byte[] bytes() {
        return xml().getBytes(StandardCharsets.UTF_8);
    }

    private static String esc(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
