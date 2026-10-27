package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.AcaoItemNfe;
import org.example.stockhubspring.dto.ConfirmacaoNfeForm;
import org.example.stockhubspring.dto.DecisaoItemNfeForm;
import org.example.stockhubspring.dto.PendenteNfe;
import org.example.stockhubspring.dto.RevisaoNfe;
import org.example.stockhubspring.dto.RevisaoNfe.ItemRevisao;
import org.example.stockhubspring.dto.RevisaoNfe.MotivoSugestao;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.ImportacaoNfe;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.NotaFiscal;
import org.example.stockhubspring.model.NotaFiscalItem;
import org.example.stockhubspring.model.OrigemMovimentacao;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.SituacaoItemNfe;
import org.example.stockhubspring.model.StatusImportacao;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.nfe.ItemNfe;
import org.example.stockhubspring.nfe.NfeParser;
import org.example.stockhubspring.nfe.NotaNfe;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ImportacaoNfeRepository;
import org.example.stockhubspring.repository.NotaFiscalItemRepository;
import org.example.stockhubspring.repository.NotaFiscalItemRepository.VinculoDoCodigo;
import org.example.stockhubspring.repository.NotaFiscalRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.ProdutoRepository.NomeDoProduto;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.util.BuscaDifusa;
import org.example.stockhubspring.util.Paginas;
import org.example.stockhubspring.util.Restricoes;
import org.example.stockhubspring.util.Textos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Importação de NF-e de compra (RN-15 em diante): envio (guarda o XML até o dono confirmar), revisão,
 * confirmação e cancelamento. Tudo escopado pela loja (RN-01). A confirmação relê SEMPRE o XML guardado
 * (conferindo o SHA-256), valida TODAS as decisões antes de gravar e roda numa única transação: se qualquer item
 * falhar, nada é gravado. O XML é zerado quando a importação é encerrada.
 */
@Service
public class ImportacaoNfeService {

    private static final Logger LOG = LoggerFactory.getLogger(ImportacaoNfeService.class);

    public static final int VALIDADE_MINUTOS = 30;
    public static final int MAXIMO_PENDENTES = 5;
    public static final int TAMANHO_MAXIMO_PAGINA = 50;
    /** Nota mínima da busca difusa para sugerir um produto pelo nome. */
    public static final double NOTA_MINIMA_NOME = 0.8;
    /** Tempo máximo gasto comparando nomes por revisão (catálogo grande + nota grande); passou, o resto vira "novo". */
    static final long ORCAMENTO_NOMES_NS = 3_000_000_000L;
    /** Mensagem quando o XML guardado não confere com o SHA-256 gravado no envio. */
    public static final String MENSAGEM_INTEGRIDADE =
            "Os dados desta importação não conferem com o arquivo enviado. Envie o arquivo novamente.";

    private static final ZoneId FUSO_LOJA = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Sort ORDEM_RECENTES = Sort.by(Sort.Order.desc("importadaEm"), Sort.Order.desc("id"));
    /** Casa as duas UNIQUE de nota_fiscal (loja+chave e loja+fornecedor+modelo+série+número). */
    private static final String UK_NOTA = "uk_nota_fiscal_";
    private static final String UK_PARCEIRO = "uk_parceiro_usuario_documento";
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("9999999.99");

    /** Nota importada com os itens (produto e movimentação já carregados). */
    public record DetalheNota(NotaFiscal nota, List<NotaFiscalItem> itens) {
    }

    private record Sugestao(Produto produto, MotivoSugestao motivo) {
    }

    /** Decisão já validada de um item (primeiro passo da confirmação). */
    private record Plano(AcaoItemNfe acao, Produto produto, Categoria categoria, String nome, BigDecimal preco) {
        boolean entra() {
            return acao == AcaoItemNfe.VINCULAR || acao == AcaoItemNfe.CRIAR;
        }
    }

    private final ImportacaoNfeRepository importacaoRepository;
    private final NotaFiscalRepository notaRepository;
    private final NotaFiscalItemRepository itemRepository;
    private final ParceiroRepository parceiroRepository;
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstoqueService estoqueService;
    private long orcamentoNomesNs = ORCAMENTO_NOMES_NS;

    public ImportacaoNfeService(ImportacaoNfeRepository importacaoRepository,
                                NotaFiscalRepository notaRepository,
                                NotaFiscalItemRepository itemRepository,
                                ParceiroRepository parceiroRepository,
                                ProdutoRepository produtoRepository,
                                CategoriaRepository categoriaRepository,
                                UsuarioRepository usuarioRepository,
                                EstoqueService estoqueService) {
        this.importacaoRepository = importacaoRepository;
        this.notaRepository = notaRepository;
        this.itemRepository = itemRepository;
        this.parceiroRepository = parceiroRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.estoqueService = estoqueService;
    }

    /** Só para testes: troca o tempo máximo da comparação de nomes (0 desliga a sugestão por nome). */
    public void definirOrcamentoNomes(long nanossegundos) {
        this.orcamentoNomesNs = nanossegundos;
    }

