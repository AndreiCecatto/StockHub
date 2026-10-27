package org.example.stockhubspring.nfe;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.nfe.NotaNfe.Pessoa;
import org.example.stockhubspring.util.Documentos;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê e valida uma NF-e 4.00 (modelo 55) em XML. Segurança em primeiro lugar:
 * <ul>
 *   <li>o tamanho é conferido em bytes ANTES de interpretar o conteúdo (máx. 2 MB), só UTF-8;</li>
 *   <li>o parser recusa qualquer DOCTYPE (sem entidades externas nem expansão de entidades: XXE e
 *       "billion laughs" não têm como acontecer), sem XInclude, sem acesso a DTD/esquema externos;</li>
 *   <li>números só por BigDecimal com formato e faixa validados; textos sem caracteres de controle e truncados;</li>
 *   <li>nenhuma mensagem de erro repete o conteúdo do arquivo.</li>
 * </ul>
 * A assinatura digital NÃO é validada e a SEFAZ NÃO é consultada (exige certificado digital).
 */
public final class NfeParser {

    public static final int TAMANHO_MAXIMO_BYTES = 2 * 1024 * 1024;
    public static final int MAXIMO_ITENS = 990;
    public static final int PROFUNDIDADE_MAXIMA = 32;
    /** 990 itens x ~40 elementos + cabeçalho; acima disso não é NF-e. */
    public static final int MAXIMO_ELEMENTOS = 100_000;
    public static final String MENSAGEM_INVALIDA = "O arquivo enviado não é uma NF-e válida.";

