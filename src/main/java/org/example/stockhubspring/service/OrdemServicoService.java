package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.EquipamentoCliente;
import org.example.stockhubspring.model.MembroLoja;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.OsPeca;
import org.example.stockhubspring.model.OsServico;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.ReservaEstoque;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.model.TipoDocumento;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.EquipamentoClienteRepository;
import org.example.stockhubspring.repository.MembroLojaRepository;
import org.example.stockhubspring.repository.OrdemServicoRepository;
import org.example.stockhubspring.repository.OsPecaRepository;
import org.example.stockhubspring.repository.OsServicoRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.util.Textos;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ordens de serviço (RN-77 a RN-90). Toda consulta é da loja (RN-01). Cada ação trava a linha da OS e só depois
 * os produtos (em ordem crescente de id, como AD-14), então duas ações na mesma OS se enfileiram.
 */
@Service
public class OrdemServicoService {

    public static final int LIMITE_LISTA = 200;
    public static final int LIMITE_ITENS = 100;
    public static final int DEFEITO_MAXIMO = 1000;
    public static final int OBSERVACAO_MAXIMA = 1000;
    public static final int DESCRICAO_MAXIMA = 255;
    public static final int IDENTIFICADOR_MAXIMO = 60;
    public static final int GARANTIA_MAXIMA_DIAS = 3650;
    public static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");
    /** Desconto máximo, sobre o total de peças e serviços, para quem não é dono nem gerente (RN-86). */
    public static final BigDecimal DESCONTO_MAXIMO_OPERACIONAL = new BigDecimal("0.10");

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final OrdemServicoRepository osRepository;
    private final OsPecaRepository pecaRepository;
    private final OsServicoRepository servicoRepository;
    private final EquipamentoClienteRepository equipamentoRepository;
    private final ParceiroRepository parceiroRepository;
    private final ProdutoRepository produtoRepository;
    private final MembroLojaRepository membroRepository;
    private final EstoqueService estoqueService;
    private final NumeradorOs numerador;

    public OrdemServicoService(OrdemServicoRepository osRepository, OsPecaRepository pecaRepository,
                               OsServicoRepository servicoRepository,
                               EquipamentoClienteRepository equipamentoRepository,
                               ParceiroRepository parceiroRepository, ProdutoRepository produtoRepository,
                               MembroLojaRepository membroRepository, EstoqueService estoqueService,
                               NumeradorOs numerador) {
        this.osRepository = osRepository;
        this.pecaRepository = pecaRepository;
        this.servicoRepository = servicoRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.parceiroRepository = parceiroRepository;
        this.produtoRepository = produtoRepository;
        this.membroRepository = membroRepository;
        this.estoqueService = estoqueService;
        this.numerador = numerador;
    }

    /** A OS com as peças, os serviços e os totais, para a tela de detalhe e a impressão. */
    public record Detalhe(OrdemServico os, List<OsPeca> pecas, List<OsServico> servicos,
                          BigDecimal subtotalPecas, BigDecimal subtotalServicos) {
        public BigDecimal subtotal() {
            return subtotalPecas.add(subtotalServicos);
        }
    }

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public List<OrdemServico> listar(Integer lojaCodigo, StatusOs status) {
        return osRepository.listar(lojaCodigo, status, PageRequest.of(0, LIMITE_LISTA));
    }

    /** Quantas OS há em cada situação (as que não existem ficam com zero). */
    @Transactional(readOnly = true)
    public Map<StatusOs, Long> contarPorStatus(Integer lojaCodigo) {
        Map<StatusOs, Long> mapa = new EnumMap<>(StatusOs.class);
        for (StatusOs s : StatusOs.values()) {
            mapa.put(s, 0L);
        }
        for (var c : osRepository.contarPorStatus(lojaCodigo)) {
            mapa.put(c.getStatus(), c.getTotal());
        }
        return mapa;
    }