    // -------------------------------------------------------------------------------------------
    // Consultas
    // -------------------------------------------------------------------------------------------

    /** Notas importadas da loja, mais recentes primeiro (página em base 1). */
    @Transactional(readOnly = true)
    public Page<NotaFiscal> listarNotas(Integer usuarioCodigo, int pagina, int tamanho) {
        return Paginas.buscar(pagina, tamanho, TAMANHO_MAXIMO_PAGINA, ORDEM_RECENTES,
                pageable -> notaRepository.findByUsuarioCodigo(usuarioCodigo, pageable));
    }

    /** Importações pendentes e ainda válidas da loja, para retomar a revisão. */
    @Transactional(readOnly = true)
    public List<PendenteNfe> listarPendentes(Integer usuarioCodigo) {
        return importacaoRepository.listarPendentes(usuarioCodigo, agora());
    }

    /** Nota importada da loja com os itens; de outra loja ou inexistente, responde "não encontrada" (RN-01). */
    @Transactional(readOnly = true)
    public DetalheNota detalhe(Integer id, Integer usuarioCodigo) {
        NotaFiscal nota = notaRepository.findByIdAndUsuarioCodigo(id, usuarioCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota não encontrada."));
        return new DetalheNota(nota,
                itemRepository.findByNotaFiscalIdAndNotaFiscalUsuarioCodigoOrderByNumeroItemAsc(id, usuarioCodigo));
    }

    // -------------------------------------------------------------------------------------------
    // Envio
    // -------------------------------------------------------------------------------------------

    /**
     * Valida o XML (tamanho, UTF-8, estrutura, regras de aceitação) e o guarda como importação PENDENTE por 30
     * minutos. Se a loja já tem uma pendente válida da mesma chave com o mesmo conteúdo (SHA-256), devolve o token
     * dela em vez de guardar outra cópia. Mantém no máximo {@value #MAXIMO_PENDENTES} pendentes por loja.
     *
     * @return o token (UUID aleatório) da importação, que só vale para esta loja
     */
    @Transactional
    public UUID enviar(byte[] conteudo, Usuario usuario) {
        Integer loja = usuario.getCodigo();
        NotaNfe nota;
        String xml;
        try {
            nota = NfeParser.ler(conteudo);
            xml = NfeParser.textoUtf8(conteudo);
        } catch (RegraNegocioException e) {
            LOG.warn("NF-e recusada no envio: loja={} motivo=\"{}\"", loja, e.getMessage());
            throw e;
        }
        try {
            recusarSeJaImportada(loja, nota.chaveAcesso());
        } catch (RegraNegocioException e) {
            LOG.warn("NF-e recusada no envio: loja={} chave={} motivo=ja-importada", loja, chaveCurta(nota.chaveAcesso()));
            throw e;
        }

        LocalDateTime agora = agora();
        // limpeza: vencidas da loja viram CANCELADA e o XML de toda importação encerrada é zerado
        importacaoRepository.cancelarVencidas(loja, agora);
        importacaoRepository.zerarXmlEncerradas();

        String sha = sha256(xml.getBytes(StandardCharsets.UTF_8));
        List<ImportacaoNfe> mesmaChave = importacaoRepository.pendentesDaChave(loja, nota.chaveAcesso(), agora);
        for (ImportacaoNfe existente : mesmaChave) {
            if (sha.equals(existente.getSha256() == null ? null : existente.getSha256().trim())) {
                LOG.info("NF-e reaproveitada (pendente igual): loja={} chave={}", loja, chaveCurta(nota.chaveAcesso()));
                return existente.getToken();
            }
        }
        if (!mesmaChave.isEmpty()) {
            // mesmo número de chave mas conteúdo diferente: a antiga sai de cena
            importacaoRepository.cancelar(loja, mesmaChave.stream().map(ImportacaoNfe::getId).toList());
        }
        List<Integer> pendentes = importacaoRepository.idsPendentesValidas(loja, agora);
        if (pendentes.size() >= MAXIMO_PENDENTES) {
            // abre espaço para a nova: cancela as mais antigas
            importacaoRepository.cancelar(loja, pendentes.subList(0, pendentes.size() - (MAXIMO_PENDENTES - 1)));
        }

        ImportacaoNfe importacao = new ImportacaoNfe();
        importacao.setUsuario(usuarioRepository.getReferenceById(loja));
        importacao.setToken(UUID.randomUUID());
        importacao.setXml(xml);
        importacao.setSha256(sha);
        importacao.setChaveAcesso(nota.chaveAcesso());
        importacao.setStatus(StatusImportacao.PENDENTE);
        importacao.setCriadaEm(agora);
        importacao.setExpiraEm(agora.plusMinutes(VALIDADE_MINUTOS));
        importacaoRepository.save(importacao);
        LOG.info("NF-e recebida para revisão: loja={} chave={} itens={}", loja, chaveCurta(nota.chaveAcesso()),
                nota.itens().size());
        return importacao.getToken();
    }

    // -------------------------------------------------------------------------------------------
    // Revisão
    // -------------------------------------------------------------------------------------------

    /** Monta a revisão a partir do XML guardado: cabeçalho, avisos e a sugestão de vínculo de cada item. */
    @Transactional(readOnly = true)
    public RevisaoNfe revisar(UUID token, Usuario usuario) {
        Integer loja = usuario.getCodigo();
        ImportacaoNfe importacao = importacaoRepository.buscar(token, loja)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação não encontrada."));
        validarPendente(importacao);
        NotaNfe nota = lerXmlGuardado(importacao);
        recusarSeJaImportada(loja, nota.chaveAcesso());

        Usuario dono = usuarioRepository.findById(loja)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação não encontrada."));
        List<String> avisos = new ArrayList<>();
        boolean lojaTemCnpj = dono.getCnpj() != null;
        Boolean destinatarioConfere = null;
        NotaNfe.Pessoa destinatario = nota.destinatario();
        if (destinatario == null || destinatario.documento() == null) {
            avisos.add("A nota não informa o CNPJ/CPF do destinatário. Confirme que ela é da sua loja.");
        } else if (lojaTemCnpj) {
            destinatarioConfere = dono.getCnpj().equals(destinatario.documento());
            if (!destinatarioConfere) {
                avisos.add("O destinatário da nota (documento " + destinatario.documento()
                        + ") é diferente do CNPJ cadastrado da sua loja. Só confirme se a nota é realmente sua.");
            }
        }
        if (!nota.assinaturaPresente()) {
            avisos.add("O arquivo não contém assinatura digital.");
        }
        if (!nota.protocoloPresente()) {
            avisos.add("O arquivo não traz o protocolo de autorização da SEFAZ (nota sem nfeProc).");
        }
        if (nota.emitidaEm() != null && nota.emitidaEm().isBefore(agora().minusMonths(12))) {
            avisos.add("A nota foi emitida em " + nota.emitidaEm().format(FORMATO_DATA).substring(0, 10)
                    + ", há mais de 12 meses. Confira se não é uma nota antiga que já foi lançada.");
        }
        avisos.add("Ao confirmar, o custo de cada produto é atualizado, e o código de barras (EAN) e o NCM da nota "
                + "são gravados nos produtos que ainda não os têm.");

        Parceiro fornecedor = nota.emitente().documento() == null ? null
                : parceiroRepository.findByUsuarioCodigoAndDocumento(loja, nota.emitente().documento()).orElse(null);

        Sugeridor sugeridor = new Sugeridor(loja, fornecedor, nota.itens());
        List<ItemRevisao> itens = new ArrayList<>();
        int importaveis = 0;
        for (ItemNfe item : nota.itens()) {
            String bloqueio = item.motivoBloqueio();
            SituacaoItemNfe situacao = bloqueio != null ? SituacaoItemNfe.IGNORADO
                    : item.fracionado() ? SituacaoItemNfe.FRACIONADO : SituacaoItemNfe.IMPORTADO;
            List<String> avisosItem = new ArrayList<>(item.avisos());
            Sugestao sugestao = null;
            boolean unidadeDiferente = false;
            if (situacao == SituacaoItemNfe.IMPORTADO) {
                importaveis++;
                sugestao = sugeridor.sugerir(item);
                if (sugestao != null) {
                    Produto sugerido = sugestao.produto();
                    if (!unidadeIgual(sugerido, item)) {
                        unidadeDiferente = true;
                        avisosItem.add("A unidade da nota (" + item.unidade() + ") é diferente da unidade do produto ("
                                + sugerido.getUnidade() + "). Não há conversão automática: confira a quantidade.");
                    }
                    if (sugerido.getEan() == null && item.ean() != null) {
                        avisosItem.add("O código de barras " + item.ean() + " será gravado no produto "
                                + "(ele ainda não tem um).");
                    }
                    if (sugerido.getNcm() == null && item.ncm() != null) {
                        avisosItem.add("O NCM " + item.ncm() + " será gravado no produto (ele ainda não tem um).");
                    }
                }
            }
            Produto sugerido = sugestao == null ? null : sugestao.produto();
            itens.add(new ItemRevisao(item.numero(), item.codigoFornecedor(), item.ean(), item.descricao(),
                    item.ncm(), item.cfop(), item.cfopEntrada(), item.unidade(), item.quantidade(),
                    item.valorUnitario(), item.valorBruto(), item.desconto(), item.custoUnitario(), situacao,
                    bloqueio != null, bloqueio, List.copyOf(avisosItem),
                    sugerido == null ? null : sugerido.getId(), sugerido == null ? null : sugerido.getNome(),
                    sugerido == null ? null : sugerido.getUnidade(),
                    sugestao == null ? MotivoSugestao.NENHUM : sugestao.motivo(), unidadeDiferente));
        }
        return new RevisaoNfe(importacao.getToken(), importacao.getExpiraEm(), nota, NotaNfe.AVISO_NAO_VERIFICADA,
                List.copyOf(avisos), lojaTemCnpj, destinatarioConfere, fornecedor != null, importaveis,
                List.copyOf(itens));
    }

    /**
     * Decisões iniciais sugeridas para o formulário de revisão: vincular quando há produto sugerido, senão criar
     * (nome da nota, preço inicial = custo arredondado a 2 casas); fracionados e recusados começam ignorados.
     */
    public ConfirmacaoNfeForm formularioInicial(RevisaoNfe revisao) {
        ConfirmacaoNfeForm form = new ConfirmacaoNfeForm();
        for (ItemRevisao item : revisao.itens()) {
            DecisaoItemNfeForm decisao = new DecisaoItemNfeForm();
            if (item.situacao() != SituacaoItemNfe.IMPORTADO) {
                decisao.setAcao(AcaoItemNfe.IGNORAR);
            } else if (item.produtoSugeridoId() != null) {
                decisao.setAcao(AcaoItemNfe.VINCULAR);
                decisao.setProdutoId(item.produtoSugeridoId());
            } else {
                decisao.setAcao(AcaoItemNfe.CRIAR);
                decisao.setNome(item.descricao());
                BigDecimal custo = item.custoUnitario();
                if (custo != null) {
                    BigDecimal preco = custo.setScale(2, RoundingMode.HALF_UP);
                    if (preco.signum() > 0 && preco.compareTo(PRECO_MAXIMO) <= 0) {
                        decisao.setPreco(preco);
                    }
                }
            }
            form.getItem().put(item.numero(), decisao);
        }
        return form;
    }

    // -------------------------------------------------------------------------------------------
    // Confirmação e cancelamento
    // -------------------------------------------------------------------------------------------

    /**
     * Confirma a importação numa ÚNICA transação: relê e revalida o XML guardado (conferindo o SHA-256; nunca dados
     * do navegador, só as decisões por item), valida TODAS as decisões antes de gravar qualquer coisa, trava os
     * produtos em ordem crescente de id (sem deadlock entre duas confirmações), cria/aproveita o fornecedor, grava a
     * nota e dá a ENTRADA de cada item com origem NFE_COMPRA. Qualquer falha desfaz tudo e a mensagem diz qual item.
     */
    @Transactional
    public NotaFiscal confirmar(UUID token, ConfirmacaoNfeForm form, Usuario usuario) {
        Integer loja = usuario.getCodigo();
        ImportacaoNfe importacao = importacaoRepository.buscarComBloqueio(token, loja)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação não encontrada."));
        validarPendente(importacao);
        NotaNfe nota = lerXmlGuardado(importacao);
        recusarSeJaImportada(loja, nota.chaveAcesso());

        if (form == null || !form.isConfirmouDestinatario()) {
            throw new RegraNegocioException("Confirme que a nota é destinada à sua loja marcando a caixa de confirmação.");
        }
        Map<Integer, DecisaoItemNfeForm> decisoes = form.getItem();
        for (Integer numero : decisoes.keySet()) {
            if (numero == null || numero < 1 || numero > nota.itens().size()) {
                throw new RegraNegocioException("A confirmação traz uma decisão para um item que não existe na nota.");
            }
        }

        // 1º passo: valida tudo, sem gravar nada
        Map<Integer, Plano> planos = planejar(nota, decisoes, loja);
        // 2º passo: trava os produtos que vão receber entrada, em ordem crescente de id
        travarProdutos(planos, loja);

        Usuario dono = usuarioRepository.getReferenceById(loja);
        Parceiro fornecedor = obterFornecedor(nota.emitente(), dono);
        NotaFiscal notaFiscal = gravarNota(nota, dono, fornecedor);

        String observacao = limitar("NF-e " + nota.numero() + " de " + fornecedor.getNome(),
                EstoqueService.OBSERVACAO_MAXIMA);
        int importados = 0;
        for (ItemNfe item : nota.itens()) {
            Plano plano = planos.get(item.numero());
            NotaFiscalItem registro = novoRegistro(notaFiscal, item);
            if (!plano.entra()) {
                registro.setSituacao(item.motivoBloqueio() == null && item.fracionado()
                        ? SituacaoItemNfe.FRACIONADO : SituacaoItemNfe.IGNORADO);
            } else {
                Produto produto = plano.acao() == AcaoItemNfe.VINCULAR ? plano.produto()
                        : criarProduto(plano, item, dono);
                MovimentacaoEstoque movimentacao = darEntrada(produto, item, observacao, usuario, notaFiscal);
                atualizarProduto(movimentacao.getProduto(), item);
                registro.setProduto(movimentacao.getProduto());
                registro.setMovimentacao(movimentacao);
                registro.setSituacao(SituacaoItemNfe.IMPORTADO);
                importados++;
            }
            itemRepository.save(registro);
        }

        importacao.setStatus(StatusImportacao.CONFIRMADA);
        importacao.setNotaFiscal(notaFiscal);
        importacao.setXml(null); // encerrada: o XML não fica guardado
        importacaoRepository.save(importacao);
        LOG.info("NF-e importada: loja={} chave={} itensNoEstoque={}", loja, chaveCurta(nota.chaveAcesso()), importados);
        return notaFiscal;
    }

    /** Cancela a importação pendente da loja (idempotente; confirmada não pode ser cancelada). */
    @Transactional
    public void cancelar(UUID token, Usuario usuario) {
        ImportacaoNfe importacao = importacaoRepository.buscar(token, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Importação não encontrada."));
        if (importacao.getStatus() == StatusImportacao.CONFIRMADA) {
            throw new RegraNegocioException("Esta importação já foi confirmada e não pode ser cancelada.");
        }
        if (importacao.getStatus() == StatusImportacao.PENDENTE) {
            importacao.setStatus(StatusImportacao.CANCELADA);
            importacao.setXml(null);
            importacaoRepository.save(importacao);
            LOG.info("NF-e cancelada: loja={} chave={}", usuario.getCodigo(), chaveCurta(importacao.getChaveAcesso()));
        }
    }

    // -------------------------------------------------------------------------------------------
    // Passos internos da confirmação
    // -------------------------------------------------------------------------------------------

    /** Valida todas as decisões (produto, categoria, nome, preço) sem gravar nada; devolve o plano por item. */
    private Map<Integer, Plano> planejar(NotaNfe nota, Map<Integer, DecisaoItemNfeForm> decisoes, Integer loja) {
        // produtos a vincular, todos numa consulta (de outra loja, inativo ou inexistente: fora do mapa)
        Set<Integer> idsProdutos = new HashSet<>();
        for (ItemNfe item : nota.itens()) {
            DecisaoItemNfeForm d = decisoes.get(item.numero());
            if (d != null && d.getAcao() == AcaoItemNfe.VINCULAR && d.getProdutoId() != null
                    && item.motivoBloqueio() == null && !item.fracionado()) {
                idsProdutos.add(d.getProdutoId());
            }
        }
        Map<Integer, Produto> produtos = new HashMap<>();
        if (!idsProdutos.isEmpty()) {
            for (Produto p : produtoRepository.findByUsuarioCodigoAndAtivoTrueAndIdIn(loja, idsProdutos)) {
                produtos.put(p.getId(), p);
            }
        }
        Map<Integer, Categoria> categorias = new HashMap<>();
        Set<String> nomesNovos = new HashSet<>();
        Map<Integer, Plano> planos = new LinkedHashMap<>();
        int entram = 0;
        for (ItemNfe item : nota.itens()) {
            DecisaoItemNfeForm d = decisoes.get(item.numero());
            AcaoItemNfe acao = d == null ? null : d.getAcao();
            String bloqueio = item.motivoBloqueio();
            Plano plano;
            if (bloqueio != null) {
                if (acao != null && acao != AcaoItemNfe.IGNORAR) {
                    throw new RegraNegocioException(prefixo(item) + "este item não pode ser importado ("
                            + bloqueio + ") Escolha ignorar. Nada foi importado.");
                }
                plano = new Plano(AcaoItemNfe.IGNORAR, null, null, null, null);
            } else if (item.fracionado()) {
                // O saldo é inteiro: o item fica registrado, mas não entra no estoque (lançar à mão)
                plano = new Plano(AcaoItemNfe.IGNORAR, null, null, null, null);
            } else if (acao == null) {
                throw new RegraNegocioException(prefixo(item)
                        + "informe o que fazer com este item (vincular, criar ou ignorar). Nada foi importado.");
            } else if (acao == AcaoItemNfe.IGNORAR) {
                plano = new Plano(AcaoItemNfe.IGNORAR, null, null, null, null);
            } else if (acao == AcaoItemNfe.VINCULAR) {
                // inexistente, inativo ou de outra loja respondem igual (RN-01: nunca vazar existência)
                Produto produto = d.getProdutoId() == null ? null : produtos.get(d.getProdutoId());
                if (d.getProdutoId() == null) {
                    throw new RegraNegocioException(prefixo(item) + "selecione o produto. Nada foi importado.");
                }
                if (produto == null) {
                    throw new RegraNegocioException(prefixo(item)
                            + "o produto selecionado não foi encontrado. Escolha outro. Nada foi importado.");
                }
                plano = new Plano(acao, produto, null, null, null);
            } else {
                plano = planejarCriacao(d, item, loja, categorias, nomesNovos);
            }
            if (plano.entra()) {
                entram++;
            }
            planos.put(item.numero(), plano);
        }
        if (entram == 0) {
            // Sem isto, ignorar tudo "gastaria" a chave e a nota não poderia mais ser importada
            throw new RegraNegocioException("Nenhum item foi marcado para entrar no estoque. Vincule ou crie ao menos "
                    + "um produto, ou cancele a importação.");
        }
        return planos;
    }

    private Plano planejarCriacao(DecisaoItemNfeForm d, ItemNfe item, Integer loja,
                                  Map<Integer, Categoria> categorias, Set<String> nomesNovos) {
        String nome = Textos.limpar(d.getNome());
        if (nome == null || nome.length() < 2 || nome.length() > 120) {
            throw new RegraNegocioException(prefixo(item)
                    + "o nome do novo produto deve ter entre 2 e 120 caracteres. Nada foi importado.");
        }
        if (d.getCategoriaId() == null) {
            throw new RegraNegocioException(prefixo(item) + "selecione a categoria do novo produto. Nada foi importado.");
        }
        Categoria categoria = categorias.get(d.getCategoriaId());
        if (categoria == null) {
            categoria = categoriaRepository.findByIdAndUsuarioCodigo(d.getCategoriaId(), loja)
                    .orElseThrow(() -> new RegraNegocioException(prefixo(item)
                            + "categoria não encontrada. Selecione outra. Nada foi importado."));
            categorias.put(categoria.getId(), categoria);
        }
        BigDecimal preco = d.getPreco();
        if (preco == null || preco.signum() <= 0 || preco.compareTo(PRECO_MAXIMO) > 0
                || preco.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException(prefixo(item)
                    + "informe o preço de venda do novo produto (maior que zero, até 9.999.999,99). Nada foi importado.");
        }
        // RN-10: nome único entre os produtos ativos da loja (e entre os itens novos desta mesma nota)
        if (!nomesNovos.add(Textos.normalizarNome(nome)) || produtoRepository.contarNomeAtivo(loja, nome, -1) > 0) {
            throw new RegraNegocioException(prefixo(item) + "já existe um produto ativo chamado \"" + nome
                    + "\". Vincule a ele ou use outro nome. Nada foi importado.");
        }
        return new Plano(AcaoItemNfe.CRIAR, null, categoria, nome, preco.setScale(2, RoundingMode.UNNECESSARY));
    }

    /** Trava os produtos a vincular em ordem crescente de id: duas confirmações nunca se travam em ordem inversa. */
    private void travarProdutos(Map<Integer, Plano> planos, Integer loja) {
        Set<Integer> ids = new TreeSet<>();
        for (Plano plano : planos.values()) {
            if (plano.acao() == AcaoItemNfe.VINCULAR) {
                ids.add(plano.produto().getId());
            }
        }
        for (Integer id : ids) {
            produtoRepository.buscarAtivoComBloqueio(id, loja).orElseThrow(() -> new RegraNegocioException(
                    "Um dos produtos selecionados não foi encontrado. Escolha outro. Nada foi importado."));
        }
    }

    private Parceiro obterFornecedor(NotaNfe.Pessoa emitente, Usuario dono) {
        Parceiro parceiro = parceiroRepository.findByUsuarioCodigoAndDocumento(dono.getCodigo(), emitente.documento())
                .orElse(null);
        if (parceiro == null) {
            parceiro = new Parceiro();
            parceiro.setUsuario(dono);
            parceiro.setDocumento(emitente.documento());
            parceiro.setCriadoEm(agora());
            // nome, fantasia e UF só entram na criação: um XML nunca reescreve o cadastro de quem já existe
            parceiro.setNome(emitente.nome());
            parceiro.setNomeFantasia(emitente.fantasia());
            parceiro.setUf(emitente.uf());
        }
        parceiro.setFornecedor(true);
        try {
            return parceiroRepository.saveAndFlush(parceiro);
        } catch (DataIntegrityViolationException e) {
            if (Restricoes.violou(e, UK_PARCEIRO)) {
                throw new RegraNegocioException("Outra importação do mesmo fornecedor está em andamento. "
                        + "Tente confirmar de novo.");
            }
            throw new RegraNegocioException(Restricoes.MENSAGEM_GENERICA);
        }
    }

    private NotaFiscal gravarNota(NotaNfe nota, Usuario dono, Parceiro fornecedor) {
        NotaFiscal notaFiscal = new NotaFiscal();
        notaFiscal.setUsuario(dono);
        notaFiscal.setChaveAcesso(nota.chaveAcesso());
        notaFiscal.setModelo(nota.modelo());
        notaFiscal.setSerie(nota.serie());
        notaFiscal.setNumero(nota.numero());
        notaFiscal.setNaturezaOperacao(nota.naturezaOperacao());
        notaFiscal.setEmitidaEm(nota.emitidaEm());
        notaFiscal.setParceiro(fornecedor);
        notaFiscal.setCnpjDestinatario(nota.destinatario() == null ? null : nota.destinatario().documento());
        notaFiscal.setValorProdutos(nota.valorProdutos());
        notaFiscal.setValorFrete(nota.valorFrete());
        notaFiscal.setValorDesconto(nota.valorDesconto());
        notaFiscal.setValorTotal(nota.valorTotal());
        notaFiscal.setImportadaEm(agora());
        try {
            // saveAndFlush: se outra confirmação gravou a mesma nota agora, a UNIQUE acusa aqui (idempotência)
            return notaRepository.saveAndFlush(notaFiscal);
        } catch (DataIntegrityViolationException e) {
            if (Restricoes.violou(e, UK_NOTA)) {
                throw new RegraNegocioException("Esta nota já foi importada nesta loja.");
            }
            throw new RegraNegocioException(Restricoes.MENSAGEM_GENERICA);
        }
    }

    private static NotaFiscalItem novoRegistro(NotaFiscal notaFiscal, ItemNfe item) {
        NotaFiscalItem registro = new NotaFiscalItem();
        registro.setNotaFiscal(notaFiscal);
        registro.setNumeroItem(item.numero());
        registro.setCodigoFornecedor(item.codigoFornecedor());
        registro.setEan(item.ean());
        registro.setDescricao(item.descricao());
        registro.setNcm(item.ncm());
        registro.setCfop(item.cfop());
        registro.setCfopEntrada(item.cfopEntrada());
        registro.setUnidade(item.unidade());
        registro.setQuantidade(item.quantidade());
        registro.setValorUnitario(item.valorUnitario());
        registro.setValorTotal(item.valorBruto());
        return registro;
    }

    private Produto criarProduto(Plano plano, ItemNfe item, Usuario dono) {
        Produto produto = new Produto();
        produto.setUsuario(dono);
        produto.setNome(plano.nome());
        produto.setPreco(plano.preco());
        produto.setCategoria(plano.categoria());
        produto.setQuantidade(0); // o saldo só nasce por movimentação (RN-03): a ENTRADA vem logo depois
        produto.setEstoqueMinimo(0);
        produto.setAtivo(true);
        produto.setCusto(item.custoUnitario());
        produto.setUnidade(item.unidade());
        produto.setEan(item.ean());
        produto.setNcm(item.ncm());
        try {
            return produtoRepository.saveAndFlush(produto);
        } catch (DataIntegrityViolationException e) {
            if (Restricoes.violou(e, Restricoes.UX_PRODUTO_NOME_ATIVO)) {
                throw new RegraNegocioException(prefixo(item) + "já existe um produto ativo chamado \""
                        + plano.nome() + "\". Nada foi importado.");
            }
            throw new RegraNegocioException(Restricoes.MENSAGEM_GENERICA);
        }
    }

    private MovimentacaoEstoque darEntrada(Produto produto, ItemNfe item, String observacao, Usuario usuario,
                                           NotaFiscal notaFiscal) {
        try {
            return estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, item.quantidadeInteira(),
                    observacao, usuario, OrigemMovimentacao.NFE_COMPRA, notaFiscal.getId(),
                    unidadeIgual(produto, item) ? item.custoUnitario() : null);
        } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
            throw new RegraNegocioException(prefixo(item) + e.getMessage() + " Nada foi importado.");
        }
    }