    private static final String NS_NFE = "http://www.portalfiscal.inf.br/nfe";
    private static final ZoneId FUSO_LOJA = ZoneId.of("America/Sao_Paulo");
    private static final Pattern DECLARACAO_ENCODING =
            Pattern.compile("^<\\?xml[^>]{0,200}?encoding\\s*=\\s*[\"']([^\"']{1,40})[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUM_QUANTIDADE = Pattern.compile("-?\\d{1,10}(\\.\\d{1,4})?");
    private static final Pattern NUM_UNITARIO = Pattern.compile("\\d{1,11}(\\.\\d{1,10})?");
    private static final Pattern NUM_DINHEIRO = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");
    private static final Pattern ID_INF_NFE = Pattern.compile("NFe(\\d{44})");

    private NfeParser() {
    }

    /** Valida o tamanho e lê a nota. Recusa com {@link RegraNegocioException} (mensagem em português, sem eco do conteúdo). */
    public static NotaNfe ler(byte[] bytes) {
        String texto = decodificar(bytes);
        try {
            Document documento = lerDom(texto);
            // o DOM já está montado (no máximo 2 MB), mas não se processa documento com elementos demais
            if (documento.getElementsByTagName("*").getLength() > MAXIMO_ELEMENTOS) {
                throw invalida();
            }
            return extrair(documento);
        } catch (RegraNegocioException e) {
            throw e;
        } catch (RuntimeException | StackOverflowError e) {
            // qualquer surpresa de estrutura (inclusive aninhamento absurdo) vira a mensagem genérica
            throw new RegraNegocioException(MENSAGEM_INVALIDA);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Entrada: tamanho, codificação e DOM seguro
    // -------------------------------------------------------------------------------------------

    /** Confere o tamanho em bytes, o BOM/primeiro caractere e decodifica UTF-8 estrito. */
    static String decodificar(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new RegraNegocioException("O arquivo está vazio. Envie o XML da NF-e.");
        }
        if (bytes.length > TAMANHO_MAXIMO_BYTES) {
            throw new RegraNegocioException("O arquivo passa do limite de 2 MB.");
        }
        int inicio = 0;
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            inicio = 3;
        }
        if (inicio >= bytes.length || bytes[inicio] != '<') {
            throw new RegraNegocioException(MENSAGEM_INVALIDA);
        }
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, inicio, bytes.length - inicio))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new RegraNegocioException("O arquivo precisa estar em UTF-8.");
        }
        Matcher declaracao = DECLARACAO_ENCODING.matcher(texto);
        if (declaracao.find() && !declaracao.group(1).equalsIgnoreCase("utf-8")) {
            throw new RegraNegocioException("O arquivo precisa estar em UTF-8.");
        }
        return texto;
    }

    /** O texto decodificado, já sem BOM e pronto para ser guardado (mesma regra de {@link #ler}). */
    public static String textoUtf8(byte[] bytes) {
        return decodificar(bytes);
    }

    /** Lê o XML já como texto (usado também na confirmação, que relê o XML guardado). */
    public static NotaNfe lerTexto(String xml) {
        if (xml == null || xml.isEmpty()) {
            throw new RegraNegocioException(MENSAGEM_INVALIDA);
        }
        return ler(xml.getBytes(StandardCharsets.UTF_8));
    }

    private static Document lerDom(String texto) {
        try {
            DocumentBuilder construtor = fabricaSegura().newDocumentBuilder();
            construtor.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // ignorado: nada de imprimir conteúdo do arquivo
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            // Entrada por texto (Reader): o parser nunca resolve URL nenhuma
            construtor.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("Entidades externas não são permitidas.");
            });
            return construtor.parse(new InputSource(new StringReader(texto)));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            // sem eco: a mensagem do parser pode citar trechos do arquivo
            throw new RegraNegocioException(MENSAGEM_INVALIDA);
        }
    }

    /** Fábrica do DOM à prova de XXE e de bombas de expansão. Se algum recurso não existir, falha (nunca relaxa). */
    static DocumentBuilderFactory fabricaSegura() throws ParserConfigurationException {
        DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setValidating(false);
        fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Qualquer DOCTYPE é recusado: sem DTD não existe entidade externa nem recursiva
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
        fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        fabrica.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        // NF-e real tem uns 8 níveis; aninhamento além disso é ataque (ou lixo)
        fabrica.setAttribute("jdk.xml.maxElementDepth", PROFUNDIDADE_MAXIMA);
        return fabrica;
    }

    // -------------------------------------------------------------------------------------------
    // Extração e regras de aceitação
    // -------------------------------------------------------------------------------------------

    private static NotaNfe extrair(Document documento) {
        Element raiz = documento.getDocumentElement();
        if (raiz == null || !NS_NFE.equals(raiz.getNamespaceURI())) {
            throw invalida();
        }
        Element nfe;
        Element protocolo = null;
        if ("nfeProc".equals(raiz.getLocalName())) {
            nfe = filho(raiz, "NFe");
            protocolo = filho(raiz, "protNFe");
        } else if ("NFe".equals(raiz.getLocalName())) {
            nfe = raiz;
        } else {
            throw invalida();
        }
        Element inf = nfe == null ? null : filho(nfe, "infNFe");
        if (inf == null) {
            throw invalida();
        }
        if (!"4.00".equals(inf.getAttribute("versao"))) {
            throw new RegraNegocioException("Só é aceita NF-e no layout 4.00.");
        }
        Matcher id = ID_INF_NFE.matcher(inf.getAttribute("Id"));
        if (!id.matches()) {
            throw invalida();
        }
        String chave = id.group(1);

        Element ide = obrigatorio(inf, "ide");
        verificarModeloEFinalidade(ide);
        // a chave precisa ter dígito verificador correto e bater com o protocolo, se houver
        if (!Documentos.chaveAcessoValida(chave)) {
            throw new RegraNegocioException("A chave de acesso da nota é inválida (dígito verificador incorreto).");
        }
        Element infProt = protocolo == null ? null : filho(protocolo, "infProt");
        String chaveProtocolo = infProt == null ? null : texto(infProt, "chNFe", 100);
        // formato fixo: lido sem truncar; tamanho diferente de 44 dígitos é recusado
        if (chaveProtocolo != null && !Documentos.soDigitosComTamanho(chaveProtocolo, 44)) {
            throw invalida();
        }
        if (chaveProtocolo != null && !chaveProtocolo.equals(chave)) {
            throw new RegraNegocioException("A chave de acesso do protocolo não confere com a da nota.");
        }

        int modelo = inteiro(textoObrigatorio(ide, "mod", 10), "mod");
        String textoSerie = textoObrigatorio(ide, "serie", 10);
        if (textoSerie.length() > 3) {
            throw numeroInvalido("serie");
        }
        int serie = inteiro(textoSerie, "serie");
        long numero = longo(textoObrigatorio(ide, "nNF", 20), "nNF");
        // A chave também carrega UF, modelo, série, número e CNPJ do emitente: precisam bater com o corpo da nota
        if (Integer.parseInt(chave.substring(20, 22)) != modelo) {
            throw new RegraNegocioException("A chave de acesso não confere com o modelo da nota.");
        }
        String textoUf = textoObrigatorio(ide, "cUF", 10);
        if (!Documentos.soDigitosComTamanho(textoUf, 2) || !textoUf.equals(chave.substring(0, 2))) {
            throw new RegraNegocioException("A chave de acesso não confere com a UF da nota.");
        }
        if (Integer.parseInt(chave.substring(22, 25)) != serie) {
            throw new RegraNegocioException("A chave de acesso não confere com a série da nota.");
        }
        if (Long.parseLong(chave.substring(25, 34)) != numero) {
            throw new RegraNegocioException("A chave de acesso não confere com o número da nota.");
        }
        String naturezaOperacao = texto(ide, "natOp", 120);
        LocalDateTime emitidaEm = data(textoObrigatorio(ide, "dhEmi", 40));

        Pessoa emitente = pessoa(obrigatorio(inf, "emit"), true, "enderEmit");
        if (emitente.documento() == null || emitente.nome() == null) {
            throw invalida();
        }
        // dígito verificador do CNPJ/CPF do emitente
        boolean documentoValido = emitente.documento().length() == 14
                ? Documentos.cnpjValido(emitente.documento()) : Documentos.cpfValido(emitente.documento());
        if (!documentoValido) {
            throw new RegraNegocioException("O CNPJ/CPF do emitente da nota é inválido.");
        }
        if (emitente.documento().length() == 14 && !emitente.documento().equals(chave.substring(6, 20))) {
            throw new RegraNegocioException("A chave de acesso não confere com o CNPJ do emitente.");
        }
        Element elementoDest = filho(inf, "dest");
        Pessoa destinatario = elementoDest == null ? null : pessoa(elementoDest, false, "enderDest");

        List<ItemNfe> itens = new ArrayList<>();
        for (Node no = inf.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element det && "det".equals(det.getLocalName())) {
                if (itens.size() >= MAXIMO_ITENS) {
                    throw new RegraNegocioException("A nota tem mais de " + MAXIMO_ITENS
                            + " itens (limite do layout).");
                }
                itens.add(item(det, itens.size() + 1));
            }
        }
        if (itens.isEmpty()) {
            throw new RegraNegocioException("A nota não tem itens.");
        }

        Element totais = null;
        Element total = filho(inf, "total");
        if (total != null) {
            totais = filho(total, "ICMSTot");
        }
        BigDecimal valorProdutos = totais == null ? null : dinheiro(texto(totais, "vProd", 60), "vProd");
        BigDecimal valorFrete = totais == null ? null : dinheiro(texto(totais, "vFrete", 60), "vFrete");
        BigDecimal valorSeguro = totais == null ? null : dinheiro(texto(totais, "vSeg", 60), "vSeg");
        BigDecimal valorOutras = totais == null ? null : dinheiro(texto(totais, "vOutro", 60), "vOutro");
        BigDecimal valorDesconto = totais == null ? null : dinheiro(texto(totais, "vDesc", 60), "vDesc");
        BigDecimal valorTotal = totais == null ? null : dinheiro(texto(totais, "vNF", 60), "vNF");

        // Só registra se o bloco existe; a assinatura NÃO é validada
        boolean assinatura = filho(nfe, "Signature") != null;

        return new NotaNfe(chave, modelo, serie, numero, naturezaOperacao, emitidaEm, emitente, destinatario,
                valorProdutos, valorFrete, valorSeguro, valorOutras, valorDesconto, valorTotal, assinatura,
                protocolo != null, itens);
    }

    /** Só NF-e modelo 55, finalidade normal (1) e saída do emitente (tpNF=1 = compra para a loja). */
    private static void verificarModeloEFinalidade(Element ide) {
        String modelo = texto(ide, "mod", 10);
        if ("65".equals(modelo)) {
            throw new RegraNegocioException("NFC-e (modelo 65) não é aceita: envie uma NF-e modelo 55.");
        }
        if (!"55".equals(modelo)) {
            throw new RegraNegocioException("Só é aceita NF-e modelo 55.");
        }
        if (!"1".equals(texto(ide, "finNFe", 10))) {
            throw new RegraNegocioException("Só é aceita NF-e normal. Notas de devolução, complementares "
                    + "ou de ajuste não entram no estoque por aqui.");
        }
        if (!"1".equals(texto(ide, "tpNF", 10))) {
            throw new RegraNegocioException("A nota é de entrada (tpNF=0). Só são aceitas notas de saída do "
                    + "emitente, que representam uma compra para a sua loja.");
        }
    }

    private static Pessoa pessoa(Element elemento, boolean obrigatorio, String nomeEndereco) {
        String cnpj = texto(elemento, "CNPJ", 40);
        String cpf = texto(elemento, "CPF", 40);
        String documento = null;
        if (cnpj != null && Documentos.soDigitosComTamanho(cnpj, 14)) {
            documento = cnpj;
        } else if (cpf != null && Documentos.soDigitosComTamanho(cpf, 11)) {
            documento = cpf;
        } else if (obrigatorio) {
            throw invalida();
        }
        String nome = texto(elemento, "xNome", 255);
        String fantasia = texto(elemento, "xFant", 255);
        String uf = null;
        Element endereco = filho(elemento, nomeEndereco);
        if (endereco != null) {
            String valor = texto(endereco, "UF", 10);
            if (valor != null && valor.matches("[A-Za-z]{2}")) {
                uf = valor.toUpperCase(Locale.ROOT);
            }
        }
        return new Pessoa(documento, nome, fantasia, uf);
    }

    private static ItemNfe item(Element det, int sequencia) {
        String nItem = det.getAttribute("nItem");
        if (!Documentos.soDigitosComTamanho(nItem, nItem.length()) || nItem.isEmpty() || nItem.length() > 3
                || Integer.parseInt(nItem) != sequencia) {
            throw invalida();
        }
        Element prod = obrigatorio(det, "prod");
        String codigo = textoObrigatorio(prod, "cProd", 60);
        String ean = normalizarEan(texto(prod, "cEAN", 40));
        String descricao = textoObrigatorio(prod, "xProd", 120);
        String ncm = texto(prod, "NCM", 20);
        if (ncm != null && !Documentos.soDigitosComTamanho(ncm, 8)) {
            ncm = null;
        }
        String cfop = textoObrigatorio(prod, "CFOP", 20);
        if (!Documentos.soDigitosComTamanho(cfop, 4)) {
            throw invalida();
        }
        String unidade = textoObrigatorio(prod, "uCom", 40).toUpperCase(Locale.ROOT);
        if (unidade.length() > 6) {
            throw invalida();
        }
        BigDecimal quantidade = numero(NUM_QUANTIDADE, textoObrigatorio(prod, "qCom", 60), "qCom");
        BigDecimal unitario = numero(NUM_UNITARIO, textoObrigatorio(prod, "vUnCom", 60), "vUnCom");
        BigDecimal bruto = numero(NUM_DINHEIRO, textoObrigatorio(prod, "vProd", 60), "vProd");
        String textoDesconto = texto(prod, "vDesc", 60);
        BigDecimal desconto = textoDesconto == null ? null : numero(NUM_DINHEIRO, textoDesconto, "vDesc");
        return new ItemNfe(sequencia, codigo, ean, descricao, ncm, cfop, cfopDeEntrada(cfop), unidade,
                quantidade, unitario, bruto, desconto,
                dinheiro(texto(prod, "vFrete", 60), "vFrete"), dinheiro(texto(prod, "vSeg", 60), "vSeg"),
                dinheiro(texto(prod, "vOutro", 60), "vOutro"));
    }

    /** 5xxx para 1xxx, 6xxx para 2xxx, 7xxx para 3xxx, mantendo os 3 últimos dígitos; outro início fica sem derivação. */
    static String cfopDeEntrada(String cfop) {
        if (cfop == null || cfop.length() != 4) {
            return null;
        }
        return switch (cfop.charAt(0)) {
            case '5' -> "1" + cfop.substring(1);
            case '6' -> "2" + cfop.substring(1);
            case '7' -> "3" + cfop.substring(1);
            default -> null;
        };
    }

    /** "SEM GTIN", vazio ou formato inesperado viram nulo; só dígitos com 8, 12, 13 ou 14 posições. */
    static String normalizarEan(String bruto) {
        if (bruto == null) {
            return null;
        }
        int tamanho = bruto.length();
        if ((tamanho == 8 || tamanho == 12 || tamanho == 13 || tamanho == 14)
                && Documentos.soDigitosComTamanho(bruto, tamanho)) {
            return bruto;
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------
    // Utilitários de leitura
    // -------------------------------------------------------------------------------------------

    private static RegraNegocioException invalida() {
        return new RegraNegocioException(MENSAGEM_INVALIDA);
    }

    /** Primeiro filho direto com o nome local dado, ou nulo. */
    private static Element filho(Element pai, String nome) {
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element e && nome.equals(e.getLocalName())) {
                return e;
            }
        }
        return null;
    }

    private static Element obrigatorio(Element pai, String nome) {
        Element e = filho(pai, nome);
        if (e == null) {
            throw invalida();
        }
        return e;
    }

    /** Texto do filho, sem caracteres de controle, aparado e truncado em {@code maximo}; nulo se ausente ou vazio. */
    private static String texto(Element pai, String nome, int maximo) {
        Element e = filho(pai, nome);
        return e == null ? null : sanitizar(e.getTextContent(), maximo);
    }

    private static String textoObrigatorio(Element pai, String nome, int maximo) {
        String valor = texto(pai, nome, maximo);
        if (valor == null) {
            throw invalida();
        }
        return valor;
    }

    /** Remove caracteres de controle e de formatação invisíveis, apara e trunca. */
    static String sanitizar(String bruto, int maximo) {
        if (bruto == null) {
            return null;
        }
        StringBuilder saida = new StringBuilder(Math.min(bruto.length(), maximo));
        bruto.codePoints().forEach(c -> {
            int tipo = Character.getType(c);
            boolean controle = tipo == Character.CONTROL || tipo == Character.FORMAT
                    || tipo == Character.UNASSIGNED || tipo == Character.SURROGATE
                    || tipo == Character.PRIVATE_USE || tipo == Character.LINE_SEPARATOR
                    || tipo == Character.PARAGRAPH_SEPARATOR;
            if (!controle && saida.length() < maximo + 1) {
                saida.appendCodePoint(c);
            }
        });
        String limpo = saida.toString().trim();
        if (limpo.length() > maximo) {
            limpo = limpo.substring(0, maximo);
            if (Character.isHighSurrogate(limpo.charAt(limpo.length() - 1))) {
                limpo = limpo.substring(0, limpo.length() - 1);
            }
            limpo = limpo.trim();
        }
        return limpo.isEmpty() ? null : limpo;
    }

    private static int inteiro(String valor, String campo) {
        if (!Documentos.soDigitosComTamanho(valor, valor.length()) || valor.length() > 9) {
            throw numeroInvalido(campo);
        }
        return Integer.parseInt(valor);
    }

    private static long longo(String valor, String campo) {
        if (!Documentos.soDigitosComTamanho(valor, valor.length()) || valor.length() > 9) {
            throw numeroInvalido(campo);
        }
        return Long.parseLong(valor);
    }

    private static BigDecimal numero(Pattern formato, String valor, String campo) {
        if (!formato.matcher(valor).matches()) {
            throw numeroInvalido(campo);
        }
        return new BigDecimal(valor);
    }

    private static BigDecimal dinheiro(String valor, String campo) {
        return valor == null ? null : numero(NUM_DINHEIRO, valor, campo);
    }

    private static RegraNegocioException numeroInvalido(String campo) {
        return new RegraNegocioException("A nota tem um valor numérico inválido no campo " + campo + ".");
    }

    /** Data ISO com fuso (ex.: 2024-03-10T10:30:00-03:00), convertida para o horário de São Paulo. */
    private static LocalDateTime data(String valor) {
        OffsetDateTime emissao;
        try {
            emissao = OffsetDateTime.parse(valor);
        } catch (DateTimeException e) {
            throw new RegraNegocioException("A data de emissão da nota é inválida.");
        }
        if (emissao.toInstant().isAfter(java.time.Instant.now().plus(java.time.Duration.ofDays(1)))) {
            throw new RegraNegocioException("A data de emissão da nota está no futuro.");
        }
        return emissao.atZoneSameInstant(FUSO_LOJA).toLocalDateTime();
    }
}
