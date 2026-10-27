package org.example.stockhubspring.integration;

import jakarta.validation.ConstraintViolationException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.NotaFiscal;
import org.example.stockhubspring.model.OrigemMovimentacao;
import org.example.stockhubspring.model.Parceiro;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.repository.NotaFiscalRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Base das migrations V3 e V4: colunas novas, parceiro, origem da movimentação, CNPJ da loja e as restrições
 * do banco. Cada violação aborta a transação do PostgreSQL, então cada teste provoca no máximo UMA.
 */
class BaseComprasNfeIntegracaoTest extends IntegracaoBase {

    @Autowired
    private ParceiroRepository parceiroRepository;
    @Autowired
    private NotaFiscalRepository notaFiscalRepository;

    private Usuario loja;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
    }

    private Parceiro parceiro(Usuario dono, String documento) {
        Parceiro p = new Parceiro();
        p.setUsuario(dono);
        p.setDocumento(documento);
        p.setNome("Fornecedor teste");
        p.setCriadoEm(LocalDateTime.now());
        return parceiroRepository.saveAndFlush(p);
    }

    private NotaFiscal nota(Usuario dono, String chave) {
        NotaFiscal n = new NotaFiscal();
        n.setUsuario(dono);
        n.setChaveAcesso(chave);
        n.setImportadaEm(LocalDateTime.now());
        return notaFiscalRepository.saveAndFlush(n);
    }

    // ---------- produto ----------

    @Test
    @DisplayName("produto novo nasce com custo 0, unidade UN e sem EAN/NCM (o formulário atual não muda)")
    void padroesDoProduto() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        limparSessao();

        Produto lido = produtoRepository.findById(p.getId()).orElseThrow();
        assertThat(lido.getCusto()).isEqualByComparingTo("0");
        assertThat(lido.getUnidade()).isEqualTo("UN");
        assertThat(lido.getEan()).isNull();
        assertThat(lido.getNcm()).isNull();
    }

    @Test
    @DisplayName("custo, unidade, EAN e NCM são gravados e lidos pela entidade")
    void gravaCamposNovos() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        p.setCusto(new java.math.BigDecimal("1.2345"));
        p.setUnidade("CX");
        p.setEan("7891000100103");
        p.setNcm("96081000");
        produtoRepository.saveAndFlush(p);
        limparSessao();

        Produto lido = produtoRepository.findById(p.getId()).orElseThrow();
        assertThat(lido.getCusto()).isEqualByComparingTo("1.2345");
        assertThat(lido.getUnidade()).isEqualTo("CX");
        assertThat(lido.getEan()).isEqualTo("7891000100103");
        assertThat(lido.getNcm()).isEqualTo("96081000");
        assertThat(produtoRepository.findFirstByUsuarioCodigoAndEanAndAtivoTrueOrderByIdAsc(
                loja.getCodigo(), "7891000100103")).isPresent();
        assertThat(produtoRepository.findFirstByUsuarioCodigoAndEanAndAtivoTrueOrderByIdAsc(
                novoUsuario().getCodigo(), "7891000100103")).isEmpty();
    }

    @Test
    @DisplayName("o banco recusa EAN que não tem 8, 12, 13 ou 14 dígitos")
    void bancoRecusaEanInvalido() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        em.flush();

        assertThatThrownBy(() -> jdbc.update("update produto set ean = '12345' where id = ?", p.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa NCM que não tem 8 dígitos")
    void bancoRecusaNcmInvalido() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        em.flush();

        assertThatThrownBy(() -> jdbc.update("update produto set ncm = '1234' where id = ?", p.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa custo negativo")
    void bancoRecusaCustoNegativo() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        em.flush();

        assertThatThrownBy(() -> jdbc.update("update produto set custo = -1 where id = ?", p.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("a entidade valida EAN e NCM antes de gravar")
    void entidadeValidaEanNcm() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        p.setEan("abc");

        assertThatThrownBy(() -> produtoRepository.saveAndFlush(p)).isInstanceOf(ConstraintViolationException.class);
    }

    // ---------- parceiro ----------

    @Test
    @DisplayName("parceiro é fornecedor por padrão, não cliente, e único por loja e documento")
    void parceiroBasico() {
        Parceiro p = parceiro(loja, NfeXml.CNPJ_FORNECEDOR);
        limparSessao();

        Parceiro lido = parceiroRepository.findByUsuarioCodigoAndDocumento(loja.getCodigo(), NfeXml.CNPJ_FORNECEDOR)
                .orElseThrow();
        assertThat(lido.getId()).isEqualTo(p.getId());
        assertThat(lido.isFornecedor()).isTrue();
        assertThat(lido.isCliente()).isFalse();
        assertThat(parceiroRepository.findByUsuarioCodigoAndDocumento(novoUsuario().getCodigo(),
                NfeXml.CNPJ_FORNECEDOR)).isEmpty();
    }

    @Test
    @DisplayName("o mesmo documento é permitido em lojas diferentes")
    void parceiroEmDuasLojas() {
        parceiro(loja, NfeXml.CNPJ_FORNECEDOR);

        assertThatCode(() -> parceiro(novoUsuario(), NfeXml.CNPJ_FORNECEDOR)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o banco recusa o mesmo documento duas vezes na mesma loja")
    void parceiroDuplicado() {
        parceiro(loja, NfeXml.CNPJ_FORNECEDOR);

        assertThatThrownBy(() -> parceiro(loja, NfeXml.CNPJ_FORNECEDOR))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa documento que não é CNPJ (14) nem CPF (11) só com dígitos")
    void parceiroDocumentoInvalido() {
        assertThatThrownBy(() -> parceiro(loja, "123"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- origem da movimentação ----------

    @Test
    @DisplayName("movimentar sem origem continua igual: origem nula")
    void movimentarSemOrigem() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);

        MovimentacaoEstoque mov = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "Compra", loja);

        assertThat(mov.getOrigemTipo()).isNull();
        assertThat(mov.getOrigemId()).isNull();
        assertThat(mov.getQuantidadeAnterior()).isEqualTo(1);
        assertThat(mov.getQuantidadeNova()).isEqualTo(6);
    }

    @Test
    @DisplayName("movimentar com origem grava tipo e id, e o saldo e o histórico seguem as mesmas regras")
    void movimentarComOrigem() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        NotaFiscal nota = nota(loja, "9".repeat(44));

        MovimentacaoEstoque mov = estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "NF-e 1",
                loja, OrigemMovimentacao.NFE_COMPRA, nota.getId());
        limparSessao();

        MovimentacaoEstoque lida = movimentacaoRepository.findById(mov.getId()).orElseThrow();
        assertThat(lida.getOrigemTipo()).isEqualTo(OrigemMovimentacao.NFE_COMPRA);
        assertThat(lida.getOrigemId()).isEqualTo(nota.getId());
        assertThat(saldoNoBanco(p.getId())).isEqualTo(6);
    }

    @Test
    @DisplayName("origem pela metade (só tipo ou só id) é recusada pelo serviço")
    void origemPelaMetade() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "x", loja,
                OrigemMovimentacao.NFE_COMPRA, null)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5, "x", loja,
                null, 3)).isInstanceOf(RegraNegocioException.class);
        assertThat(saldoNoBanco(p.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("o motivo reservado \"Estoque inicial\" continua recusado mesmo com origem")
    void motivoReservadoComOrigem() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);

        assertThatThrownBy(() -> estoqueService.movimentar(p.getId(), TipoMovimentacao.ENTRADA, 5,
                "estoque inicial", loja, OrigemMovimentacao.NFE_COMPRA, 1))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("o banco recusa origem só com o tipo (sem id)")
    void bancoRecusaOrigemPelaMetade() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(p);
        mov.setUsuario(loja);
        mov.setTipo(TipoMovimentacao.ENTRADA);
        mov.setQuantidade(1);
        mov.setQuantidadeAnterior(1);
        mov.setQuantidadeNova(2);
        mov.definirOrigem(OrigemMovimentacao.NFE_COMPRA, null);

        assertThatThrownBy(() -> movimentacaoRepository.saveAndFlush(mov))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa tipo de origem que ainda não existe (VENDA entra por nova migration)")
    void bancoRecusaOrigemDesconhecida() {
        Produto p = novoProduto(loja, categoria, "Caneta", 1, 0);
        MovimentacaoEstoque mov = novaMovimentacao(p);
        movimentacaoRepository.saveAndFlush(mov);

        assertThatThrownBy(() -> jdbc.update(
                "update movimentacao_estoque set origem_tipo = 'VENDA', origem_id = 1 where id = ?", mov.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private MovimentacaoEstoque novaMovimentacao(Produto p) {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(p);
        mov.setUsuario(loja);
        mov.setTipo(TipoMovimentacao.ENTRADA);
        mov.setQuantidade(1);
        mov.setQuantidadeAnterior(1);
        mov.setQuantidadeNova(2);
        return mov;
    }

    // ---------- CNPJ da loja ----------

    @Test
    @DisplayName("CNPJ da loja é opcional; quando informado precisa ter dígito verificador válido")
    void cnpjDaLoja() {
        assertThatCode(() -> usuarioRepository.saveAndFlush(loja)).doesNotThrowAnyException();
        loja.setCnpj(NfeXml.CNPJ_LOJA);
        assertThatCode(() -> usuarioRepository.saveAndFlush(loja)).doesNotThrowAnyException();
        limparSessao();
        assertThat(usuarioRepository.findById(loja.getCodigo()).orElseThrow().getCnpj()).isEqualTo(NfeXml.CNPJ_LOJA);
    }

    @Test
    @DisplayName("CNPJ com dígito verificador errado é recusado pela validação")
    void cnpjInvalido() {
        loja.setCnpj(NfeXml.CNPJ_LOJA.substring(0, 13) + ((NfeXml.CNPJ_LOJA.charAt(13) - '0' + 1) % 10));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(loja)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa CNPJ que não tem 14 dígitos")
    void bancoRecusaCnpjMalformado() {
        em.flush();

        assertThatThrownBy(() -> jdbc.update("update usuario set cnpj = '123' where codigo = ?", loja.getCodigo()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- nota fiscal e importação ----------

    @Test
    @DisplayName("o banco recusa a mesma chave duas vezes na mesma loja")
    void notaDuplicadaNaLoja() {
        String chave = NfeXml.nota().item(NfeXml.item()).chave();
        nota(loja, chave);

        assertThatThrownBy(() -> nota(loja, chave)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("a mesma chave é permitida em lojas diferentes")
    void notaEmDuasLojas() {
        String chave = NfeXml.nota().item(NfeXml.item()).chave();
        nota(loja, chave);

        assertThatCode(() -> nota(novoUsuario(), chave)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o banco recusa token de importação repetido")
    void tokenRepetido() {
        UUID token = UUID.randomUUID();
        String sql = "insert into importacao_nfe (usuario_codigo, token, xml, status, criada_em, expira_em) "
                + "values (?, ?, '<a/>', 'PENDENTE', now(), now())";
        jdbc.update(sql, loja.getCodigo(), token);

        assertThatThrownBy(() -> jdbc.update(sql, loja.getCodigo(), token))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("o banco recusa situação de item e status de importação desconhecidos")
    void bancoRecusaEnumsDesconhecidos() {
        assertThatThrownBy(() -> jdbc.update("insert into importacao_nfe (usuario_codigo, token, xml, status, "
                + "criada_em, expira_em) values (?, ?, '<a/>', 'OUTRO', now(), now())",
                loja.getCodigo(), UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------- V5 ----------

    @Test
    @DisplayName("V5: o xml da importação aceita NULL (encerrada)")
    void xmlAceitaNulo() {
        assertThatCode(() -> jdbc.update("insert into importacao_nfe (usuario_codigo, token, xml, status, criada_em, "
                + "expira_em) values (?, ?, null, 'CANCELADA', now(), now())", loja.getCodigo(), UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("V5: o banco recusa a mesma nota (loja, fornecedor, modelo, série, número) com outra chave")
    void bancoRecusaNotaDuplicadaDeNegocio() {
        Parceiro fornecedor = parceiro(loja, NfeXml.CNPJ_FORNECEDOR);
        notaDoFornecedor(fornecedor, "1".repeat(44));

        assertThatThrownBy(() -> notaDoFornecedor(fornecedor, "2".repeat(44)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V5: o mesmo número de nota é permitido em outro fornecedor ou em outra loja")
    void mesmoNumeroEmOutroFornecedorOuLoja() {
        Parceiro a = parceiro(loja, NfeXml.CNPJ_FORNECEDOR);
        Parceiro b = parceiro(loja, NfeXml.CNPJ_OUTRO);
        Usuario outra = novoUsuario();
        Parceiro c = parceiro(outra, NfeXml.CNPJ_FORNECEDOR);
        notaDoFornecedor(a, "1".repeat(44));

        assertThatCode(() -> {
            notaDoFornecedor(b, "2".repeat(44));
            notaDoFornecedor(c, "3".repeat(44));
        }).doesNotThrowAnyException();
    }

    private NotaFiscal notaDoFornecedor(Parceiro fornecedor, String chave) {
        NotaFiscal n = new NotaFiscal();
        n.setUsuario(fornecedor.getUsuario());
        n.setParceiro(fornecedor);
        n.setChaveAcesso(chave);
        n.setModelo(55);
        n.setSerie(1);
        n.setNumero(77L);
        n.setImportadaEm(LocalDateTime.now());
        return notaFiscalRepository.saveAndFlush(n);
    }
}
