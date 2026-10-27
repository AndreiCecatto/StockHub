package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.SaldoProduto;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.PapelMembro;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.ReservaEstoque;
import org.example.stockhubspring.model.StatusReserva;
import org.example.stockhubspring.model.TipoDocumento;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migration V10 e as regras RN-67 a RN-76 contra o PostgreSQL: documento de origem, preço no movimento,
 * estorno, reserva de estoque e clientes. Cada violação de restrição aborta a transação do PostgreSQL,
 * então cada teste provoca no máximo UMA.
 */
class EstornoReservaIntegracaoTest extends IntegracaoBase {

    @Autowired
    private ClienteService clienteService;
    @Autowired
    private ParceiroRepository parceiroRepository;

    private Usuario loja;
    private Usuario dono;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
        dono = comPapel(loja, PapelMembro.DONO);
    }

    /** A mesma loja, vista por uma pessoa com o papel dado (o papel só existe na sessão, não no banco). */
    private Usuario comPapel(Usuario base, PapelMembro papel) {
        Usuario u = new Usuario();
        u.setCodigo(base.getCodigo());
        u.setNome(base.getNome());
        u.setEmail(base.getEmail());
        u.setPapel(papel);
        return u;
    }

    // ---------- V10: documento de origem ----------

    @Test
    @DisplayName("movimento sem documento nasce MANUAL e o banco tem o padrão MANUAL")
    void movimentoManual() {
        Produto p = novoProduto(loja, categoria, "Caneta", 5, 0);

        MovimentacaoEstoque mov = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 2, "Compra", dono);
        limparSessao();

        MovimentacaoEstoque lida = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        assertThat(lida.getTipoDocumento()).isEqualTo(TipoDocumento.MANUAL);
        assertThat(lida.getDocumentoId()).isNull();
        assertThat(lida.getPrecoUnitario()).isNull();
        assertThat(lida.getEstornoDeId()).isNull();
    }

    @Test
    @DisplayName("compra, venda e OS exigem o código do documento; manual não pode ter")
    void documentoObrigatorioPorTipo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 5, 0);

        for (TipoDocumento tipo : List.of(TipoDocumento.COMPRA_NFE, TipoDocumento.VENDA, TipoDocumento.OS)) {
            assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, "x", dono, tipo, null))
                    .isInstanceOf(RegraNegocioException.class);
        }
        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, "x", dono,
                TipoDocumento.MANUAL, 4)).isInstanceOf(RegraNegocioException.class);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("perda, inventário e devolução podem ou não ter documento")
    void documentoOpcional() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);

        estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 1, "Quebrou", dono, TipoDocumento.PERDA, null);
        estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 1, "Contagem", dono, TipoDocumento.INVENTARIO, 3);
        MovimentacaoEstoque devolucao = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, "Voltou",
                dono, TipoDocumento.DEVOLUCAO, null);

        assertThat(devolucao.getTipoDocumento()).isEqualTo(TipoDocumento.DEVOLUCAO);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(9);
    }

    @Test
    @DisplayName("o preço de venda informado fica gravado no movimento; preço negativo é recusado")
    void precoNoMovimento() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);

        MovimentacaoEstoque venda = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 2, "Venda", dono,
                TipoDocumento.VENDA, 77, null, new BigDecimal("12.5000"));
        limparSessao();

        assertThat(movimentacaoRepository.findById(venda.getId()).orElseThrow().getPrecoUnitario())
                .isEqualByComparingTo("12.50");
        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 1, "x", dono,
                TipoDocumento.VENDA, 78, null, new BigDecimal("-1")))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("o produto aceita preço de venda nulo e o efetivo cai no preço do cadastro")
    void precoVendaDoProduto() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0, new BigDecimal("10.00"));
        assertThat(p.getPrecoVenda()).isNull();
        assertThat(p.getPrecoDeVenda()).isEqualByComparingTo("10.00");

        p.setPrecoVenda(new BigDecimal("14.9000"));
        produtoRepository.saveAndFlush(p);
        limparSessao();

        assertThat(produtoRepository.findById(p.getId()).orElseThrow().getPrecoDeVenda()).isEqualByComparingTo("14.90");
    }

    @Test
    @DisplayName("o banco recusa preço negativo no movimento")
    void bancoRecusaPrecoNegativo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        MovimentacaoEstoque mov = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 1, "x", dono);

        assertThatThrownBy(() -> jdbc.update("update movimentacao_estoque set preco_unitario = -1 where id = ?", mov.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- estorno ----------

    @Test
    @DisplayName("desfazer uma entrada gera uma saída que aponta para a original, e a original fica intacta")
    void estornoDeEntrada() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque entrada = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 4, "Compra", dono,
                TipoDocumento.DEVOLUCAO, null, new BigDecimal("3.0000"), new BigDecimal("9.0000"));

        MovimentacaoEstoque estorno = estoqueService.estornar(entrada.getId(), "Lancei errado", dono);
        limparSessao();

        assertThat(estorno.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(estorno.getQuantidade()).isEqualTo(4);
        assertThat(estorno.getEstornoDeId()).isEqualTo(entrada.getId());
        assertThat(estorno.getObservacao()).isEqualTo("Estorno: Lancei errado");
        assertThat(estorno.getTipoDocumento()).isEqualTo(TipoDocumento.DEVOLUCAO);
        assertThat(estorno.getCustoUnitario()).isEqualByComparingTo("3.0000");
        assertThat(estorno.getPrecoUnitario()).isEqualByComparingTo("9.0000");
        assertThat(saldoNoBanco(p.getId())).isEqualTo(10);
        MovimentacaoEstoque original = movimentacaoRepository.findById(entrada.getId()).orElseThrow();
        assertThat(original.getQuantidade()).isEqualTo(4);
        assertThat(original.getQuantidadeNova()).isEqualTo(14);
        assertThat(original.getObservacao()).isEqualTo("Compra");
    }

    @Test
    @DisplayName("desfazer uma saída devolve a quantidade ao saldo")
    void estornoDeSaida() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);

        MovimentacaoEstoque estorno = estoqueService.estornar(saida.getId(), "Cliente desistiu", dono);

        assertThat(estorno.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(10);
    }

    @Test
    @DisplayName("desfazer um ajuste aplica a diferença que ele causou, não a contagem")
    void estornoDeAjuste() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque ajuste = estoqueService.movimentar(p.getId(), TipoMovimentacao.AJUSTE, 6, "Contagem", dono);
        estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "Compra", dono);

        MovimentacaoEstoque estorno = estoqueService.estornar(ajuste.getId(), "Contei errado", dono);

        // o ajuste tirou 4; desfazer devolve 4 ao saldo de agora (11), não volta para 10
        assertThat(estorno.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(estorno.getQuantidade()).isEqualTo(4);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(15);
    }

    @Test
    @DisplayName("cada movimentação só é desfeita uma vez, e um estorno não é desfeito")
    void umaVezSo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);
        MovimentacaoEstoque estorno = estoqueService.estornar(saida.getId(), "Erro", dono);

        assertThatThrownBy(() -> estoqueService.estornar(saida.getId(), "De novo", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("já foi desfeita");
        assertThatThrownBy(() -> estoqueService.estornar(estorno.getId(), "Desfazer o estorno", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("estorno não pode ser desfeito");
        assertThat(saldoNoBanco(p.getId())).isEqualTo(10);
        assertThat(contarMovimentacoesDoProduto(p.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("o banco impede dois estornos da mesma movimentação")
    void bancoImpedeEstornoDuplo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);
        estoqueService.estornar(saida.getId(), "Erro", dono);

        MovimentacaoEstoque segundo = new MovimentacaoEstoque();
        segundo.setProduto(p);
        segundo.setUsuario(loja);
        segundo.setTipo(TipoMovimentacao.ENTRADA);
        segundo.setQuantidade(3);
        segundo.setQuantidadeAnterior(10);
        segundo.setQuantidadeNova(13);
        segundo.setEstornoDeId(saida.getId());

        assertThatThrownBy(() -> movimentacaoRepository.saveAndFlush(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("não desfaz se o saldo ficaria negativo, e nada é gravado")
    void estornoRespeitaSaldoNaoNegativo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 2, 0);
        MovimentacaoEstoque entrada = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "Compra", dono);
        estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 6, "Venda", dono);

        assertThatThrownBy(() -> estoqueService.estornar(entrada.getId(), "Erro", dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(1);
        assertThat(contarMovimentacoesDoProduto(p.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("só dono e gerente desfazem; motivo é obrigatório")
    void papelEMotivo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);

        for (PapelMembro papel : List.of(PapelMembro.OPERADOR, PapelMembro.VENDEDOR, PapelMembro.MECANICO)) {
            assertThatThrownBy(() -> estoqueService.estornar(saida.getId(), "Erro", comPapel(loja, papel)))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("dono ou o gerente");
        }
        assertThatThrownBy(() -> estoqueService.estornar(saida.getId(), "   ", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("motivo");
        assertThat(saldoNoBanco(p.getId())).isEqualTo(7);

        estoqueService.estornar(saida.getId(), "Erro", comPapel(loja, PapelMembro.GERENTE));
        assertThat(saldoNoBanco(p.getId())).isEqualTo(10);
    }

    @Test
    @DisplayName("o estoque inicial não se desfaz")
    void estoqueInicialNaoSeDesfaz() {
        Produto p = novoProduto(loja, categoria, "Caneta", 0, 0);
        MovimentacaoEstoque inicial = estoqueService.registrarEstoqueInicial(p.getId(), 5, dono);

        assertThatThrownBy(() -> estoqueService.estornar(inicial.getId(), "Erro", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("estoque inicial");
        assertThat(estoqueService.estornaveis(List.of(inicial), dono)).isEmpty();
    }

    @Test
    @DisplayName("movimentação de outra loja não existe para quem pede")
    void estornoNaoCruzaLojas() {
        Usuario outra = comPapel(novoUsuario(), PapelMembro.DONO);
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);

        assertThatThrownBy(() -> estoqueService.estornar(saida.getId(), "Erro", outra))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(7);
    }

    @Test
    @DisplayName("estornaveis e jaDesfeitas dizem o que o botão Desfazer deve mostrar")
    void estornaveisEMarcas() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque a = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 1, "A", dono);
        MovimentacaoEstoque b = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 1, "B", dono);
        MovimentacaoEstoque estornoDeA = estoqueService.estornar(a.getId(), "Erro", dono);
        limparSessao();
        List<MovimentacaoEstoque> todas = movimentacaoRepository.findAllById(
                List.of(a.getId(), b.getId(), estornoDeA.getId()));

        assertThat(estoqueService.jaDesfeitas(todas)).containsExactly(a.getId());
        assertThat(estoqueService.estornaveis(todas, dono)).containsExactly(b.getId());
        assertThat(estoqueService.estornaveis(todas, comPapel(loja, PapelMembro.OPERADOR))).isEmpty();
    }

    @Test
    @DisplayName("o estorno sai da conta das análises: a saída desfeita e o estorno não contam")
    void analisesIgnoramEstorno() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        MovimentacaoEstoque saida = estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Venda", dono);
        estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 1, "Outra venda", dono);
        estoqueService.estornar(saida.getId(), "Erro", dono);
        limparSessao();

        var inicio = java.time.LocalDateTime.now().minusDays(1);
        var fim = java.time.LocalDateTime.now().plusDays(1);
        var porProduto = movimentacaoRepository.saidasPorProduto(loja.getCodigo(), inicio, fim);

        assertThat(porProduto).hasSize(1);
        assertThat(porProduto.get(0).unidades()).isEqualTo(1L);
        assertThat(movimentacaoRepository.saidasPorDia(loja.getCodigo(), inicio, fim)).hasSize(1);
        assertThat(movimentacaoRepository.caixaPorMes(loja.getCodigo(), inicio, fim)).hasSize(1);
    }

    // ---------- reserva ----------

    @Test
    @DisplayName("reservar não muda o saldo e reduz o disponível")
    void reservaReduzDisponivel() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);

        ReservaEstoque r = estoqueService.reservar(p.getId(), 4, TipoDocumento.VENDA, 100, dono);
        limparSessao();

        assertThat(r.getStatus()).isEqualTo(StatusReserva.ATIVA);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(10);
        Produto lido = produtoRepository.findById(p.getId()).orElseThrow();
        assertThat(estoqueService.saldoDoProduto(lido, loja.getCodigo()))
                .isEqualTo(new SaldoProduto(10, 4, 6));
    }

    @Test
    @DisplayName("não reserva mais que o disponível nem para outro tipo de documento")
    void reservaLimites() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        estoqueService.reservar(p.getId(), 8, TipoDocumento.VENDA, 1, dono);

        assertThatThrownBy(() -> estoqueService.reservar(p.getId(), 3, TipoDocumento.OS, 2, dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Só há 2");
        assertThatThrownBy(() -> estoqueService.reservar(p.getId(), 1, TipoDocumento.MANUAL, 2, dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoqueService.reservar(p.getId(), 0, TipoDocumento.OS, 2, dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoqueService.reservar(p.getId(), 1, TipoDocumento.OS, null, dono))
                .isInstanceOf(RegraNegocioException.class);
        estoqueService.reservar(p.getId(), 2, TipoDocumento.OS, 2, dono);
    }

    @Test
    @DisplayName("saída manual não consome o que está reservado, mas usa o disponível")
    void saidaManualRespeitaReserva() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        estoqueService.reservar(p.getId(), 8, TipoDocumento.VENDA, 1, dono);

        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 3, "Uso", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("reservada");
        assertThat(saldoNoBanco(p.getId())).isEqualTo(10);

        estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 2, "Uso", dono);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(8);
    }

    @Test
    @DisplayName("ajuste para baixo e estorno de entrada também não passam do que está reservado")
    void ajusteEEstornoRespeitamReserva() {
        Produto p = novoProduto(loja, categoria, "Caneta", 5, 0);
        MovimentacaoEstoque entrada = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "Compra", dono);
        estoqueService.reservar(p.getId(), 8, TipoDocumento.VENDA, 1, dono);

        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.AJUSTE, 7, "Contagem", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("reservada");
        assertThatThrownBy(() -> estoqueService.estornar(entrada.getId(), "Erro", dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("reservada");
        estoqueService.movimentar(p.getId(), TipoMovimentacao.AJUSTE, 12, "Contagem maior", dono);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(12);
    }

    @Test
    @DisplayName("liberar devolve a quantidade ao disponível e não se libera duas vezes")
    void liberar() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        ReservaEstoque r = estoqueService.reservar(p.getId(), 8, TipoDocumento.VENDA, 1, dono);

        assertThat(estoqueService.liberar(r.getId(), dono).getStatus()).isEqualTo(StatusReserva.CANCELADA);
        limparSessao();
        estoqueService.movimentar(p.getId(), TipoMovimentacao.SAIDA, 10, "Tudo", dono);

        assertThatThrownBy(() -> estoqueService.liberar(r.getId(), dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("cancelada");
        assertThatThrownBy(() -> estoqueService.baixarReserva(r.getId(), dono))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("baixar a reserva gera a saída com o documento de origem e o preço, e baixa uma vez só")
    void baixarReserva() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        ReservaEstoque r = estoqueService.reservar(p.getId(), 8, TipoDocumento.VENDA, 55, dono);

        MovimentacaoEstoque saida = estoqueService.baixarReserva(r.getId(), dono, new BigDecimal("20.00"));
        limparSessao();

        assertThat(saida.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(saida.getQuantidade()).isEqualTo(8);
        assertThat(saida.getTipoDocumento()).isEqualTo(TipoDocumento.VENDA);
        assertThat(saida.getDocumentoId()).isEqualTo(55);
        assertThat(saida.getPrecoUnitario()).isEqualByComparingTo("20.00");
        assertThat(saldoNoBanco(p.getId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("select status from reserva_estoque where id = ?", String.class, r.getId()))
                .isEqualTo("BAIXADA");
        assertThatThrownBy(() -> estoqueService.baixarReserva(r.getId(), dono))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("baixada");
        assertThat(saldoNoBanco(p.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("baixar uma reserva não toca no que outras reservas seguram")
    void baixaMantemOutrasReservas() {
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        ReservaEstoque a = estoqueService.reservar(p.getId(), 4, TipoDocumento.VENDA, 1, dono);
        estoqueService.reservar(p.getId(), 6, TipoDocumento.OS, 2, dono);

        estoqueService.baixarReserva(a.getId(), dono);

        Produto lido = produtoRepository.findById(p.getId()).orElseThrow();
        assertThat(estoqueService.saldoDoProduto(lido, loja.getCodigo())).isEqualTo(new SaldoProduto(6, 6, 0));
    }

    @Test
    @DisplayName("baixar ou liberar o documento inteiro trata todas as reservas dele")
    void documentoInteiro() {
        Produto a = novoProduto(loja, categoria, "A", 10, 0);
        Produto b = novoProduto(loja, categoria, "B", 10, 0);
        estoqueService.reservar(b.getId(), 2, TipoDocumento.VENDA, 9, dono);
        estoqueService.reservar(a.getId(), 3, TipoDocumento.VENDA, 9, dono);
        estoqueService.reservar(a.getId(), 1, TipoDocumento.VENDA, 10, dono);

        List<MovimentacaoEstoque> saidas = estoqueService.baixarReservasDoDocumento(TipoDocumento.VENDA, 9, dono);

        // em ordem crescente de produto (A antes de B), como a trava de saldo exige
        assertThat(saidas).extracting(m -> m.getProduto().getId()).containsExactly(a.getId(), b.getId());
        assertThat(saldoNoBanco(a.getId())).isEqualTo(7);
        assertThat(saldoNoBanco(b.getId())).isEqualTo(8);
        assertThat(estoqueService.liberarReservasDoDocumento(TipoDocumento.VENDA, 10, dono)).isEqualTo(1);
        assertThat(estoqueService.liberarReservasDoDocumento(TipoDocumento.VENDA, 10, dono)).isZero();
    }

    @Test
    @DisplayName("reserva de outra loja não existe para quem pede")
    void reservaNaoCruzaLojas() {
        Usuario outra = comPapel(novoUsuario(), PapelMembro.DONO);
        Produto p = novoProduto(loja, categoria, "Caneta", 10, 0);
        ReservaEstoque r = estoqueService.reservar(p.getId(), 2, TipoDocumento.VENDA, 1, dono);

        assertThatThrownBy(() -> estoqueService.liberar(r.getId(), outra))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> estoqueService.baixarReserva(r.getId(), outra))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("produto com reserva não é apagado de vez: só inativado")
    void produtoComReservaSoInativa() {
        Produto p = novoProduto(loja, categoria, "Caneta", 5, 0);
        ReservaEstoque r = estoqueService.reservar(p.getId(), 2, TipoDocumento.VENDA, 1, dono);
        estoqueService.liberar(r.getId(), dono);
        limparSessao();

        var resultado = produtoService.excluir(p.getId(), loja.getCodigo());

        assertThat(resultado.name()).isEqualTo("INATIVADO");
        assertThat(jdbc.queryForObject("select ativo from produto where id = ?", Boolean.class, p.getId())).isFalse();
    }

    @Test
    @DisplayName("o banco recusa reserva com quantidade zero")
    void bancoRecusaReservaInvalida() {
        Produto p = novoProduto(loja, categoria, "Caneta", 5, 0);

        assertThatThrownBy(() -> jdbc.update("insert into reserva_estoque (usuario_codigo, produto_id, quantidade, "
                + "documento_tipo, documento_id) values (?, ?, 0, 'VENDA', 1)", loja.getCodigo(), p.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- clientes ----------

    @Test
    @DisplayName("cadastra cliente com CPF válido e só da própria loja")
    void cadastraCliente() {
        Parceiro c = clienteService.cadastrar(loja, "529.982.247-25", "  Ana Souza ", "rs");
        limparSessao();

        Parceiro lido = parceiroRepository.findById(c.getId()).orElseThrow();
        assertThat(lido.isCliente()).isTrue();
        assertThat(lido.isFornecedor()).isFalse();
        assertThat(lido.getDocumento()).isEqualTo("52998224725");
        assertThat(lido.getNome()).isEqualTo("Ana Souza");
        assertThat(lido.getUf()).isEqualTo("RS");
        assertThat(clienteService.listar(loja.getCodigo())).extracting(Parceiro::getId).containsExactly(c.getId());
        assertThat(clienteService.listar(novoUsuario().getCodigo())).isEmpty();
    }

    @Test
    @DisplayName("documento inválido, nome vazio, UF errada e cliente repetido são recusados")
    void validaCliente() {
        assertThatThrownBy(() -> clienteService.cadastrar(loja, "11111111111", "Ana", null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> clienteService.cadastrar(loja, "52998224725", "  ", null))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> clienteService.cadastrar(loja, "52998224725", "Ana", "RSX"))
                .isInstanceOf(RegraNegocioException.class);
        clienteService.cadastrar(loja, "52998224725", "Ana", null);
        assertThatThrownBy(() -> clienteService.cadastrar(loja, "529.982.247-25", "Outra Ana", null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
    }

    @Test
    @DisplayName("um fornecedor já cadastrado vira também cliente, sem trocar o nome")
    void fornecedorViraCliente() {
        Parceiro fornecedor = new Parceiro();
        fornecedor.setUsuario(loja);
        fornecedor.setDocumento("52998224725");
        fornecedor.setNome("Nome do fornecedor");
        fornecedor.setCriadoEm(java.time.LocalDateTime.now());
        parceiroRepository.saveAndFlush(fornecedor);

        Parceiro cliente = clienteService.cadastrar(loja, "52998224725", "Outro nome", null);
        limparSessao();

        Parceiro lido = parceiroRepository.findById(cliente.getId()).orElseThrow();
        assertThat(lido.getId()).isEqualTo(fornecedor.getId());
        assertThat(lido.isCliente()).isTrue();
        assertThat(lido.isFornecedor()).isTrue();
        assertThat(lido.getNome()).isEqualTo("Nome do fornecedor");
        assertThat(Set.copyOf(clienteService.listar(loja.getCodigo()))).hasSize(1);
    }
}
