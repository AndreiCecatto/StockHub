package org.example.stockhubspring.integration;

import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.OrdemServicoService;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concorrência simples da OS (RN-78, RN-84): aprovações e criações ao mesmo tempo, cada uma na sua transação.
 * Os dados precisam ser gravados de verdade; por isso a loja é sintética (e-mail .invalid) e, no fim, só as linhas
 * dela são removidas (dos filhos para os pais).
 */
@SpringBootTest
class OrdemServicoConcorrenciaIntegracaoTest {

    @Autowired
    private OrdemServicoService osService;
    @Autowired
    private ClienteService clienteService;
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
    private Parceiro cliente;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transacoes);
        tx.executeWithoutResult(status -> {
            Usuario loja = new Usuario();
            loja.setNome("Loja de concorrência OS");
            loja.setEmail("os-conc-" + UUID.randomUUID() + "@teste.invalid");
            loja.setSenha("hash-sintetico");
            loja.setAtivo(true);
            loja = usuarioRepository.saveAndFlush(loja);
            lojaCodigo = loja.getCodigo();

            Categoria categoria = new Categoria();
            categoria.setNome("Peças");
            categoria.setUsuario(loja);
            categoria = categoriaRepository.saveAndFlush(categoria);

            Produto p = new Produto();
            p.setNome("Peça disputada");
            p.setPreco(new BigDecimal("10.00"));
            p.setQuantidade(10);
            p.setCategoria(categoria);
            p.setUsuario(loja);
            p.setAtivo(true);
            produto = produtoRepository.saveAndFlush(p);
            cliente = clienteService.cadastrar(loja, "529.982.247-25", "Cliente sintético", null);
        });
        dono = new Usuario();
        dono.setCodigo(lojaCodigo);
        dono.setNome("Loja de concorrência OS");
        dono.setPapel(PapelMembro.DONO);
    }

    /** Remove só o que este teste criou: tudo da loja sintética, dos filhos para os pais. */
    @AfterEach
    void limpar() {
        if (lojaCodigo == null) {
            return;
        }
        jdbc.update("update os_peca set reserva_id = null, movimentacao_id = null where os_id in "
                + "(select id from ordem_servico where usuario_codigo = ?)", lojaCodigo);
        jdbc.update("delete from os_peca where os_id in (select id from ordem_servico where usuario_codigo = ?)", lojaCodigo);
        jdbc.update("delete from os_servico where os_id in (select id from ordem_servico where usuario_codigo = ?)", lojaCodigo);
        jdbc.update("delete from ordem_servico where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from equipamento_cliente where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from os_contador where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from reserva_estoque where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from movimentacao_estoque where usuario_codigo = ? and estorno_de_id is not null", lojaCodigo);
        jdbc.update("delete from movimentacao_estoque where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from produto where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from categoria where usuario_codigo = ?", lojaCodigo);
        jdbc.update("delete from parceiro where usuario_codigo = ?", lojaCodigo);
        int removidas = jdbc.update("delete from usuario where codigo = ? and email like '%@teste.invalid'", lojaCodigo);
        assertThat(removidas).as("a loja sintética foi removida").isEqualTo(1);
    }

    private <T> List<Object> rodarJuntas(List<Callable<T>> tarefas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tarefas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<T>> futuros = new ArrayList<>();
        List<Object> resultados = new ArrayList<>();
        try {
            for (Callable<T> tarefa : tarefas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            for (Future<T> futuro : futuros) {
                try {
                    resultados.add(futuro.get(60, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).as("a recusa tem de ser de regra, não erro técnico")
                            .isInstanceOf(RegraNegocioException.class);
                    resultados.add(e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return resultados;
    }

    @Test
    @DisplayName("criar várias OS ao mesmo tempo nunca repete número")
    void numerosNaoRepetem() throws Exception {
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tarefas.add(() -> osService.criar(dono, cliente.getId(), "Fiat Uno", null, "Barulho", null, 0, null));
        }
        List<Object> resultados = rodarJuntas(tarefas);

        assertThat(resultados).allMatch(r -> r instanceof OrdemServico);
        List<Integer> numeros = jdbc.queryForList("select numero from ordem_servico where usuario_codigo = ? order by numero",
                Integer.class, lojaCodigo);
        assertThat(numeros).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    @DisplayName("duas OS aprovadas ao mesmo tempo disputando as mesmas peças: só uma reserva, a outra é recusada")
    void aprovacoesSimultaneas() throws Exception {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            OrdemServico os = osService.criar(dono, cliente.getId(), "Fiat Uno", null, "Barulho", null, 0, null);
            osService.adicionarPeca(dono, os.getId(), produto.getId(), 6, null);
            ids.add(os.getId());
        }
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (Integer id : ids) {
            tarefas.add(() -> osService.avancar(dono, id));
        }
        List<Object> resultados = rodarJuntas(tarefas);

        long sucessos = resultados.stream().filter(r -> r instanceof OrdemServico).count();
        assertThat(sucessos).isEqualTo(1);
        Long reservado = jdbc.queryForObject("select coalesce(sum(quantidade), 0) from reserva_estoque "
                + "where usuario_codigo = ? and status = 'ATIVA'", Long.class, lojaCodigo);
        assertThat(reservado).isEqualTo(6);
        Long aprovadas = jdbc.queryForObject("select count(*) from ordem_servico where usuario_codigo = ? "
                + "and status = 'APROVADA'", Long.class, lojaCodigo);
        assertThat(aprovadas).isEqualTo(1);
    }

    @Test
    @DisplayName("concluir a mesma OS duas vezes ao mesmo tempo baixa o estoque uma só vez")
    void conclusaoSimultanea() throws Exception {
        OrdemServico os = osService.criar(dono, cliente.getId(), "Fiat Uno", null, "Barulho", null, 0, null);
        osService.adicionarPeca(dono, os.getId(), produto.getId(), 4, null);
        osService.avancar(dono, os.getId());
        osService.avancar(dono, os.getId());

        List<Callable<Object>> tarefas = List.of(() -> osService.avancar(dono, os.getId()),
                () -> osService.avancar(dono, os.getId()));
        List<Object> resultados = rodarJuntas(tarefas);

        // as duas pedem "o próximo passo": a segunda, já concluída, vai para ENTREGUE; a baixa é uma só
        assertThat(resultados).hasSize(2);
        Long saidas = jdbc.queryForObject("select count(*) from movimentacao_estoque where usuario_codigo = ? "
                + "and tipo_documento = 'OS' and tipo = 'SAIDA'", Long.class, lojaCodigo);
        assertThat(saidas).isEqualTo(1);
        Integer saldo = jdbc.queryForObject("select quantidade from produto where id = ?", Integer.class, produto.getId());
        assertThat(saldo).isEqualTo(6);
    }
}
