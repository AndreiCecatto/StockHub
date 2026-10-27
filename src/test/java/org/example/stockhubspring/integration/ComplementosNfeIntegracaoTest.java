package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.AcaoItemNfe;
import org.example.stockhubspring.dto.ConfirmacaoNfeForm;
import org.example.stockhubspring.dto.DecisaoItemNfeForm;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.NotaFiscal;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.ImportacaoNfeService;
import org.example.stockhubspring.service.ImportacaoNfeService.DetalheNota;
import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Complementos da NF-e (RN-53 a RN-60) contra o PostgreSQL real; a transação de cada teste é revertida. */
class ComplementosNfeIntegracaoTest extends IntegracaoBase {

    @Autowired
    private ImportacaoNfeService service;
    private Usuario loja;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
    }

    private static DecisaoItemNfeForm criar(String nome, Categoria categoria) {
        DecisaoItemNfeForm d = new DecisaoItemNfeForm();
        d.setAcao(AcaoItemNfe.CRIAR);
        d.setNome(nome);
        d.setCategoriaId(categoria.getId());
        d.setPreco(new BigDecimal("9.90"));
        return d;
    }

    private NotaFiscal importar(NfeXml nota, String... nomesDosProdutos) {
        UUID token = service.enviar(nota.bytes(), loja);
        ConfirmacaoNfeForm form = new ConfirmacaoNfeForm();
        form.setConfirmouDestinatario(true);
        for (int i = 0; i < nomesDosProdutos.length; i++) {
            form.getItem().put(i + 1, criar(nomesDosProdutos[i], categoria));
        }
        NotaFiscal salva = service.confirmar(token, form, loja);
        limparSessao();
        return salva;
    }

    @Test
    @DisplayName("RN-53/54: guarda seguro, outras despesas e o rateio por item; custo da movimentação inclui o rateio")
    void guardaRateioEOCustoSoma() {
        NfeXml nota = NfeXml.nota().frete("8.00").seguro("1.00").outras("0.50")
                .item(NfeXml.item().codigo("A").ean("7891000100103").quantidade("10.0000").unitario("5.0000000000"))
                .item(NfeXml.item().codigo("B").ean("7891000100110").quantidade("3.0000").unitario("10.0000000000"));
        NotaFiscal salva = importar(nota, "Produto A", "Produto B");

        Map<String, Object> linhaNota = jdbc.queryForMap("select * from nota_fiscal where id = ?", salva.getId());
        assertThat((BigDecimal) linhaNota.get("valor_frete")).isEqualByComparingTo("8.00");
        assertThat((BigDecimal) linhaNota.get("valor_seguro")).isEqualByComparingTo("1.00");
        assertThat((BigDecimal) linhaNota.get("valor_outras")).isEqualByComparingTo("0.50");

        List<Map<String, Object>> itens = jdbc.queryForList(
                "select * from nota_fiscal_item where nota_fiscal_id = ? order by numero_item", salva.getId());
        // item A: 50/80 do total; item B: 30/80
        assertThat((BigDecimal) itens.get(0).get("valor_frete")).isEqualByComparingTo("5.00");
        assertThat((BigDecimal) itens.get(1).get("valor_frete")).isEqualByComparingTo("3.00");
        assertThat((BigDecimal) itens.get(0).get("valor_seguro")).isEqualByComparingTo("0.63");
        assertThat((BigDecimal) itens.get(1).get("valor_seguro")).isEqualByComparingTo("0.37");
        assertThat((BigDecimal) itens.get(0).get("valor_outras")).isEqualByComparingTo("0.31");
        assertThat((BigDecimal) itens.get(1).get("valor_outras")).isEqualByComparingTo("0.19");
        // custo A = (50 + 5,00 + 0,63 + 0,31) / 10 = 5,5940
        assertThat((BigDecimal) itens.get(0).get("custo_unitario")).isEqualByComparingTo("5.5940");

        Integer produtoA = (Integer) itens.get(0).get("produto_id");
        assertThat((BigDecimal) jdbc.queryForMap("select custo from produto where id = ?", produtoA).get("custo"))
                .isEqualByComparingTo("5.5940");
        assertThat((BigDecimal) jdbc.queryForMap("select custo_unitario from movimentacao_estoque where produto_id = ?",
                produtoA).get("custo_unitario")).isEqualByComparingTo("5.5940");
    }

    @Test
    @DisplayName("nota importada antes da V8 (colunas novas nulas) continua abrindo no detalhe")
    void notaAntigaSemColunasNovas() {
        NotaFiscal salva = importar(NfeXml.nota().item(NfeXml.item()), "Produto A");
        jdbc.update("update nota_fiscal set valor_seguro = null, valor_outras = null where id = ?", salva.getId());
        jdbc.update("update nota_fiscal_item set valor_desconto = null, valor_frete = null, valor_seguro = null, "
                + "valor_outras = null, custo_unitario = null where nota_fiscal_id = ?", salva.getId());
        limparSessao();

        DetalheNota detalhe = service.detalhe(salva.getId(), loja.getCodigo());

        assertThat(detalhe.nota().getValorSeguro()).isNull();
        assertThat(detalhe.itens().get(0).getRateio()).isEqualByComparingTo("0");
        assertThat(detalhe.itens().get(0).getCustoUnitario()).isNull();
        assertThat(detalhe.pagamentos()).isEmpty();
        assertThat(detalhe.parcelas()).isEmpty();
    }

    @Test
    @DisplayName("RN-57: guarda como foi pago e as parcelas e devolve no detalhe, em ordem")
    void guardaPagamentoEParcelas() {
        NfeXml nota = NfeXml.nota()
                .item(NfeXml.item().quantidade("10.0000").unitario("5.0000000000"))
                .pagamento("15", "30.00", null).pagamento("17", "20.00", null)
                .parcela("2026-04-10", "25.00").parcela("2026-05-10", "25.00");
        NotaFiscal salva = importar(nota, "Produto A");

        DetalheNota detalhe = service.detalhe(salva.getId(), loja.getCodigo());

        assertThat(detalhe.pagamentos()).extracting("descricao").containsExactly("Boleto", "Pix");
        assertThat(detalhe.pagamentos().get(0).getValor()).isEqualByComparingTo("30.00");
        assertThat(detalhe.parcelas()).hasSize(2);
        assertThat(detalhe.parcelas().get(0).getVencimento()).isEqualTo(java.time.LocalDate.of(2026, 4, 10));
        assertThat(detalhe.parcelas().get(1).getNumero()).isEqualTo(2);
    }

    @Test
    @DisplayName("RN-57: a revisão avisa quando a soma das parcelas difere do total da nota")
    void avisaParcelasDivergentes() {
        UUID token = service.enviar(NfeXml.nota().item(NfeXml.item()).parcela("2026-04-10", "10.00").bytes(), loja);

        assertThat(service.revisar(token, loja).avisos()).anyMatch(a -> a.contains("soma das parcelas"));

        UUID certo = service.enviar(NfeXml.nota().numero(77).item(NfeXml.item()).parcela("2026-04-10", "50.00").bytes(), loja);

        assertThat(service.revisar(certo, loja).avisos()).noneMatch(a -> a.contains("soma das parcelas"));
    }

    @Test
    @DisplayName("RN-57: outra loja não vê pagamentos nem parcelas de uma nota que não é dela")
    void isolamentoEntreLojas() {
        NotaFiscal salva = importar(NfeXml.nota().item(NfeXml.item()).pagamento("15", "50.00", null), "Produto A");
        Usuario outra = novoUsuario();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.detalhe(salva.getId(), outra.getCodigo()))
                .isInstanceOf(org.example.stockhubspring.exception.RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("RN-58: nota de devolução é recusada no envio e não deixa importação pendente nem mexe no estoque")
    void devolucaoNaoGeraNada() {
        long antes = contarImportacoes();

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.enviar(NfeXml.nota().finalidade("4").item(NfeXml.item()).bytes(), loja))
                .isInstanceOf(org.example.stockhubspring.exception.RegraNegocioException.class)
                .hasMessageContaining("nota de devolução");

        assertThat(contarImportacoes()).isEqualTo(antes);
        assertThat(jdbc.queryForObject("select count(*) from movimentacao_estoque where usuario_codigo = ?",
                Long.class, loja.getCodigo())).isZero();
    }

    private long contarImportacoes() {
        em.flush();
        return jdbc.queryForObject("select count(*) from importacao_nfe where usuario_codigo = ?", Long.class,
                loja.getCodigo());
    }

    // ---------- RN-59: avisos estruturados ----------

    private org.example.stockhubspring.model.Produto produtoSugerivel(String ean, String ncm) {
        org.example.stockhubspring.model.Produto p = novoProduto(loja, categoria, "Cabo USB", 0, 0);
        p.setEan(ean);
        p.setNcm(ncm);
        return produtoRepository.saveAndFlush(p);
    }

    @Test
    @DisplayName("RN-59: avisoEan e avisoNcm são verdadeiros quando o produto sugerido não tem o dado")
    void flagsQuandoProdutoNaoTem() {
        produtoSugerivel(null, null);
        UUID token = service.enviar(NfeXml.nota().item(NfeXml.item().descricao("CABO USB").ean("7891000100103")
                .ncm("85444200")).bytes(), loja);
        // sem EAN igual, a sugestão vem do nome
        var item = service.revisar(token, loja).itens().get(0);

        assertThat(item.produtoSugeridoId()).isNotNull();
        assertThat(item.avisoEan()).isTrue();
        assertThat(item.avisoNcm()).isTrue();
        assertThat(item.avisos()).noneMatch(a -> a.contains("será gravado"));
    }

    @Test
    @DisplayName("RN-59: as flags ficam falsas quando o produto já tem EAN e NCM, e sem sugestão")
    void flagsQuandoProdutoJaTem() {
        produtoSugerivel("7891000100110", "85444200");
        UUID token = service.enviar(NfeXml.nota().item(NfeXml.item().descricao("CABO USB").ean("7891000100103")
                .ncm("85444200")).bytes(), loja);
        var item = service.revisar(token, loja).itens().get(0);

        assertThat(item.produtoSugeridoId()).isNotNull();
        assertThat(item.avisoEan()).isFalse();
        assertThat(item.avisoNcm()).isFalse();

        UUID semProduto = service.enviar(NfeXml.nota().numero(2).item(NfeXml.item().descricao("COISA SEM PAR")
                .codigo("ZZ").ean("7891000100127")).bytes(), loja);
        var novo = service.revisar(semProduto, loja).itens().get(0);

        assertThat(novo.produtoSugeridoId()).isNull();
        assertThat(novo.avisoEan()).isFalse();
    }
}
