package org.example.stockhubspring.integration;

import org.example.stockhubspring.dto.AcaoItemNfe;
import org.example.stockhubspring.dto.ConfirmacaoNfeForm;
import org.example.stockhubspring.dto.DecisaoItemNfeForm;
import org.example.stockhubspring.dto.RevisaoNfe;
import org.example.stockhubspring.dto.RevisaoNfe.ItemRevisao;
import org.example.stockhubspring.dto.RevisaoNfe.MotivoSugestao;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.NotaFiscal;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.SituacaoItemNfe;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.service.ImportacaoNfeService;
import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import java.sql.Connection;
import java.sql.Savepoint;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Importação de NF-e contra o PostgreSQL real (a transação de cada teste é revertida). Notas e CNPJs são
 * sintéticos (testsupport.NfeXml).
 */
class ImportacaoNfeIntegracaoTest extends IntegracaoBase {

    @Autowired
    private ImportacaoNfeService service;
    private Usuario loja;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        loja = novoUsuario();
        categoria = novaCategoria(loja, "Geral");
    }

    // ---------- ajudantes ----------

    private UUID enviar(Usuario dono, NfeXml nota) {
        return service.enviar(nota.bytes(), dono);
    }

    private static DecisaoItemNfeForm vincular(Produto produto) {
        DecisaoItemNfeForm d = new DecisaoItemNfeForm();
        d.setAcao(AcaoItemNfe.VINCULAR);
        d.setProdutoId(produto == null ? null : produto.getId());
        return d;
    }

    private static DecisaoItemNfeForm vincularId(Integer produtoId) {
        DecisaoItemNfeForm d = new DecisaoItemNfeForm();
        d.setAcao(AcaoItemNfe.VINCULAR);
        d.setProdutoId(produtoId);
        return d;
    }

    private static DecisaoItemNfeForm criar(String nome, Categoria categoria, String preco) {
        DecisaoItemNfeForm d = new DecisaoItemNfeForm();
        d.setAcao(AcaoItemNfe.CRIAR);
        d.setNome(nome);
        d.setCategoriaId(categoria == null ? null : categoria.getId());
        d.setPreco(preco == null ? null : new BigDecimal(preco));
        return d;
    }

    private static DecisaoItemNfeForm ignorar() {
        DecisaoItemNfeForm d = new DecisaoItemNfeForm();
        d.setAcao(AcaoItemNfe.IGNORAR);
        return d;
    }

    private static ConfirmacaoNfeForm confirmacao(DecisaoItemNfeForm... decisoes) {
        ConfirmacaoNfeForm form = new ConfirmacaoNfeForm();
        form.setConfirmouDestinatario(true);
        for (int i = 0; i < decisoes.length; i++) {
            form.getItem().put(i + 1, decisoes[i]);
        }
        return form;
    }

    private Produto produtoCom(String nome, int saldo) {
        return novoProduto(loja, categoria, nome, saldo, 0);
    }

    private List<Map<String, Object>> movimentacoesDe(Integer produtoId) {
        em.flush();
        return jdbc.queryForList("select * from movimentacao_estoque where produto_id = ? order by id", produtoId);
    }

    private Map<String, Object> linhaProduto(Integer produtoId) {
        em.flush();
        return jdbc.queryForMap("select * from produto where id = ?", produtoId);
    }

    private long contar(String tabela, Integer usuarioCodigo) {
        em.flush();
        return jdbc.queryForObject("select count(*) from " + tabela + " where usuario_codigo = ?", Long.class,
                usuarioCodigo);
    }

    private String statusDa(UUID token) {
        em.flush();
        return jdbc.queryForObject("select status from importacao_nfe where token = ?", String.class, token);
    }

    /**
     * Confirma dentro de um savepoint: se falhar, o que o serviço gravou é desfeito (como o rollback da
     * transação real) e o teste pode continuar com os dados que preparou.
     */
    private NotaFiscal tentar(UUID token, ConfirmacaoNfeForm form, Usuario dono) {
        em.flush();
        // o JdbcTemplate usa a mesma conexão (e transação) do Hibernate neste teste
        Savepoint ponto = jdbc.execute((ConnectionCallback<Savepoint>) Connection::setSavepoint);
        try {
            return service.confirmar(token, form, dono);
        } catch (RuntimeException e) {
            em.clear();
            jdbc.execute((ConnectionCallback<Object>) conexao -> {
                conexao.rollback(ponto);
                return null;
            });
            throw e;
        }
    }

    private ItemRevisao itemDaRevisao(Usuario dono, UUID token, int numero) {
        return service.revisar(token, dono).itens().get(numero - 1);
    }

    // ---------- importação feliz ----------

    @Nested
    @DisplayName("confirmar a importação")
    class Confirmar {

        @Test
        @DisplayName("vincula um produto existente, cria um novo e gera ENTRADAs com origem e saldos corretos")
        void vinculaECria() {
            Produto caneta = produtoCom("Caneta Azul", 5);
            caneta.setEan("7891000100103");
            produtoRepository.saveAndFlush(caneta);
            NfeXml nota = NfeXml.nota().numero(1234)
                    .item(NfeXml.item().codigo("C1").ean("7891000100103").descricao("CANETA AZUL CX")
                            .quantidade("10.0000").unitario("5.0000000000").ncm("96081000"))
                    .item(NfeXml.item().codigo("L9").ean("7891000100110").descricao("LAPIS PRETO")
                            .quantidade("3.0000").unitario("2.0000000000").ncm("96091000"));
            UUID token = enviar(loja, nota);

            RevisaoNfe revisao = service.revisar(token, loja);
            assertThat(revisao.itens().get(0).produtoSugeridoId()).isEqualTo(caneta.getId());
            assertThat(revisao.itens().get(0).motivoSugestao()).isEqualTo(MotivoSugestao.EAN);
            assertThat(revisao.itens().get(1).motivoSugestao()).isEqualTo(MotivoSugestao.NENHUM);
            assertThat(revisao.avisoVerificacao()).contains("não foi verificada").doesNotContain("autêntica");

            NotaFiscal salva = service.confirmar(token,
                    confirmacao(vincular(caneta), criar("Lapis Preto", categoria, "4.50")), loja);
            limparSessao();

            // nota e itens
            assertThat(salva.getId()).isNotNull();
            Map<String, Object> linhaNota = jdbc.queryForMap("select * from nota_fiscal where id = ?", salva.getId());
            assertThat(linhaNota.get("usuario_codigo")).isEqualTo(loja.getCodigo());
            assertThat(linhaNota.get("chave_acesso").toString().trim()).isEqualTo(nota.chave());
            assertThat(linhaNota.get("numero")).isEqualTo(1234L);
            assertThat(linhaNota.get("cnpj_destinatario")).isEqualTo(NfeXml.CNPJ_LOJA);
            assertThat((BigDecimal) linhaNota.get("valor_total")).isEqualByComparingTo("56.00");

            // produto existente: saldo 5 -> 15, custo, ncm preenchido (ean já existia e não muda)
            Map<String, Object> existente = linhaProduto(caneta.getId());
            assertThat(existente.get("quantidade")).isEqualTo(15);
            assertThat((BigDecimal) existente.get("custo")).isEqualByComparingTo("5.0000");
            assertThat(existente.get("ean")).isEqualTo("7891000100103");
            assertThat(existente.get("ncm")).isEqualTo("96081000");
            List<Map<String, Object>> movs = movimentacoesDe(caneta.getId());
            assertThat(movs).hasSize(1);
            assertThat(movs.get(0).get("tipo")).isEqualTo("ENTRADA");
            assertThat(movs.get(0).get("quantidade")).isEqualTo(10);
            assertThat(movs.get(0).get("quantidade_anterior")).isEqualTo(5);
            assertThat(movs.get(0).get("quantidade_nova")).isEqualTo(15);
            assertThat(movs.get(0).get("tipo_documento")).isEqualTo("COMPRA_NFE");
            assertThat(movs.get(0).get("documento_id")).isEqualTo(salva.getId());
            assertThat(movs.get(0).get("observacao")).isEqualTo("NF-e 1234 de FORNECEDOR DE TESTE LTDA");

            // produto novo: nasce com saldo 0 e recebe a ENTRADA (livro-razão fecha: 0 -> 3)
            Integer novoId = jdbc.queryForObject(
                    "select id from produto where usuario_codigo = ? and nome = 'Lapis Preto'", Integer.class,
                    loja.getCodigo());
            Map<String, Object> novo = linhaProduto(novoId);
            assertThat(novo.get("quantidade")).isEqualTo(3);
            assertThat((BigDecimal) novo.get("preco")).isEqualByComparingTo("4.50");
            assertThat((BigDecimal) novo.get("custo")).isEqualByComparingTo("2.0000");
            assertThat(novo.get("unidade")).isEqualTo("UN");
            assertThat(novo.get("ean")).isEqualTo("7891000100110");
            assertThat(novo.get("ncm")).isEqualTo("96091000");
            assertThat(novo.get("usuario_codigo")).isEqualTo(loja.getCodigo());
            assertThat(novo.get("ativo")).isEqualTo(true);
            List<Map<String, Object>> movsNovo = movimentacoesDe(novoId);
            assertThat(movsNovo).hasSize(1);
            assertThat(movsNovo.get(0).get("quantidade_anterior")).isEqualTo(0);
            assertThat(movsNovo.get(0).get("quantidade_nova")).isEqualTo(3);
            assertThat(movsNovo.get(0).get("documento_id")).isEqualTo(salva.getId());

            // itens da nota ligados às movimentações
            List<Map<String, Object>> itens = jdbc.queryForList(
                    "select * from nota_fiscal_item where nota_fiscal_id = ? order by numero_item", salva.getId());
            assertThat(itens).hasSize(2);
            assertThat(itens).allSatisfy(i -> assertThat(i.get("situacao")).isEqualTo("IMPORTADO"));
            assertThat(itens.get(0).get("produto_id")).isEqualTo(caneta.getId());
            assertThat(itens.get(0).get("movimentacao_id")).isEqualTo(movs.get(0).get("id"));
            assertThat(itens.get(0).get("cfop").toString().trim()).isEqualTo("5102");
            assertThat(itens.get(0).get("cfop_entrada").toString().trim()).isEqualTo("1102");
            assertThat(itens.get(0).get("codigo_fornecedor")).isEqualTo("C1");
            assertThat(itens.get(1).get("produto_id")).isEqualTo(novoId);

            // importação marcada como confirmada e ligada à nota; fornecedor criado
            Map<String, Object> importacao = jdbc.queryForMap("select * from importacao_nfe where token = ?", token);
            assertThat(importacao.get("status")).isEqualTo("CONFIRMADA");
            assertThat(importacao.get("nota_fiscal_id")).isEqualTo(salva.getId());
            assertThat(importacao.get("sha256").toString().trim()).hasSize(64);
            Map<String, Object> parceiro = jdbc.queryForMap(
                    "select * from parceiro where usuario_codigo = ?", loja.getCodigo());
            assertThat(parceiro.get("documento")).isEqualTo(NfeXml.CNPJ_FORNECEDOR);
            assertThat(parceiro.get("eh_fornecedor")).isEqualTo(true);
            assertThat(parceiro.get("eh_cliente")).isEqualTo(false);
            assertThat(parceiro.get("uf").toString().trim()).isEqualTo("RS");
        }

        @Test
        @DisplayName("EAN e NCM do produto só são preenchidos quando estão vazios")
        void eanNcmSoSeVazios() {
            Produto vazio = produtoCom("Sem codigos", 1);
            Produto cheio = produtoCom("Com codigos", 1);
            cheio.setEan("7890000000009");
            cheio.setNcm("11111111");
            produtoRepository.saveAndFlush(cheio);
            UUID token = enviar(loja, NfeXml.nota().numero(10)
                    .item(NfeXml.item().codigo("A").ean("7891000100103").ncm("22021000").quantidade("2.0000"))
                    .item(NfeXml.item().codigo("B").ean("7891000100110").ncm("33030010").quantidade("2.0000")));

            service.confirmar(token, confirmacao(vincular(vazio), vincular(cheio)), loja);

            Map<String, Object> a = linhaProduto(vazio.getId());
            assertThat(a.get("ean")).isEqualTo("7891000100103");
            assertThat(a.get("ncm")).isEqualTo("22021000");
            Map<String, Object> b = linhaProduto(cheio.getId());
            assertThat(b.get("ean")).isEqualTo("7890000000009");
            assertThat(b.get("ncm")).isEqualTo("11111111");
        }

        @Test
        @DisplayName("custo com desconto do item: valor líquido / quantidade, 4 casas")
        void custoComDesconto() {
            Produto p = produtoCom("Resma", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(11).item(
                    NfeXml.item().quantidade("3.0000").unitario("10.0000000000").desconto("1.00")));

            service.confirmar(token, confirmacao(vincular(p)), loja);

            assertThat((BigDecimal) linhaProduto(p.getId()).get("custo")).isEqualByComparingTo("9.6667");
        }

        @Test
        @DisplayName("segunda nota do produto atualiza o custo para o último valor")
        void ultimoCusto() {
            Produto p = produtoCom("Cabo", 0);
            service.confirmar(enviar(loja, NfeXml.nota().numero(1).item(NfeXml.item().unitario("5.0000000000"))),
                    confirmacao(vincular(p)), loja);
            service.confirmar(enviar(loja, NfeXml.nota().numero(2).item(NfeXml.item().unitario("6.5000000000"))),
                    confirmacao(vincular(p)), loja);

            Map<String, Object> linha = linhaProduto(p.getId());
            assertThat((BigDecimal) linha.get("custo")).isEqualByComparingTo("6.5000");
            assertThat(linha.get("quantidade")).isEqualTo(20);
        }

        @Test
        @DisplayName("unidade da nota diferente da do produto: avisa e não mexe no custo, mas a entrada acontece")
        void unidadeDiferente() {
            Produto p = produtoCom("Lampada", 0);
            p.setEan("7891000100103");
            produtoRepository.saveAndFlush(p);
            UUID token = enviar(loja, NfeXml.nota().numero(12).item(
                    NfeXml.item().ean("7891000100103").unidade("CX").quantidade("2.0000").unitario("50.0000000000")));

            ItemRevisao item = itemDaRevisao(loja, token, 1);
            assertThat(item.unidadeDiferente()).isTrue();

            service.confirmar(token, confirmacao(vincular(p)), loja);
            Map<String, Object> linha = linhaProduto(p.getId());
            assertThat(linha.get("quantidade")).isEqualTo(2);
            assertThat((BigDecimal) linha.get("custo")).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("fracionado não entra no estoque; item recusado só aceita ignorar; os demais seguem")
        void fracionadoEBloqueado() {
            Produto p = produtoCom("Cabo", 0);
            Produto outro = produtoCom("Outro", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(13)
                    .item(NfeXml.item().codigo("F").quantidade("2.5000"))
                    .item(NfeXml.item().codigo("Z").quantidade("0.0000").bruto("0.00"))
                    .item(NfeXml.item().codigo("N").quantidade("4.0000")));

            RevisaoNfe revisao = service.revisar(token, loja);
            assertThat(revisao.itens()).extracting(ItemRevisao::situacao).containsExactly(
                    SituacaoItemNfe.FRACIONADO, SituacaoItemNfe.IGNORADO, SituacaoItemNfe.IMPORTADO);
            assertThat(revisao.itens().get(1).bloqueado()).isTrue();
            assertThat(revisao.totalImportaveis()).isEqualTo(1);

            // tentar importar o item recusado é erro
            assertThatThrownBy(() -> tentar(token,
                    confirmacao(ignorar(), vincular(outro), vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Item 2");

            // fracionado forçado a VINCULAR continua fora do estoque
            NotaFiscal salva = service.confirmar(token,
                    confirmacao(vincular(outro), ignorar(), vincular(p)), loja);
            assertThat(movimentacoesDe(outro.getId())).isEmpty();
            assertThat(linhaProduto(p.getId()).get("quantidade")).isEqualTo(4);
            List<Map<String, Object>> itens = jdbc.queryForList(
                    "select situacao, produto_id, movimentacao_id from nota_fiscal_item where nota_fiscal_id = ? "
                            + "order by numero_item", salva.getId());
            assertThat(itens).extracting(i -> i.get("situacao")).containsExactly("FRACIONADO", "IGNORADO", "IMPORTADO");
            assertThat(itens.get(0).get("movimentacao_id")).isNull();
            assertThat(itens.get(1).get("movimentacao_id")).isNull();
        }

        @Test
        @DisplayName("se nenhum item entra no estoque a confirmação é recusada e a chave não é gasta")
        void nadaParaImportar() {
            UUID token = enviar(loja, NfeXml.nota().numero(14).item(NfeXml.item().quantidade("1.5000")));

            assertThatThrownBy(() -> tentar(token, confirmacao(ignorar()), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Nenhum item");
        }

        @Test
        @DisplayName("item sem decisão é recusado, com o número do item")
        void semDecisao() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(15)
                    .item(NfeXml.item()).item(NfeXml.item()));

            assertThatThrownBy(() -> tentar(token, confirmacao(vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Item 2");
        }

        @Test
        @DisplayName("decisão para item que não existe na nota é recusada")
        void decisaoDeItemInexistente() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(16).item(NfeXml.item()));
            ConfirmacaoNfeForm form = confirmacao(vincular(p));
            form.getItem().put(5, ignorar());

            assertThatThrownBy(() -> tentar(token, form, loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("não existe na nota");
        }

        @Test
        @DisplayName("sem marcar a confirmação do destinatário, nada é importado")
        void semConfirmarDestinatario() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(17).item(NfeXml.item()));
            ConfirmacaoNfeForm form = confirmacao(vincular(p));
            form.setConfirmouDestinatario(false);

            assertThatThrownBy(() -> tentar(token, form, loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Confirme que a nota");
            assertThat(movimentacoesDe(p.getId())).isEmpty();
        }

        @Test
        @DisplayName("fornecedor é reaproveitado por loja e documento, e o cadastro dele NÃO é reescrito pelo XML")
        void fornecedorReaproveitado() {
            Produto p = produtoCom("Cabo", 0);
            service.confirmar(enviar(loja, NfeXml.nota().numero(1).item(NfeXml.item())),
                    confirmacao(vincular(p)), loja);
            service.confirmar(enviar(loja, NfeXml.nota().numero(2).nomeEmitente("FORNECEDOR RENOMEADO SA")
                    .item(NfeXml.item())), confirmacao(vincular(p)), loja);

            List<Map<String, Object>> parceiros = jdbc.queryForList(
                    "select * from parceiro where usuario_codigo = ?", loja.getCodigo());
            assertThat(parceiros).hasSize(1);
            assertThat(parceiros.get(0).get("nome")).isEqualTo("FORNECEDOR DE TESTE LTDA");
            assertThat(parceiros.get(0).get("nome_fantasia")).isEqualTo("FORNECEDOR TESTE");
        }

        @Test
        @DisplayName("observação é cortada no tamanho máximo e nunca usa o motivo reservado")
        void observacaoLimitada() {
            Produto p = produtoCom("Cabo", 0);
            String nomeLongo = "F".repeat(250);
            service.confirmar(enviar(loja, NfeXml.nota().numero(3).nomeEmitente(nomeLongo).item(NfeXml.item())),
                    confirmacao(vincular(p)), loja);

            String obs = (String) movimentacoesDe(p.getId()).get(0).get("observacao");
            assertThat(obs).hasSizeLessThanOrEqualTo(255).startsWith("NF-e 3 de FFFF");
        }
    }

    // ---------- produto novo ----------

    @Nested
    @DisplayName("criar produto novo na importação")
    class CriarProduto {

        private UUID umItem() {
            return enviar(loja, NfeXml.nota().numero(20).item(NfeXml.item().descricao("PRODUTO NOVO")));
        }

        @Test
        @DisplayName("nome já usado por produto ativo é recusado (RN-10); inativo não atrapalha")
        void nomeDuplicado() {
            produtoCom("Repetido", 1);
            UUID token = umItem();

            assertThatThrownBy(() -> tentar(token,
                    confirmacao(criar("  repetido ", categoria, "9.90")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("já existe um produto ativo");
        }

        @Test
        @DisplayName("dois itens novos com o mesmo nome na mesma nota: o segundo é recusado")
        void mesmoNomeNaMesmaNota() {
            UUID token = enviar(loja, NfeXml.nota().numero(21).item(NfeXml.item()).item(NfeXml.item()));

            assertThatThrownBy(() -> tentar(token,
                    confirmacao(criar("Igual", categoria, "1.00"), criar("igual", categoria, "1.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Item 2");
        }

        @Test
        @DisplayName("exige nome, categoria da loja e preço maior que zero dentro do limite")
        void camposObrigatorios() {
            UUID token = umItem();

            assertThatThrownBy(() -> tentar(token, confirmacao(criar("", categoria, "1.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("nome");
            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", null, "1.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("categoria");
            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", categoria, null)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("preço");
            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", categoria, "0.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("preço");
            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", categoria, "10000000.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("preço");
            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", categoria, "1.005")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("preço");
        }

        @Test
        @DisplayName("categoria de outra loja é recusada sem revelar que existe")
        void categoriaDeOutraLoja() {
            Usuario outra = novoUsuario();
            Categoria alheia = novaCategoria(outra, "Alheia");
            UUID token = umItem();

            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", alheia, "1.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("categoria não encontrada");
            // categoria que não existe: mesma mensagem da categoria de outra loja
            Categoria inexistente = new Categoria();
            inexistente.setId(Integer.MAX_VALUE);
            assertThatThrownBy(() -> tentar(token, confirmacao(criar("Novo", inexistente, "1.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("categoria não encontrada");
        }

    }

    // ---------- idempotência ----------

    @Nested
    @DisplayName("idempotência: a mesma nota nunca entra duas vezes na mesma loja")
    class Idempotencia {

        @Test
        @DisplayName("reenviar a nota já importada é recusado com a data da importação")
        void reenvio() {
            Produto p = produtoCom("Cabo", 0);
            NfeXml nota = NfeXml.nota().numero(30).item(NfeXml.item());
            service.confirmar(enviar(loja, nota), confirmacao(vincular(p)), loja);

            assertThatThrownBy(() -> service.enviar(nota.bytes(), loja))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageMatching("Esta nota já foi importada em \\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}\\.");
        }

        @Test
        @DisplayName("reenviar o mesmo arquivo enquanto há pendente igual devolve a mesma importação (sem guardar outra cópia)")
        void duasPendentes() {
            Produto p = produtoCom("Cabo", 0);
            NfeXml nota = NfeXml.nota().numero(31).item(NfeXml.item().quantidade("4.0000"));
            UUID primeira = enviar(loja, nota);
            UUID segunda = enviar(loja, nota);

            assertThat(segunda).isEqualTo(primeira);
            assertThat(contar("importacao_nfe", loja.getCodigo())).isEqualTo(1);
            service.confirmar(primeira, confirmacao(vincular(p)), loja);

            assertThatThrownBy(() -> service.enviar(nota.bytes(), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("já foi importada");
            assertThatThrownBy(() -> service.revisar(primeira, loja))
                    .isInstanceOf(RegraNegocioException.class);
            assertThat(linhaProduto(p.getId()).get("quantidade")).isEqualTo(4);
        }

        @Test
        @DisplayName("confirmar duas vezes o mesmo token: a segunda é recusada")
        void mesmoTokenDuasVezes() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(32).item(NfeXml.item().quantidade("4.0000")));
            service.confirmar(token, confirmacao(vincular(p)), loja);

            assertThatThrownBy(() -> tentar(token, confirmacao(vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class);
            assertThat(linhaProduto(p.getId()).get("quantidade")).isEqualTo(4);
        }

        @Test
        @DisplayName("a mesma chave é permitida em lojas diferentes")
        void lojasDiferentes() {
            Usuario outra = novoUsuario();
            Categoria catOutra = novaCategoria(outra, "Geral");
            Produto minha = produtoCom("Cabo", 0);
            Produto dela = novoProduto(outra, catOutra, "Cabo", 0, 0);
            NfeXml nota = NfeXml.nota().numero(33).item(NfeXml.item().quantidade("2.0000"));

            service.confirmar(enviar(loja, nota), confirmacao(vincular(minha)), loja);
            service.confirmar(enviar(outra, nota), confirmacao(vincular(dela)), outra);

            assertThat(linhaProduto(minha.getId()).get("quantidade")).isEqualTo(2);
            assertThat(linhaProduto(dela.getId()).get("quantidade")).isEqualTo(2);
            assertThat(contar("nota_fiscal", loja.getCodigo())).isEqualTo(1);
            assertThat(contar("nota_fiscal", outra.getCodigo())).isEqualTo(1);
        }
    }

    // ---------- tudo ou nada ----------

    @Nested
    @DisplayName("transação tudo-ou-nada")
    class TudoOuNada {

        @Test
        @DisplayName("um item que estoura o saldo máximo desfaz tudo e a mensagem diz qual item")
        void itemInvalidoNaoGravaNada() {
            Produto primeiro = produtoCom("Primeiro", 5);
            Produto cheio = produtoCom("Cheio", 2_000_000_000);
            UUID token = enviar(loja, NfeXml.nota().numero(40)
                    .item(NfeXml.item().codigo("A").quantidade("10.0000"))
                    .item(NfeXml.item().codigo("B").quantidade("10.0000")));
            ConfirmacaoNfeForm form = confirmacao(vincular(primeiro), vincular(cheio));
            // O serviço roda dentro de um savepoint: se falhar, só o que ele gravou é desfeito
            // (os dados do teste continuam), reproduzindo o rollback da transação real.

            assertThatThrownBy(() -> tentar(token, form, loja))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Item 2")
                    .hasMessageContaining("2.000.000.000");
            em.clear();

            assertThat(saldoNoBanco(primeiro.getId())).isEqualTo(5);
            assertThat(saldoNoBanco(cheio.getId())).isEqualTo(2_000_000_000);
            assertThat(jdbc.queryForObject("select count(*) from movimentacao_estoque where tipo_documento = 'COMPRA_NFE' "
                    + "and usuario_codigo = ?", Long.class, loja.getCodigo())).isZero();
            assertThat(contar("nota_fiscal", loja.getCodigo())).isZero();
            assertThat(contar("parceiro", loja.getCodigo())).isZero();
            assertThat(statusDa(token)).isEqualTo("PENDENTE");
        }

        @Test
        @DisplayName("produto criado antes do item que falha também é desfeito")
        void produtoNovoDesfeito() {
            Produto cheio = produtoCom("Cheio", 2_000_000_000);
            UUID token = enviar(loja, NfeXml.nota().numero(41)
                    .item(NfeXml.item().codigo("A").descricao("NOVO A"))
                    .item(NfeXml.item().codigo("B").quantidade("10.0000")));
            ConfirmacaoNfeForm form = confirmacao(criar("Novo A", categoria, "3.00"), vincular(cheio));

            assertThatThrownBy(() -> tentar(token, form, loja))
                    .isInstanceOf(RegraNegocioException.class);
            em.clear();

            assertThat(jdbc.queryForObject("select count(*) from produto where usuario_codigo = ? and nome = 'Novo A'",
                    Long.class, loja.getCodigo())).isZero();
        }
    }

    // ---------- isolamento entre lojas ----------

    @Nested
    @DisplayName("isolamento entre lojas")
    class Isolamento {

        @Test
        @DisplayName("token de outra loja responde 'não encontrado' em revisar, confirmar e cancelar")
        void tokenDeOutraLoja() {
            Usuario outra = novoUsuario();
            UUID token = enviar(loja, NfeXml.nota().numero(50).item(NfeXml.item()));
            Produto dela = novoProduto(outra, novaCategoria(outra, "Geral"), "Cabo", 0, 0);

            assertThatThrownBy(() -> service.revisar(token, outra)).isInstanceOf(RecursoNaoEncontradoException.class);
            assertThatThrownBy(() -> tentar(token, confirmacao(vincular(dela)), outra))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            assertThatThrownBy(() -> service.cancelar(token, outra)).isInstanceOf(RecursoNaoEncontradoException.class);
            assertThatThrownBy(() -> service.revisar(UUID.randomUUID(), loja))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            assertThat(statusDa(token)).isEqualTo("PENDENTE");
            assertThat(service.listarPendentes(outra.getCodigo())).isEmpty();
            assertThat(service.listarPendentes(loja.getCodigo())).hasSize(1);
        }

        @Test
        @DisplayName("produto de outra loja (ou inexistente) é recusado com a mesma mensagem, sem alterar saldo")
        void produtoDeOutraLoja() {
            Usuario outra = novoUsuario();
            Produto dela = novoProduto(outra, novaCategoria(outra, "Geral"), "Cabo", 7, 0);
            UUID token = enviar(loja, NfeXml.nota().numero(51).item(NfeXml.item()));

            Throwable alheio = catchConfirmar(token, vincular(dela));
            Throwable inexistente = catchConfirmar(token, vincularId(Integer.MAX_VALUE));
            assertThat(alheio).isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("produto selecionado não foi encontrado");
            assertThat(inexistente.getMessage()).isEqualTo(alheio.getMessage());
            assertThat(saldoNoBanco(dela.getId())).isEqualTo(7);
        }

        private Throwable catchConfirmar(UUID token, DecisaoItemNfeForm decisao) {
            try {
                tentar(token, confirmacao(decisao), loja);
            } catch (RuntimeException e) {
                return e;
            }
            throw new AssertionError("deveria ter recusado");
        }

        @Test
        @DisplayName("nota importada de outra loja responde 'não encontrada' e a lista só traz as da própria loja")
        void notaDeOutraLoja() {
            Usuario outra = novoUsuario();
            Produto p = produtoCom("Cabo", 0);
            NotaFiscal nota = service.confirmar(enviar(loja, NfeXml.nota().numero(52).item(NfeXml.item())),
                    confirmacao(vincular(p)), loja);

            assertThatThrownBy(() -> service.detalhe(nota.getId(), outra.getCodigo()))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            assertThat(service.listarNotas(outra.getCodigo(), 1, 20).getTotalElements()).isZero();
            assertThat(service.listarNotas(loja.getCodigo(), 1, 20).getTotalElements()).isEqualTo(1);
            ImportacaoNfeService.DetalheNota detalhe = service.detalhe(nota.getId(), loja.getCodigo());
            assertThat(detalhe.itens()).hasSize(1);
            assertThat(detalhe.itens().get(0).getProduto().getId()).isEqualTo(p.getId());
        }

        @Test
        @DisplayName("memória do fornecedor de outra loja não vaza para esta")
        void memoriaNaoVaza() {
            Usuario outra = novoUsuario();
            Categoria catOutra = novaCategoria(outra, "Geral");
            Produto dela = novoProduto(outra, catOutra, "Item da outra", 0, 0);
            service.confirmar(enviar(outra, NfeXml.nota().numero(53).item(NfeXml.item().codigo("X1").ean(null)
                    .descricao("ITEM DA OUTRA"))), confirmacao(vincular(dela)), outra);

            UUID token = enviar(loja, NfeXml.nota().numero(54).item(NfeXml.item().codigo("X1").ean(null)
                    .descricao("DESCRICAO SEM RELACAO NENHUMA")));

            assertThat(itemDaRevisao(loja, token, 1).motivoSugestao()).isEqualTo(MotivoSugestao.NENHUM);
        }
    }

    // ---------- expiração e limite de pendentes ----------

    @Nested
    @DisplayName("expiração e limite de pendentes")
    class Pendentes {

        @Test
        @DisplayName("no máximo 5 pendentes por loja: a mais antiga é cancelada")
        void limiteDeCinco() {
            UUID primeira = enviar(loja, NfeXml.nota().numero(61).item(NfeXml.item()));
            // garante a ordem de criação mesmo com relógio de milissegundos
            em.flush();
            jdbc.update("update importacao_nfe set criada_em = criada_em - interval '1 minute' where token = ?", primeira);
            limparSessao();
            List<UUID> demais = new java.util.ArrayList<>();
            for (int i = 62; i <= 65; i++) {
                demais.add(enviar(loja, NfeXml.nota().numero(i).item(NfeXml.item())));
            }
            assertThat(service.listarPendentes(loja.getCodigo())).hasSize(5);

            UUID sexta = enviar(loja, NfeXml.nota().numero(66).item(NfeXml.item()));

            assertThat(service.listarPendentes(loja.getCodigo())).hasSize(5);
            assertThat(statusDa(primeira)).isEqualTo("CANCELADA");
            assertThat(statusDa(sexta)).isEqualTo("PENDENTE");
            demais.forEach(t -> assertThat(statusDa(t)).isEqualTo("PENDENTE"));
            assertThatThrownBy(() -> service.revisar(primeira, loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("concluída ou cancelada");
        }

        @Test
        @DisplayName("o limite é por loja")
        void limitePorLoja() {
            Usuario outra = novoUsuario();
            for (int i = 70; i < 75; i++) {
                enviar(loja, NfeXml.nota().numero(i).item(NfeXml.item()));
            }
            enviar(outra, NfeXml.nota().numero(75).item(NfeXml.item()));

            assertThat(service.listarPendentes(loja.getCodigo())).hasSize(5);
            assertThat(service.listarPendentes(outra.getCodigo())).hasSize(1);
        }

        @Test
        @DisplayName("pendente vence em 30 minutos: não revisa, não confirma e sai da lista")
        void expiracao() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(80).item(NfeXml.item()));
            em.flush();
            assertThat(jdbc.queryForObject("select expira_em - criada_em from importacao_nfe where token = ?",
                    String.class, token)).startsWith("00:30:00");

            jdbc.update("update importacao_nfe set expira_em = ? where token = ?",
                    java.time.LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).minusMinutes(1), token);
            limparSessao();

            assertThatThrownBy(() -> service.revisar(token, loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("expirou");
            assertThatThrownBy(() -> tentar(token, confirmacao(vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("expirou");
            assertThat(service.listarPendentes(loja.getCodigo())).isEmpty();
            assertThat(movimentacoesDe(p.getId())).isEmpty();

            // um novo envio limpa as vencidas
            enviar(loja, NfeXml.nota().numero(81).item(NfeXml.item()));
            assertThat(statusDa(token)).isEqualTo("CANCELADA");
        }

        @Test
        @DisplayName("cancelar: pendente vira cancelada (idempotente); confirmada não pode ser cancelada")
        void cancelar() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(82).item(NfeXml.item()));

            service.cancelar(token, loja);
            service.cancelar(token, loja);
            assertThat(statusDa(token)).isEqualTo("CANCELADA");
            assertThatThrownBy(() -> tentar(token, confirmacao(vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class);

            UUID outra = enviar(loja, NfeXml.nota().numero(83).item(NfeXml.item()));
            service.confirmar(outra, confirmacao(vincular(p)), loja);
            assertThatThrownBy(() -> service.cancelar(outra, loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("já foi confirmada");
        }

        @Test
        @DisplayName("envio recusado não cria importação")
        void envioRecusado() {
            assertThatThrownBy(() -> service.enviar(NfeXml.nota().modelo("65").item(NfeXml.item()).bytes(), loja))
                    .isInstanceOf(RegraNegocioException.class);
            assertThat(contar("importacao_nfe", loja.getCodigo())).isZero();
        }
    }

    // ---------- sugestão de vínculo ----------

    @Nested
    @DisplayName("sugestão de vínculo")
    class Sugestao {

        @Test
        @DisplayName("1º por EAN; produto inativo com o mesmo EAN não é sugerido")
        void porEan() {
            Produto ativo = produtoCom("Qualquer nome", 0);
            ativo.setEan("7891000100103");
            produtoRepository.saveAndFlush(ativo);
            Produto inativo = produtoCom("Inativo", 0);
            inativo.setEan("7891000100110");
            inativo.setAtivo(false);
            produtoRepository.saveAndFlush(inativo);
            UUID token = enviar(loja, NfeXml.nota().numero(90)
                    .item(NfeXml.item().ean("7891000100103").descricao("NADA A VER"))
                    .item(NfeXml.item().ean("7891000100110").descricao("INATIVO XYZ")));

            RevisaoNfe revisao = service.revisar(token, loja);

            assertThat(revisao.itens().get(0).produtoSugeridoId()).isEqualTo(ativo.getId());
            assertThat(revisao.itens().get(0).motivoSugestao()).isEqualTo(MotivoSugestao.EAN);
            assertThat(revisao.itens().get(1).motivoSugestao()).isNotEqualTo(MotivoSugestao.EAN);
            assertThat(revisao.itens().get(1).produtoSugeridoId()).isNotEqualTo(inativo.getId());
        }

        @Test
        @DisplayName("2º pelo código do fornecedor, aprendido em nota anterior do mesmo fornecedor")
        void porCodigoDoFornecedor() {
            Produto p = produtoCom("Cabo flat", 0);
            service.confirmar(enviar(loja, NfeXml.nota().numero(91)
                    .item(NfeXml.item().codigo("X1").ean(null).descricao("DESCRICAO ANTIGA"))),
                    confirmacao(vincular(p)), loja);

            UUID mesmoFornecedor = enviar(loja, NfeXml.nota().numero(92)
                    .item(NfeXml.item().codigo("X1").ean(null).descricao("OUTRA DESCRICAO BEM DIFERENTE")));
            UUID outroFornecedor = enviar(loja, NfeXml.nota().numero(93).emitente(NfeXml.CNPJ_OUTRO)
                    .item(NfeXml.item().codigo("X1").ean(null).descricao("OUTRA DESCRICAO BEM DIFERENTE")));

            ItemRevisao igual = itemDaRevisao(loja, mesmoFornecedor, 1);
            assertThat(igual.motivoSugestao()).isEqualTo(MotivoSugestao.CODIGO_FORNECEDOR);
            assertThat(igual.produtoSugeridoId()).isEqualTo(p.getId());
            assertThat(itemDaRevisao(loja, outroFornecedor, 1).motivoSugestao()).isEqualTo(MotivoSugestao.NENHUM);
        }

        @Test
        @DisplayName("3º pelo nome parecido (nota >= 0,8); nome sem relação sugere produto novo")
        void porNome() {
            Produto caneta = produtoCom("Caneta Esferografica Azul", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(94)
                    .item(NfeXml.item().codigo("N1").ean(null).descricao("CANETA ESFEROGRAFICA AZUL CX 50"))
                    .item(NfeXml.item().codigo("N2").ean(null).descricao("BORRACHA BRANCA GRANDE")));

            RevisaoNfe revisao = service.revisar(token, loja);

            assertThat(revisao.itens().get(0).motivoSugestao()).isEqualTo(MotivoSugestao.NOME);
            assertThat(revisao.itens().get(0).produtoSugeridoId()).isEqualTo(caneta.getId());
            assertThat(revisao.itens().get(1).motivoSugestao()).isEqualTo(MotivoSugestao.NENHUM);
            assertThat(revisao.itens().get(1).produtoSugeridoId()).isNull();
        }

        @Test
        @DisplayName("o formulário inicial vincula o sugerido, cria os novos com o custo como preço e ignora fracionados")
        void formularioInicial() {
            Produto p = produtoCom("Cabo", 0);
            p.setEan("7891000100103");
            produtoRepository.saveAndFlush(p);
            UUID token = enviar(loja, NfeXml.nota().numero(95)
                    .item(NfeXml.item().ean("7891000100103"))
                    .item(NfeXml.item().ean(null).codigo("Z").descricao("ITEM NOVO TOTALMENTE").unitario("3.4560000000"))
                    .item(NfeXml.item().quantidade("1.5000")));

            ConfirmacaoNfeForm form = service.formularioInicial(service.revisar(token, loja));

            assertThat(form.getItem().get(1).getAcao()).isEqualTo(AcaoItemNfe.VINCULAR);
            assertThat(form.getItem().get(1).getProdutoId()).isEqualTo(p.getId());
            assertThat(form.getItem().get(2).getAcao()).isEqualTo(AcaoItemNfe.CRIAR);
            assertThat(form.getItem().get(2).getNome()).isEqualTo("ITEM NOVO TOTALMENTE");
            assertThat(form.getItem().get(2).getPreco()).isEqualByComparingTo("3.46");
            assertThat(form.getItem().get(3).getAcao()).isEqualTo(AcaoItemNfe.IGNORAR);
            assertThat(form.isConfirmouDestinatario()).isFalse();
        }
    }

    // ---------- revisão: destinatário e avisos ----------

    @Nested
    @DisplayName("revisão: destinatário e avisos")
    class Revisao {

        @Test
        @DisplayName("loja sem CNPJ: sem comparação, mas a revisão mostra sempre o destinatário")
        void lojaSemCnpj() {
            RevisaoNfe revisao = service.revisar(enviar(loja, NfeXml.nota().numero(100).item(NfeXml.item())), loja);

            assertThat(revisao.lojaTemCnpj()).isFalse();
            assertThat(revisao.destinatarioConfere()).isNull();
            assertThat(revisao.nota().destinatario().documento()).isEqualTo(NfeXml.CNPJ_LOJA);
            assertThat(revisao.nota().assinaturaPresente()).isTrue();
            assertThat(revisao.avisoVerificacao()).isEqualTo(
                    org.example.stockhubspring.nfe.NotaNfe.AVISO_NAO_VERIFICADA);
        }

        @Test
        @DisplayName("destinatário diferente do CNPJ da loja gera aviso (não bloqueia a confirmação)")
        void destinatarioDiferente() {
            loja.setCnpj(NfeXml.CNPJ_OUTRO);
            usuarioRepository.saveAndFlush(loja);
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(101).item(NfeXml.item()));

            RevisaoNfe revisao = service.revisar(token, loja);

            assertThat(revisao.lojaTemCnpj()).isTrue();
            assertThat(revisao.destinatarioConfere()).isFalse();
            assertThat(revisao.avisos()).anyMatch(a -> a.contains("diferente do CNPJ cadastrado"));
            assertThat(service.confirmar(token, confirmacao(vincular(p)), loja).getId()).isNotNull();
        }

        @Test
        @DisplayName("destinatário igual ao CNPJ da loja confere, sem aviso")
        void destinatarioIgual() {
            loja.setCnpj(NfeXml.CNPJ_LOJA);
            usuarioRepository.saveAndFlush(loja);

            RevisaoNfe revisao = service.revisar(enviar(loja, NfeXml.nota().numero(102).item(NfeXml.item())), loja);

            assertThat(revisao.destinatarioConfere()).isTrue();
            assertThat(revisao.avisos()).noneMatch(a -> a.contains("diferente"));
        }

        @Test
        @DisplayName("avisos de assinatura e protocolo ausentes, e CFOP de não revenda no item")
        void avisosDoArquivo() {
            RevisaoNfe revisao = service.revisar(enviar(loja, NfeXml.nota().numero(103).semNfeProc().semAssinatura()
                    .item(NfeXml.item().cfop("5910"))), loja);

            assertThat(revisao.avisos()).anyMatch(a -> a.contains("assinatura"));
            assertThat(revisao.avisos()).anyMatch(a -> a.contains("protocolo"));
            assertThat(revisao.itens().get(0).avisos()).anyMatch(a -> a.contains("revenda"));
            assertThat(revisao.itens().get(0).cfopEntrada()).isEqualTo("1910");
        }

        @Test
        @DisplayName("a revisão não expõe o XML cru e traz o fornecedor conhecido")
        void semXmlCru() {
            UUID token = enviar(loja, NfeXml.nota().numero(104).item(NfeXml.item()));
            assertThat(service.revisar(token, loja).fornecedorConhecido()).isFalse();
            assertThat(service.listarPendentes(loja.getCodigo()).get(0).toString()).doesNotContain("<");
        }
    }

    // ---------- endurecimento (auditoria de segurança) ----------

    @Nested
    @DisplayName("endurecimento (auditoria)")
    class Endurecimento {

        private boolean xmlZerado(UUID token) {
            em.flush();
            return Boolean.TRUE.equals(jdbc.queryForObject(
                    "select xml is null from importacao_nfe where token = ?", Boolean.class, token));
        }

        @Test
        @DisplayName("XML fica guardado enquanto pendente e é zerado ao confirmar")
        void zeradoAoConfirmar() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(200).item(NfeXml.item()));
            assertThat(xmlZerado(token)).isFalse();

            service.confirmar(token, confirmacao(vincular(p)), loja);

            assertThat(xmlZerado(token)).isTrue();
            assertThat(statusDa(token)).isEqualTo("CONFIRMADA");
        }

        @Test
        @DisplayName("XML é zerado ao cancelar")
        void zeradoAoCancelar() {
            UUID token = enviar(loja, NfeXml.nota().numero(201).item(NfeXml.item()));

            service.cancelar(token, loja);

            assertThat(xmlZerado(token)).isTrue();
            assertThat(statusDa(token)).isEqualTo("CANCELADA");
        }

        @Test
        @DisplayName("XML é zerado ao expirar (na limpeza do próximo envio) e quando o limite de 5 cancela a mais antiga")
        void zeradoAoExpirarEAoPassarDoLimite() {
            UUID vencida = enviar(loja, NfeXml.nota().numero(202).item(NfeXml.item()));
            em.flush();
            jdbc.update("update importacao_nfe set expira_em = ? where token = ?",
                    java.time.LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).minusMinutes(1), vencida);
            limparSessao();

            UUID nova = enviar(loja, NfeXml.nota().numero(203).item(NfeXml.item()));

            assertThat(xmlZerado(vencida)).isTrue();
            assertThat(statusDa(vencida)).isEqualTo("CANCELADA");
            assertThat(xmlZerado(nova)).isFalse();

            // limite de 5: a mais antiga cancelada também tem o XML zerado
            em.flush();
            jdbc.update("update importacao_nfe set criada_em = criada_em - interval '1 minute' where token = ?", nova);
            limparSessao();
            for (int i = 204; i <= 207; i++) {
                enviar(loja, NfeXml.nota().numero(i).item(NfeXml.item()));
            }
            enviar(loja, NfeXml.nota().numero(208).item(NfeXml.item()));
            assertThat(statusDa(nova)).isEqualTo("CANCELADA");
            assertThat(xmlZerado(nova)).isTrue();
        }

        @Test
        @DisplayName("mesma chave com conteúdo DIFERENTE: a pendente antiga é cancelada e vem uma nova")
        void mesmaChaveOutroConteudo() {
            UUID antiga = enviar(loja, NfeXml.nota().numero(210).item(NfeXml.item().quantidade("1.0000")));
            UUID nova = enviar(loja, NfeXml.nota().numero(210).item(NfeXml.item().quantidade("2.0000")));

            assertThat(nova).isNotEqualTo(antiga);
            assertThat(statusDa(antiga)).isEqualTo("CANCELADA");
            assertThat(xmlZerado(antiga)).isTrue();
            assertThat(service.listarPendentes(loja.getCodigo())).hasSize(1);
        }

        @Test
        @DisplayName("XML adulterado depois do envio (SHA-256 não confere) é recusado na revisão e na confirmação")
        void shaAdulterado() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(220).item(NfeXml.item().descricao("ORIGINAL")));
            em.flush();
            jdbc.update("update importacao_nfe set xml = replace(xml, 'ORIGINAL', 'TROCADO') where token = ?", token);
            limparSessao();

            assertThatThrownBy(() -> service.revisar(token, loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessage(ImportacaoNfeService.MENSAGEM_INTEGRIDADE);
            assertThatThrownBy(() -> tentar(token, confirmacao(vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessage(ImportacaoNfeService.MENSAGEM_INTEGRIDADE);
        }

        @Test
        @DisplayName("todas as decisões são validadas ANTES de gravar: erro no item 3 não deixa nada dos itens 1 e 2")
        void validaTudoAntesDeGravar() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(230)
                    .item(NfeXml.item().codigo("A")).item(NfeXml.item().codigo("B")).item(NfeXml.item().codigo("C")));
            // sem savepoint de propósito: se algo fosse gravado antes da validação, ficaria visível aqui
            assertThatThrownBy(() -> service.confirmar(token,
                    confirmacao(vincular(p), criar("Novo B", categoria, "2.00"), criar("Novo C", null, "2.00")), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Item 3");

            assertThat(contar("nota_fiscal", loja.getCodigo())).isZero();
            assertThat(contar("parceiro", loja.getCodigo())).isZero();
            assertThat(movimentacoesDe(p.getId())).isEmpty();
            assertThat(linhaProduto(p.getId()).get("quantidade")).isEqualTo(0);
            assertThat(jdbc.queryForObject("select count(*) from produto where usuario_codigo = ? and nome = 'Novo B'",
                    Long.class, loja.getCodigo())).isZero();
        }

        @Test
        @DisplayName("produto inexistente no item 2 também é barrado antes de gravar")
        void produtoInvalidoAntesDeGravar() {
            Produto p = produtoCom("Cabo", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(231).item(NfeXml.item()).item(NfeXml.item()));

            assertThatThrownBy(() -> service.confirmar(token,
                    confirmacao(vincular(p), vincularId(Integer.MAX_VALUE)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Item 2");

            assertThat(contar("nota_fiscal", loja.getCodigo())).isZero();
            assertThat(movimentacoesDe(p.getId())).isEmpty();
        }

        @Test
        @DisplayName("mesma nota (loja, fornecedor, modelo, série, número) com outra chave não entra duas vezes")
        void unicidadeDeNegocio() {
            Produto p = produtoCom("Cabo", 0);
            service.confirmar(enviar(loja, NfeXml.nota().numero(240).codigoNumerico(11111111).item(NfeXml.item())),
                    confirmacao(vincular(p)), loja);
            UUID outraChave = enviar(loja, NfeXml.nota().numero(240).codigoNumerico(22222222).item(NfeXml.item()));

            assertThatThrownBy(() -> tentar(outraChave, confirmacao(vincular(p)), loja))
                    .isInstanceOf(RegraNegocioException.class).hasMessage("Esta nota já foi importada nesta loja.");
            assertThat(linhaProduto(p.getId()).get("quantidade")).isEqualTo(10);
        }

        @Test
        @DisplayName("a revisão avisa que EAN e NCM serão gravados no produto que ainda não os tem")
        void avisoEanNcm() {
            Produto p = produtoCom("Cabo", 0);
            service.confirmar(enviar(loja, NfeXml.nota().numero(250)
                    .item(NfeXml.item().codigo("X9").ean(null).ncm(""))), confirmacao(vincular(p)), loja);
            assertThat(linhaProduto(p.getId()).get("ean")).isNull();

            RevisaoNfe revisao = service.revisar(enviar(loja, NfeXml.nota().numero(251)
                    .item(NfeXml.item().codigo("X9").ean("7891000100103").ncm("22021000"))), loja);

            ItemRevisao item = revisao.itens().get(0);
            assertThat(item.motivoSugestao()).isEqualTo(MotivoSugestao.CODIGO_FORNECEDOR);
            assertThat(item.avisoEan()).isTrue();
            assertThat(item.avisoNcm()).isTrue();
            assertThat(item.avisos()).noneMatch(a -> a.contains("será gravado"));
            assertThat(revisao.avisos()).anyMatch(a -> a.startsWith("Ao confirmar"));
        }

        @Test
        @DisplayName("nota emitida há mais de 12 meses gera aviso")
        void notaAntiga() {
            RevisaoNfe antiga = service.revisar(enviar(loja, NfeXml.nota().numero(260)
                    .emissao("2024-01-10T10:00:00-03:00").item(NfeXml.item())), loja);
            RevisaoNfe recente = service.revisar(enviar(loja, NfeXml.nota().numero(261).item(NfeXml.item())), loja);

            assertThat(antiga.avisos()).anyMatch(a -> a.contains("mais de 12 meses"));
            assertThat(recente.avisos()).noneMatch(a -> a.contains("mais de 12 meses"));
        }

        @Test
        @DisplayName("o orçamento de tempo da busca por nome vale dentro do laço: sem orçamento, não há sugestão por nome")
        void orcamentoDeNomes() {
            produtoCom("Caneta Esferografica Azul", 0);
            UUID token = enviar(loja, NfeXml.nota().numero(270)
                    .item(NfeXml.item().ean(null).codigo("N1").descricao("CANETA ESFEROGRAFICA AZUL")));
            try {
                service.definirOrcamentoNomes(0);
                assertThat(itemDaRevisao(loja, token, 1).motivoSugestao()).isEqualTo(MotivoSugestao.NENHUM);
            } finally {
                service.definirOrcamentoNomes(3_000_000_000L);
            }
            assertThat(itemDaRevisao(loja, token, 1).motivoSugestao()).isEqualTo(MotivoSugestao.NOME);
        }

        @Test
        @DisplayName("a revisão busca EAN e memória do fornecedor em lote: nota de 170 itens faz poucas consultas")
        void consultasEmLote() {
            String[] eans = {"7891000100103", "7891000100110", "7891000100127"};
            for (int i = 0; i < eans.length; i++) {
                Produto p = produtoCom("Produto " + i, 0);
                p.setEan(eans[i]);
                produtoRepository.saveAndFlush(p);
            }
            NfeXml nota = NfeXml.nota().numero(280);
            for (int i = 0; i < 150; i++) {
                nota.item(NfeXml.item().codigo("E" + i).ean(eans[i % 3]).descricao("ITEM " + i));
            }
            for (int i = 0; i < 20; i++) {
                nota.item(NfeXml.item().codigo("S" + i).ean(null).descricao("SEM VINCULO QUALQUER " + i));
            }
            UUID token = enviar(loja, nota);
            limparSessao();
            org.hibernate.stat.Statistics estatisticas = em.getEntityManagerFactory()
                    .unwrap(org.hibernate.SessionFactory.class).getStatistics();
            estatisticas.setStatisticsEnabled(true);
            estatisticas.clear();

            RevisaoNfe revisao = service.revisar(token, loja);

            assertThat(revisao.itens()).hasSize(170);
            assertThat(revisao.itens().stream().filter(i -> i.motivoSugestao() == MotivoSugestao.EAN)).hasSize(150);
            assertThat(estatisticas.getPrepareStatementCount()).as("consultas ao banco").isLessThan(15);
        }
    }
}
