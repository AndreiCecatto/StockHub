package org.example.stockhubspring.integration;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.ReservaEstoque;
import org.example.stockhubspring.model.TipoDocumento;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.EstoqueService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concorrência básica de estorno e reserva (RN-07, RN-70, RN-72): várias threads, cada uma na sua transação,
 * disputam o mesmo produto. Diferente dos outros testes de integração, aqui os dados PRECISAM ser gravados
 * (outra thread não enxerga o que não foi confirmado). Por isso a loja é sintética (e-mail .invalid, código
 * novo) e, no fim, só as linhas dessa loja são removidas.
 */
@SpringBootTest
class ConcorrenciaEstoqueIntegracaoTest {

    private static final int THREADS = 4;

    @Autowired
    private EstoqueService estoqueService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transacoes;

    private TransactionTemplate tx;
    private Integer lojaCodigo;
    private Usuario dono;
    private Produto produto;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            Usuario loja = new Usuario();
            loja.setNome("Loja de concorrência");
            loja.setEmail("conc-" + UUID.randomUUID() + "@teste.invalid");
            loja.setSenha("hash-sintetico");
            loja.setAtivo(true);
            loja = usuarioRepository.saveAndFlush(loja);
            lojaCodigo = loja.getCodigo();

            Categoria categoria = new Categoria();
            categoria.setNome("Geral");
            categoria.setUsuario(loja);
            categoria = categoriaRepository.saveAndFlush(categoria);

            Produto p = new Produto();
            p.setNome("Produto disputado");
            p.setPreco(new BigDecimal("10.00"));
            p.setQuantidade(10);
            p.setCategoria(categoria);
            p.setUsuario(loja);
            p.setAtivo(true);
            produto = produtoRepository.saveAndFlush(p);
        });
        dono = new Usuario();
        dono.setCodigo(lojaCodigo);
        dono.setNome("Loja de concorrência");
        dono.setPapel(PapelMembro.DONO);
    }

    /** Remove só o que este teste criou: tudo da loja sintética, dos filhos para os pais. */
    @AfterEach
    void limpar() {
        if (lojaCodigo == null) {
            return;
        }
        jdbc.update("delete from reserva_estoque where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from movimentacao_estoque where usuario_codigo = ? and estorno_de_id is not null", lojaCodigo);
        jdbc.update("delete from movimentacao_estoque where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from produto where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from categoria where usuario_codigo = ?", lojaCodigo);
        int removidas = jdbc.update("delete from usuario where codigo = ? and email like '%@teste.invalid'", lojaCodigo);
        assertThat(removidas).as("a loja sintética foi removida").isEqualTo(1);
    }

    private int saldo() {
        return jdbc.queryForObject("select quantidade from produto where id = ?", Integer.class, produto.getId());
    }

    /** Dispara as tarefas juntas e devolve quantas terminaram sem erro de regra (as demais devem ser RegraNegocioException). */
    private int rodarJuntas(List<Callable<Object>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        try {
            for (Callable<Object> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            int sucessos = 0;
            for (Future<Object> futuro : futuros) {
                try {
                    futuro.get(60, TimeUnit.SECONDS);
                    sucessos++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause()).as("a recusa tem de ser de regra, não erro técnico")
                            .isInstanceOf(RegraNegocioException.class);
                }
            }
            return sucessos;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("quatro pessoas desfazendo a mesma movimentação ao mesmo tempo: só um estorno acontece")
    void estornoSimultaneo() throws Exception {
        MovimentacaoEstoque saida = estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tarefas.add(() -> estoqueService.estornar(saida.getId(), "Erro", dono));
        }

        int sucessos = rodarJuntas(tarefas);

        assertThat(sucessos).isEqualTo(1);
        assertThat(saldo()).isEqualTo(10);
        assertThat(jdbc.queryForObject("select count(*) from movimentacao_estoque where estorno_de_id = ?",
                Long.class, saida.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("saídas manuais simultâneas só consomem o disponível, nunca o reservado")
    void saidasContraReserva() throws Exception {
        estoqueService.reservar(produto.getId(), 8, TipoDocumento.VENDA, 1, dono);
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tarefas.add(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 2, "Uso", dono));
        }

        int sucessos = rodarJuntas(tarefas);

        assertThat(sucessos).isEqualTo(1);
        assertThat(saldo()).isEqualTo(8);
    }

    @Test
    @DisplayName("baixas simultâneas da mesma reserva geram uma só saída")
    void baixaSimultanea() throws Exception {
        ReservaEstoque reserva = estoqueService.reservar(produto.getId(), 4, TipoDocumento.VENDA, 7, dono);
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tarefas.add(() -> estoqueService.baixarReserva(reserva.getId(), dono));
        }

        int sucessos = rodarJuntas(tarefas);

        assertThat(sucessos).isEqualTo(1);
        assertThat(saldo()).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from movimentacao_estoque where tipo_documento = 'VENDA' "
                + "and documento_id = 7 and usuario_codigo = ?", Long.class, lojaCodigo)).isEqualTo(1L);
    }

    @Test
    @DisplayName("reservas simultâneas nunca passam do disponível")
    void reservasSimultaneas() throws Exception {
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            int documento = 100 + i;
            tarefas.add(() -> estoqueService.reservar(produto.getId(), 4, TipoDocumento.VENDA, documento, dono));
        }

        int sucessos = rodarJuntas(tarefas);

        assertThat(sucessos).isEqualTo(2);
        assertThat(jdbc.queryForObject("select coalesce(sum(quantidade), 0) from reserva_estoque "
                + "where produto_id = ? and status = 'ATIVA'", Long.class, produto.getId())).isEqualTo(8L);
    }
}