    /**
     * Último custo, e EAN/NCM só quando o produto ainda não tem. O custo só é atualizado se a unidade da nota for a
     * do produto (sem conversão nesta versão: custo por caixa não pode virar custo por unidade).
     */
    private void atualizarProduto(Produto produto, ItemNfe item) {
        if (unidadeIgual(produto, item)) {
            produto.setCusto(item.custoUnitario());
        }
        if (produto.getEan() == null && item.ean() != null) {
            produto.setEan(item.ean());
        }
        if (produto.getNcm() == null && item.ncm() != null) {
            produto.setNcm(item.ncm());
        }
        produtoRepository.save(produto);
    }

    // -------------------------------------------------------------------------------------------
    // Sugestão de vínculo
    // -------------------------------------------------------------------------------------------

    /**
     * Calcula a sugestão item a item: 1º EAN, 2º memória do fornecedor, 3º nome parecido (nota >= 0,8). EANs e
     * memória do fornecedor são buscados em LOTE (uma consulta cada) no início; o tempo da comparação de nomes é
     * limitado dentro do próprio laço.
     */
    private final class Sugeridor {
        private final Integer loja;
        private final long limite = System.nanoTime() + orcamentoNomesNs;
        private final Map<String, Produto> porEan = new HashMap<>();
        private final Map<String, Produto> porCodigo = new HashMap<>();
        private final Map<Integer, Produto> cache = new HashMap<>();
        private List<NomeDoProduto> nomes;
        private boolean estourou;