    @Transactional(readOnly = true)
    public Detalhe detalhe(Integer lojaCodigo, Integer osId) {
        OrdemServico os = osRepository.buscar(osId, lojaCodigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        List<OsPeca> pecas = pecaRepository.listarDaOs(osId);
        List<OsServico> servicos = servicoRepository.findByOsIdOrderByIdAsc(osId);
        return new Detalhe(os, pecas, servicos, somaPecas(pecas), somaServicos(servicos));
    }

    /** Pessoas ativas da loja que podem ser o responsável. */
    @Transactional(readOnly = true)
    public List<MembroLoja> responsaveis(Integer lojaCodigo) {
        return membroRepository.findByLojaCodigoOrderByAtivoDescNomeAsc(lojaCodigo).stream()
                .filter(MembroLoja::isAtivo).toList();
    }

    // ---------------------------------------------------------------- criação e dados

    @Transactional
    public OrdemServico criar(Usuario usuario, Integer clienteId, String equipamentoDescricao,
                              String identificador, String defeito, Integer responsavelId,
                              Integer garantiaDias, String observacoes) {
        Parceiro cliente = clienteId == null ? null : parceiroRepository.findById(clienteId)
                .filter(p -> p.isCliente() && p.getUsuario().getCodigo().equals(usuario.getCodigo())).orElse(null);
        if (cliente == null) {
            throw new RegraNegocioException("Escolha um cliente da lista. Se ele ainda não existe, cadastre em Clientes.");
        }
        String descricao = Textos.limpar(equipamentoDescricao);
        if (descricao == null) {
            throw new RegraNegocioException("Informe o equipamento ou veículo, por exemplo \"Notebook Dell\" ou \"Fiat Uno\".");
        }
        if (descricao.length() > DESCRICAO_MAXIMA) {
            throw new RegraNegocioException("O equipamento deve ter no máximo " + DESCRICAO_MAXIMA + " caracteres.");
        }
        String ident = Textos.limpar(identificador);
        if (ident != null && ident.length() > IDENTIFICADOR_MAXIMO) {
            throw new RegraNegocioException("A placa ou o número de série deve ter no máximo "
                    + IDENTIFICADOR_MAXIMO + " caracteres.");
        }
        String problema = validarDefeito(defeito);
        if (problema != null) {
            throw new RegraNegocioException(problema);
        }
        int garantia = validarGarantia(garantiaDias);
        String obs = validarObservacoes(observacoes);
        MembroLoja responsavel = escolherResponsavel(usuario, responsavelId, true);

        OrdemServico os = new OrdemServico();
        os.setUsuario(usuario);
        os.setNumero(numerador.proximo(usuario.getCodigo()));
        os.setCliente(cliente);
        os.setEquipamento(equipamentoDe(usuario, cliente, descricao, ident));
        os.setDefeito(Textos.limpar(defeito));
        os.setResponsavel(responsavel);
        os.setGarantiaDias(garantia);
        os.setObservacoes(obs);
        os.setCriadaPorId(usuario.getMembroId());
        return osRepository.saveAndFlush(os);
    }

    @Transactional
    public OrdemServico atualizarDados(Usuario usuario, Integer osId, String defeito, Integer responsavelId,
                                       Integer garantiaDias, String observacoes, BigDecimal desconto) {
        OrdemServico os = travar(usuario, osId);
        exigirEditavel(os);
        String problema = validarDefeito(defeito);
        if (problema != null) {
            throw new RegraNegocioException(problema);
        }
        os.setDefeito(Textos.limpar(defeito));
        os.setGarantiaDias(validarGarantia(garantiaDias));
        os.setObservacoes(validarObservacoes(observacoes));
        if (responsavelId != null) {
            os.setResponsavel(escolherResponsavel(usuario, responsavelId, false));
        }
        BigDecimal novo = desconto == null ? BigDecimal.ZERO : desconto.setScale(2, RoundingMode.HALF_UP);
        if (novo.compareTo(os.getDesconto()) != 0) {
            validarDesconto(usuario, novo, subtotalDe(os));
            os.setDesconto(novo);
        }
        recalcular(os);
        return osRepository.saveAndFlush(os);
    }

    // ---------------------------------------------------------------- peças e serviços

    @Transactional
    public OsPeca adicionarPeca(Usuario usuario, Integer osId, Integer produtoId, int quantidade,
                                BigDecimal precoUnitario) {
        OrdemServico os = travar(usuario, osId);
        if (!os.getStatus().podeTrocarPecas()) {
            throw new RegraNegocioException("As peças só mudam enquanto a OS está em orçamento ou aprovada.");
        }
        if (quantidade <= 0 || quantidade > EstoqueService.QUANTIDADE_MAXIMA_MOVIMENTACAO) {
            throw new RegraNegocioException("A quantidade deve estar entre 1 e "
                    + EstoqueService.QUANTIDADE_MAXIMA_MOVIMENTACAO + ".");
        }
        if (pecaRepository.listarDaOs(osId).size() >= LIMITE_ITENS) {
            throw new RegraNegocioException("Uma OS aceita até " + LIMITE_ITENS + " peças.");
        }
        Produto produto = produtoId == null ? null : produtoRepository
                .findByIdAndUsuarioCodigoAndAtivoTrue(produtoId, usuario.getCodigo()).orElse(null);
        if (produto == null) {
            throw new RegraNegocioException("Escolha uma peça da lista de produtos.");
        }
        BigDecimal preco = precoUnitario != null ? precoUnitario : produto.getPrecoDeVenda();
        if (preco == null || preco.signum() < 0 || preco.compareTo(VALOR_MAXIMO) > 0) {
            throw new RegraNegocioException("Informe um preço válido para a peça.");
        }
        OsPeca peca = new OsPeca();
        peca.setOs(os);
        peca.setProduto(produto);
        peca.setQuantidade(quantidade);
        peca.setPrecoUnitario(preco.setScale(4, RoundingMode.HALF_UP));
        peca.setCustoUnitario(produto.getCusto() == null ? BigDecimal.ZERO : produto.getCusto());
        if (os.getStatus() == StatusOs.APROVADA) {
            peca.setReservaId(reservar(usuario, os, peca));
        }
        pecaRepository.saveAndFlush(peca);
        recalcular(os);
        osRepository.saveAndFlush(os);
        return peca;
    }

    @Transactional
    public void removerPeca(Usuario usuario, Integer osId, Integer pecaId) {
        OrdemServico os = travar(usuario, osId);
        if (!os.getStatus().podeTrocarPecas()) {
            throw new RegraNegocioException("As peças só mudam enquanto a OS está em orçamento ou aprovada.");
        }
        OsPeca peca = pecaRepository.findById(pecaId)
                .filter(p -> p.getOs().getId().equals(osId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada nesta OS."));
        if (peca.getReservaId() != null) {
            estoqueService.liberar(peca.getReservaId(), usuario);
        }
        pecaRepository.delete(peca);
        pecaRepository.flush();
        recalcular(os);
        validarDescontoAindaCabe(os);
        osRepository.saveAndFlush(os);
    }

    @Transactional
    public OsServico adicionarServico(Usuario usuario, Integer osId, String descricao, BigDecimal valor) {
        OrdemServico os = travar(usuario, osId);
        exigirEditavel(os);
        String texto = Textos.limpar(descricao);
        if (texto == null) {
            throw new RegraNegocioException("Descreva o serviço, por exemplo \"Troca de óleo\".");
        }
        if (texto.length() > DESCRICAO_MAXIMA) {
            throw new RegraNegocioException("A descrição deve ter no máximo " + DESCRICAO_MAXIMA + " caracteres.");
        }
        if (valor == null || valor.signum() < 0 || valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new RegraNegocioException("Informe o valor do serviço (zero ou mais).");
        }
        if (servicoRepository.findByOsIdOrderByIdAsc(osId).size() >= LIMITE_ITENS) {
            throw new RegraNegocioException("Uma OS aceita até " + LIMITE_ITENS + " serviços.");
        }
        OsServico servico = new OsServico();
        servico.setOs(os);
        servico.setDescricao(texto);
        servico.setValor(valor.setScale(2, RoundingMode.HALF_UP));
        servicoRepository.saveAndFlush(servico);
        recalcular(os);
        osRepository.saveAndFlush(os);
        return servico;
    }

    @Transactional
    public void removerServico(Usuario usuario, Integer osId, Integer servicoId) {
        OrdemServico os = travar(usuario, osId);
        exigirEditavel(os);
        OsServico servico = servicoRepository.findById(servicoId)
                .filter(s -> s.getOs().getId().equals(osId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado nesta OS."));
        servicoRepository.delete(servico);
        servicoRepository.flush();
        recalcular(os);
        validarDescontoAindaCabe(os);
        osRepository.saveAndFlush(os);
    }

    // ---------------------------------------------------------------- status

    /** Leva a OS ao passo seguinte do fluxo (aprovar, iniciar, concluir, entregar). */
    @Transactional
    public OrdemServico avancar(Usuario usuario, Integer osId) {
        OrdemServico os = travar(usuario, osId);
        StatusOs proximo = os.getStatus().proximo();
        if (proximo == null) {
            throw new RegraNegocioException("Esta OS está \"" + os.getStatus().getRotulo() + "\" e não tem próximo passo.");
        }
        return mudarStatus(usuario, osId, proximo);
    }

    @Transactional
    public OrdemServico cancelar(Usuario usuario, Integer osId) {
        return mudarStatus(usuario, osId, StatusOs.CANCELADA);
    }

    @Transactional
    public OrdemServico mudarStatus(Usuario usuario, Integer osId, StatusOs destino) {
        OrdemServico os = travar(usuario, osId);
        StatusOs atual = os.getStatus();
        if (!atual.transicaoValida(destino)) {
            throw new RegraNegocioException("Esta OS está \"" + atual.getRotulo() + "\" e não pode ir para \""
                    + (destino == null ? "?" : destino.getRotulo()) + "\".");
        }
        if (destino == StatusOs.CANCELADA && atual == StatusOs.CONCLUIDA && !gerencia(usuario)) {
            throw new RegraNegocioException("Só o dono ou o gerente cancela uma OS já concluída.");
        }
        LocalDateTime agora = LocalDateTime.now(FUSO);
        List<OsPeca> pecas = pecaRepository.listarDaOs(osId);
        switch (destino) {
            case APROVADA -> {
                for (OsPeca peca : pecas) {
                    peca.setReservaId(reservar(usuario, os, peca));
                }
                os.setAprovadaEm(agora);
            }
            case EM_EXECUCAO -> os.setIniciadaEm(agora);
            case CONCLUIDA -> {
                for (OsPeca peca : pecas) {
                    if (peca.getReservaId() == null) {
                        throw new RegraNegocioException("A peça \"" + peca.getProduto().getNome()
                                + "\" não está reservada. Nada foi baixado.");
                    }
                    MovimentacaoEstoque saida = estoqueService.baixarReserva(peca.getReservaId(), usuario,
                            peca.getPrecoUnitario(), peca.getCustoUnitario());
                    peca.setMovimentacaoId(saida.getId());
                }
                os.setConcluidaEm(agora);
            }
            case ENTREGUE -> os.setEntregueEm(agora);
            case CANCELADA -> {
                for (OsPeca peca : pecas) {
                    if (atual == StatusOs.CONCLUIDA) {
                        if (peca.getMovimentacaoId() != null) {
                            estoqueService.estornar(peca.getMovimentacaoId(),
                                    "OS " + os.getNumero() + " cancelada", usuario);
                        }
                    } else if (peca.getReservaId() != null) {
                        estoqueService.liberar(peca.getReservaId(), usuario);
                    }
                }
                os.setCanceladaEm(agora);
            }
            default -> throw new RegraNegocioException("Passo inválido.");
        }
        os.setStatus(destino);
        pecaRepository.saveAll(pecas);
        return osRepository.saveAndFlush(os);
    }

    // ---------------------------------------------------------------- internos

    private OrdemServico travar(Usuario usuario, Integer osId) {
        OrdemServico os = osRepository.buscarComBloqueio(osId, usuario.getCodigo())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ordem de serviço não encontrada."));
        return os;
    }

    private static boolean gerencia(Usuario usuario) {
        return usuario.getPapel() != null && usuario.getPapel().gerenciaLoja();
    }

    private static void exigirEditavel(OrdemServico os) {
        if (!os.getStatus().editavel()) {
            throw new RegraNegocioException("Esta OS está \"" + os.getStatus().getRotulo() + "\" e não pode mais ser alterada.");
        }
    }

    private Integer reservar(Usuario usuario, OrdemServico os, OsPeca peca) {
        try {
            ReservaEstoque reserva = estoqueService.reservar(peca.getProduto().getId(), peca.getQuantidade(),
                    TipoDocumento.OS, os.getId(), usuario);
            return reserva.getId();
        } catch (RegraNegocioException e) {
            throw new RegraNegocioException("Não foi possível reservar a peça: " + e.getMessage());
        } catch (RecursoNaoEncontradoException e) {
            throw new RegraNegocioException("A peça \"" + peca.getProduto().getNome()
                    + "\" não está mais disponível no estoque.");
        }
    }

    private MembroLoja escolherResponsavel(Usuario usuario, Integer responsavelId, boolean padraoQuemCria) {
        Integer id = responsavelId != null ? responsavelId : (padraoQuemCria ? usuario.getMembroId() : null);
        if (id == null) {
            return null;
        }
        return membroRepository.findByIdAndLojaCodigo(id, usuario.getCodigo()).filter(MembroLoja::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("Escolha um responsável da equipe."));
    }

    private EquipamentoCliente equipamentoDe(Usuario loja, Parceiro cliente, String descricao, String ident) {
        for (EquipamentoCliente e : equipamentoRepository
                .findByUsuarioCodigoAndParceiroIdOrderByIdAsc(loja.getCodigo(), cliente.getId())) {
            boolean mesmaDescricao = e.getDescricao().equalsIgnoreCase(descricao);
            boolean mesmoIdent = e.getIdentificador() == null ? ident == null : e.getIdentificador().equalsIgnoreCase(ident);
            if (mesmaDescricao && mesmoIdent) {
                return e;
            }
        }
        EquipamentoCliente novo = new EquipamentoCliente();
        novo.setUsuario(loja);
        novo.setParceiro(cliente);
        novo.setDescricao(descricao);
        novo.setIdentificador(ident);
        return equipamentoRepository.saveAndFlush(novo);
    }

    private static String validarDefeito(String defeito) {
        String texto = Textos.limpar(defeito);
        if (texto == null) {
            return "Descreva o defeito relatado pelo cliente.";
        }
        if (texto.length() > DEFEITO_MAXIMO) {
            return "O defeito deve ter no máximo " + DEFEITO_MAXIMO + " caracteres.";
        }
        return null;
    }

    private static int validarGarantia(Integer dias) {
        int valor = dias == null ? 0 : dias;
        if (valor < 0 || valor > GARANTIA_MAXIMA_DIAS) {
            throw new RegraNegocioException("A garantia deve ficar entre 0 e " + GARANTIA_MAXIMA_DIAS + " dias.");
        }
        return valor;
    }

    private static String validarObservacoes(String observacoes) {
        String texto = Textos.limpar(observacoes);
        if (texto != null && texto.length() > OBSERVACAO_MAXIMA) {
            throw new RegraNegocioException("As observações devem ter no máximo " + OBSERVACAO_MAXIMA + " caracteres.");
        }
        return texto;
    }

    /** Desconto em reais: não passa do total e, para quem não é dono nem gerente, de 10% (RN-86). */
    private static void validarDesconto(Usuario usuario, BigDecimal desconto, BigDecimal subtotal) {
        if (desconto.signum() < 0 || desconto.compareTo(VALOR_MAXIMO) > 0) {
            throw new RegraNegocioException("O desconto não pode ser negativo.");
        }
        if (desconto.compareTo(subtotal) > 0) {
            throw new RegraNegocioException("O desconto não pode ser maior que o total de peças e serviços (R$ "
                    + subtotal.toPlainString().replace('.', ',') + ").");
        }
        if (!gerencia(usuario) && desconto.signum() > 0) {
            BigDecimal limite = subtotal.multiply(DESCONTO_MAXIMO_OPERACIONAL).setScale(2, RoundingMode.DOWN);
            if (desconto.compareTo(limite) > 0) {
                throw new RegraNegocioException("Você pode dar até 10% de desconto (R$ "
                        + limite.toPlainString().replace('.', ',')
                        + "). Para mais, peça ao dono ou ao gerente.");
            }
        }
    }

    private void validarDescontoAindaCabe(OrdemServico os) {
        BigDecimal subtotal = subtotalDe(os);
        if (os.getDesconto().compareTo(subtotal) > 0) {
            throw new RegraNegocioException("Reduza o desconto antes: ele ficaria maior que o total de peças e serviços.");
        }
    }

    private BigDecimal subtotalDe(OrdemServico os) {
        return somaPecas(pecaRepository.listarDaOs(os.getId()))
                .add(somaServicos(servicoRepository.findByOsIdOrderByIdAsc(os.getId())));
    }

    private void recalcular(OrdemServico os) {
        BigDecimal total = subtotalDe(os).subtract(os.getDesconto());
        os.setTotal(total.signum() < 0 ? BigDecimal.ZERO : total);
    }

    private static BigDecimal somaPecas(List<OsPeca> pecas) {
        return pecas.stream().map(OsPeca::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal somaServicos(List<OsServico> servicos) {
        return servicos.stream().map(OsServico::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
