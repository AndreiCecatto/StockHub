package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.SaldoProduto;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.OrdemServico;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.StatusOs;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.ClienteService;
import org.example.stockhubspring.service.OrdemServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration V11 e as regras da OS (RN-77 a RN-90) contra o PostgreSQL. Tudo dentro da transação do teste, sempre
 * revertida. Cada violação de restrição aborta a transação do PostgreSQL, então cada teste provoca no máximo UMA.
 */
class OrdemServicoIntegracaoTest extends IntegracaoBase {

    private static final String CPF_VALIDO = "529.982.247-25";

    @Autowired
    private OrdemServicoService osService;
    @Autowired
    private ClienteService clienteService;

    private Usuario loja;
    private Usuario dono;
    private Usuario mecanico;
    private Produto produto;
    private Parceiro cliente;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        Categoria categoria = novaCategoria(loja, "Peças");
        produto = novoProduto(loja, categoria, "Filtro de óleo", 10, 0);
        produto.setCusto(new BigDecimal("4.00"));
        produto.setPrecoVenda(new BigDecimal("20.00"));
        produto = produtoRepository.saveAndFlush(produto);
        dono = comPapel(loja, PapelMembro.DONO);
        mecanico = comPapel(loja, PapelMembro.MECANICO);
        cliente = clienteService.cadastrar(loja, CPF_VALIDO, "Maria de Teste", "RS");
    }

    private static Usuario comPapel(Usuario base, PapelMembro papel) {
        Usuario u = new Usuario();
        u.setCodigo(base.getCodigo());
        u.setNome(base.getNome());
        u.setEmail(base.getEmail());
        u.setPapel(papel);
        return u;
    }

    private OrdemServico novaOs(Usuario quem) {
        return osService.criar(quem, cliente.getId(), "Fiat Uno", "ABC1D23", "Barulho no motor", null, 90, null);
    }

    private long reservadoAtivo() {
        Long total = jdbc.queryForObject("select coalesce(sum(quantidade), 0) from reserva_estoque "
                + "where produto_id = ? and status = 'ATIVA'", Long.class, produto.getId());
        return total == null ? 0 : total;
    }

    // ---------- V11 ----------

    @Test
    @DisplayName("V11: o número da OS é sequencial por loja e cada loja começa do 1")
    void numeracaoPorLoja() {
        assertThat(novaOs(dono).getNumero()).isEqualTo(1);
        assertThat(novaOs(mecanico).getNumero()).isEqualTo(2);
        assertThat(novaOs(dono).getNumero()).isEqualTo(3);

        Usuario outraLoja = novoUsuario();
        Parceiro outroCliente = clienteService.cadastrar(outraLoja, CPF_VALIDO, "Cliente da outra", null);
        OrdemServico daOutra = osService.criar(comPapel(outraLoja, PapelMembro.DONO), outroCliente.getId(), "Moto", null,
                "Não pega", null, 0, null);
        assertThat(daOutra.getNumero()).isEqualTo(1);
    }

    @Test
    @DisplayName("V11: o banco recusa dois números iguais na mesma loja")
    void numeroUnicoNoBanco() {
        OrdemServico os = novaOs(dono);
        limparSessao();
        assertThatThrownBy(() -> jdbc.update("insert into ordem_servico (usuario_codigo, numero, parceiro_id, "
                        + "equipamento_id, defeito) select usuario_codigo, numero, parceiro_id, equipamento_id, 'x' "
                        + "from ordem_servico where id = ?", os.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V11: o banco recusa status inventado")
    void statusInvalidoNoBanco() {
        OrdemServico os = novaOs(dono);
        limparSessao();
        assertThatThrownBy(() -> jdbc.update("update ordem_servico set status = 'PRONTA' where id = ?", os.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o mesmo equipamento do mesmo cliente é reaproveitado entre ordens")
    void equipamentoReaproveitado() {
        OrdemServico a = novaOs(dono);
        OrdemServico b = novaOs(dono);
        assertThat(b.getEquipamento().getId()).isEqualTo(a.getEquipamento().getId());
        Long total = jdbc.queryForObject("select count(*) from equipamento_cliente where usuario_codigo = ?",
                Long.class, loja.getCodigo());
        assertThat(total).isEqualTo(1);
    }

    // ---------- fluxo com estoque ----------

    @Test
    @DisplayName("aprovar reserva sem mexer no saldo; concluir baixa com preço e custo gravados e documento OS")
    void fluxoCompleto() {
        OrdemServico os = novaOs(mecanico);
        osService.adicionarPeca(mecanico, os.getId(), produto.getId(), 3, null);
        osService.adicionarServico(mecanico, os.getId(), "Troca do filtro", new BigDecimal("50.00"));
        assertThat(osService.detalhe(loja.getCodigo(), os.getId()).os().getTotal()).isEqualByComparingTo("110.00");

        osService.avancar(mecanico, os.getId());
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(10);
        assertThat(reservadoAtivo()).isEqualTo(3);
        SaldoProduto saldo = estoqueService.saldoDoProduto(produto, loja.getCodigo());
        assertThat(saldo.disponivel()).isEqualTo(7);

        // o preço do cadastro muda depois: a OS guarda o preço da hora
        produto.setPrecoVenda(new BigDecimal("99.00"));
        produto.setCusto(new BigDecimal("8.00"));
        produtoRepository.saveAndFlush(produto);

        osService.avancar(mecanico, os.getId());
        osService.avancar(mecanico, os.getId());
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(7);
        assertThat(reservadoAtivo()).isZero();

        Map<String, Object> saida = jdbc.queryForMap("select tipo, quantidade, tipo_documento, documento_id, "
                + "preco_unitario, custo_unitario from movimentacao_estoque where produto_id = ? and tipo = 'SAIDA'",
                produto.getId());
        assertThat(saida.get("tipo_documento")).isEqualTo("OS");
        assertThat(saida.get("documento_id")).isEqualTo(os.getId());
        assertThat(saida.get("quantidade")).isEqualTo(3);
        assertThat((BigDecimal) saida.get("preco_unitario")).isEqualByComparingTo("20.00");
        assertThat((BigDecimal) saida.get("custo_unitario")).isEqualByComparingTo("4.00");
        assertThat(jdbc.queryForObject("select status from reserva_estoque where documento_id = ?", String.class,
                os.getId())).isEqualTo("BAIXADA");

        OrdemServico entregue = osService.avancar(mecanico, os.getId());
        assertThat(entregue.getStatus()).isEqualTo(StatusOs.ENTREGUE);
        assertThat(entregue.getEntregueEm()).isNotNull();
    }

    @Test
    @DisplayName("peça sem saldo disponível bloqueia a aprovação e nada fica reservado")
    void aprovarSemSaldo() {
        OrdemServico os = novaOs(dono);
        osService.adicionarPeca(dono, os.getId(), produto.getId(), 11, null);

        assertThatThrownBy(() -> osService.avancar(dono, os.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Só há 10 unidade(s) disponíveis");
        assertThat(reservadoAtivo()).isZero();
        assertThat(jdbc.queryForObject("select status from ordem_servico where id = ?", String.class, os.getId()))
                .isEqualTo("ORCAMENTO");
    }

    @Test
    @DisplayName("duas OS aprovadas disputam o disponível: a segunda é recusada")
    void duasOsDisputamSaldo() {
        OrdemServico a = novaOs(dono);
        osService.adicionarPeca(dono, a.getId(), produto.getId(), 6, null);
        OrdemServico b = novaOs(dono);
        osService.adicionarPeca(dono, b.getId(), produto.getId(), 6, null);

        osService.avancar(dono, a.getId());
        assertThatThrownBy(() -> osService.avancar(dono, b.getId()))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Só há 4");
    }

    @Test
    @DisplayName("cancelar OS aprovada libera a reserva; editar peça em aprovada reajusta a reserva")
    void cancelarELiberar() {
        OrdemServico os = novaOs(dono);
        osService.adicionarPeca(dono, os.getId(), produto.getId(), 3, null);
        osService.avancar(dono, os.getId());
        assertThat(reservadoAtivo()).isEqualTo(3);

        var peca = osService.adicionarPeca(dono, os.getId(), produto.getId(), 2, null);
        assertThat(reservadoAtivo()).isEqualTo(5);
        osService.removerPeca(dono, os.getId(), peca.getId());
        assertThat(reservadoAtivo()).isEqualTo(3);

        osService.cancelar(mecanico, os.getId());
        assertThat(reservadoAtivo()).isZero();
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(10);
        assertThat(osService.detalhe(loja.getCodigo(), os.getId()).os().getStatus()).isEqualTo(StatusOs.CANCELADA);
    }

    @Test
    @DisplayName("dono cancela OS concluída: a saída é estornada e o saldo volta; mecânico não pode")
    void cancelarConcluida() {
        OrdemServico os = novaOs(dono);
        osService.adicionarPeca(dono, os.getId(), produto.getId(), 4, null);
        osService.avancar(dono, os.getId());
        osService.avancar(dono, os.getId());
        osService.avancar(dono, os.getId());
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(6);

        assertThatThrownBy(() -> osService.cancelar(mecanico, os.getId()))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(6);

        osService.cancelar(dono, os.getId());
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(10);
        Long estornos = jdbc.queryForObject("select count(*) from movimentacao_estoque where produto_id = ? "
                + "and estorno_de_id is not null and tipo_documento = 'OS'", Long.class, produto.getId());
        assertThat(estornos).isEqualTo(1);
    }

    // ---------- isolamento e lista ----------

    @Test
    @DisplayName("outra loja não vê nem mexe na OS (se comporta como inexistente)")
    void isolamentoPorLoja() {
        OrdemServico os = novaOs(dono);
        Usuario outra = comPapel(novoUsuario(), PapelMembro.DONO);

        assertThatThrownBy(() -> osService.detalhe(outra.getCodigo(), os.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> osService.avancar(outra, os.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> osService.adicionarServico(outra, os.getId(), "x", BigDecimal.ONE))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(osService.listar(outra.getCodigo(), null)).isEmpty();
    }

    @Test
    @DisplayName("a OS não aceita cliente de outra loja")
    void clienteDeOutraLoja() {
        Usuario outra = comPapel(novoUsuario(), PapelMembro.DONO);
        assertThatThrownBy(() -> osService.criar(outra, cliente.getId(), "Moto", null, "x", null, 0, null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("cliente");
    }

    @Test
    @DisplayName("a lista filtra por status e conta cada situação")
    void listaPorStatus() {
        OrdemServico a = novaOs(dono);
        novaOs(dono);
        osService.avancar(dono, a.getId());

        List<OrdemServico> aprovadas = osService.listar(loja.getCodigo(), StatusOs.APROVADA);
        assertThat(aprovadas).extracting(OrdemServico::getId).containsExactly(a.getId());
        assertThat(osService.listar(loja.getCodigo(), null)).hasSize(2);
        Map<StatusOs, Long> contagem = osService.contarPorStatus(loja.getCodigo());
        assertThat(contagem.get(StatusOs.ORCAMENTO)).isEqualTo(1);
        assertThat(contagem.get(StatusOs.APROVADA)).isEqualTo(1);
        assertThat(contagem.get(StatusOs.CANCELADA)).isZero();
    }

    @Test
    @DisplayName("o desconto de mecânico acima de 10% é recusado no banco real também")
    void descontoMecanico() {
        OrdemServico os = novaOs(mecanico);
        osService.adicionarServico(mecanico, os.getId(), "Mão de obra", new BigDecimal("100.00"));
        osService.atualizarDados(mecanico, os.getId(), "Barulho", null, 90, null, new BigDecimal("10.00"));
        assertThatThrownBy(() -> osService.atualizarDados(mecanico, os.getId(), "Barulho", null, 90, null,
                new BigDecimal("10.01"))).isInstanceOf(RegraNegocioException.class);
        osService.atualizarDados(dono, os.getId(), "Barulho", null, 90, null, new BigDecimal("30.00"));
        assertThat(osService.detalhe(loja.getCodigo(), os.getId()).os().getTotal()).isEqualByComparingTo("70.00");
    }
}
