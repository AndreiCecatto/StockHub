package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.service.ProdutoService.ResultadoExclusao;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regras de ProdutoService: RN-01, RN-03, RN-04, RN-08, RN-10. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProdutoServiceTest {

    private static final int LOJA = 7;

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;
    @Mock
    private EstoqueService estoqueService;
    @Mock
    private org.example.stockhubspring.repository.UsuarioRepository usuarioRepository;

    private ProdutoService service;
    private Usuario usuario;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(produtoRepository, categoriaRepository, movimentacaoRepository, estoqueService, usuarioRepository);
        usuario = Fabrica.usuario(LOJA);
        categoria = Fabrica.categoria(3, "Papelaria", usuario);
        when(categoriaRepository.findByIdAndUsuarioCodigo(3, LOJA)).thenReturn(Optional.of(categoria));
        when(produtoRepository.saveAndFlush(any(Produto.class))).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(55);
            }
            return p;
        });
        when(produtoRepository.contarNomeAtivo(anyInt(), any(), anyInt())).thenReturn(0L);
    }

    private ProdutoForm formNovo(String nome, Integer quantidade) {
        ProdutoForm form = new ProdutoForm();
        form.setNome(nome);
        form.setPreco(new BigDecimal("12.50"));
        form.setQuantidade(quantidade);
        form.setEstoqueMinimo(2);
        form.setCategoriaId(3);
        return form;
    }

    @Nested
    @DisplayName("criar produto")
    class Criar {

        @Test
        @DisplayName("com estoque inicial grava o produto com saldo 0 e gera ENTRADA 'Estoque inicial' pelo EstoqueService")
        void estoqueInicialGeraEntrada() {
            ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);

            Produto salvo = service.salvar(formNovo("Caneta", 10), usuario);

            verify(produtoRepository).saveAndFlush(captor.capture());
            // O saldo só nasce por movimentação (RN-03): o produto é gravado zerado
            assertThat(captor.getValue().getQuantidade()).isZero();
            assertThat(salvo.getUsuario()).isSameAs(usuario);
            assertThat(salvo.isAtivo()).isTrue();
            verify(estoqueService).registrarEstoqueInicial(55, 10, usuario);
            verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
        }

        @Test
        @DisplayName("sem estoque inicial (0 ou vazio) não gera movimentação")
        void semEstoqueInicialNaoGeraMovimentacao() {
            service.salvar(formNovo("Caneta", 0), usuario);
            service.salvar(formNovo("Lápis", null), usuario);
            verifyNoInteractions(estoqueService);
        }

        @Test
        @DisplayName("copia os campos do formulário e define o dono pela sessão")
        void copiaCamposEDefineODono() {
            ProdutoForm form = formNovo("  Caneta Azul ", 0);
            form.setDescricao("  Ponta fina ");

            Produto salvo = service.salvar(form, usuario);

            assertThat(salvo.getNome()).isEqualTo("Caneta Azul");
            assertThat(salvo.getDescricao()).isEqualTo("Ponta fina");
            assertThat(salvo.getPreco()).isEqualByComparingTo("12.50");
            assertThat(salvo.getEstoqueMinimo()).isEqualTo(2);
            assertThat(salvo.getCategoria()).isSameAs(categoria);
            assertThat(salvo.getUsuario().getCodigo()).isEqualTo(LOJA);
        }

        @Test
        @DisplayName("estoque inicial negativo é recusado e nada é gravado")
        void estoqueInicialNegativoRecusado() {
            assertThatThrownBy(() -> service.salvar(formNovo("Caneta", -1), usuario))
                    .isInstanceOf(RegraNegocioException.class);
            verify(produtoRepository, never()).saveAndFlush(any(Produto.class));
            verifyNoInteractions(estoqueService);
        }

        @Test
        @DisplayName("estoque inicial acima de 1.000.000 é recusado")
        void estoqueInicialAcimaDoLimiteRecusado() {
            assertThatThrownBy(() -> service.salvar(formNovo("Caneta", 1_000_001), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("1.000.000");
            verify(produtoRepository, never()).saveAndFlush(any(Produto.class));
        }

        @Test
        @DisplayName("estoque inicial de exatamente 1.000.000 é aceito")
        void estoqueInicialNoLimiteAceito() {
            service.salvar(formNovo("Caneta", 1_000_000), usuario);
            verify(estoqueService).registrarEstoqueInicial(55, 1_000_000, usuario);
        }

        @Test
        @DisplayName("nome em branco é recusado")
        void nomeEmBrancoRecusado() {
            assertThatThrownBy(() -> service.salvar(formNovo("   ", 1), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("obrigatório");
            verify(produtoRepository, never()).saveAndFlush(any(Produto.class));
        }

        @Test
        @DisplayName("nome com <script> é gravado como dado, sem alteração (o escape é feito na saída)")
        void nomeComScriptEGravadoComoDado() {
            Produto salvo = service.salvar(formNovo("<script>alert('x')</script>", 0), usuario);
            assertThat(salvo.getNome()).isEqualTo("<script>alert('x')</script>");
        }

        @Test
        @DisplayName("categoria de outra loja (ou inexistente) é recusada")
        void categoriaDeOutraLojaRecusada() {
            when(categoriaRepository.findByIdAndUsuarioCodigo(99, LOJA)).thenReturn(Optional.empty());
            ProdutoForm form = formNovo("Caneta", 1);
            form.setCategoriaId(99);

            assertThatThrownBy(() -> service.salvar(form, usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Categoria não encontrada");
            verify(produtoRepository, never()).saveAndFlush(any(Produto.class));
        }

        @Test
        @DisplayName("categoria sempre é buscada com o código da loja logada")
        void categoriaBuscadaComALojaLogada() {
            service.salvar(formNovo("Caneta", 0), usuario);
            verify(categoriaRepository).findByIdAndUsuarioCodigo(3, LOJA);
        }
    }

    @Nested
    @DisplayName("nome único (RN-10)")
    class NomeUnico {

        @Test
        @DisplayName("nome duplicado é recusado com mensagem que cita o nome")
        void nomeDuplicadoRecusado() {
            when(produtoRepository.contarNomeAtivo(LOJA, "Caneta", -1)).thenReturn(1L);

            assertThatThrownBy(() -> service.salvar(formNovo("Caneta", 5), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Já existe um produto ativo chamado")
                    .hasMessageContaining("Caneta");
            verify(produtoRepository, never()).saveAndFlush(any(Produto.class));
            verifyNoInteractions(estoqueService);
        }

        @Test
        @DisplayName("o nome vai à consulta sem passar por lowercase do Java (o banco aplica lower(trim()) dos dois lados)")
        void comparacaoDeixaACaixaParaOBanco() {
            service.salvar(formNovo("  ÁGUA Mineral  ", 0), usuario);
            verify(produtoRepository).contarNomeAtivo(LOJA, "ÁGUA Mineral", -1);
        }

        @Test
        @DisplayName("na edição o próprio produto é ignorado na conferência")
        void naEdicaoIgnoraOProprioProduto() {
            Produto existente = Fabrica.produto(21, "Caneta", 4, 1, usuario);
            existente.setCategoria(categoria);
            when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(21, LOJA)).thenReturn(Optional.of(existente));
            ProdutoForm form = formNovo("CANETA", 0);
            form.setId(21);

            service.salvar(form, usuario);

            verify(produtoRepository).contarNomeAtivo(LOJA, "CANETA", 21);
            assertThat(existente.getNome()).isEqualTo("CANETA");
        }

        @Test
        @DisplayName("violação do índice único numa corrida entre dois cadastros vira erro de negócio")
        void corridaViraErroDeNegocio() {
            when(produtoRepository.saveAndFlush(any(Produto.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint "
                            + "\"ux_produto_usuario_nome_ativo\""));

            assertThatThrownBy(() -> service.salvar(formNovo("Caneta", 3), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Já existe um produto ativo");
            verifyNoInteractions(estoqueService);
        }

        @Test
        @DisplayName("outra violação de integridade (ex.: FK) NÃO vira 'nome duplicado': mensagem genérica sem detalhes")
        void outraViolacaoNaoViraNomeDuplicado() {
            when(produtoRepository.saveAndFlush(any(Produto.class)))
                    .thenThrow(new DataIntegrityViolationException("insert or update on table \"produto\" violates "
                            + "foreign key constraint \"fk_produto_categoria\" Detail: Key (categoria_id)=(3)"));

            assertThatThrownBy(() -> service.salvar(formNovo("Caneta", 3), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Não foi possível salvar: o registro relacionado foi alterado ou removido. Tente de novo.")
                    .hasMessageNotContaining("Já existe")
                    .hasMessageNotContaining("fk_produto_categoria");
            verifyNoInteractions(estoqueService);
        }
    }

    @Nested
    @DisplayName("editar produto")
    class Editar {

        @Test
        @DisplayName("ignora a quantidade do formulário: o saldo só muda por movimentação (RN-03)")
        void editarIgnoraQuantidade() {
            Produto existente = Fabrica.produto(21, "Caneta", 7, 1, usuario);
            existente.setCategoria(categoria);
            when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(21, LOJA)).thenReturn(Optional.of(existente));
            ProdutoForm form = formNovo("Caneta Nova", 999);
            form.setId(21);

            Produto salvo = service.salvar(form, usuario);

            assertThat(salvo.getQuantidade()).isEqualTo(7);
            assertThat(salvo.getNome()).isEqualTo("Caneta Nova");
            verifyNoInteractions(estoqueService);
        }

        @Test
        @DisplayName("editar produto de outra loja (IDOR) responde 'não encontrado' e nada é gravado")
        void editarProdutoDeOutraLojaNaoEncontrado() {
            when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(21, LOJA)).thenReturn(Optional.empty());
            ProdutoForm form = formNovo("Invasão", 0);
            form.setId(21);

            assertThatThrownBy(() -> service.salvar(form, usuario))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verify(produtoRepository, never()).saveAndFlush(any(Produto.class));
        }

        @Test
        @DisplayName("editar não troca o dono nem reativa o produto")
        void editarNaoTrocaODono() {
            Usuario dono = Fabrica.usuario(LOJA);
            Produto existente = Fabrica.produto(21, "Caneta", 7, 1, dono);
            existente.setCategoria(categoria);
            when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(21, LOJA)).thenReturn(Optional.of(existente));
            ProdutoForm form = formNovo("Caneta", 0);
            form.setId(21);

            Produto salvo = service.salvar(form, usuario);

            assertThat(salvo.getUsuario()).isSameAs(dono);
        }
    }

    @Nested
    @DisplayName("excluir produto (RN-08)")
    class Excluir {

        private Produto produto;

        @BeforeEach
        void preparar() {
            produto = Fabrica.produto(21, "Caneta", 5, 1, usuario);
            when(produtoRepository.buscarAtivoComBloqueio(21, LOJA)).thenReturn(Optional.of(produto));
        }

        private MovimentacaoEstoque mov(TipoMovimentacao tipo, int anterior, String obs) {
            MovimentacaoEstoque m = new MovimentacaoEstoque();
            m.setTipo(tipo);
            m.setQuantidadeAnterior(anterior);
            m.setObservacao(obs);
            return m;
        }

        @Test
        @DisplayName("sem nenhuma movimentação apaga de vez")
        void semMovimentacaoApagaDeVez() {
            when(movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(21)).thenReturn(List.of());

            assertThat(service.excluir(21, LOJA)).isEqualTo(ResultadoExclusao.EXCLUIDO);

            verify(produtoRepository).delete(produto);
            assertThat(produto.isAtivo()).isTrue();
        }

        @Test
        @DisplayName("só com o estoque inicial apaga o produto e o registro inicial")
        void soComEstoqueInicialApaga() {
            List<MovimentacaoEstoque> historico = List.of(mov(TipoMovimentacao.ENTRADA, 0, "Estoque inicial"));
            when(movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(21)).thenReturn(historico);

            assertThat(service.excluir(21, LOJA)).isEqualTo(ResultadoExclusao.EXCLUIDO);

            verify(movimentacaoRepository).deleteAll(historico);
            verify(produtoRepository).delete(produto);
        }

        @Test
        @DisplayName("com histórico além do estoque inicial apenas inativa e preserva o histórico")
        void comHistoricoInativa() {
            when(movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(21)).thenReturn(List.of(
                    mov(TipoMovimentacao.ENTRADA, 0, "Estoque inicial"),
                    mov(TipoMovimentacao.SAIDA, 10, "Venda")));

            assertThat(service.excluir(21, LOJA)).isEqualTo(ResultadoExclusao.INATIVADO);

            assertThat(produto.isAtivo()).isFalse();
            verify(produtoRepository, never()).delete(any(Produto.class));
            verify(movimentacaoRepository, never()).deleteAll(ArgumentMatchers.<Iterable<MovimentacaoEstoque>>any());
        }

        @Test
        @DisplayName("uma única movimentação que não é o estoque inicial (outro motivo) também inativa")
        void umaMovimentacaoQueNaoEEstoqueInicialInativa() {
            when(movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(21)).thenReturn(List.of(
                    mov(TipoMovimentacao.ENTRADA, 0, "Compra do fornecedor")));

            assertThat(service.excluir(21, LOJA)).isEqualTo(ResultadoExclusao.INATIVADO);
            assertThat(produto.isAtivo()).isFalse();
            verify(produtoRepository, never()).delete(any(Produto.class));
        }

        @Test
        @DisplayName("um ajuste isolado conta como histórico e inativa")
        void ajusteIsoladoInativa() {
            when(movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(21)).thenReturn(List.of(
                    mov(TipoMovimentacao.AJUSTE, 0, "Estoque inicial")));

            assertThat(service.excluir(21, LOJA)).isEqualTo(ResultadoExclusao.INATIVADO);
        }

        @Test
        @DisplayName("produto de outra loja ou inexistente responde 'não encontrado' (IDOR)")
        void produtoDeOutraLojaNaoEncontrado() {
            when(produtoRepository.buscarAtivoComBloqueio(21, 8)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluir(21, 8))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verify(produtoRepository, never()).delete(any(Produto.class));
            verify(movimentacaoRepository, never()).deleteAll(ArgumentMatchers.<Iterable<MovimentacaoEstoque>>any());
        }

        @Test
        @DisplayName("a exclusão trava a linha do produto antes de decidir (sem corrida com movimentação)")
        void exclusaoUsaBloqueio() {
            when(movimentacaoRepository.findTop2ByProdutoIdOrderByIdAsc(21)).thenReturn(List.of());
            service.excluir(21, LOJA);
            verify(produtoRepository).buscarAtivoComBloqueio(21, LOJA);
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("buscarPorId de produto de outra loja responde 'não encontrado'")
        void buscarPorIdDeOutraLoja() {
            when(produtoRepository.findByIdAndUsuarioCodigoAndAtivoTrue(21, 8)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.buscarPorId(21, 8))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("listagem ordena por nome sem diferenciar caixa e desempata por id; tamanho limitado a 100")
        void listagemOrdenadaPorNome() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(produtoRepository.findAll(ArgumentMatchers.<Specification<Produto>>any(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<Produto>(List.of(), inv.getArgument(1), 0));

            service.listarPagina(ProdutoFiltro.de(null, null, null, "nome", "asc", 1, 5000), LOJA);

            verify(produtoRepository).findAll(ArgumentMatchers.<Specification<Produto>>any(), captor.capture());
            Pageable pageable = captor.getValue();
            assertThat(pageable.getPageSize()).isEqualTo(100);
            Sort.Order nome = pageable.getSort().getOrderFor("nome");
            assertThat(nome.getDirection()).isEqualTo(Sort.Direction.ASC);
            assertThat(nome.isIgnoreCase()).isTrue();
            assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        }

        @Test
        @DisplayName("alerta de estoque baixo ordena do menor saldo para o maior")
        void estoqueBaixoOrdenadoPeloMenorSaldo() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(produtoRepository.findAll(ArgumentMatchers.<Specification<Produto>>any(), any(Pageable.class)))
                    .thenAnswer(inv -> new PageImpl<Produto>(List.of(), inv.getArgument(1), 0));

            service.listarEstoqueBaixo(LOJA, 1, 6);

            verify(produtoRepository).findAll(ArgumentMatchers.<Specification<Produto>>any(), captor.capture());
            Sort.Order primeiro = captor.getValue().getSort().iterator().next();
            assertThat(primeiro.getProperty()).isEqualTo("quantidade");
            assertThat(primeiro.getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("listarRecentes limita o tamanho pedido")
        void listarRecentesLimitaTamanho() {
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            when(produtoRepository.findByUsuarioCodigoAndAtivoTrue(eq(LOJA), any(Pageable.class))).thenReturn(List.of());

            service.listarRecentes(100_000, LOJA);

            verify(produtoRepository).findByUsuarioCodigoAndAtivoTrue(eq(LOJA), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        }
    }
}