        Sugeridor(Integer loja, Parceiro fornecedor, List<ItemNfe> itens) {
            this.loja = loja;
            Set<String> eans = new HashSet<>();
            Set<String> codigos = new HashSet<>();
            for (ItemNfe item : itens) {
                if (item.motivoBloqueio() == null && !item.fracionado()) {
                    if (item.ean() != null) {
                        eans.add(item.ean());
                    }
                    codigos.add(item.codigoFornecedor());
                }
            }
            if (!eans.isEmpty()) {
                // ordenado por id: o produto mais antigo de cada EAN vence
                for (Produto p : produtoRepository.findByUsuarioCodigoAndAtivoTrueAndEanInOrderByIdAsc(loja, eans)) {
                    porEan.putIfAbsent(p.getEan(), p);
                }
            }
            if (fornecedor != null && !codigos.isEmpty()) {
                // mais recente primeiro: o vínculo mais novo de cada código vence
                Map<String, Integer> vinculo = new LinkedHashMap<>();
                for (VinculoDoCodigo v : itemRepository.vinculosDosCodigos(loja, fornecedor.getId(), codigos,
                        PageRequest.of(0, ProdutoService.BUSCA_TOLERANTE_MAXIMO_NOMES))) {
                    vinculo.putIfAbsent(v.getCodigo(), v.getProdutoId());
                }
                if (!vinculo.isEmpty()) {
                    Map<Integer, Produto> carregados = new HashMap<>();
                    for (Produto p : produtoRepository.findByUsuarioCodigoAndAtivoTrueAndIdIn(loja,
                            new HashSet<>(vinculo.values()))) {
                        carregados.put(p.getId(), p);
                    }
                    vinculo.forEach((codigo, id) -> {
                        Produto p = carregados.get(id);
                        if (p != null) {
                            porCodigo.put(codigo, p);
                        }
                    });
                }
            }
        }

