package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.dto.SaldoProduto;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.ReservaEstoque;
import org.example.stockhubspring.model.StatusReserva;
import org.example.stockhubspring.model.TipoDocumento;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.MovimentacaoSpecs;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.ReservaEstoqueRepository;
import org.example.stockhubspring.util.Paginas;
import org.example.stockhubspring.util.Textos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Único ponto que altera o saldo de um produto (RN-03). Cada movimentação lê o produto com
 * bloqueio pessimista, calcula e grava saldo e registro na mesma transação (RN-07).
 */
@Service
public class EstoqueService {

    /** Motivo gravado na entrada gerada ao cadastrar um produto com saldo (RN-04). */
    public static final String MOTIVO_ESTOQUE_INICIAL = "Estoque inicial";
    /** Limite de uma movimentação de ENTRADA ou SAIDA (RN-05). */
    public static final int QUANTIDADE_MAXIMA_MOVIMENTACAO = 1_000_000;
    /** Maior saldo permitido, bem abaixo do limite do int (RN-05). */
    public static final int SALDO_MAXIMO = 2_000_000_000;
    public static final int OBSERVACAO_MAXIMA = 255;
    /** Motivo de um estorno; com o prefixo "Estorno: " cabe na observação de 255 caracteres. */
    public static final int MOTIVO_ESTORNO_MAXIMO = 200;
    static final String MENSAGEM_JA_DESFEITA = "Esta movimentação já foi desfeita.";
    public static final int TAMANHO_MAXIMO_PAGINA = 100;
    /** Máximo de linhas de uma exportação CSV do histórico (proteção de memória). */
    public static final int LIMITE_EXPORTACAO = 10_000;

    private static final Sort ORDEM_RECENTES = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ReservaEstoqueRepository reservaRepository;

    public EstoqueService(ProdutoRepository produtoRepository,
                          MovimentacaoEstoqueRepository movimentacaoRepository,
                          ReservaEstoqueRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
        this.produtoRepository = produtoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    /** Histórico da loja sem filtros, do mais recente para o mais antigo (página em base 1). */
    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> listarMovimentacoes(Integer usuarioCodigo, int pagina, int tamanho) {
        return listarMovimentacoes(usuarioCodigo, MovimentacaoFiltro.VAZIO, pagina, tamanho);
    }

    /**
     * Histórico da loja com filtros opcionais (produto, tipo e período), feitos no banco e sempre
     * escopados à loja (RN-01). Do mais recente para o mais antigo (página em base 1).
     */
    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> listarMovimentacoes(Integer usuarioCodigo, MovimentacaoFiltro filtro,
                                                         int pagina, int tamanho) {
        Specification<MovimentacaoEstoque> spec = MovimentacaoSpecs.comFiltro(usuarioCodigo, filtro);
        return Paginas.buscar(pagina, tamanho, TAMANHO_MAXIMO_PAGINA, ORDEM_RECENTES,
                pageable -> movimentacaoRepository.findAll(spec, pageable));
    }

    /**
     * Movimentações para exportação: mesmos filtros do histórico, sem paginação, no máximo
     * {@link #LIMITE_EXPORTACAO} linhas (as mais recentes). Se {@code getTotalElements()} for maior
     * que {@code getNumberOfElements()}, a exportação foi truncada pelo limite.
     */
    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> listarParaExportar(Integer usuarioCodigo, MovimentacaoFiltro filtro) {
        return movimentacaoRepository.findAll(MovimentacaoSpecs.comFiltro(usuarioCodigo, filtro),
                PageRequest.of(0, LIMITE_EXPORTACAO, ORDEM_RECENTES));
    }

    /**
     * Últimas movimentações de um produto da loja, da mais recente para a mais antiga. O chamador deve
     * ter confirmado que o produto é da loja; a consulta também filtra pela loja (RN-01).
     */
    @Transactional(readOnly = true)
    public List<MovimentacaoEstoque> ultimasDoProduto(Integer produtoId, Integer usuarioCodigo, int limite) {
        int tamanho = Math.max(1, Math.min(limite, TAMANHO_MAXIMO_PAGINA));
        return movimentacaoRepository.findByProdutoIdAndUsuarioCodigo(produtoId, usuarioCodigo,
                PageRequest.of(0, tamanho, ORDEM_RECENTES));
    }


    /**
     * Registra uma movimentação e atualiza o saldo (RN-02 a RN-07).
     * <ul>
     *   <li>ENTRADA: soma; SAIDA: subtrai e nunca deixa o saldo negativo;</li>
     *   <li>AJUSTE: {@code quantidade} é a contagem FINAL e o motivo (observação) é obrigatório;</li>
     *   <li>nada que baixe o saldo pode consumir o que está reservado (RN-72).</li>
     * </ul>
     *
     * @throws RegraNegocioException         se alguma regra for violada (nada é gravado)
     * @throws RecursoNaoEncontradoException se o produto não existe, está inativo ou é de outra loja
     */
    @Transactional
    public MovimentacaoEstoque movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario) {
        return movimentar(produtoId, tipo, quantidade, observacao, usuario, null, null);
    }

