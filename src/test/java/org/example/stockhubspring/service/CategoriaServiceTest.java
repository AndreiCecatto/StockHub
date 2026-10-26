package org.example.stockhubspring.service;

import org.example.stockhubspring.dto.CategoriaForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regras de CategoriaService: RN-01, RN-09 e RN-10. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoriaServiceTest {

    private static final int LOJA = 7;

    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ProdutoRepository produtoRepository;

    private CategoriaService service;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new CategoriaService(categoriaRepository, produtoRepository);
        usuario = Fabrica.usuario(LOJA);
        when(categoriaRepository.saveAndFlush(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(40);
            }
            return c;
        });
        when(categoriaRepository.contarNome(anyInt(), anyString(), anyInt())).thenReturn(0L);
    }

    private CategoriaForm form(Integer id, String nome) {
        CategoriaForm form = new CategoriaForm();
        form.setId(id);
        form.setNome(nome);
        return form;
    }

    @Nested
    @DisplayName("salvar")
    class Salvar {

        @Test
        @DisplayName("nova categoria é da loja logada, com nome aparado")
        void novaCategoriaDaLojaLogada() {
            CategoriaForm form = form(null, "  Livros ");
            form.setDescricao("  Tudo de leitura ");

            Categoria salva = service.salvar(form, usuario);

            assertThat(salva.getNome()).isEqualTo("Livros");
            assertThat(salva.getDescricao()).isEqualTo("Tudo de leitura");
            assertThat(salva.getUsuario()).isSameAs(usuario);
        }

        @Test
        @DisplayName("nome duplicado (sem diferenciar caixa e espaços) é recusado")
        void nomeDuplicadoRecusado() {
            when(categoriaRepository.contarNome(LOJA, "LIVROS", -1)).thenReturn(1L);

            assertThatThrownBy(() -> service.salvar(form(null, "  LIVROS  "), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Já existe uma categoria chamada")
                    .hasMessageContaining("LIVROS");
            verify(categoriaRepository, never()).saveAndFlush(any(Categoria.class));
        }

        @Test
        @DisplayName("a conferência de nome usa o nome aparado (sem lowercase no Java) e a loja logada")
        void conferenciaUsaNomeNormalizado() {
            service.salvar(form(null, "  LiVrOs "), usuario);
            verify(categoriaRepository).contarNome(LOJA, "LiVrOs", -1);
        }

        @Test
        @DisplayName("nome em branco é recusado")
        void nomeEmBrancoRecusado() {
            assertThatThrownBy(() -> service.salvar(form(null, "   "), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("obrigatório");
            verify(categoriaRepository, never()).saveAndFlush(any(Categoria.class));
        }

        @Test
        @DisplayName("nome com <script> é gravado como dado, sem alteração")
        void nomeComScriptEGravadoComoDado() {
            Categoria salva = service.salvar(form(null, "<script>alert(1)</script>"), usuario);
            assertThat(salva.getNome()).isEqualTo("<script>alert(1)</script>");
        }

        @Test
        @DisplayName("editar ignora a própria categoria na conferência de nome")
        void editarIgnoraAPropria() {
            Categoria existente = Fabrica.categoria(12, "Livros", usuario);
            when(categoriaRepository.findByIdAndUsuarioCodigo(12, LOJA)).thenReturn(Optional.of(existente));

            Categoria salva = service.salvar(form(12, "LIVROS"), usuario);

            verify(categoriaRepository).contarNome(LOJA, "LIVROS", 12);
            assertThat(salva.getNome()).isEqualTo("LIVROS");
        }

        @Test
        @DisplayName("editar categoria de outra loja (IDOR) responde 'não encontrado' e nada é gravado")
        void editarDeOutraLojaNaoEncontrada() {
            when(categoriaRepository.findByIdAndUsuarioCodigo(12, LOJA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.salvar(form(12, "Invasão"), usuario))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verify(categoriaRepository, never()).saveAndFlush(any(Categoria.class));
        }

        @Test
        @DisplayName("corrida no índice único vira erro de negócio")
        void corridaViraErroDeNegocio() {
            when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint "
                            + "\"ux_categoria_usuario_nome\""));

            assertThatThrownBy(() -> service.salvar(form(null, "Livros"), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Já existe uma categoria");
        }

        @Test
        @DisplayName("outra violação de integridade NÃO vira 'nome duplicado': mensagem genérica sem detalhes")
        void outraViolacaoNaoViraNomeDuplicado() {
            when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                    .thenThrow(new DataIntegrityViolationException("insert or update on table \"categoria\" violates "
                            + "foreign key constraint \"fk_categoria_usuario\""));

            assertThatThrownBy(() -> service.salvar(form(null, "Livros"), usuario))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Não foi possível salvar: o registro relacionado foi alterado ou removido. Tente de novo.")
                    .hasMessageNotContaining("Já existe")
                    .hasMessageNotContaining("fk_categoria_usuario");
        }
    }

    @Nested
    @DisplayName("excluir (RN-09)")
    class Excluir {

        private Categoria categoria;

        @BeforeEach
        void preparar() {
            categoria = Fabrica.categoria(12, "Livros", usuario);
            when(categoriaRepository.findByIdAndUsuarioCodigo(12, LOJA)).thenReturn(Optional.of(categoria));
        }

        @Test
        @DisplayName("sem produtos exclui")
        void semProdutosExclui() {
            service.excluir(12, LOJA);
            verify(categoriaRepository).delete(categoria);
            verify(categoriaRepository).flush();
        }

        @Test
        @DisplayName("com 1 produto ativo não exclui e a mensagem diz que 1 produto a usa")
        void comUmProdutoAtivoNaoExclui() {
            when(produtoRepository.countByCategoriaIdAndUsuarioCodigoAndAtivoTrue(12, LOJA)).thenReturn(1L);

            assertThatThrownBy(() -> service.excluir(12, LOJA))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("1 produto a usa");
            verify(categoriaRepository, never()).delete(any(Categoria.class));
        }

        @Test
        @DisplayName("com vários produtos ativos a mensagem diz quantos produtos a usam")
        void comVariosProdutosAtivosDizQuantos() {
            when(produtoRepository.countByCategoriaIdAndUsuarioCodigoAndAtivoTrue(12, LOJA)).thenReturn(3L);

            assertThatThrownBy(() -> service.excluir(12, LOJA))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("3 produtos a usam");
            verify(categoriaRepository, never()).delete(any(Categoria.class));
        }

        @Test
        @DisplayName("só com produtos inativados também não exclui (preserva o histórico)")
        void comProdutosInativadosNaoExclui() {
            when(produtoRepository.countByCategoriaIdAndUsuarioCodigoAndAtivoTrue(12, LOJA)).thenReturn(0L);
            when(produtoRepository.countByCategoriaIdAndUsuarioCodigoAndAtivoFalse(12, LOJA)).thenReturn(2L);

            assertThatThrownBy(() -> service.excluir(12, LOJA))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("2 produtos inativados");
            verify(categoriaRepository, never()).delete(any(Categoria.class));
        }

        @Test
        @DisplayName("categoria de outra loja ou inexistente responde 'não encontrada' (IDOR)")
        void deOutraLojaNaoEncontrada() {
            when(categoriaRepository.findByIdAndUsuarioCodigo(12, 8)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluir(12, 8))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verify(categoriaRepository, never()).delete(any(Categoria.class));
        }

        @Test
        @DisplayName("violação de chave estrangeira na exclusão vira erro de negócio")
        void violacaoDeFkViraErroDeNegocio() {
            org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk"))
                    .when(categoriaRepository).flush();

            assertThatThrownBy(() -> service.excluir(12, LOJA))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("ainda está em uso");
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("buscarPorId de outra loja responde 'não encontrada'")
        void buscarPorIdDeOutraLoja() {
            when(categoriaRepository.findByIdAndUsuarioCodigo(12, 8)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.buscarPorId(12, 8))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        @DisplayName("listar preenche o total de produtos de cada categoria e escapa os curingas da busca")
        void listarPreencheTotaisEEscapaBusca() {
            Categoria a = Fabrica.categoria(1, "Alfa", usuario);
            Categoria b = Fabrica.categoria(2, "Beta", usuario);
            List<Object[]> linhas = List.of(new Object[]{a, 4L}, new Object[]{b, 0L});
            when(categoriaRepository.listarComTotalDeProdutos(LOJA, "%50!%!_%")).thenReturn(linhas);

            List<Categoria> resultado = service.listar("50%_", LOJA);

            assertThat(resultado).extracting(Categoria::getNome).containsExactly("Alfa", "Beta");
            assertThat(resultado).extracting(Categoria::getTotalProdutos).containsExactly(4L, 0L);
        }

        @Test
        @DisplayName("listar sem busca usa o padrão que casa com tudo")
        void listarSemBusca() {
            when(categoriaRepository.listarComTotalDeProdutos(LOJA, "%")).thenReturn(Collections.emptyList());
            assertThat(service.listar(null, LOJA)).isEmpty();
            verify(categoriaRepository).listarComTotalDeProdutos(LOJA, "%");
        }
    }
}
