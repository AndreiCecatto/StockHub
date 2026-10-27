package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.OsPeca;
import org.example.stockhubspring.model.OsServico;
import org.example.stockhubspring.model.PapelMembro;
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
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Transições de status, reserva e baixa de peças, desconto e papéis da OS (RN-77 a RN-90), com tudo simulado. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository osRepository;
    @Mock
    private OsPecaRepository pecaRepository;
    @Mock
    private OsServicoRepository servicoRepository;
    @Mock
    private EquipamentoClienteRepository equipamentoRepository;
    @Mock
    private ParceiroRepository parceiroRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MembroLojaRepository membroRepository;
    @Mock
    private EstoqueService estoque;
    @Mock
    private NumeradorOs numerador;

    private OrdemServicoService service;
    private Usuario dono;
    private Usuario mecanico;
    private OrdemServico os;
    private final List<OsPeca> pecas = new ArrayList<>();
    private final List<OsServico> servicos = new ArrayList<>();

    @BeforeEach
    void preparar() {
        service = new OrdemServicoService(osRepository, pecaRepository, servicoRepository, equipamentoRepository,
                parceiroRepository, produtoRepository, membroRepository, estoque, numerador);
        dono = usuario(PapelMembro.DONO);
        mecanico = usuario(PapelMembro.MECANICO);
        os = new OrdemServico();
        os.setNumero(5);
        when(osRepository.buscarComBloqueio(any(), eq(7))).thenReturn(Optional.of(os));
        when(osRepository.saveAndFlush(any(OrdemServico.class))).thenAnswer(i -> i.getArgument(0));
        when(pecaRepository.listarDaOs(any())).thenAnswer(i -> new ArrayList<>(pecas));
        when(servicoRepository.findByOsIdOrderByIdAsc(any())).thenAnswer(i -> new ArrayList<>(servicos));
    }

    private static Usuario usuario(PapelMembro papel) {
        Usuario u = Fabrica.usuario(7);
        u.setPapel(papel);
        u.setMembroId(70);
        return u;
    }

    private OsPeca peca(int id, int produtoId, int quantidade, String preco) {
        Produto p = Fabrica.produto(produtoId, "Peça " + produtoId, 50, 0, Fabrica.usuario(7));
        OsPeca peca = new OsPeca();
        peca.setProduto(p);
        peca.setQuantidade(quantidade);
        peca.setPrecoUnitario(new BigDecimal(preco));
        peca.setCustoUnitario(new BigDecimal("3.00"));
        pecas.add(peca);
        return peca;
    }

    private ReservaEstoque reserva(int id) {
        ReservaEstoque r = new ReservaEstoque();
        org.springframework.test.util.ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    // ---------- transições ----------

    @Test
    @DisplayName("o fluxo normal vai de orçamento a entregue, na ordem, gravando cada data")
    void fluxoNormal() {
        os.setStatus(StatusOs.ORCAMENTO);
        service.mudarStatus(dono, 1, StatusOs.APROVADA);
        assertThat(os.getAprovadaEm()).isNotNull();
        service.mudarStatus(dono, 1, StatusOs.EM_EXECUCAO);
        assertThat(os.getIniciadaEm()).isNotNull();
        service.mudarStatus(dono, 1, StatusOs.CONCLUIDA);
        assertThat(os.getConcluidaEm()).isNotNull();
        service.avancar(dono, 1);
        assertThat(os.getStatus()).isEqualTo(StatusOs.ENTREGUE);
        assertThat(os.getEntregueEm()).isNotNull();
    }

    @ParameterizedTest(name = "de {0} só para o passo seguinte ou cancelada")
    @EnumSource(StatusOs.class)
    @DisplayName("nenhum pulo nem volta de status é aceito")
    void transicoesInvalidas(StatusOs atual) {
        for (StatusOs destino : StatusOs.values()) {
            boolean esperado = destino == atual.proximo() || (destino == StatusOs.CANCELADA && atual.cancelavel());
            assertThat(atual.transicaoValida(destino)).as(atual + " -> " + destino).isEqualTo(esperado);
        }
    }

    @Test
    @DisplayName("pular da aprovação direto para concluída é recusado e nada muda")
    void puloRecusado() {
        os.setStatus(StatusOs.APROVADA);
        assertThatThrownBy(() -> service.mudarStatus(dono, 1, StatusOs.CONCLUIDA))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("não pode ir para");
        assertThat(os.getStatus()).isEqualTo(StatusOs.APROVADA);
        verify(estoque, never()).baixarReserva(anyInt(), any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusOs.class, names = {"ENTREGUE", "CANCELADA"})
    @DisplayName("entregue e cancelada são finais: não avançam nem cancelam")
    void finais(StatusOs final_) {
        os.setStatus(final_);
        assertThatThrownBy(() -> service.avancar(dono, 1)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.cancelar(dono, 1)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("OS de outra loja se comporta como inexistente")
    void outraLoja() {
        when(osRepository.buscarComBloqueio(any(), eq(8))).thenReturn(Optional.empty());
        Usuario outra = Fabrica.usuario(8);
        outra.setPapel(PapelMembro.DONO);
        assertThatThrownBy(() -> service.avancar(outra, 1)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- reserva e baixa ----------

    @Test
    @DisplayName("aprovar reserva cada peça no documento OS e guarda a reserva na peça")
    void aprovarReserva() {
        os.setStatus(StatusOs.ORCAMENTO);
        OsPeca a = peca(1, 10, 2, "20.00");
        OsPeca b = peca(2, 11, 1, "5.00");
        when(estoque.reservar(eq(10), eq(2), eq(TipoDocumento.OS), any(), any())).thenReturn(reserva(100));
        when(estoque.reservar(eq(11), eq(1), eq(TipoDocumento.OS), any(), any())).thenReturn(reserva(101));

        service.avancar(dono, 1);

        assertThat(os.getStatus()).isEqualTo(StatusOs.APROVADA);
        assertThat(a.getReservaId()).isEqualTo(100);
        assertThat(b.getReservaId()).isEqualTo(101);
    }

    @Test
    @DisplayName("peça sem saldo disponível bloqueia a aprovação com mensagem clara e a OS segue em orçamento")
    void aprovarSemSaldo() {
        os.setStatus(StatusOs.ORCAMENTO);
        peca(1, 10, 99, "20.00");
        when(estoque.reservar(anyInt(), anyInt(), any(), any(), any())).thenThrow(
                new RegraNegocioException("Só há 3 unidade(s) disponíveis de \"Peça 10\". Nada foi reservado."));

        assertThatThrownBy(() -> service.avancar(dono, 1))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Não foi possível reservar a peça")
                .hasMessageContaining("Só há 3");
        assertThat(os.getStatus()).isEqualTo(StatusOs.ORCAMENTO);
    }

    @Test
    @DisplayName("concluir baixa cada reserva com o preço e o custo gravados na peça")
    void concluirBaixa() {
        os.setStatus(StatusOs.EM_EXECUCAO);
        OsPeca a = peca(1, 10, 2, "20.00");
        a.setReservaId(100);
        MovimentacaoEstoque saida = org.mockito.Mockito.mock(MovimentacaoEstoque.class);
        when(saida.getId()).thenReturn(900);
        when(estoque.baixarReserva(eq(100), any(), any(), any())).thenReturn(saida);

        service.avancar(dono, 1);

        verify(estoque).baixarReserva(100, dono, new BigDecimal("20.00"), new BigDecimal("3.00"));
        assertThat(a.getMovimentacaoId()).isEqualTo(900);
        assertThat(os.getStatus()).isEqualTo(StatusOs.CONCLUIDA);
    }

    @Test
    @DisplayName("cancelar antes de concluir libera as reservas")
    void cancelarLibera() {
        os.setStatus(StatusOs.APROVADA);
        OsPeca a = peca(1, 10, 2, "20.00");
        a.setReservaId(100);

        service.cancelar(mecanico, 1);

        verify(estoque).liberar(100, mecanico);
        assertThat(os.getStatus()).isEqualTo(StatusOs.CANCELADA);
        assertThat(os.getCanceladaEm()).isNotNull();
    }

    @Test
    @DisplayName("cancelar uma OS concluída (dono) estorna as saídas geradas")
    void cancelarConcluidaEstorna() {
        os.setStatus(StatusOs.CONCLUIDA);
        OsPeca a = peca(1, 10, 2, "20.00");
        a.setReservaId(100);
        a.setMovimentacaoId(900);

        service.cancelar(dono, 1);

        verify(estoque).estornar(eq(900), any(), eq(dono));
        verify(estoque, never()).liberar(anyInt(), any());
        assertThat(os.getStatus()).isEqualTo(StatusOs.CANCELADA);
    }

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"MECANICO", "OPERADOR", "VENDEDOR"})
    @DisplayName("papéis operacionais não cancelam OS concluída")
    void operacionalNaoCancelaConcluida(PapelMembro papel) {
        os.setStatus(StatusOs.CONCLUIDA);
        peca(1, 10, 2, "20.00").setMovimentacaoId(900);

        assertThatThrownBy(() -> service.cancelar(usuario(papel), 1))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("dono ou o gerente");
        verify(estoque, never()).estornar(anyInt(), any(), any());
        assertThat(os.getStatus()).isEqualTo(StatusOs.CONCLUIDA);
    }

    // ---------- peças ----------

    @Test
    @DisplayName("peça incluída em OS aprovada já nasce reservada; em orçamento, não")
    void incluirPeca() {
        Produto p = Fabrica.produto(10, "Filtro", 20, 0, Fabrica.usuario(7));
        p.setPrecoVenda(new BigDecimal("12.50"));
        p.setCusto(new BigDecimal("6.00"));
        when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(10, 7)).thenReturn(Optional.of(p));
        when(estoque.reservar(eq(10), eq(2), eq(TipoDocumento.OS), any(), any())).thenReturn(reserva(55));

        os.setStatus(StatusOs.ORCAMENTO);
        OsPeca emOrcamento = service.adicionarPeca(mecanico, 1, 10, 2, null);
        assertThat(emOrcamento.getReservaId()).isNull();
        assertThat(emOrcamento.getPrecoUnitario()).isEqualByComparingTo("12.50");
        assertThat(emOrcamento.getCustoUnitario()).isEqualByComparingTo("6.00");
        verify(estoque, never()).reservar(anyInt(), anyInt(), any(), any(), any());

        os.setStatus(StatusOs.APROVADA);
        OsPeca emAprovada = service.adicionarPeca(mecanico, 1, 10, 2, new BigDecimal("15"));
        assertThat(emAprovada.getReservaId()).isEqualTo(55);
        assertThat(emAprovada.getPrecoUnitario()).isEqualByComparingTo("15.00");
    }

    @ParameterizedTest
    @EnumSource(value = StatusOs.class, names = {"EM_EXECUCAO", "CONCLUIDA", "ENTREGUE", "CANCELADA"})
    @DisplayName("as peças só mudam em orçamento ou aprovada")
    void pecasTravadas(StatusOs status) {
        os.setStatus(status);
        assertThatThrownBy(() -> service.adicionarPeca(dono, 1, 10, 1, null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("orçamento ou aprovada");
        assertThatThrownBy(() -> service.removerPeca(dono, 1, 3))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("orçamento ou aprovada");
    }

    @Test
    @DisplayName("retirar peça de OS aprovada libera a reserva dela")
    void retirarPecaLibera() {
        os.setStatus(StatusOs.APROVADA);
        OsPeca a = peca(1, 10, 2, "20.00");
        a.setReservaId(100);
        a.setOs(os);
        when(pecaRepository.findById(3)).thenReturn(Optional.of(a));
        // o id da OS é nulo nos dois lados: a peça pertence a esta OS
        org.springframework.test.util.ReflectionTestUtils.setField(os, "id", 1);
        service.removerPeca(dono, 1, 3);
        verify(estoque).liberar(100, dono);
        verify(pecaRepository).delete(a);
    }

    // ---------- desconto ----------

    @Test
    @DisplayName("mecânico dá até 10% de desconto e é barrado acima; dono dá mais")
    void descontoPorPapel() {
        os.setStatus(StatusOs.ORCAMENTO);
        os.setDefeito("Barulho");
        OsServico s = new OsServico();
        s.setValor(new BigDecimal("200.00"));
        servicos.add(s);

        service.atualizarDados(mecanico, 1, "Barulho", null, 0, null, new BigDecimal("20.00"));
        assertThat(os.getDesconto()).isEqualByComparingTo("20.00");
        assertThat(os.getTotal()).isEqualByComparingTo("180.00");

        assertThatThrownBy(() -> service.atualizarDados(mecanico, 1, "Barulho", null, 0, null, new BigDecimal("20.01")))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("10%");
        assertThat(os.getDesconto()).isEqualByComparingTo("20.00");

        service.atualizarDados(dono, 1, "Barulho", null, 0, null, new BigDecimal("100.00"));
        assertThat(os.getTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("desconto maior que o total é recusado até para o dono")
    void descontoMaiorQueTotal() {
        os.setStatus(StatusOs.ORCAMENTO);
        assertThatThrownBy(() -> service.atualizarDados(dono, 1, "x", null, 0, null, new BigDecimal("1.00")))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("maior que o total");
    }

    @Test
    @DisplayName("o total soma peças e serviços e tira o desconto")
    void totalSomaTudo() {
        os.setStatus(StatusOs.ORCAMENTO);
        peca(1, 10, 2, "20.00");
        OsServico s = new OsServico();
        s.setValor(new BigDecimal("50.00"));
        servicos.add(s);
        service.atualizarDados(dono, 1, "x", null, 0, null, new BigDecimal("10.00"));
        assertThat(os.getTotal()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("serviço com valor negativo ou sem descrição é recusado")
    void servicoInvalido() {
        os.setStatus(StatusOs.ORCAMENTO);
        assertThatThrownBy(() -> service.adicionarServico(dono, 1, "Troca", new BigDecimal("-1")))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.adicionarServico(dono, 1, "  ", new BigDecimal("1")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("OS concluída não aceita mais serviço nem mudança de dados")
    void concluidaFechada() {
        os.setStatus(StatusOs.CONCLUIDA);
        assertThatThrownBy(() -> service.adicionarServico(dono, 1, "Troca", BigDecimal.TEN))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.atualizarDados(dono, 1, "x", null, 0, null, null))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("criar exige cliente da loja")
    void criarSemCliente() {
        assertThatThrownBy(() -> service.criar(dono, null, "Uno", null, "Não liga", null, 0, null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("cliente");
        verify(numerador, never()).proximo(anyInt());
    }
}
