package org.example.stockhubspring.service;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

/** Regras RN-02, RN-05, RN-06 e RN-01 do EstoqueService, com os repositórios simulados. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstoqueServiceTest {

    private static final int PRODUTO_ID = 10;

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    private EstoqueService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new EstoqueService(produtoRepository, movimentacaoRepository);
        usuario = Fabrica.usuario(7);
        when(movimentacaoRepository.saveAndFlush(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    private Produto produtoComSaldo(int saldo) {
        Produto produto = Fabrica.produto(PRODUTO_ID, "Caneta", saldo, 5, usuario);
        when(produtoRepository.buscarAtivoComBloqueio(PRODUTO_ID, usuario.getCodigo()))
                .thenReturn(Optional.of(produto));
        return produto;
    }

    private void verificarQueNadaFoiGravado() {
        verify(produtoRepository, never()).save(any(Produto.class));
        verify(movimentacaoRepository, never()).saveAndFlush(any(MovimentacaoEstoque.class));
        verify(movimentacaoRepository, never()).save(any(MovimentacaoEstoque.class));
    }

    @Nested
    @DisplayName("ENTRADA")
    class Entrada {

        @Test
        @DisplayName("soma a quantidade ao saldo e grava saldo anterior e novo")
        void somaAoSaldoEGravaAnteriorENovo() {
            Produto produto = produtoComSaldo(10);

            MovimentacaoEstoque mov = service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 5, "Compra", usuario);

            assertThat(produto.getQuantidade()).isEqualTo(15);
            assertThat(mov.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
            assertThat(mov.getQuantidade()).isEqualTo(5);
            assertThat(mov.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(mov.getQuantidadeNova()).isEqualTo(15);
            assertThat(mov.getObservacao()).isEqualTo("Compra");
            assertThat(mov.getProduto()).isSameAs(produto);
            assertThat(mov.getUsuario()).isSameAs(usuario);
            verify(produtoRepository).save(produto);
        }

        @Test
        @DisplayName("observação em branco é gravada como null e a com espaços é aparada")
        void observacaoEmBrancoViraNula() {
            produtoComSaldo(0);
            assertThat(service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1, "   ", usuario).getObservacao())
                    .isNull();
            assertThat(service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1, "  Reposição ", usuario)
                    .getObservacao()).isEqualTo("Reposição");
        }

        @ParameterizedTest(name = "quantidade {0} é recusada")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("quantidade zero ou negativa é recusada e nada é gravado")
        void quantidadeZeroOuNegativaRecusada(int quantidade) {
            Produto produto = produtoComSaldo(10);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, quantidade, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("maior que zero");

            assertThat(produto.getQuantidade()).isEqualTo(10);
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("aceita exatamente 1.000.000 e recusa 1.000.001 por movimentação")
        void limiteDeUmMilhaoPorMovimentacao() {
            Produto produto = produtoComSaldo(0);

            service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1_000_000, null, usuario);
            assertThat(produto.getQuantidade()).isEqualTo(1_000_000);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1_000_001, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("no máximo");
            assertThat(produto.getQuantidade()).isEqualTo(1_000_000);
        }

        @Test
        @DisplayName("aceita chegar exatamente em 2.000.000.000 de saldo")
        void aceitaSaldoNoLimiteExato() {
            Produto produto = produtoComSaldo(EstoqueService.SALDO_MAXIMO - 1_000_000);

            service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1_000_000, null, usuario);

            assertThat(produto.getQuantidade()).isEqualTo(EstoqueService.SALDO_MAXIMO);
        }

        @Test
        @DisplayName("recusa saldo acima de 2.000.000.000 e não grava")
        void recusaSaldoAcimaDoLimite() {
            Produto produto = produtoComSaldo(EstoqueService.SALDO_MAXIMO);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("não pode passar");

            assertThat(produto.getQuantidade()).isEqualTo(EstoqueService.SALDO_MAXIMO);
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("não estoura o int: saldo próximo de Integer.MAX_VALUE é recusado em vez de ficar negativo")
        void semOverflowDeInt() {
            Produto produto = produtoComSaldo(Integer.MAX_VALUE);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1_000_000, null, usuario))
                    .isInstanceOf(RegraNegocioException.class);

            assertThat(produto.getQuantidade()).isEqualTo(Integer.MAX_VALUE);
            verificarQueNadaFoiGravado();
        }
    }

    @Nested
    @DisplayName("SAIDA")
    class Saida {

        @Test
        @DisplayName("subtrai do saldo e grava saldo anterior e novo")
        void subtraiDoSaldo() {
            Produto produto = produtoComSaldo(10);

            MovimentacaoEstoque mov = service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 4, "Venda", usuario);

            assertThat(produto.getQuantidade()).isEqualTo(6);
            assertThat(mov.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(mov.getQuantidadeNova()).isEqualTo(6);
            assertThat(mov.getQuantidade()).isEqualTo(4);
        }

        @Test
        @DisplayName("saída igual ao saldo zera o estoque (permitido)")
        void saidaIgualAoSaldoZeraOEstoque() {
            Produto produto = produtoComSaldo(10);

            MovimentacaoEstoque mov = service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 10, null, usuario);

            assertThat(produto.getQuantidade()).isZero();
            assertThat(mov.getQuantidadeNova()).isZero();
        }

        @Test
        @DisplayName("saída acima do saldo é recusada, o saldo não muda e nada é gravado")
        void saidaAcimaDoSaldoRecusadaESemGravar() {
            Produto produto = produtoComSaldo(10);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 11, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("maior que o saldo")
                    .hasMessageContaining("Nada foi registrado");

            assertThat(produto.getQuantidade()).isEqualTo(10);
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("saída em produto com saldo zero é recusada")
        void saidaComSaldoZeroRecusada() {
            produtoComSaldo(0);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 1, null, usuario))
                    .isInstanceOf(RegraNegocioException.class);
            verificarQueNadaFoiGravado();
        }

        @ParameterizedTest(name = "quantidade {0} é recusada")
        @ValueSource(ints = {0, -5})
        @DisplayName("quantidade zero ou negativa é recusada")
        void quantidadeZeroOuNegativaRecusada(int quantidade) {
            produtoComSaldo(10);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, quantidade, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("saída")
                    .hasMessageContaining("maior que zero");
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("saída acima de 1.000.000 é recusada pelo limite da movimentação")
        void saidaAcimaDoLimiteDaMovimentacao() {
            produtoComSaldo(2_000_000_000);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 1_000_001, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("no máximo");
            verificarQueNadaFoiGravado();
        }
    }

    @Nested
    @DisplayName("AJUSTE")
    class Ajuste {

        @Test
        @DisplayName("a quantidade é a contagem final: o saldo passa a ser exatamente esse valor")
        void quantidadeEAContagemFinal() {
            Produto produto = produtoComSaldo(10);

            MovimentacaoEstoque mov = service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, 3, "Contagem física", usuario);

            assertThat(produto.getQuantidade()).isEqualTo(3);
            assertThat(mov.getQuantidade()).isEqualTo(3);
            assertThat(mov.getQuantidadeAnterior()).isEqualTo(10);
            assertThat(mov.getQuantidadeNova()).isEqualTo(3);
        }

        @Test
        @DisplayName("ajuste pode aumentar o saldo para a contagem final")
        void ajustePodeAumentar() {
            Produto produto = produtoComSaldo(2);
            service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, 50, "Achamos mais", usuario);
            assertThat(produto.getQuantidade()).isEqualTo(50);
        }

        @Test
        @DisplayName("aceita contagem final zero")
        void aceitaContagemZero() {
            Produto produto = produtoComSaldo(10);

            MovimentacaoEstoque mov = service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, 0, "Perda total", usuario);

            assertThat(produto.getQuantidade()).isZero();
            assertThat(mov.getQuantidadeNova()).isZero();
        }

        @ParameterizedTest(name = "motivo [{0}] é recusado")
        @ValueSource(strings = {"", "   ", "\t\n"})
        @DisplayName("exige motivo preenchido")
        void exigeMotivoPreenchido(String motivo) {
            Produto produto = produtoComSaldo(10);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, 5, motivo, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("motivo");

            assertThat(produto.getQuantidade()).isEqualTo(10);
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("motivo nulo é recusado")
        void motivoNuloRecusado() {
            produtoComSaldo(10);
            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, 5, null, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("motivo");
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("contagem negativa é recusada")
        void contagemNegativaRecusada() {
            Produto produto = produtoComSaldo(10);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, -1, "Erro", usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("contagem");

            assertThat(produto.getQuantidade()).isEqualTo(10);
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("contagem acima de 2.000.000.000 é recusada; exatamente 2.000.000.000 é aceita")
        void limiteDaContagem() {
            Produto produto = produtoComSaldo(1);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE,
                    EstoqueService.SALDO_MAXIMO + 1, "Muito grande", usuario))
                    .isInstanceOf(RegraNegocioException.class);
            assertThat(produto.getQuantidade()).isEqualTo(1);

            service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, EstoqueService.SALDO_MAXIMO, "No limite", usuario);
            assertThat(produto.getQuantidade()).isEqualTo(EstoqueService.SALDO_MAXIMO);
        }

        @Test
        @DisplayName("ajuste não tem o limite de 1.000.000 por movimentação (é contagem final)")
        void ajusteNaoTemLimiteDeUmMilhao() {
            Produto produto = produtoComSaldo(0);
            service.movimentar(PRODUTO_ID, TipoMovimentacao.AJUSTE, 5_000_000, "Inventário grande", usuario);
            assertThat(produto.getQuantidade()).isEqualTo(5_000_000);
        }
    }

    @Nested
    @DisplayName("validações gerais")
    class Gerais {

        @Test
        @DisplayName("tipo nulo é recusado sem consultar o banco")
        void tipoNuloRecusado() {
            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, null, 5, "x", usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("tipo");
            verifyNoInteractions(produtoRepository);
            verifyNoInteractions(movimentacaoRepository);
        }

        @Test
        @DisplayName("quantidade inválida é recusada antes de bloquear a linha do produto")
        void quantidadeInvalidaNaoBloqueiaProduto() {
            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 0, null, usuario))
                    .isInstanceOf(RegraNegocioException.class);
            verifyNoInteractions(produtoRepository);
        }

        @Test
        @DisplayName("observação com 255 caracteres é aceita e com 256 é recusada")
        void limiteDaObservacao() {
            produtoComSaldo(0);
            String ok = "a".repeat(255);
            assertThat(service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1, ok, usuario).getObservacao())
                    .hasSize(255);

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1, "a".repeat(256), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("255");
        }

        @Test
        @DisplayName("texto com <script> na observação é gravado como dado, sem alteração (o escape é feito na saída)")
        void observacaoComScriptEGravadaComoDado() {
            produtoComSaldo(0);
            MovimentacaoEstoque mov = service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1,
                    "<script>alert(1)</script>", usuario);
            assertThat(mov.getObservacao()).isEqualTo("<script>alert(1)</script>");
        }

        @Test
        @DisplayName("produto inexistente, inativo ou de outra loja responde 'não encontrado' e nada é gravado")
        void produtoNaoEncontradoOuDeOutraLoja() {
            when(produtoRepository.buscarAtivoComBloqueio(anyInt(), anyInt())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.movimentar(999, TipoMovimentacao.ENTRADA, 1, null, usuario))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessage("Produto não encontrado.");
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("a busca do produto sempre é feita com o código do usuário logado (RN-01)")
        void buscaSempreComOUsuarioLogado() {
            produtoComSaldo(1);
            service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 1, null, usuario);
            verify(produtoRepository).buscarAtivoComBloqueio(PRODUTO_ID, 7);
        }

        @Test
        @DisplayName("outro usuário não consegue movimentar o produto da loja 7 (consulta filtrada pelo dono)")
        void outroUsuarioNaoEnxergaOProduto() {
            produtoComSaldo(10); // só a loja 7 enxerga o produto 10
            Usuario invasor = Fabrica.usuario(8);
            when(produtoRepository.buscarAtivoComBloqueio(PRODUTO_ID, 8)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.movimentar(PRODUTO_ID, TipoMovimentacao.SAIDA, 1, null, invasor))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verificarQueNadaFoiGravado();
        }

        @Test
        @DisplayName("a movimentação grava o produto e o registro (saldo e histórico juntos)")
        void gravaSaldoERegistro() {
            Produto produto = produtoComSaldo(1);
            ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);

            service.movimentar(PRODUTO_ID, TipoMovimentacao.ENTRADA, 2, null, usuario);

            verify(produtoRepository).save(produto);
            verify(movimentacaoRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getQuantidadeNova()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("histórico")
    class Historico {

        @Test
        @DisplayName("lista do mais recente para o mais antigo e limita o tamanho da página a 100")
        void ordenaDescEPaginaLimitada() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(movimentacaoRepository.findByUsuarioCodigo(any(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<MovimentacaoEstoque>(List.of(), inv.getArgument(1), 0));

            service.listarMovimentacoes(7, 0, 100_000);

            verify(movimentacaoRepository).findByUsuarioCodigo(any(), captor.capture());
            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageNumber()).isZero();
            assertThat(pageable.getPageSize()).isEqualTo(EstoqueService.TAMANHO_MAXIMO_PAGINA);
            assertThat(pageable.getSort().getOrderFor("criadoEm").getDirection()).isEqualTo(Sort.Direction.DESC);
            assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
        }
    }
}