        Sugestao sugerir(ItemNfe item) {
            Produto p = item.ean() == null ? null : porEan.get(item.ean());
            if (p != null) {
                return new Sugestao(p, MotivoSugestao.EAN);
            }
            p = porCodigo.get(item.codigoFornecedor());
            if (p != null) {
                return new Sugestao(p, MotivoSugestao.CODIGO_FORNECEDOR);
            }
            return porNome(item);
        }

        private Sugestao porNome(ItemNfe item) {
            if (estourou || System.nanoTime() >= limite) {
                estourou = true;
                return null;
            }
            if (nomes == null) {
                nomes = produtoRepository.listarNomesAtivos(loja,
                        PageRequest.of(0, ProdutoService.BUSCA_TOLERANTE_MAXIMO_NOMES));
            }
            Integer melhorId = null;
            double melhorNota = 0;
            int contador = 0;
            for (NomeDoProduto nome : nomes) {
                // o orçamento é conferido DENTRO do laço: um item com catálogo enorme não passa do limite
                if ((++contador & 63) == 0 && System.nanoTime() >= limite) {
                    estourou = true;
                    break;
                }
                // as duas direções: o nome cadastrado dentro da descrição da nota e o contrário
                double nota = Math.max(BuscaDifusa.pontuar(item.descricao(), nome.getNome()),
                        BuscaDifusa.pontuar(nome.getNome(), item.descricao()));
                if (nota >= NOTA_MINIMA_NOME && nota > melhorNota) {
                    melhorNota = nota;
                    melhorId = nome.getId();
                }
            }
            if (melhorId == null) {
                return null;
            }
            Produto escolhido = cache.get(melhorId);
            if (escolhido == null) {
                escolhido = produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(melhorId, loja).orElse(null);
                if (escolhido != null) {
                    cache.put(melhorId, escolhido);
                }
            }
            return escolhido == null ? null : new Sugestao(escolhido, MotivoSugestao.NOME);
        }
    }

