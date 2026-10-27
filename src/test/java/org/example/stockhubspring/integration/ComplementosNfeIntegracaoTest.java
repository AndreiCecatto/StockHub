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
    }
}
