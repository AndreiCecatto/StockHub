package org.example.stockhubspring.service;

import jakarta.el.ELProcessor;
import org.example.stockhubspring.dto.AcaoItemNfe;
import org.example.stockhubspring.dto.ConfirmacaoNfeForm;
import org.example.stockhubspring.dto.DecisaoItemNfeForm;
import org.example.stockhubspring.dto.PendenteNfe;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.ImportacaoNfe;
import org.example.stockhubspring.model.Produto;
import java.util.List;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.nfe.NfeParser;
import org.example.stockhubspring.nfe.NotaNfe;
import org.example.stockhubspring.repository.CategoriaRepository;
import org.example.stockhubspring.repository.ImportacaoNfeRepository;
import org.example.stockhubspring.repository.NotaFiscalItemRepository;
import org.example.stockhubspring.repository.NotaFiscalRepository;
import org.example.stockhubspring.repository.ParceiroRepository;
import org.example.stockhubspring.repository.ProdutoRepository;
import org.example.stockhubspring.repository.UsuarioRepository;
import org.example.stockhubspring.testsupport.Fabrica;
import org.example.stockhubspring.testsupport.NfeXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cenários que o banco não reproduz bem (corrida entre duas confirmações) e contrato com as JSPs. */
class ImportacaoNfeServiceTest {

    private ImportacaoNfeRepository importacaoRepository;
    private NotaFiscalRepository notaRepository;
    private ParceiroRepository parceiroRepository;
    private EstoqueService estoqueService;
    private ProdutoRepository produtoRepository;
    private UsuarioRepository usuarioRepository;
    private ImportacaoNfeService service;
    private final Usuario loja = Fabrica.usuario(5);