    /**
     * Igual a {@link #movimentar(Integer, TipoMovimentacao, int, String, Usuario)}, registrando o documento
     * que originou a movimentação (ex.: COMPRA_NFE e o id da nota). Sem tipo e sem código, é lançamento
     * MANUAL. Compra, venda e OS exigem o código do documento (RN-67).
     */
    @Transactional
    public MovimentacaoEstoque movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario,
                                          TipoDocumento tipoDocumento, Integer documentoId) {
        return movimentar(produtoId, tipo, quantidade, observacao, usuario, tipoDocumento, documentoId, null);
    }

    /**
     * Igual à anterior, gravando na movimentação o custo unitário informado (ex.: o custo da nota na entrada
     * por NF-e). Sem custo informado, grava o custo atual do produto (RN-45).
     */
    @Transactional
    public MovimentacaoEstoque movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario,
                                          TipoDocumento tipoDocumento, Integer documentoId,
                                          BigDecimal custoUnitario) {
        return movimentar(produtoId, tipo, quantidade, observacao, usuario, tipoDocumento, documentoId,
                custoUnitario, null);
    }

    /** Igual à anterior, gravando também o preço de venda praticado (RN-68). */
    @Transactional
    public MovimentacaoEstoque movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario,
                                          TipoDocumento tipoDocumento, Integer documentoId,
                                          BigDecimal custoUnitario, BigDecimal precoUnitario) {
        if (custoUnitario != null && custoUnitario.signum() < 0) {
            throw new RegraNegocioException("O custo não pode ser negativo.");
        }
        if (precoUnitario != null && precoUnitario.signum() < 0) {
            throw new RegraNegocioException("O preço não pode ser negativo.");
        }
        validarDocumento(tipoDocumento, documentoId);
        // RN-08: o motivo "Estoque inicial" identifica o único registro que pode ser apagado junto
        // com o produto; por isso só o sistema o grava (ver registrarEstoqueInicial).
        if (ehMotivoReservado(observacao)) {
            throw new RegraNegocioException("O motivo \"" + MOTIVO_ESTOQUE_INICIAL + "\" é reservado ao sistema "
                    + "(é gerado ao cadastrar um produto com saldo). Informe outro motivo.");
        }
        return registrar(new Pedido(produtoId, tipo, quantidade, observacao, usuario,
                tipoDocumento, documentoId, custoUnitario, precoUnitario, null));
    }

    /**
     * Caminho interno, usado só pelo {@code ProdutoService} ao cadastrar um produto com saldo (RN-04):
     * grava a ENTRADA com o motivo reservado. Não é alcançado por nenhum formulário.
     */
    @Transactional
    public MovimentacaoEstoque registrarEstoqueInicial(Integer produtoId, int quantidade, Usuario usuario) {
        return registrar(new Pedido(produtoId, TipoMovimentacao.ENTRADA, quantidade, MOTIVO_ESTOQUE_INICIAL,
                usuario, null, null, null, null, null));
    }

    /** O motivo é o texto reservado do estoque inicial? Ignora maiúsculas e espaços nas pontas. */
    public static boolean ehMotivoReservado(String observacao) {
        return observacao != null
                && Textos.normalizarNome(observacao).equals(Textos.normalizarNome(MOTIVO_ESTOQUE_INICIAL));
    }

    // ------------------------------------------------------------------------------------------
    // Estorno (RN-69 a RN-71)
    // ------------------------------------------------------------------------------------------

    /**
     * Desfaz uma movimentação gerando a contrária, que aponta para a original (RN-69). A original não é
     * alterada. Só dono e gerente; o motivo é obrigatório; cada movimentação é desfeita uma só vez.
     * Respeita o saldo nunca negativo e o que está reservado.
     */
    @Transactional
    public MovimentacaoEstoque estornar(Integer movimentacaoId, String motivo, Usuario usuario) {
        if (usuario.getPapel() == null || !usuario.getPapel().gerenciaLoja()) {
            throw new RegraNegocioException("Só o dono ou o gerente da loja pode desfazer uma movimentação.");
        }
        String texto = Textos.limpar(motivo);
        if (texto == null) {
            throw new RegraNegocioException("Informe o motivo de desfazer a movimentação.");
        }
        if (texto.length() > MOTIVO_ESTORNO_MAXIMO) {
            throw new RegraNegocioException("O motivo deve ter no máximo " + MOTIVO_ESTORNO_MAXIMO + " caracteres.");
        }
        MovimentacaoEstoque original = movimentacaoRepository
                .findByIdAndUsuarioCodigo(movimentacaoId, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Movimentação não encontrada."));
        String impedimento = impedimentoDeEstorno(original);
        if (impedimento != null) {
            throw new RegraNegocioException(impedimento);
        }

        TipoMovimentacao contrario;
        int quantidade;
        if (original.getTipo() == TipoMovimentacao.AJUSTE) {
            // O ajuste é desfeito pela diferença que ele causou, e não pela contagem (outras movimentações podem ter vindo depois)
            int diferenca = original.getQuantidadeNova() - original.getQuantidadeAnterior();
            contrario = diferenca > 0 ? TipoMovimentacao.SAIDA : TipoMovimentacao.ENTRADA;
            quantidade = Math.abs(diferenca);
        } else {
            contrario = original.getTipo() == TipoMovimentacao.ENTRADA ? TipoMovimentacao.SAIDA : TipoMovimentacao.ENTRADA;
            quantidade = original.getQuantidade();
        }
        return registrar(new Pedido(original.getProduto().getId(), contrario, quantidade,
                "Estorno: " + texto, usuario, original.getTipoDocumento(), original.getDocumentoId(),
                original.getCustoUnitario(), original.getPrecoUnitario(), original.getId()));
    }

    /** Por que a movimentação não pode ser desfeita (null se pode). Não confere o papel de quem pede. */
    private String impedimentoDeEstorno(MovimentacaoEstoque mov) {
        if (mov.getEstornoDeId() != null) {
            return "Um estorno não pode ser desfeito. Se precisar, faça um novo lançamento.";
        }
        if (mov.getTipo() == TipoMovimentacao.ENTRADA && mov.getQuantidadeAnterior() == 0
                && MOTIVO_ESTOQUE_INICIAL.equals(mov.getObservacao())) {
            return "O estoque inicial não pode ser desfeito. Corrija o saldo com um ajuste.";
        }
        if (mov.getTipo() == TipoMovimentacao.AJUSTE && mov.getQuantidadeNova() == mov.getQuantidadeAnterior()) {
            return "Este ajuste não mudou o saldo, não há o que desfazer.";
        }
        if (movimentacaoRepository.existsByEstornoDeId(mov.getId())) {
            return MENSAGEM_JA_DESFEITA;
        }
        return null;
    }

    /** Dos códigos dados, os que já foram desfeitos (para marcar o histórico). */
    @Transactional(readOnly = true)
    public Set<Integer> jaDesfeitas(Collection<MovimentacaoEstoque> movimentacoes) {
        List<Integer> ids = movimentacoes.stream().map(MovimentacaoEstoque::getId).toList();
        if (ids.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(movimentacaoRepository.idsEstornados(ids));
    }

    /**
     * Das movimentações dadas, as que o usuário pode desfazer agora: precisa ser dono ou gerente e a
     * movimentação não pode ser estorno, estoque inicial, ajuste sem efeito nem já estar desfeita.
     * É só para esconder o botão; quem decide é {@link #estornar}.
     */
    @Transactional(readOnly = true)
    public Set<Integer> estornaveis(Collection<MovimentacaoEstoque> movimentacoes, Usuario usuario) {
        if (usuario.getPapel() == null || !usuario.getPapel().gerenciaLoja()) {
            return Set.of();
        }
        Set<Integer> desfeitas = jaDesfeitas(movimentacoes);
        Set<Integer> resultado = new HashSet<>();
        for (MovimentacaoEstoque mov : movimentacoes) {
            if (mov.getEstornoDeId() == null && !desfeitas.contains(mov.getId())
                    && impedimentoDeEstorno(mov) == null) {
                resultado.add(mov.getId());
            }
        }
        return resultado;
    }

    // ------------------------------------------------------------------------------------------
    // Reserva de estoque (RN-72 a RN-75)
    // ------------------------------------------------------------------------------------------

    /** Em estoque, reservado e disponível de um produto da loja (RN-72). */
    @Transactional(readOnly = true)
    public SaldoProduto saldoDoProduto(Produto produto, Integer usuarioCodigo) {
        long reservado = reservaRepository.somarAtivas(produto.getId(), usuarioCodigo);
        return SaldoProduto.de(produto.getQuantidade(), reservado);
    }

    /**
     * Separa {@code quantidade} do produto para um documento (venda ou OS). Só reserva o que está disponível
     * (saldo menos as outras reservas ativas). Não muda o saldo.
     */
    @Transactional
    public ReservaEstoque reservar(Integer produtoId, int quantidade, TipoDocumento documentoTipo,
                                   Integer documentoId, Usuario usuario) {
        if (documentoTipo != TipoDocumento.VENDA && documentoTipo != TipoDocumento.OS) {
            throw new RegraNegocioException("Só venda ou ordem de serviço reservam estoque.");
        }
        if (documentoId == null) {
            throw new RegraNegocioException("Informe o código do documento da reserva.");
        }
        if (quantidade <= 0 || quantidade > QUANTIDADE_MAXIMA_MOVIMENTACAO) {
            throw new RegraNegocioException("A quantidade reservada deve estar entre 1 e "
                    + formatar(QUANTIDADE_MAXIMA_MOVIMENTACAO) + ".");
        }
        Produto produto = produtoRepository.buscarAtivoComBloqueio(produtoId, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        SaldoProduto saldo = SaldoProduto.de(produto.getQuantidade(),
                reservaRepository.somarAtivas(produtoId, usuario.getCodigo()));
        if (quantidade > saldo.disponivel()) {
            throw new RegraNegocioException("Só há " + formatar(saldo.disponivel()) + " unidade(s) disponíveis de \""
                    + produto.getNome() + "\" (" + formatar(saldo.emEstoque()) + " em estoque, "
                    + formatar(saldo.reservado()) + " reservadas). Nada foi reservado.");
        }
        ReservaEstoque reserva = new ReservaEstoque();
        reserva.setUsuario(usuario);
        reserva.setProduto(produto);
        reserva.setQuantidade(quantidade);
        reserva.setDocumentoTipo(documentoTipo);
        reserva.setDocumentoId(documentoId);
        return reservaRepository.saveAndFlush(reserva);
    }

    /** Cancela uma reserva ativa: a quantidade volta a ficar disponível. */
    @Transactional
    public ReservaEstoque liberar(Integer reservaId, Usuario usuario) {
        ReservaEstoque reserva = travarReservaAtiva(reservaId, usuario);
        reserva.setStatus(StatusReserva.CANCELADA);
        return reservaRepository.saveAndFlush(reserva);
    }

    /**
     * Dá baixa na reserva: ela vira BAIXADA e gera a SAÍDA no estoque, com o documento da reserva como origem
     * (RN-73).
     */
    @Transactional
    public MovimentacaoEstoque baixarReserva(Integer reservaId, Usuario usuario) {
        return baixarReserva(reservaId, usuario, null);
    }

    /** Igual à anterior; o preço de venda informado fica gravado na saída (RN-68). */
    @Transactional
    public MovimentacaoEstoque baixarReserva(Integer reservaId, Usuario usuario, BigDecimal precoUnitario) {
        if (precoUnitario != null && precoUnitario.signum() < 0) {
            throw new RegraNegocioException("O preço não pode ser negativo.");
        }
        ReservaEstoque reserva = travarReservaAtiva(reservaId, usuario);
        reserva.setStatus(StatusReserva.BAIXADA);
        reservaRepository.saveAndFlush(reserva);
        String observacao = reserva.getDocumentoTipo() == TipoDocumento.OS
                ? "Baixa da reserva da OS " + reserva.getDocumentoId()
                : "Baixa da reserva da venda " + reserva.getDocumentoId();
        return registrar(new Pedido(reserva.getProduto().getId(), TipoMovimentacao.SAIDA, reserva.getQuantidade(),
                observacao, usuario, reserva.getDocumentoTipo(), reserva.getDocumentoId(), null, precoUnitario,
                null));
    }

    /** Dá baixa em todas as reservas ativas de um documento, travando os produtos em ordem crescente de id. */
    @Transactional
    public List<MovimentacaoEstoque> baixarReservasDoDocumento(TipoDocumento tipo, Integer documentoId,
                                                               Usuario usuario) {
        List<MovimentacaoEstoque> saidas = new ArrayList<>();
        for (var r : reservaRepository.listarDoDocumento(usuario.getCodigo(), tipo, documentoId, StatusReserva.ATIVA)) {
            saidas.add(baixarReserva(r.getId(), usuario));
        }
        return saidas;
    }

    /** Cancela todas as reservas ativas de um documento (ordem crescente de produto). */
    @Transactional
    public int liberarReservasDoDocumento(TipoDocumento tipo, Integer documentoId, Usuario usuario) {
        int total = 0;
        for (var r : reservaRepository.listarDoDocumento(usuario.getCodigo(), tipo, documentoId, StatusReserva.ATIVA)) {
            liberar(r.getId(), usuario);
            total++;
        }
        return total;
    }

    /**
     * Trava o produto da reserva (mesma trava de toda alteração de saldo, RN-07) e só então lê a reserva:
     * duas baixas ou liberações da mesma reserva se enfileiram e a segunda enxerga o resultado da primeira.
     */
    private ReservaEstoque travarReservaAtiva(Integer reservaId, Usuario usuario) {
        Integer produtoId = reservaRepository.buscarProdutoId(reservaId, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        produtoRepository.buscarQualquerComBloqueio(produtoId, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        ReservaEstoque reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        if (reserva.getStatus() != StatusReserva.ATIVA) {
            throw new RegraNegocioException("Esta reserva já foi "
                    + (reserva.getStatus() == StatusReserva.BAIXADA ? "baixada." : "cancelada."));
        }
        return reserva;
    }

    // ------------------------------------------------------------------------------------------

    private static void validarDocumento(TipoDocumento tipoDocumento, Integer documentoId) {
        if (tipoDocumento == null) {
            if (documentoId != null) {
                throw new RegraNegocioException("Informe o tipo e o código do documento juntos.");
            }
            return;
        }
        if (tipoDocumento.exigeCodigo() && documentoId == null) {
            throw new RegraNegocioException("Informe o tipo e o código do documento juntos.");
        }
        if (tipoDocumento == TipoDocumento.MANUAL && documentoId != null) {
            throw new RegraNegocioException("Lançamento manual não tem documento de origem.");
        }
    }

    /** Tudo que uma movimentação precisa para ser registrada. */
    private record Pedido(Integer produtoId, TipoMovimentacao tipo, int quantidade, String observacao,
                          Usuario usuario, TipoDocumento tipoDocumento, Integer documentoId,
                          BigDecimal custoUnitario, BigDecimal precoUnitario, Integer estornoDeId) {
    }

    private MovimentacaoEstoque registrar(Pedido pedido) {
        TipoMovimentacao tipo = pedido.tipo();
        int quantidade = pedido.quantidade();
        Usuario usuario = pedido.usuario();
        if (tipo == null) {
            throw new RegraNegocioException("Informe o tipo da movimentação.");
        }
        String motivo = Textos.limpar(pedido.observacao());
        if (motivo != null && motivo.length() > OBSERVACAO_MAXIMA) {
            throw new RegraNegocioException("A observação deve ter no máximo " + OBSERVACAO_MAXIMA + " caracteres.");
        }
        validarQuantidade(tipo, quantidade, motivo);

        // Bloqueio pessimista: duas movimentações do mesmo produto se enfileiram (sem lost update).
        Produto produto = produtoRepository.buscarAtivoComBloqueio(pedido.produtoId(), usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));

        // Já com o produto travado: um estorno só pode existir uma vez (o índice único do banco é a rede de segurança)
        if (pedido.estornoDeId() != null && movimentacaoRepository.existsByEstornoDeId(pedido.estornoDeId())) {
            throw new RegraNegocioException(MENSAGEM_JA_DESFEITA);
        }

        int anterior = produto.getQuantidade();
        long nova = switch (tipo) {
            case ENTRADA -> (long) anterior + quantidade;
            case SAIDA -> (long) anterior - quantidade;
            case AJUSTE -> quantidade;
        };
        if (nova < 0) {
            throw new RegraNegocioException("Saída de " + quantidade + " unidade(s) maior que o saldo atual ("
                    + anterior + "). Nada foi registrado.");
        }
        if (nova > SALDO_MAXIMO) {
            throw new RegraNegocioException("O saldo do produto não pode passar de " + formatar(SALDO_MAXIMO)
                    + " unidades.");
        }
        // RN-72: o que baixa o saldo não pode consumir o que está reservado para outros documentos
        if (nova < anterior) {
            // a reserva que está sendo baixada já saiu das ativas antes da saída
            long reservadoPorOutros = reservaRepository.somarAtivas(produto.getId(), usuario.getCodigo());

            if (nova < reservadoPorOutros) {
                throw new RegraNegocioException("Há " + formatar(reservadoPorOutros) + " unidade(s) reservada(s) de \""
                        + produto.getNome() + "\" e o saldo ficaria em " + formatar(nova)
                        + ". Cancele a reserva antes. Nada foi registrado.");
            }
        }

        produto.setQuantidade((int) nova);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(produto);
        movimentacao.setUsuario(usuario);
        movimentacao.setMembroId(usuario.getMembroId());
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setQuantidadeAnterior(anterior);
        movimentacao.setQuantidadeNova((int) nova);
        movimentacao.setObservacao(motivo);
        movimentacao.definirDocumento(pedido.tipoDocumento(), pedido.documentoId());
        movimentacao.setCustoUnitario(pedido.custoUnitario() != null ? pedido.custoUnitario() : produto.getCusto());
        movimentacao.setPrecoUnitario(pedido.precoUnitario());
        movimentacao.setEstornoDeId(pedido.estornoDeId());
        // saveAndFlush: grava o saldo e o registro juntos, ainda dentro da transação
        produtoRepository.save(produto);
        return movimentacaoRepository.saveAndFlush(movimentacao);
    }

    private void validarQuantidade(TipoMovimentacao tipo, int quantidade, String motivo) {
        if (tipo == TipoMovimentacao.AJUSTE) {
            if (motivo == null) {
                throw new RegraNegocioException("Informe o motivo do ajuste.");
            }
            if (quantidade < 0 || quantidade > SALDO_MAXIMO) {
                throw new RegraNegocioException("A contagem do ajuste deve estar entre 0 e "
                        + formatar(SALDO_MAXIMO) + ".");
            }
            return;
        }
        String nome = tipo == TipoMovimentacao.ENTRADA ? "entrada" : "saída";
        if (quantidade <= 0) {
            throw new RegraNegocioException("A quantidade da " + nome + " deve ser maior que zero.");
        }
        if (quantidade > QUANTIDADE_MAXIMA_MOVIMENTACAO) {
            throw new RegraNegocioException("A quantidade da " + nome + " deve ser no máximo "
                    + formatar(QUANTIDADE_MAXIMA_MOVIMENTACAO) + ".");
        }
    }

    private static String formatar(long numero) {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,d", numero);
    }
}