    // -------------------------------------------------------------------------------------------
    // Utilitários
    // -------------------------------------------------------------------------------------------

    /** Relê o XML guardado, conferindo antes o SHA-256 gravado no envio (detecta adulteração do que foi guardado). */
    private NotaNfe lerXmlGuardado(ImportacaoNfe importacao) {
        String xml = importacao.getXml();
        String esperado = importacao.getSha256() == null ? null : importacao.getSha256().trim();
        if (xml == null || esperado == null || !esperado.equals(sha256(xml.getBytes(StandardCharsets.UTF_8)))) {
            throw new RegraNegocioException(MENSAGEM_INTEGRIDADE);
        }
        return NfeParser.lerTexto(xml);
    }

    private void recusarSeJaImportada(Integer loja, String chave) {
        notaRepository.findByUsuarioCodigoAndChaveAcesso(loja, chave).ifPresent(existente -> {
            throw new RegraNegocioException("Esta nota já foi importada em "
                    + existente.getImportadaEm().format(FORMATO_DATA) + ".");
        });
    }

    private void validarPendente(ImportacaoNfe importacao) {
        if (importacao.getStatus() != StatusImportacao.PENDENTE) {
            throw new RegraNegocioException("Esta importação já foi concluída ou cancelada.");
        }
        if (!importacao.getExpiraEm().isAfter(agora())) {
            throw new RegraNegocioException("Esta importação expirou (" + VALIDADE_MINUTOS
                    + " minutos). Envie o arquivo novamente.");
        }
    }

    private static boolean unidadeIgual(Produto produto, ItemNfe item) {
        return produto.getUnidade() != null && produto.getUnidade().equalsIgnoreCase(item.unidade());
    }

    private static String prefixo(ItemNfe item) {
        return "Item " + item.numero() + " (" + limitar(item.descricao(), 40) + "): ";
    }

    private static String limitar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo).trim();
    }

    /** Só os 8 últimos dígitos da chave vão para o log (suficiente para correlacionar, sem expor a chave inteira). */
    static String chaveCurta(String chave) {
        return chave == null || chave.length() <= 8 ? "?" : "..." + chave.substring(chave.length() - 8);
    }

    private static LocalDateTime agora() {
        return LocalDateTime.now(FUSO_LOJA);
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
