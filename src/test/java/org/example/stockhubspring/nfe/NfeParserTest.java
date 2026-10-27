package org.example.stockhubspring.nfe;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NfeParserTest {

    private static NotaNfe ler(NfeXml nota) {
        return NfeParser.ler(nota.bytes());
    }

    private static void recusa(byte[] bytes, String trechoDaMensagem) {
        assertThatThrownBy(() -> NfeParser.ler(bytes))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining(trechoDaMensagem);
    }

    @Nested
    @DisplayName("nota válida")
    class NotaValida {

        @Test
        @DisplayName("lê cabeçalho, emitente, destinatário e totais")
        void cabecalho() {
            NfeXml xml = NfeXml.nota().numero(4321)
                    .item(NfeXml.item().codigo("A1").descricao("CANETA AZUL").quantidade("10.0000").unitario("2.5000000000"))
                    .item(NfeXml.item().codigo("B2").descricao("LAPIS PRETO").quantidade("3.0000").unitario("1.0000000000"));
            NotaNfe nota = ler(xml);

            assertThat(nota.chaveAcesso()).isEqualTo(xml.chave()).hasSize(44);
            assertThat(nota.modelo()).isEqualTo(55);
            assertThat(nota.serie()).isEqualTo(1);
            assertThat(nota.numero()).isEqualTo(4321);
            assertThat(nota.emitidaEm()).isEqualTo(LocalDateTime.of(2026, 3, 10, 10, 30));
            assertThat(nota.emitente().documento()).isEqualTo(NfeXml.CNPJ_FORNECEDOR);
            assertThat(nota.emitente().nome()).isEqualTo("FORNECEDOR DE TESTE LTDA");
            assertThat(nota.emitente().fantasia()).isEqualTo("FORNECEDOR TESTE");
            assertThat(nota.emitente().uf()).isEqualTo("RS");
            assertThat(nota.destinatario().documento()).isEqualTo(NfeXml.CNPJ_LOJA);
            assertThat(nota.valorProdutos()).isEqualByComparingTo("28.00");
            assertThat(nota.valorTotal()).isEqualByComparingTo("28.00");
            assertThat(nota.assinaturaPresente()).isTrue();
            assertThat(nota.protocoloPresente()).isTrue();
            assertThat(nota.itens()).hasSize(2);
            assertThat(nota.itens().get(0).numero()).isEqualTo(1);
            assertThat(nota.itens().get(1).codigoFornecedor()).isEqualTo("B2");
        }

        @Test
        @DisplayName("aceita a raiz NFe sem nfeProc (sem protocolo)")
        void raizNfe() {
            NotaNfe nota = ler(NfeXml.notaComUmItem().semNfeProc());
            assertThat(nota.protocoloPresente()).isFalse();
            assertThat(nota.itens()).hasSize(1);
        }

        @Test
        @DisplayName("aceita arquivo com BOM UTF-8")
        void comBom() {
            byte[] xml = NfeXml.notaComUmItem().bytes();
            byte[] comBom = new byte[xml.length + 3];
            comBom[0] = (byte) 0xEF;
            comBom[1] = (byte) 0xBB;
            comBom[2] = (byte) 0xBF;
            System.arraycopy(xml, 0, comBom, 3, xml.length);
            assertThat(NfeParser.ler(comBom).itens()).hasSize(1);
        }

        @Test
        @DisplayName("EAN \"SEM GTIN\" vira nulo; EAN válido é mantido")
        void ean() {
            NotaNfe nota = ler(NfeXml.nota()
                    .item(NfeXml.item().ean(null))
                    .item(NfeXml.item().ean("7891000100103")));
            assertThat(nota.itens().get(0).ean()).isNull();
            assertThat(nota.itens().get(1).ean()).isEqualTo("7891000100103");
        }

        @Test
        @DisplayName("quantidade 12.0000 é inteira; 2.5 é fracionada")
        void fracionado() {
            NotaNfe nota = ler(NfeXml.nota()
                    .item(NfeXml.item().quantidade("12.0000"))
                    .item(NfeXml.item().quantidade("2.5000")));
            ItemNfe inteiro = nota.itens().get(0);
            ItemNfe fracionado = nota.itens().get(1);
            assertThat(inteiro.fracionado()).isFalse();
            assertThat(inteiro.quantidadeInteira()).isEqualTo(12);
            assertThat(inteiro.motivoBloqueio()).isNull();
            assertThat(fracionado.fracionado()).isTrue();
            assertThat(fracionado.motivoBloqueio()).isNull();
            assertThat(fracionado.avisos()).anyMatch(a -> a.contains("fracionada"));
        }

        @Test
        @DisplayName("custo unitário = valor líquido / quantidade, 4 casas HALF_UP, com desconto do item")
        void custoComDesconto() {
            // 3 un a R$ 10,00 = 30,00; desconto 1,00 => 29,00 / 3 = 9,6667
            NotaNfe nota = ler(NfeXml.nota().item(
                    NfeXml.item().quantidade("3.0000").unitario("10.0000000000").desconto("1.00")));
            ItemNfe item = nota.itens().get(0);
            assertThat(item.valorBruto()).isEqualByComparingTo("30.00");
            assertThat(item.valorLiquido()).isEqualByComparingTo("29.00");
            assertThat(item.custoUnitario()).isEqualByComparingTo("9.6667");
            assertThat(item.custoUnitario().scale()).isEqualTo(4);
        }

        @Test
        @DisplayName("CFOP de entrada: 5102 vira 1102, 6102 vira 2102, 7102 vira 3102")
        void cfop() {
            NotaNfe nota = ler(NfeXml.nota()
                    .item(NfeXml.item().cfop("5102"))
                    .item(NfeXml.item().cfop("6102"))
                    .item(NfeXml.item().cfop("7102")));
            assertThat(nota.itens()).extracting(ItemNfe::cfop).containsExactly("5102", "6102", "7102");
            assertThat(nota.itens()).extracting(ItemNfe::cfopEntrada).containsExactly("1102", "2102", "3102");
        }

        @ParameterizedTest(name = "CFOP {0} gera aviso de que não é mercadoria para revenda")
        @ValueSource(strings = {"5910", "5949", "5915", "5916", "5920", "5921", "5927", "6949"})
        void cfopSuspeito(String cfop) {
            ItemNfe item = ler(NfeXml.nota().item(NfeXml.item().cfop(cfop))).itens().get(0);
            assertThat(item.avisos()).anyMatch(a -> a.contains("revenda"));
        }

        @Test
        @DisplayName("CFOP comum não gera aviso")
        void cfopComum() {
            assertThat(ler(NfeXml.notaComUmItem()).itens().get(0).avisos()).isEmpty();
        }

        @Test
        @DisplayName("quantidade zero ou acima de 1.000.000 bloqueia o item (não a nota)")
        void quantidadeForaDaFaixa() {
            NotaNfe nota = ler(NfeXml.nota()
                    .item(NfeXml.item().quantidade("0.0000").bruto("0.00"))
                    .item(NfeXml.item().quantidade("1000001.0000").unitario("0.0100000000").bruto("10000.01"))
                    .item(NfeXml.item().quantidade("1000000.0000").unitario("0.0100000000").bruto("10000.00")));
            assertThat(nota.itens().get(0).motivoBloqueio()).contains("zero");
            assertThat(nota.itens().get(1).motivoBloqueio()).contains("1.000.000");
            assertThat(nota.itens().get(2).motivoBloqueio()).isNull();
        }

        @Test
        @DisplayName("textos têm caracteres de controle e invisíveis removidos e são truncados")
        void sanitiza() {
            assertThat(NfeParser.sanitizar("  A\u200Bb\u202Ec\u0007d  ", 50)).isEqualTo("Abcd");
            assertThat(NfeParser.sanitizar("x".repeat(300), 120)).hasSize(120);
            assertThat(NfeParser.sanitizar("   ", 10)).isNull();
        }
    }

    @Nested
    @DisplayName("notas recusadas pelas regras de aceitação")
    class Recusadas {

        @Test
        @DisplayName("NFC-e (modelo 65)")
        void nfce() {
            recusa(NfeXml.notaComUmItem().modelo("65").bytes(), "NFC-e");
        }

        @Test
        @DisplayName("modelo desconhecido")
        void modeloDesconhecido() {
            recusa(NfeXml.notaComUmItem().modelo("57").bytes(), "modelo 55");
        }

        @Test
        @DisplayName("finalidade diferente de 1 (devolução, complementar, ajuste)")
        void finalidade() {
            for (String finalidade : new String[]{"2", "3", "4"}) {
                recusa(NfeXml.notaComUmItem().finalidade(finalidade).bytes(), "NF-e normal");
            }
        }

        @Test
        @DisplayName("tpNF=0 (nota de entrada)")
        void entrada() {
            recusa(NfeXml.notaComUmItem().tipoNota("0").bytes(), "de entrada");
        }

        @Test
        @DisplayName("layout diferente de 4.00")
        void layout() {
            recusa(NfeXml.notaComUmItem().versao("3.10").bytes(), "4.00");
        }

        @Test
        @DisplayName("chave com dígito verificador errado")
        void dvErrado() {
            NfeXml ok = NfeXml.notaComUmItem();
            String chave = ok.chave();
            int dvCerto = chave.charAt(43) - '0';
            String errada = chave.substring(0, 43) + ((dvCerto + 1) % 10);
            recusa(ok.idChave(errada).chaveProtocolo(errada).bytes(), "dígito verificador");
        }

        @Test
        @DisplayName("chave do protocolo diferente da chave da nota")
        void protocoloDiferente() {
            NfeXml ok = NfeXml.notaComUmItem();
            String outra = NfeXml.nota().numero(999).item(NfeXml.item()).chave();
            recusa(ok.chaveProtocolo(outra).bytes(), "protocolo");
        }

        @Test
        @DisplayName("Id com chave válida, mas de outra nota (não confere com o corpo)")
        void idDiferenteDoCorpo() {
            NfeXml ok = NfeXml.notaComUmItem();
            String outraChave = NfeXml.nota().numero(1234).emitente(NfeXml.CNPJ_OUTRO).item(NfeXml.item()).chave();
            recusa(ok.idChave(outraChave).chaveProtocolo(outraChave).bytes(), "CNPJ do emitente");
        }

        @Test
        @DisplayName("Id que não segue o padrão NFe+44 dígitos")
        void idMalFormado() {
            recusa(NfeXml.notaComUmItem().idChave("123").bytes(), "não é uma NF-e válida");
        }

        @Test
        @DisplayName("mais de 990 itens")
        void itensDemais() {
            NfeXml nota = NfeXml.nota();
            for (int i = 0; i < 991; i++) {
                nota.item(NfeXml.item().codigo("C" + i));
            }
            recusa(nota.bytes(), "990");
        }

        @Test
        @DisplayName("exatamente 990 itens é aceito")
        void limiteDeItens() {
            NfeXml nota = NfeXml.nota();
            for (int i = 0; i < 990; i++) {
                nota.item(NfeXml.item().codigo("C" + i));
            }
            assertThat(NfeParser.ler(nota.bytes()).itens()).hasSize(990);
        }

        @Test
        @DisplayName("nota sem itens")
        void semItens() {
            recusa(NfeXml.nota().bytes(), "não tem itens");
        }

        @Test
        @DisplayName("valor numérico inválido não vaza o conteúdo")
        void numeroInvalido() {
            byte[] xml = NfeXml.notaComUmItem().xml().replace("<qCom>10.0000</qCom>", "<qCom>dez</qCom>")
                    .getBytes(StandardCharsets.UTF_8);
            assertThatThrownBy(() -> NfeParser.ler(xml)).isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("qCom").hasMessageNotContaining("dez");
        }

        @Test
        @DisplayName("data de emissão sem fuso é inválida")
        void dataSemFuso() {
            byte[] xml = NfeXml.notaComUmItem().xml().replace("2026-03-10T10:30:00-03:00", "2026-03-10T10:30:00")
                    .getBytes(StandardCharsets.UTF_8);
            recusa(xml, "data de emissão");
        }
    }

    @Nested
    @DisplayName("entrada: tamanho, codificação e formato")
    class Entrada {

        @Test
        @DisplayName("arquivo vazio")
        void vazio() {
            recusa(new byte[0], "vazio");
            recusa(null, "vazio");
        }

        @Test
        @DisplayName("arquivo acima de 2 MB é recusado antes de interpretar o conteúdo")
        void grande() {
            byte[] grande = new byte[NfeParser.TAMANHO_MAXIMO_BYTES + 1];
            Arrays.fill(grande, (byte) 'a');
            grande[0] = '<';
            recusa(grande, "2 MB");
        }

        @Test
        @DisplayName("arquivo de exatamente 2 MB passa do filtro de tamanho (e falha só por não ser NF-e)")
        void noLimite() {
            byte[] limite = new byte[NfeParser.TAMANHO_MAXIMO_BYTES];
            Arrays.fill(limite, (byte) 'a');
            limite[0] = '<';
            recusa(limite, "não é uma NF-e válida");
        }

        @ParameterizedTest(name = "não é XML: {0}")
        @ValueSource(strings = {"texto qualquer", "{\"json\": true}", "   <nfeProc/>", "<nfeProc>", "<a><b></a>"})
        void naoXml(String conteudo) {
            recusa(conteudo.getBytes(StandardCharsets.UTF_8), "não é uma NF-e válida");
        }

        @Test
        @DisplayName("bytes binários")
        void binario() {
            recusa(new byte[]{'<', 0, 1, 2, (byte) 0xFF, (byte) 0xFE, 3}, "UTF-8");
        }

        @Test
        @DisplayName("XML válido que não é NF-e")
        void xmlQualquer() {
            recusa("<?xml version=\"1.0\"?><pedido><item/></pedido>".getBytes(StandardCharsets.UTF_8),
                    "não é uma NF-e válida");
        }

        @Test
        @DisplayName("declaração de outra codificação (ISO-8859-1) é recusada")
        void outraCodificacao() {
            String xml = NfeXml.notaComUmItem().xml().replace("encoding=\"UTF-8\"", "encoding=\"ISO-8859-1\"");
            recusa(xml.getBytes(StandardCharsets.UTF_8), "UTF-8");
        }

        @Test
        @DisplayName("bytes em ISO-8859-1 (UTF-8 inválido) são recusados")
        void bytesLatin1() {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            saida.writeBytes("<a>".getBytes(StandardCharsets.UTF_8));
            saida.write(0xE9); // "é" em ISO-8859-1: sequência inválida em UTF-8
            saida.writeBytes("</a>".getBytes(StandardCharsets.UTF_8));
            recusa(saida.toByteArray(), "UTF-8");
        }
    }

    @Nested
    @DisplayName("ataques ao parser XML")
    class Ataques {

        @Test
        @DisplayName("XXE com entidade externa file:// é recusado e o conteúdo do arquivo nunca aparece")
        void xxeArquivo() {
            String doctype = "<!DOCTYPE nfeProc [<!ENTITY xxe SYSTEM \"file:///C:/Windows/win.ini\">]>";
            String xml = NfeXml.notaComUmItem().antesDaRaiz(doctype).xml()
                    .replace("<natOp>VENDA DE MERCADORIA</natOp>", "<natOp>&xxe;</natOp>");
            assertThatThrownBy(() -> NfeParser.ler(xml.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage(NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("XXE com URL http:// também é recusado (nenhuma resolução de rede)")
        void xxeHttp() {
            String doctype = "<!DOCTYPE nfeProc [<!ENTITY xxe SYSTEM \"http://127.0.0.1:9/x\">]>";
            String xml = NfeXml.notaComUmItem().antesDaRaiz(doctype).xml()
                    .replace("<natOp>VENDA DE MERCADORIA</natOp>", "<natOp>&xxe;</natOp>");
            recusa(xml.getBytes(StandardCharsets.UTF_8), NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("billion laughs (entidades internas recursivas) é recusado rapidamente")
        void bilhaoDeRisadas() {
            String doctype = "<!DOCTYPE nfeProc [<!ENTITY a \"aaaaaaaaaa\">"
                    + "<!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">"
                    + "<!ENTITY c \"&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;\">"
                    + "<!ENTITY d \"&c;&c;&c;&c;&c;&c;&c;&c;&c;&c;\">"
                    + "<!ENTITY e \"&d;&d;&d;&d;&d;&d;&d;&d;&d;&d;\">]>";
            String xml = NfeXml.notaComUmItem().antesDaRaiz(doctype).xml()
                    .replace("<natOp>VENDA DE MERCADORIA</natOp>", "<natOp>&e;</natOp>");
            long inicio = System.nanoTime();
            recusa(xml.getBytes(StandardCharsets.UTF_8), NfeParser.MENSAGEM_INVALIDA);
            assertThat((System.nanoTime() - inicio) / 1_000_000).as("ms").isLessThan(2000);
        }

        @Test
        @DisplayName("qualquer DOCTYPE, mesmo simples, é recusado")
        void doctypeSimples() {
            recusa(NfeXml.notaComUmItem().antesDaRaiz("<!DOCTYPE nfeProc>").bytes(), NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("DOCTYPE com DTD externo é recusado")
        void dtdExterno() {
            recusa(NfeXml.notaComUmItem()
                    .antesDaRaiz("<!DOCTYPE nfeProc SYSTEM \"http://127.0.0.1:9/evil.dtd\">").bytes(),
                    NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("entidade de parâmetro externa é recusada")
        void entidadeDeParametro() {
            recusa(NfeXml.notaComUmItem()
                    .antesDaRaiz("<!DOCTYPE nfeProc [<!ENTITY % p SYSTEM \"file:///C:/Windows/win.ini\"> %p;]>").bytes(),
                    NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("a fábrica de DOM é configurada à prova de XXE")
        void fabricaSegura() throws Exception {
            var fabrica = NfeParser.fabricaSegura();
            assertThat(fabrica.getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isTrue();
            assertThat(fabrica.getFeature("http://xml.org/sax/features/external-general-entities")).isFalse();
            assertThat(fabrica.getFeature("http://xml.org/sax/features/external-parameter-entities")).isFalse();
            assertThat(fabrica.getFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING)).isTrue();
            assertThat(fabrica.isXIncludeAware()).isFalse();
            assertThat(fabrica.isExpandEntityReferences()).isFalse();
            assertThat(fabrica.getAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD)).isEqualTo("");
            assertThat(fabrica.getAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA)).isEqualTo("");
        }

        @Test
        @DisplayName("custo muito grande não vaza: item bloqueado")
        void custoGigante() {
            ItemNfe item = ler(NfeXml.nota().item(NfeXml.item().quantidade("1.0000")
                    .unitario("99999999999.0000000000").bruto("999999999999.00"))).itens().get(0);
            assertThat(item.motivoBloqueio()).contains("Custo");
            assertThat(item.valorUnitario()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("endurecimento (auditoria)")
    class Endurecimento {

        private byte[] comTroca(NfeXml nota, String de, String para) {
            String xml = nota.xml();
            assertThat(xml).contains(de);
            return xml.replace(de, para).getBytes(StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("aninhamento acima de 32 níveis é recusado (sem estourar a pilha)")
        void profundidade() {
            String abre = "<a>".repeat(40);
            String fecha = "</a>".repeat(40);
            String xml = "<nfeProc xmlns=\"" + NfeXml.NS + "\">" + abre + fecha + "</nfeProc>";
            recusa(xml.getBytes(StandardCharsets.UTF_8), NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("aninhamento gigantesco (milhares de níveis) vira a mensagem genérica")
        void aninhamentoGigante() {
            String xml = "<nfeProc xmlns=\"" + NfeXml.NS + "\">" + "<a>".repeat(50_000) + "</nfeProc>";
            recusa(xml.getBytes(StandardCharsets.UTF_8), NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("elementos demais (mesmo dentro de 2 MB) são recusados")
        void elementosDemais() {
            String xml = "<nfeProc xmlns=\"" + NfeXml.NS + "\">" + "<a/>".repeat(NfeParser.MAXIMO_ELEMENTOS + 1)
                    + "</nfeProc>";
            assertThat(xml.length()).isLessThan(NfeParser.TAMANHO_MAXIMO_BYTES);
            recusa(xml.getBytes(StandardCharsets.UTF_8), NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("UF, série e número da chave precisam bater com o corpo da nota")
        void chaveContraCorpo() {
            recusa(comTroca(NfeXml.notaComUmItem(), "<cUF>43</cUF>", "<cUF>35</cUF>"), "UF");
            recusa(comTroca(NfeXml.notaComUmItem(), "<serie>1</serie>", "<serie>2</serie>"), "série");
            recusa(comTroca(NfeXml.notaComUmItem(), "<nNF>1234</nNF>", "<nNF>1235</nNF>"), "número");
        }

        @Test
        @DisplayName("série com mais de 3 dígitos é recusada")
        void serieGrande() {
            recusa(comTroca(NfeXml.notaComUmItem(), "<serie>1</serie>", "<serie>0001</serie>"), "serie");
        }

        @Test
        @DisplayName("campos de formato fixo são lidos sem truncar: chNFe, CFOP, CNPJ e NCM")
        void formatoFixo() {
            NfeXml base = NfeXml.notaComUmItem();
            String chave = base.chave();
            // chNFe com 45 dígitos (a chave certa + 1 dígito) não pode ser "cortado" para 44 e aceito
            recusa(comTroca(base, "<chNFe>" + chave + "</chNFe>", "<chNFe>" + chave + "7</chNFe>"),
                    NfeParser.MENSAGEM_INVALIDA);
            recusa(comTroca(NfeXml.notaComUmItem(), "<CFOP>5102</CFOP>", "<CFOP>51029</CFOP>"),
                    NfeParser.MENSAGEM_INVALIDA);
            recusa(comTroca(NfeXml.notaComUmItem(), "<CNPJ>" + NfeXml.CNPJ_FORNECEDOR + "</CNPJ>",
                    "<CNPJ>" + NfeXml.CNPJ_FORNECEDOR + "99</CNPJ>"), NfeParser.MENSAGEM_INVALIDA);
            // NCM com 9 dígitos não vira os 8 primeiros: é descartado
            NotaNfe nota = NfeParser.ler(comTroca(NfeXml.notaComUmItem(), "<NCM>22021000</NCM>",
                    "<NCM>220210001</NCM>"));
            assertThat(nota.itens().get(0).ncm()).isNull();
        }

        @Test
        @DisplayName("CNPJ do emitente com dígito verificador errado é recusado")
        void emitenteInvalido() {
            String errado = NfeXml.CNPJ_FORNECEDOR.substring(0, 13)
                    + ((NfeXml.CNPJ_FORNECEDOR.charAt(13) - '0' + 1) % 10);
            recusa(NfeXml.nota().emitente(errado).item(NfeXml.item()).bytes(), "emitente");
        }

        @Test
        @DisplayName("uCom: maiúscula antes de truncar; unidade que passa de 6 caracteres é recusada")
        void unidade() {
            assertThat(ler(NfeXml.nota().item(NfeXml.item().unidade("un"))).itens().get(0).unidade()).isEqualTo("UN");
            // "ß" vira "SS" em maiúsculas: 4 letras viram 8, e não pode ser cortado em silêncio
            recusa(NfeXml.nota().item(NfeXml.item().unidade("ßßßß")).bytes(), NfeParser.MENSAGEM_INVALIDA);
            recusa(NfeXml.nota().item(NfeXml.item().unidade("unidade")).bytes(), NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("data de emissão mais de 1 dia no futuro é recusada; ontem e amanhã cedo passam")
        void dataNoFuturo() {
            recusa(NfeXml.notaComUmItem().emissao("2099-01-01T10:00:00-03:00").bytes(), "futuro");
            String ontem = java.time.OffsetDateTime.now().minusDays(1).withNano(0).toString();
            assertThat(ler(NfeXml.notaComUmItem().emissao(ontem)).itens()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("frete, seguro, outras despesas e desconto (RN-53)")
    class Complementos {

        @Test
        @DisplayName("lê vFrete, vSeg, vOutro e vDesc da nota")
        void totaisDaNota() {
            NotaNfe nota = ler(NfeXml.nota().frete("10.00").seguro("2.50").outras("1.25")
                    .item(NfeXml.item().desconto("3.00")));

            assertThat(nota.valorFrete()).isEqualByComparingTo("10.00");
            assertThat(nota.valorSeguro()).isEqualByComparingTo("2.50");
            assertThat(nota.valorOutras()).isEqualByComparingTo("1.25");
            assertThat(nota.valorDesconto()).isEqualByComparingTo("3.00");
        }

        @Test
        @DisplayName("sem as tags, seguro e outras despesas ficam nulos (nota antiga continua válida)")
        void ausentes() {
            NotaNfe nota = ler(NfeXml.notaComUmItem());

            assertThat(nota.valorSeguro()).isNull();
            assertThat(nota.valorOutras()).isNull();
            ItemNfe item = nota.itens().get(0);
            assertThat(item.frete()).isNull();
            assertThat(item.seguro()).isNull();
            assertThat(item.outras()).isNull();
        }

        @Test
        @DisplayName("lê frete, seguro, outras despesas e desconto do item")
        void itens() {
            ItemNfe item = ler(NfeXml.nota().item(NfeXml.item().frete("1.10").seguro("0.20").outras("0.30")
                    .desconto("0.50"))).itens().get(0);

            assertThat(item.frete()).isEqualByComparingTo("1.10");
            assertThat(item.seguro()).isEqualByComparingTo("0.20");
            assertThat(item.outras()).isEqualByComparingTo("0.30");
            assertThat(item.desconto()).isEqualByComparingTo("0.50");
        }

        @Test
        @DisplayName("valor negativo ou com formato inválido é recusado")
        void invalidos() {
            recusa(NfeXml.nota().seguro("-1.00").item(NfeXml.item()).bytes(), "vSeg");
            recusa(NfeXml.nota().outras("1.00").item(NfeXml.item()).xml().replace("<vOutro>1.00<", "<vOutro>abc<")
                    .getBytes(StandardCharsets.UTF_8), "vOutro");
            recusa(NfeXml.nota().item(NfeXml.item().frete("1.234")).bytes(), "vFrete");
        }
    }

    @Nested
    @DisplayName("pagamento e parcelas (RN-57)")
    class Cobranca {

        @Test
        @DisplayName("lê as formas de pagamento com nome em linguagem simples")
        void pagamentos() {
            NotaNfe nota = ler(NfeXml.nota().item(NfeXml.item())
                    .pagamento("15", "30.00", null).pagamento("17", "20.00", null)
                    .pagamento("99", "5.00", "Permuta").pagamento("99", "1.00", null));

            assertThat(nota.pagamentos()).extracting(NotaNfe.Pagamento::descricao)
                    .containsExactly("Boleto", "Pix", "Permuta", "Outra forma de pagamento");
            assertThat(nota.pagamentos().get(0).forma()).isEqualTo("15");
            assertThat(nota.pagamentos().get(0).valor()).isEqualByComparingTo("30.00");
        }

        @Test
        @DisplayName("lê as parcelas, com vencimento opcional")
        void parcelas() {
            NotaNfe nota = ler(NfeXml.nota().item(NfeXml.item())
                    .parcela("2026-04-10", "25.00").parcela("2026-05-10", "25.00").parcela(null, "0.50"));

            assertThat(nota.parcelas()).hasSize(3);
            assertThat(nota.parcelas().get(1).numero()).isEqualTo(2);
            assertThat(nota.parcelas().get(1).vencimento()).isEqualTo(java.time.LocalDate.of(2026, 5, 10));
            assertThat(nota.parcelas().get(2).vencimento()).isNull();
            assertThat(nota.parcelas().get(2).valor()).isEqualByComparingTo("0.50");
        }

        @Test
        @DisplayName("nota sem cobr e sem pag tem listas vazias")
        void ausentes() {
            NotaNfe nota = ler(NfeXml.notaComUmItem());

            assertThat(nota.pagamentos()).isEmpty();
            assertThat(nota.parcelas()).isEmpty();
        }

        @Test
        @DisplayName("vencimento inválido, valor inválido e código de pagamento inválido são recusados")
        void invalidos() {
            recusa(NfeXml.nota().item(NfeXml.item()).parcela("31/12/2026", "10.00").bytes(), "vencimento");
            recusa(NfeXml.nota().item(NfeXml.item()).parcela("2026-02-30", "10.00").bytes(), "vencimento");
            recusa(NfeXml.nota().item(NfeXml.item()).pagamento("15", "-5.00", null).bytes(), "vPag");
            recusa(NfeXml.nota().item(NfeXml.item()).pagamento("1", "5.00", null).bytes(),
                    NfeParser.MENSAGEM_INVALIDA);
        }

        @Test
        @DisplayName("mais de 120 parcelas ou 100 formas de pagamento é recusado")
        void limites() {
            NfeXml parcelas = NfeXml.nota().item(NfeXml.item());
            for (int i = 0; i < 121; i++) {
                parcelas.parcela("2026-04-10", "1.00");
            }
            recusa(parcelas.bytes(), NfeParser.MENSAGEM_INVALIDA);
            NfeXml pagamentos = NfeXml.nota().item(NfeXml.item());
            for (int i = 0; i < 101; i++) {
                pagamentos.pagamento("01", "1.00", null);
            }
            recusa(pagamentos.bytes(), NfeParser.MENSAGEM_INVALIDA);
        }
    }
}
