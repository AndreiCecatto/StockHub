package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.OrigemMovimentacao;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.MovimentacaoSpecs;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.util.Paginas;
import org.example.stockhubspring.util.Textos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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
    public static final int TAMANHO_MAXIMO_PAGINA = 100;
    /** Máximo de linhas de uma exportação CSV do histórico (proteção de memória). */
    public static final int LIMITE_EXPORTACAO = 10_000;

    private static final Sort ORDEM_RECENTES = Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    public EstoqueService(ProdutoRepository produtoRepository,
                          MovimentacaoEstoqueRepository movimentacaoRepository) {
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
     *   <li>AJUSTE: {@code quantidade} é a contagem FINAL e o motivo (observação) é obrigatório.</li>
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
     * que originou a movimentação (ex.: NFE_COMPRA e o id da nota). Origem é tudo ou nada: tipo e id juntos,
     * ou os dois nulos.
     */
    @Transactional
    public MovimentacaoEstoque movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario,
                                          OrigemMovimentacao origemTipo, Integer origemId) {
        return movimentar(produtoId, tipo, quantidade, observacao, usuario, origemTipo, origemId, null);
    }

    /**
     * Igual à anterior, gravando na movimentação o custo unitário informado (ex.: o custo da nota na entrada
     * por NF-e). Sem custo informado, grava o custo atual do produto (RN-45).
     */
    @Transactional
    public MovimentacaoEstoque movimentar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario,
                                          OrigemMovimentacao origemTipo, Integer origemId,
                                          BigDecimal custoUnitario) {
        if (custoUnitario != null && custoUnitario.signum() < 0) {
            throw new RegraNegocioException("O custo não pode ser negativo.");
        }
        if ((origemTipo == null) != (origemId == null)) {
            throw new RegraNegocioException("Informe o tipo e o código da origem juntos.");
        }
        // RN-08: o motivo "Estoque inicial" identifica o único registro que pode ser apagado junto
        // com o produto; por isso só o sistema o grava (ver registrarEstoqueInicial).
        if (ehMotivoReservado(observacao)) {
            throw new RegraNegocioException("O motivo \"" + MOTIVO_ESTOQUE_INICIAL + "\" é reservado ao sistema "
                    + "(é gerado ao cadastrar um produto com saldo). Informe outro motivo.");
        }
        return registrar(produtoId, tipo, quantidade, observacao, usuario, origemTipo, origemId, custoUnitario);
    }

    /**
     * Caminho interno, usado só pelo {@code ProdutoService} ao cadastrar um produto com saldo (RN-04):
     * grava a ENTRADA com o motivo reservado. Não é alcançado por nenhum formulário.
     */
    @Transactional
    public MovimentacaoEstoque registrarEstoqueInicial(Integer produtoId, int quantidade, Usuario usuario) {
        return registrar(produtoId, TipoMovimentacao.ENTRADA, quantidade, MOTIVO_ESTOQUE_INICIAL, usuario, null, null, null);
    }

    /** O motivo é o texto reservado do estoque inicial? Ignora maiúsculas e espaços nas pontas. */
    public static boolean ehMotivoReservado(String observacao) {
        return observacao != null
                && Textos.normalizarNome(observacao).equals(Textos.normalizarNome(MOTIVO_ESTOQUE_INICIAL));
    }

    private MovimentacaoEstoque registrar(Integer produtoId, TipoMovimentacao tipo, int quantidade,
                                          String observacao, Usuario usuario,
                                          OrigemMovimentacao origemTipo, Integer origemId,
                                          BigDecimal custoUnitario) {
        if (tipo == null) {
            throw new RegraNegocioException("Informe o tipo da movimentação.");
        }
        String motivo = Textos.limpar(observacao);
        if (motivo != null && motivo.length() > OBSERVACAO_MAXIMA) {
            throw new RegraNegocioException("A observação deve ter no máximo " + OBSERVACAO_MAXIMA + " caracteres.");
        }
        validarQuantidade(tipo, quantidade, motivo);

        // Bloqueio pessimista: duas movimentações do mesmo produto se enfileiram (sem lost update).
        Produto produto = produtoRepository.buscarAtivoComBloqueio(produtoId, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));

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

        produto.setQuantidade((int) nova);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(produto);
        movimentacao.setUsuario(usuario);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setQuantidadeAnterior(anterior);
        movimentacao.setQuantidadeNova((int) nova);
        movimentacao.setObservacao(motivo);
        movimentacao.definirOrigem(origemTipo, origemId);
        movimentacao.setCustoUnitario(custoUnitario != null ? custoUnitario : produto.getCusto());
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

    private static String formatar(int numero) {
        return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,d", numero);
    }
}
