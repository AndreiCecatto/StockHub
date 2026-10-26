package org.example.stockhubspring.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.MovimentacaoEstoqueRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.DashboardService;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base dos testes de integração com o PostgreSQL local (banco "stockhub", schema pelo Flyway).
 *
 * <p>SEGURANÇA DOS DADOS: toda classe é {@code @Transactional} e a transação do teste SEMPRE é
 * revertida (nenhum teste usa {@code @Commit}). Os dados são sintéticos, criados dentro do teste,
 * com e-mails {@code *.invalid}. O {@link #garantirRollback()} falha o teste se a transação não
 * estiver marcada para reversão.
 */
@SpringBootTest
@Transactional
abstract class IntegracaoBase {

    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected CategoriaRepository categoriaRepository;
    @Autowired
    protected ProdutoRepository produtoRepository;
    @Autowired
    protected MovimentacaoEstoqueRepository movimentacaoRepository;
    @Autowired
    protected ProdutoService produtoService;
    @Autowired
    protected CategoriaService categoriaService;
    @Autowired
    protected EstoqueService estoqueService;
    @Autowired
    protected DashboardService dashboardService;
    @Autowired
    protected UsuarioService usuarioService;
    @Autowired
    protected JdbcTemplate jdbc;

    @PersistenceContext
    protected EntityManager em;

    @BeforeEach
    void garantirRollback() {
        assertThat(TestTransaction.isActive()).as("o teste precisa rodar dentro de uma transação").isTrue();
        assertThat(TestTransaction.isFlaggedForRollback())
                .as("a transação do teste tem de ser revertida para não deixar dados no banco").isTrue();
    }

    protected Usuario novoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Loja de teste");
        usuario.setEmail("it-" + UUID.randomUUID() + "@teste.invalid");
        usuario.setSenha("hash-sintetico");
        usuario.setAtivo(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    protected Categoria novaCategoria(Usuario dono, String nome) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setUsuario(dono);
        return categoriaRepository.saveAndFlush(categoria);
    }

    protected Produto novoProduto(Usuario dono, Categoria categoria, String nome, int quantidade, int estoqueMinimo) {
        return novoProduto(dono, categoria, nome, quantidade, estoqueMinimo, new BigDecimal("10.00"));
    }

    protected Produto novoProduto(Usuario dono, Categoria categoria, String nome, int quantidade,
                                  int estoqueMinimo, BigDecimal preco) {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setPreco(preco);
        produto.setQuantidade(quantidade);
        produto.setEstoqueMinimo(estoqueMinimo);
        produto.setCategoria(categoria);
        produto.setUsuario(dono);
        produto.setAtivo(true);
        return produtoRepository.saveAndFlush(produto);
    }

    /** Grava tudo e esvazia a sessão, para as próximas leituras irem de fato ao banco. */
    protected void limparSessao() {
        em.flush();
        em.clear();
    }

    protected long contarMovimentacoesDoProduto(Integer produtoId) {
        Long total = jdbc.queryForObject("select count(*) from movimentacao_estoque where produto_id = ?",
                Long.class, produtoId);
        return total == null ? 0 : total;
    }

    protected Integer saldoNoBanco(Integer produtoId) {
        return jdbc.queryForObject("select quantidade from produto where id = ?", Integer.class, produtoId);
    }
}
