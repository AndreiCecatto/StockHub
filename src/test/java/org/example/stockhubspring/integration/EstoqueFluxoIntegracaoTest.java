package org.example.stockhubspring.integration;

import jakarta.persistence.LockModeType;
import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.EstoqueService;
import org.example.stockhubspring.service.ProdutoService.ResultadoExclusao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Fluxos de estoque de ponta a ponta (services + repositórios + PostgreSQL real, transação revertida). */
class EstoqueFluxoIntegracaoTest extends IntegracaoBase {

    private Usuario loja;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
    }

    private ProdutoForm form(String nome, Integer quantidade) {
        ProdutoForm form = new ProdutoForm();
        form.setNome(nome);
        form.setPreco(new BigDecimal("12.50"));
        form.setQuantidade(quantidade);
        form.setEstoqueMinimo(2);
        form.setCategoriaId(categoria.getId());
        return form;
    }

    private List<MovimentacaoEstoque> historicoDoProduto(Integer produtoId) {
        return movimentacaoRepository.findAll().stream()
                .filter(m -> m.getProduto().getId().equals(produtoId))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
    }

    // ----- RN-04: estoque inicial -----

    @Test
    @DisplayName("criar produto com estoque inicial grava o saldo e uma ENTRADA 'Estoque inicial' (anterior 0, nova = inicial)")
    void estoqueInicialGeraEntradaNoBanco() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        limparSessao();

        assertThat(saldoNoBanco(produto.getId())).isEqualTo(10);
        List<MovimentacaoEstoque> historico = historicoDoProduto(produto.getId());
        assertThat(historico).hasSize(1);
        MovimentacaoEstoque mov = historico.get(0);
        assertThat(mov.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(mov.getObservacao()).isEqualTo("Estoque inicial");
        assertThat(mov.getQuantidade()).isEqualTo(10);
        assertThat(mov.getQuantidadeAnterior()).isZero();
        assertThat(mov.getQuantidadeNova()).isEqualTo(10);
        assertThat(mov.getUsuario().getCodigo()).isEqualTo(loja.getCodigo());
        assertThat(mov.getCriadoEm()).isNotNull();
    }

    @Test
    @DisplayName("criar produto sem estoque inicial não gera movimentação")
    void semEstoqueInicialNaoGeraMovimentacao() {
        Produto produto = produtoService.salvar(form("Caneta", 0), loja);
        limparSessao();

        assertThat(saldoNoBanco(produto.getId())).isZero();
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isZero();
    }

    // ----- RN-02, RN-05, RN-06: movimentações -----

    @Test
    @DisplayName("entrada, saída e ajuste gravam saldo e histórico coerentes (anterior/nova encadeados)")
    void sequenciaDeMovimentacoes() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);

        estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 5, "Compra", loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 3, "Venda", loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.AJUSTE, 8, "Contagem", loja);
        limparSessao();

        assertThat(saldoNoBanco(produto.getId())).isEqualTo(8);
        List<MovimentacaoEstoque> historico = historicoDoProduto(produto.getId());
        assertThat(historico).extracting(MovimentacaoEstoque::getQuantidadeAnterior).containsExactly(0, 10, 15, 12);
        assertThat(historico).extracting(MovimentacaoEstoque::getQuantidadeNova).containsExactly(10, 15, 12, 8);
        assertThat(historico).extracting(MovimentacaoEstoque::getTipo).containsExactly(
                TipoMovimentacao.ENTRADA, TipoMovimentacao.ENTRADA, TipoMovimentacao.SAIDA, TipoMovimentacao.AJUSTE);
    }

    @Test
    @DisplayName("saída acima do saldo é recusada: o saldo e o histórico no banco não mudam")
    void saidaAcimaDoSaldoNaoGravaNada() {
        Produto produto = produtoService.salvar(form("Caneta", 6), loja);
        limparSessao();

        assertThatThrownBy(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 7, null, loja))
                .isInstanceOf(RegraNegocioException.class);

        assertThat(saldoNoBanco(produto.getId())).isEqualTo(6);
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isEqualTo(1); // só o estoque inicial
    }

    @Test
    @DisplayName("ajuste para zero é aceito; ajuste sem motivo é recusado e nada muda")
    void ajusteParaZeroEAjusteSemMotivo() {
        Produto produto = produtoService.salvar(form("Caneta", 6), loja);

        assertThatThrownBy(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.AJUSTE, 1, "  ", loja))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(6);

        estoqueService.movimentar(produto.getId(), TipoMovimentacao.AJUSTE, 0, "Perda total", loja);
        assertThat(saldoNoBanco(produto.getId())).isZero();
    }

    @Test
    @DisplayName("limite de saldo no banco: entrada que passaria de 2.000.000.000 é recusada sem estourar o int")
    void limiteDeSaldoNoBanco() {
        Produto produto = novoProduto(loja, categoria, "Gigante", EstoqueService.SALDO_MAXIMO - 10, 0);
        limparSessao();

        assertThatThrownBy(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 11, null, loja))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(EstoqueService.SALDO_MAXIMO - 10);

        estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 10, null, loja);
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(EstoqueService.SALDO_MAXIMO);
    }

    @Test
    @DisplayName("movimentação é imutável: alterar a entidade depois de gravada não muda a linha no banco (RN-06)")
    void movimentacaoEImutavel() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        MovimentacaoEstoque mov = estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 4, "Venda", loja);
        limparSessao();

        MovimentacaoEstoque carregada = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        carregada.setQuantidade(999);
        carregada.setQuantidadeNova(999);
        carregada.setObservacao("adulterado");
        carregada.setTipo(TipoMovimentacao.ENTRADA);
        limparSessao();

        MovimentacaoEstoque relida = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        assertThat(relida.getQuantidade()).isEqualTo(4);
        assertThat(relida.getQuantidadeNova()).isEqualTo(6);
        assertThat(relida.getObservacao()).isEqualTo("Venda");
        assertThat(relida.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
    }

    // ----- RN-07: bloqueio pessimista -----

    @Test
    @DisplayName("a leitura para movimentar trava a linha do produto (PESSIMISTIC_WRITE)")
    void leituraParaMovimentarUsaBloqueioPessimista() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 5, 1);
        limparSessao();

        Optional<Produto> lido = produtoRepository.buscarAtivoComBloqueio(produto.getId(), loja.getCodigo());

        assertThat(lido).isPresent();
        assertThat(em.getLockMode(lido.get())).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @DisplayName("a consulta com bloqueio respeita dono e ativo: outra loja ou produto inativo não é encontrado")
    void consultaComBloqueioRespeitaDonoEAtivo() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 5, 1);
        Usuario outra = novoUsuario();
        limparSessao();

        assertThat(produtoRepository.buscarAtivoComBloqueio(produto.getId(), outra.getCodigo())).isEmpty();

        jdbc.update("update produto set ativo = false where id = ?", produto.getId());
        limparSessao();
        assertThat(produtoRepository.buscarAtivoComBloqueio(produto.getId(), loja.getCodigo())).isEmpty();
    }

    @Test
    @DisplayName("@DynamicUpdate: editar o nome não reescreve o saldo (não pisa numa movimentação concorrente)")
    void editarNomeNaoReescreveOSaldo() {
        Produto produto = novoProduto(loja, categoria, "Caneta", 5, 1);
        limparSessao();
        Produto carregado = produtoRepository.findById(produto.getId()).orElseThrow(); // lê saldo 5

        // simula outra transação que mexeu no saldo depois da leitura
        jdbc.update("update produto set quantidade = 9 where id = ?", produto.getId());
        carregado.setNome("Caneta Nova");
        em.flush();

        assertThat(saldoNoBanco(produto.getId())).as("saldo não pode voltar para o valor lido (5)").isEqualTo(9);
        assertThat(jdbc.queryForObject("select nome from produto where id = ?", String.class, produto.getId()))
                .isEqualTo("Caneta Nova");
    }

    // ----- Edição (RN-03) -----

    @Test
    @DisplayName("editar produto pelo formulário ignora a quantidade enviada (RN-03)")
    void editarIgnoraQuantidadeNoBanco() {
        Produto produto = produtoService.salvar(form("Caneta", 7), loja);
        limparSessao();

        ProdutoForm edicao = form("Caneta Premium", 999);
        edicao.setId(produto.getId());
        produtoService.salvar(edicao, loja);
        limparSessao();

        assertThat(saldoNoBanco(produto.getId())).isEqualTo(7);
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isEqualTo(1);
        assertThat(produtoService.buscarPorId(produto.getId(), loja.getCodigo()).getNome()).isEqualTo("Caneta Premium");
    }

    // ----- RN-08: exclusão -----

    @Test
    @DisplayName("excluir produto sem nenhuma movimentação apaga a linha de vez")
    void excluirSemMovimentacaoApaga() {
        Produto produto = produtoService.salvar(form("Caneta", 0), loja);
        limparSessao();

        assertThat(produtoService.excluir(produto.getId(), loja.getCodigo())).isEqualTo(ResultadoExclusao.EXCLUIDO);
        limparSessao();

        assertThat(produtoRepository.existsById(produto.getId())).isFalse();
    }

    @Test
    @DisplayName("excluir produto só com o estoque inicial apaga produto e registro inicial (respeitando a chave estrangeira)")
    void excluirSoComEstoqueInicialApagaTudo() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        limparSessao();
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isEqualTo(1);

        assertThat(produtoService.excluir(produto.getId(), loja.getCodigo())).isEqualTo(ResultadoExclusao.EXCLUIDO);
        limparSessao();

        assertThat(produtoRepository.existsById(produto.getId())).isFalse();
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isZero();
    }

    @Test
    @DisplayName("excluir produto com histórico só inativa: a linha e o histórico permanecem, e ele some das listagens")
    void excluirComHistoricoInativa() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 4, "Venda", loja);
        limparSessao();

        assertThat(produtoService.excluir(produto.getId(), loja.getCodigo())).isEqualTo(ResultadoExclusao.INATIVADO);
        limparSessao();

        assertThat(produtoRepository.findById(produto.getId())).get().extracting(Produto::isAtivo).isEqualTo(false);
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isEqualTo(2);
        assertThat(produtoService.listarPagina(ProdutoFiltro.de(null, null, null, "nome", "asc", 1, 10),
                loja.getCodigo()).getContent()).isEmpty();
        assertThat(produtoService.listarParaSelecao(loja.getCodigo())).isEmpty();
        assertThat(dashboardService.buscarResumo(loja.getCodigo()).getTotalProdutos()).isZero();
        assertThatThrownBy(() -> produtoService.buscarPorId(produto.getId(), loja.getCodigo()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("produto inativado não aceita movimentação")
    void produtoInativadoNaoAceitaMovimentacao() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 4, "Venda", loja);
        produtoService.excluir(produto.getId(), loja.getCodigo());
        limparSessao();

        assertThatThrownBy(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 1, null, loja))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(saldoNoBanco(produto.getId())).isEqualTo(6);
    }

    @Test
    @DisplayName("excluir de novo um produto já inativado responde 'não encontrado'")
    void excluirDuasVezes() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 1, null, loja);
        produtoService.excluir(produto.getId(), loja.getCodigo());
        limparSessao();

        assertThatThrownBy(() -> produtoService.excluir(produto.getId(), loja.getCodigo()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ----- RN-10: nomes únicos -----

    @Test
    @DisplayName("nome duplicado de produto ativo é recusado sem diferenciar caixa e espaços")
    void nomeDuplicadoDeProdutoRecusado() {
        produtoService.salvar(form("Caneta", 0), loja);
        limparSessao();

        assertThatThrownBy(() -> produtoService.salvar(form("  CANETA ", 0), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe um produto ativo");
    }

    @Test
    @DisplayName("o mesmo nome pode existir em outra loja")
    void mesmoNomeEmOutraLojaPermitido() {
        produtoService.salvar(form("Caneta", 0), loja);
        Usuario outra = novoUsuario();
        Categoria outraCategoria = novaCategoria(outra, "Geral");
        ProdutoForm formOutra = form("Caneta", 0);
        formOutra.setCategoriaId(outraCategoria.getId());

        Produto criado = produtoService.salvar(formOutra, outra);

        assertThat(criado.getId()).isNotNull();
    }

    @Test
    @DisplayName("depois de inativar um produto, o nome dele pode ser reutilizado")
    void nomeDeProdutoInativadoPodeSerReutilizado() {
        Produto produto = produtoService.salvar(form("Caneta", 5), loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 1, null, loja);
        produtoService.excluir(produto.getId(), loja.getCodigo());
        limparSessao();

        Produto novo = produtoService.salvar(form("Caneta", 3), loja);

        assertThat(novo.getId()).isNotEqualTo(produto.getId());
    }

    @Test
    @DisplayName("nome com acento: 'ÁGUA' e 'água' são o mesmo nome de produto (a comparação é toda no banco)")
    void nomeComAcentoDetectaDuplicidadeDeProduto() {
        produtoService.salvar(form("ÁGUA", 0), loja);
        limparSessao();

        assertThat(produtoRepository.contarNomeAtivo(loja.getCodigo(), "água", -1)).isEqualTo(1);
        assertThat(produtoRepository.contarNomeAtivo(loja.getCodigo(), "  Água ", -1)).isEqualTo(1);
        assertThatThrownBy(() -> produtoService.salvar(form("água", 0), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe um produto ativo");
    }

    @Test
    @DisplayName("nome com sigma final: o lowercase do Java ('ς') difere do do banco ('σ'), mas a duplicidade é detectada")
    void nomeComSigmaFinalDetectaDuplicidadeDeProduto() {
        // Java: "ΟΔΟΣ".toLowerCase() = "οδος" (sigma final); PostgreSQL lower() = "οδοσ".
        // Comparar um lado em Java e o outro no banco deixava passar a duplicata até o índice único.
        produtoService.salvar(form("ΟΔΟΣ", 0), loja);
        limparSessao();

        assertThat(produtoRepository.contarNomeAtivo(loja.getCodigo(), "ΟΔΟΣ", -1)).isEqualTo(1);
        assertThatThrownBy(() -> produtoService.salvar(form("ΟΔΟΣ", 0), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe um produto ativo");
    }

    @Test
    @DisplayName("nome com acento: 'ÁGUAS' e 'águas' são o mesmo nome de categoria; sigma final também")
    void nomeComAcentoDetectaDuplicidadeDeCategoria() {
        categoriaService.salvar(categoriaForm("ÁGUAS"), loja);
        categoriaService.salvar(categoriaForm("ΟΔΟΣ"), loja);
        limparSessao();

        assertThat(categoriaRepository.contarNome(loja.getCodigo(), "águas", -1)).isEqualTo(1);
        assertThat(categoriaRepository.contarNome(loja.getCodigo(), "ΟΔΟΣ", -1)).isEqualTo(1);
        assertThatThrownBy(() -> categoriaService.salvar(categoriaForm("águas"), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe uma categoria");
        assertThatThrownBy(() -> categoriaService.salvar(categoriaForm("ΟΔΟΣ"), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe uma categoria");
    }

    private org.example.stockhubspring.dto.CategoriaForm categoriaForm(String nome) {
        org.example.stockhubspring.dto.CategoriaForm form = new org.example.stockhubspring.dto.CategoriaForm();
        form.setNome(nome);
        return form;
    }

    @Test
    @DisplayName("movimentação manual com o motivo reservado 'Estoque inicial' é recusada e não grava nada (RN-08)")
    void motivoReservadoRecusadoNaMovimentacaoManual() {
        Produto produto = produtoService.salvar(form("Caneta", 5), loja);
        limparSessao();

        assertThatThrownBy(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 3,
                " ESTOQUE inicial ", loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("reservado");

        assertThat(saldoNoBanco(produto.getId())).isEqualTo(5);
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isEqualTo(1);
        // Com o histórico intacto (só o estoque inicial do sistema), o produto ainda pode ser excluído de vez
        assertThat(produtoService.excluir(produto.getId(), loja.getCodigo())).isEqualTo(ResultadoExclusao.EXCLUIDO);
    }

    @Test
    @DisplayName("editar mantendo o próprio nome não é conflito, mas trocar para o nome de outro produto é")
    void editarComNomeDeOutroProduto() {
        Produto caneta = produtoService.salvar(form("Caneta", 0), loja);
        produtoService.salvar(form("Lápis", 0), loja);
        limparSessao();

        ProdutoForm mesmoNome = form("caneta", 0);
        mesmoNome.setId(caneta.getId());
        produtoService.salvar(mesmoNome, loja); // só muda a caixa

        ProdutoForm nomeDeOutro = form("LÁPIS", 0);
        nomeDeOutro.setId(caneta.getId());
        assertThatThrownBy(() -> produtoService.salvar(nomeDeOutro, loja)).isInstanceOf(RegraNegocioException.class);
    }

    // ----- RN-01: IDOR -----

    @Test
    @DisplayName("outra loja não vê, não edita, não movimenta e não exclui o produto (IDOR) e nada muda no banco")
    void idorEmProduto() {
        Produto produto = produtoService.salvar(form("Caneta", 10), loja);
        Usuario invasor = novoUsuario();
        Categoria categoriaInvasor = novaCategoria(invasor, "Geral");
        limparSessao();

        assertThatThrownBy(() -> produtoService.buscarPorId(produto.getId(), invasor.getCodigo()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 1, null, invasor))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> produtoService.excluir(produto.getId(), invasor.getCodigo()))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        ProdutoForm edicao = form("Invadido", 0);
        edicao.setId(produto.getId());
        edicao.setCategoriaId(categoriaInvasor.getId());
        assertThatThrownBy(() -> produtoService.salvar(edicao, invasor)).isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(saldoNoBanco(produto.getId())).isEqualTo(10);
        assertThat(jdbc.queryForObject("select nome from produto where id = ?", String.class, produto.getId()))
                .isEqualTo("Caneta");
        assertThat(contarMovimentacoesDoProduto(produto.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("não é possível cadastrar produto usando a categoria de outra loja")
    void produtoComCategoriaDeOutraLojaRecusado() {
        Usuario outra = novoUsuario();
        Categoria alheia = novaCategoria(outra, "Alheia");
        ProdutoForm formulario = form("Caneta", 0);
        formulario.setCategoriaId(alheia.getId());

        assertThatThrownBy(() -> produtoService.salvar(formulario, loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Categoria não encontrada");
    }

    // ----- Histórico -----

    @Test
    @DisplayName("histórico lista do mais recente para o mais antigo, só da própria loja, e pagina")
    void historicoOrdenadoPaginadoEIsolado() {
        Produto produto = produtoService.salvar(form("Caneta", 1), loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 2, "segunda", loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.ENTRADA, 3, "terceira", loja);

        Usuario outra = novoUsuario();
        Produto alheio = novoProduto(outra, novaCategoria(outra, "X"), "Alheio", 1, 0);
        estoqueService.movimentar(alheio.getId(), TipoMovimentacao.ENTRADA, 1, "alheia", outra);
        limparSessao();

        Page<MovimentacaoEstoque> primeira = estoqueService.listarMovimentacoes(loja.getCodigo(), 1, 2);
        Page<MovimentacaoEstoque> segunda = estoqueService.listarMovimentacoes(loja.getCodigo(), 2, 2);

        assertThat(primeira.getTotalElements()).isEqualTo(3);
        assertThat(primeira.getContent()).extracting(MovimentacaoEstoque::getObservacao).containsExactly("terceira", "segunda");
        assertThat(segunda.getContent()).extracting(MovimentacaoEstoque::getObservacao).containsExactly("Estoque inicial");
        // produto e categoria já vêm carregados para a tela
        em.clear();
        assertThat(primeira.getContent().get(0).getProduto().getCategoria().getNome()).isEqualTo("Geral");
    }

    // ----- Categorias (RN-09, RN-10) -----

    @Test
    @DisplayName("categoria com produto ativo não exclui e a mensagem diz quantos produtos a usam")
    void categoriaComProdutoAtivoNaoExclui() {
        novoProduto(loja, categoria, "p1", 1, 0);
        novoProduto(loja, categoria, "p2", 1, 0);
        limparSessao();

        assertThatThrownBy(() -> categoriaService.excluir(categoria.getId(), loja.getCodigo()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("2 produtos a usam");
        assertThat(categoriaRepository.existsById(categoria.getId())).isTrue();
    }

    @Test
    @DisplayName("categoria só com produtos inativados também não exclui (histórico preservado)")
    void categoriaComProdutoInativadoNaoExclui() {
        Produto produto = produtoService.salvar(form("Caneta", 5), loja);
        estoqueService.movimentar(produto.getId(), TipoMovimentacao.SAIDA, 1, null, loja);
        produtoService.excluir(produto.getId(), loja.getCodigo());
        limparSessao();

        assertThatThrownBy(() -> categoriaService.excluir(categoria.getId(), loja.getCodigo()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativado");
        assertThat(categoriaRepository.existsById(categoria.getId())).isTrue();
    }

    @Test
    @DisplayName("categoria sem produtos é excluída de verdade")
    void categoriaSemProdutosExclui() {
        Categoria vazia = novaCategoria(loja, "Vazia");
        limparSessao();

        categoriaService.excluir(vazia.getId(), loja.getCodigo());
        limparSessao();

        assertThat(categoriaRepository.existsById(vazia.getId())).isFalse();
    }

    @Test
    @DisplayName("categoria de outra loja não pode ser vista, editada nem excluída (IDOR)")
    void idorEmCategoria() {
        Usuario invasor = novoUsuario();
        limparSessao();

        assertThatThrownBy(() -> categoriaService.buscarPorId(categoria.getId(), invasor.getCodigo()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> categoriaService.excluir(categoria.getId(), invasor.getCodigo()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        org.example.stockhubspring.dto.CategoriaForm edicao = new org.example.stockhubspring.dto.CategoriaForm();
        edicao.setId(categoria.getId());
        edicao.setNome("Invadida");
        assertThatThrownBy(() -> categoriaService.salvar(edicao, invasor))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(jdbc.queryForObject("select nome from categoria where id = ?", String.class, categoria.getId()))
                .isEqualTo("Geral");
    }

    @Test
    @DisplayName("nome duplicado de categoria é recusado sem diferenciar caixa e espaços; em outra loja é permitido")
    void nomeDuplicadoDeCategoria() {
        limparSessao();
        org.example.stockhubspring.dto.CategoriaForm igual = new org.example.stockhubspring.dto.CategoriaForm();
        igual.setNome("  GERAL ");

        assertThatThrownBy(() -> categoriaService.salvar(igual, loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe uma categoria");

        Usuario outra = novoUsuario();
        assertThat(categoriaService.salvar(igual, outra).getId()).isNotNull();
    }

    @Test
    @DisplayName("editar categoria mantendo o próprio nome (só trocando a caixa) é permitido")
    void editarCategoriaMantendoONome() {
        limparSessao();
        org.example.stockhubspring.dto.CategoriaForm edicao = new org.example.stockhubspring.dto.CategoriaForm();
        edicao.setId(categoria.getId());
        edicao.setNome("GERAL");
        edicao.setDescricao("Nova descrição");

        Categoria salva = categoriaService.salvar(edicao, loja);

        assertThat(salva.getNome()).isEqualTo("GERAL");
        assertThat(salva.getDescricao()).isEqualTo("Nova descrição");
    }
}
