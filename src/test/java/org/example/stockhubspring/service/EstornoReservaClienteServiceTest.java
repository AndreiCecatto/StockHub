package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.SaldoProduto;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoDocumento;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.ReservaEstoqueRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regras de estorno (RN-69, RN-70), reserva (RN-72 a RN-75) e clientes (RN-76) com os repositórios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstornoReservaClienteServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Mock
    private ReservaEstoqueRepository reservaRepository;
    @Mock
    private ParceiroRepository parceiroRepository;
    @Mock
    private org.example.stockhubspring.repository.OrdemServicoRepository osRepository;

    private EstoqueService estoque;
    private ClienteService clientes;
    private Usuario dono;

    @BeforeEach
    void preparar() {
        estoque = new EstoqueService(produtoRepository, movimentacaoRepository, reservaRepository);
        clientes = new ClienteService(parceiroRepository, osRepository);
        dono = Fabrica.usuario(7);
        dono.setPapel(PapelMembro.DONO);
        when(parceiroRepository.saveAndFlush(any(Parceiro.class))).thenAnswer(i -> i.getArgument(0));
    }

    private MovimentacaoEstoque movimentacao(int id, TipoMovimentacao tipo, int qtd, int antes, int depois, String obs) {
        MovimentacaoEstoque m = new MovimentacaoEstoque();
        m.setId(id);
        m.setProduto(Fabrica.produto(10, "Caneta", depois, 0, dono));
        m.setUsuario(dono);
        m.setTipo(tipo);
        m.setQuantidade(qtd);
        m.setQuantidadeAnterior(antes);
        m.setQuantidadeNova(depois);
        m.setObservacao(obs);
        return m;
    }

    // ---------- estorno ----------

    @ParameterizedTest
    @EnumSource(value = PapelMembro.class, names = {"OPERADOR", "VENDEDOR", "MECANICO"})
    @DisplayName("papel operacional não desfaz: nada é consultado nem gravado")
    void papelNaoDesfaz(PapelMembro papel) {
        Usuario operador = Fabrica.usuario(7);
        operador.setPapel(papel);

        assertThatThrownBy(() -> estoque.estornar(1, "Erro", operador)).isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(movimentacaoRepository);
        verifyNoInteractions(produtoRepository);
    }

    @Test
    @DisplayName("sem papel conhecido também não desfaz")
    void semPapel() {
        Usuario semPapel = Fabrica.usuario(7);

        assertThatThrownBy(() -> estoque.estornar(1, "Erro", semPapel)).isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(movimentacaoRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("motivo vazio é recusado")
    void motivoVazio(String motivo) {
        assertThatThrownBy(() -> estoque.estornar(1, motivo, dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("motivo");
        verifyNoInteractions(movimentacaoRepository);
    }

    @Test
    @DisplayName("motivo acima do limite é recusado")
    void motivoLongo() {
        assertThatThrownBy(() -> estoque.estornar(1, "x".repeat(EstoqueService.MOTIVO_ESTORNO_MAXIMO + 1), dono))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("movimentação inexistente ou de outra loja é 'não encontrada'")
    void naoEncontrada() {
        when(movimentacaoRepository.findByIdAndUsuarioCodigo(1, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoque.estornar(1, "Erro", dono)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("desfazer uma saída grava uma entrada ligada à original e devolve o saldo")
    void estornaSaida() {
        MovimentacaoEstoque original = movimentacao(1, TipoMovimentacao.SAIDA, 3, 10, 7, "Venda");
        Produto produto = Fabrica.produto(10, "Caneta", 7, 0, dono);
        when(movimentacaoRepository.findByIdAndUsuarioCodigo(1, 7)).thenReturn(Optional.of(original));
        when(produtoRepository.buscarAtivoComBloqueio(10, 7)).thenReturn(Optional.of(produto));
        when(movimentacaoRepository.saveAndFlush(any(MovimentacaoEstoque.class))).thenAnswer(i -> i.getArgument(0));

        MovimentacaoEstoque estorno = estoque.estornar(1, "Cliente desistiu", dono);

        assertThat(estorno.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(estorno.getQuantidade()).isEqualTo(3);
        assertThat(estorno.getEstornoDeId()).isEqualTo(1);
        assertThat(estorno.getObservacao()).isEqualTo("Estorno: Cliente desistiu");
        assertThat(produto.getQuantidade()).isEqualTo(10);
        assertThat(estorno.getTipoDocumento()).isEqualTo(TipoDocumento.MANUAL);
    }

    @Test
    @DisplayName("já desfeita, estorno de estorno e estoque inicial são recusados antes de mexer no saldo")
    void impedimentos() {
        MovimentacaoEstoque jaDesfeita = movimentacao(1, TipoMovimentacao.SAIDA, 3, 10, 7, "Venda");
        MovimentacaoEstoque umEstorno = movimentacao(2, TipoMovimentacao.ENTRADA, 3, 7, 10, "Estorno: x");
        umEstorno.setEstornoDeId(1);
        MovimentacaoEstoque inicial = movimentacao(3, TipoMovimentacao.ENTRADA, 5, 0, 5, EstoqueService.MOTIVO_ESTOQUE_INICIAL);
        when(movimentacaoRepository.findByIdAndUsuarioCodigo(1, 7)).thenReturn(Optional.of(jaDesfeita));
        when(movimentacaoRepository.findByIdAndUsuarioCodigo(2, 7)).thenReturn(Optional.of(umEstorno));
        when(movimentacaoRepository.findByIdAndUsuarioCodigo(3, 7)).thenReturn(Optional.of(inicial));
        when(movimentacaoRepository.existsByEstornoDeId(1)).thenReturn(true);

        assertThatThrownBy(() -> estoque.estornar(1, "Erro", dono)).hasMessageContaining("já foi desfeita");
        assertThatThrownBy(() -> estoque.estornar(2, "Erro", dono)).hasMessageContaining("estorno não pode ser desfeito");
        assertThatThrownBy(() -> estoque.estornar(3, "Erro", dono)).hasMessageContaining("estoque inicial");
        verify(movimentacaoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("estornaveis esconde tudo de quem não é dono ou gerente, sem consultar o banco")
    void estornaveisPorPapel() {
        Usuario operador = Fabrica.usuario(7);
        operador.setPapel(PapelMembro.OPERADOR);

        assertThat(estoque.estornaveis(List.of(movimentacao(1, TipoMovimentacao.SAIDA, 1, 2, 1, "x")), operador)).isEmpty();
        verifyNoInteractions(movimentacaoRepository);
    }

    // ---------- reserva ----------

    @Test
    @DisplayName("disponível é o saldo menos o reservado e nunca fica negativo")
    void saldoDisponivel() {
        assertThat(SaldoProduto.de(10, 4)).isEqualTo(new SaldoProduto(10, 4, 6));
        assertThat(SaldoProduto.de(3, 5).disponivel()).isZero();
        Produto produto = Fabrica.produto(10, "Caneta", 10, 0, dono);
        when(reservaRepository.somarAtivas(10, 7)).thenReturn(4L);

        assertThat(estoque.saldoDoProduto(produto, 7)).isEqualTo(new SaldoProduto(10, 4, 6));
    }

    @Test
    @DisplayName("só venda e OS reservam, com código e quantidade válidos")
    void reservaValida() {
        assertThatThrownBy(() -> estoque.reservar(10, 1, TipoDocumento.COMPRA_NFE, 1, dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoque.reservar(10, 1, null, 1, dono)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoque.reservar(10, 1, TipoDocumento.VENDA, null, dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoque.reservar(10, -1, TipoDocumento.VENDA, 1, dono))
                .isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(produtoRepository);
    }

    @Test
    @DisplayName("produto de outra loja ou inexistente não reserva")
    void reservaProdutoInexistente() {
        when(produtoRepository.buscarAtivoComBloqueio(anyInt(), anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoque.reservar(10, 1, TipoDocumento.VENDA, 1, dono))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("reserva inexistente ou de outra loja não é liberada nem baixada")
    void reservaInexistente() {
        when(reservaRepository.buscarProdutoId(5, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoque.liberar(5, dono)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> estoque.baixarReserva(5, dono)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(movimentacaoRepository);
    }

    @Test
    @DisplayName("preço negativo na baixa é recusado antes de mexer na reserva")
    void baixaPrecoNegativo() {
        assertThatThrownBy(() -> estoque.baixarReserva(5, dono, new java.math.BigDecimal("-1")))
                .isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(reservaRepository);
    }

    // ---------- clientes ----------

    @Test
    @DisplayName("cliente novo nasce só como cliente, com documento só em dígitos e UF em maiúsculas")
    void clienteNovo() {
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, "52998224725")).thenReturn(Optional.empty());

        Parceiro c = clientes.cadastrar(dono, "529.982.247-25", "  Ana  ", "rs");

        assertThat(c.isCliente()).isTrue();
        assertThat(c.isFornecedor()).isFalse();
        assertThat(c.getDocumento()).isEqualTo("52998224725");
        assertThat(c.getNome()).isEqualTo("Ana");
        assertThat(c.getUf()).isEqualTo("RS");
        assertThat(c.getUsuario()).isSameAs(dono);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "11111111111", "52998224726", "11222333000100"})
    @DisplayName("CPF ou CNPJ inválido é recusado")
    void documentoInvalido(String documento) {
        assertThatThrownBy(() -> clientes.cadastrar(dono, documento, "Ana", null))
                .isInstanceOf(RegraNegocioException.class);
        verify(parceiroRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("CNPJ válido é aceito")
    void cnpjValido() {
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, "11222333000181")).thenReturn(Optional.empty());

        assertThat(clientes.cadastrar(dono, "11.222.333/0001-81", "Empresa", null).getDocumento())
                .isEqualTo("11222333000181");
    }

    @Test
    @DisplayName("quem já é cliente não se cadastra de novo")
    void clienteRepetido() {
        Parceiro existente = new Parceiro();
        existente.setCliente(true);
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(7, "52998224725")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> clientes.cadastrar(dono, "52998224725", "Ana", null))
                .isInstanceOf(RegraNegocioException.class);
        verify(parceiroRepository, never()).saveAndFlush(any());
    }
}