    @BeforeEach
    void preparar() {
        importacaoRepository = mock(ImportacaoNfeRepository.class);
        notaRepository = mock(NotaFiscalRepository.class);
        parceiroRepository = mock(ParceiroRepository.class);
        estoqueService = mock(EstoqueService.class);
        produtoRepository = mock(ProdutoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        service = new ImportacaoNfeService(importacaoRepository, notaRepository, mock(NotaFiscalItemRepository.class),
                parceiroRepository, produtoRepository, mock(CategoriaRepository.class),
                usuarioRepository, estoqueService);
        when(usuarioRepository.getReferenceById(anyInt())).thenReturn(loja);
    }

    private ImportacaoNfe pendenteCom(NfeXml nota) {
        ImportacaoNfe importacao = new ImportacaoNfe();
        importacao.setToken(UUID.randomUUID());
        importacao.setXml(nota.xml());
        importacao.setSha256(sha(nota.xml()));
        importacao.setExpiraEm(LocalDateTime.now().plusMinutes(10));
        when(importacaoRepository.buscarComBloqueio(importacao.getToken(), loja.getCodigo()))
                .thenReturn(Optional.of(importacao));
        return importacao;
    }

    /** Decisão de vincular o item 1 ao produto 3 (que o mock de produtos devolve como ativo da loja). */
    private ConfirmacaoNfeForm confirmacaoIgnorando() {
        Produto tres = Fabrica.produto(3, "Tres", 0, 0, loja);
        when(produtoRepository.findByUsuarioCodigoAndAtivoTrueAndIdIn(anyInt(), any())).thenReturn(List.of(tres));
        when(produtoRepository.buscarAtivoComBloqueio(anyInt(), anyInt())).thenReturn(Optional.of(tres));
        ConfirmacaoNfeForm form = new ConfirmacaoNfeForm();
        form.setConfirmouDestinatario(true);
        DecisaoItemNfeForm decisao = new DecisaoItemNfeForm();
        decisao.setAcao(AcaoItemNfe.VINCULAR);
        decisao.setProdutoId(3);
        form.getItem().put(1, decisao);
        return form;
    }

    @Test
    @DisplayName("corrida: se outra confirmação gravou a mesma chave no meio do caminho, a UNIQUE vira mensagem e nada entra no estoque")
    void corridaNaConfirmacao() {
        ImportacaoNfe importacao = pendenteCom(NfeXml.notaComUmItem());
        when(notaRepository.findByUsuarioCodigoAndChaveAcesso(anyInt(), any())).thenReturn(Optional.empty());
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(anyInt(), any())).thenReturn(Optional.empty());
        when(parceiroRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(notaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uk_nota_fiscal_usuario_chave\""));

        assertThatThrownBy(() -> service.confirmar(importacao.getToken(), confirmacaoIgnorando(), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já foi importada");
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("violação de outra restrição não vaza detalhes do banco")
    void outraViolacao() {
        ImportacaoNfe importacao = pendenteCom(NfeXml.notaComUmItem());
        when(notaRepository.findByUsuarioCodigoAndChaveAcesso(anyInt(), any())).thenReturn(Optional.empty());
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(anyInt(), any())).thenReturn(Optional.empty());
        when(parceiroRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(notaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("detalhe interno do banco"));

        assertThatThrownBy(() -> service.confirmar(importacao.getToken(), confirmacaoIgnorando(), loja))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageNotContaining("detalhe interno");
    }

    @Test
    @DisplayName("confirmar relê o XML guardado: XML adulterado no banco (DOCTYPE) é recusado")
    void revalidaXmlGuardado() {
        ImportacaoNfe importacao = pendenteCom(NfeXml.notaComUmItem());
        String adulterado = NfeXml.notaComUmItem().antesDaRaiz("<!DOCTYPE nfeProc>").xml();
        importacao.setXml(adulterado);
        importacao.setSha256(sha(adulterado)); // SHA coerente: quem barra é o parser

        assertThatThrownBy(() -> service.confirmar(importacao.getToken(), confirmacaoIgnorando(), loja))
                .isInstanceOf(RegraNegocioException.class).hasMessage(NfeParser.MENSAGEM_INVALIDA);
        verify(notaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("envio acima de 2 MB é recusado antes de qualquer leitura e nada é guardado")
    void envioGrande() {
        assertThatThrownBy(() -> service.enviar(new byte[NfeParser.TAMANHO_MAXIMO_BYTES + 1], loja))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("2 MB");
        verify(importacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("envio de XXE é recusado com mensagem genérica e nada é guardado")
    void envioXxe() {
        byte[] xxe = NfeXml.notaComUmItem()
                .antesDaRaiz("<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///C:/Windows/win.ini\">]>").bytes();

        assertThatThrownBy(() -> service.enviar(xxe, loja))
                .isInstanceOf(RegraNegocioException.class).hasMessage(NfeParser.MENSAGEM_INVALIDA);
        verify(importacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("contrato das JSPs: o EL (Tomcat 11) lê as propriedades dos records da revisão")
    void elLeRecords() {
        ELProcessor el = new ELProcessor();
        el.defineBean("p", new PendenteNfe(UUID.randomUUID(), "1".repeat(44), LocalDateTime.now(), LocalDateTime.now()));
        el.defineBean("n", new NotaNfe("1".repeat(44), 55, 1, 99, "VENDA", LocalDateTime.now(),
                new NotaNfe.Pessoa("12345678000195", "FORNECEDOR", null, "RS"), null, null, null, null, null,
                false, false, java.util.List.of()));

        assertThat((Object) el.eval("p.chaveAcesso")).isEqualTo("1".repeat(44));
        assertThat((Object) el.eval("n.emitente.nome")).isEqualTo("FORNECEDOR");
        assertThat((Object) el.eval("n.numero")).isEqualTo(99L);
    }

    private static String sha(String texto) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("confirmar confere o SHA-256 guardado: XML trocado depois do envio é recusado sem ler nada")
    void shaDiferente() {
        ImportacaoNfe importacao = pendenteCom(NfeXml.notaComUmItem());
        importacao.setXml(NfeXml.nota().numero(555).item(NfeXml.item()).xml()); // outro XML, SHA antigo

        assertThatThrownBy(() -> service.confirmar(importacao.getToken(), confirmacaoIgnorando(), loja))
                .isInstanceOf(RegraNegocioException.class).hasMessage(ImportacaoNfeService.MENSAGEM_INTEGRIDADE);
        verify(notaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("sem SHA-256 guardado, ou com XML já zerado, a confirmação é recusada")
    void semShaOuSemXml() {
        ImportacaoNfe semSha = pendenteCom(NfeXml.notaComUmItem());
        semSha.setSha256(null);
        assertThatThrownBy(() -> service.confirmar(semSha.getToken(), confirmacaoIgnorando(), loja))
                .isInstanceOf(RegraNegocioException.class).hasMessage(ImportacaoNfeService.MENSAGEM_INTEGRIDADE);

        ImportacaoNfe semXml = pendenteCom(NfeXml.notaComUmItem());
        semXml.setXml(null);
        assertThatThrownBy(() -> service.confirmar(semXml.getToken(), confirmacaoIgnorando(), loja))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("os produtos são travados em ordem crescente de id, qualquer que seja a ordem dos itens")
    void travaEmOrdemCrescente() {
        ImportacaoNfe importacao = pendenteCom(NfeXml.nota()
                .item(NfeXml.item().codigo("A")).item(NfeXml.item().codigo("B")).item(NfeXml.item().codigo("C")));
        ProdutoRepository produtos = org.mockito.Mockito.mock(ProdutoRepository.class);
        service = new ImportacaoNfeService(importacaoRepository, notaRepository, mock(NotaFiscalItemRepository.class),
                parceiroRepository, produtos, mock(CategoriaRepository.class), usuarioRepository, estoqueService);
        Produto p9 = Fabrica.produto(9, "Nove", 0, 0, loja);
        Produto p2 = Fabrica.produto(2, "Dois", 0, 0, loja);
        Produto p5 = Fabrica.produto(5, "Cinco", 0, 0, loja);
        when(produtos.findByUsuarioCodigoAndAtivoTrueAndIdIn(anyInt(), any()))
                .thenReturn(java.util.List.of(p9, p2, p5));
        when(produtos.buscarAtivoComBloqueio(anyInt(), anyInt())).thenAnswer(i -> Optional.of(Fabrica.produto(
                i.getArgument(0), "x", 0, 0, loja)));
        when(notaRepository.findByUsuarioCodigoAndChaveAcesso(anyInt(), any())).thenReturn(Optional.empty());
        when(parceiroRepository.findByUsuarioCodigoAndDocumento(anyInt(), any())).thenReturn(Optional.empty());
        when(parceiroRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        // encerra logo depois do bloqueio, com a violação da UNIQUE da nota
        when(notaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_nota_fiscal_usuario_chave"));
        ConfirmacaoNfeForm form = new ConfirmacaoNfeForm();
        form.setConfirmouDestinatario(true);
        int[] ids = {9, 2, 5};
        for (int i = 0; i < 3; i++) {
            DecisaoItemNfeForm d = new DecisaoItemNfeForm();
            d.setAcao(AcaoItemNfe.VINCULAR);
            d.setProdutoId(ids[i]);
            form.getItem().put(i + 1, d);
        }

        assertThatThrownBy(() -> service.confirmar(importacao.getToken(), form, loja))
                .isInstanceOf(RegraNegocioException.class);

        var ordem = org.mockito.Mockito.inOrder(produtos);
        ordem.verify(produtos).buscarAtivoComBloqueio(2, loja.getCodigo());
        ordem.verify(produtos).buscarAtivoComBloqueio(5, loja.getCodigo());
        ordem.verify(produtos).buscarAtivoComBloqueio(9, loja.getCodigo());
    }

    @Test
    @DisplayName("o log só leva os 8 últimos dígitos da chave")
    void chaveCurta() {
        String chave = NfeXml.notaComUmItem().chave();
        assertThat(ImportacaoNfeService.chaveCurta(chave)).isEqualTo("..." + chave.substring(36)).hasSize(11);
        assertThat(ImportacaoNfeService.chaveCurta(null)).isEqualTo("?");
    }
}
