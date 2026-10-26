package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.MovimentacaoFiltro;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class EstoqueControllerTest extends ControllerTestBase {

    @BeforeEach
    void stubs() {
        when(estoqueService.listarMovimentacoes(anyInt(), any(MovimentacaoFiltro.class), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private MockHttpServletRequestBuilder movimentar() {
        return post("/estoque/movimentar").with(logadoComo(7)).with(csrf());
    }

    @Test
    @DisplayName("tela de estoque lista só o histórico e os produtos da loja da sessão")
    void telaDeEstoque() throws Exception {
        mvc.perform(get("/estoque").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeExists("produtos", "movimentacoes", "tipos", "paginaAtual", "totalPaginas"));
        verify(estoqueService).listarMovimentacoes(eq(7), eq(MovimentacaoFiltro.VAZIO), eq(1), anyInt());
        verify(produtoService).listarParaSelecao(7);
    }

    @Test
    @DisplayName("movimentação válida chama o service com a loja da sessão e informa saldo anterior e novo")
    void movimentacaoValida() throws Exception {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setQuantidadeAnterior(10);
        mov.setQuantidadeNova(15);
        when(estoqueService.movimentar(eq(4), eq(TipoMovimentacao.ENTRADA), eq(5), eq("Compra"), any(Usuario.class)))
                .thenReturn(mov);

        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "ENTRADA")
                        .param("quantidade", "5").param("observacao", " Compra "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estoque"))
                .andExpect(flash().attribute("mensagem", containsString("passou de 10 para 15")));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(estoqueService).movimentar(eq(4), eq(TipoMovimentacao.ENTRADA), eq(5), eq("Compra"), usuario.capture());
        assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
    }

    @Test
    @DisplayName("formulário vazio volta à mesma tela com erros por campo e não chama o service")
    void formularioVazioVoltaComErros() throws Exception {
        mvc.perform(movimentar())
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "produtoId", "tipo", "quantidade"))
                .andExpect(model().attribute("errosCampos", hasKey("quantidade")))
                .andExpect(model().attributeExists("produtos", "movimentacoes", "erro"));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("quantidade negativa é recusada pela validação do formulário")
    void quantidadeNegativaRecusada() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "-3"))
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "quantidade"));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("quantidade com letras ou gigante dá erro de formulário em português, não 500")
    void quantidadeInvalidaDaErroDeFormulario() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "abc"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "quantidade"));
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "99999999999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "quantidade"));
    }

    @Test
    @DisplayName("tipo de movimentação inexistente dá erro de formulário")
    void tipoInexistenteDaErro() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "TRANSFERENCIA").param("quantidade", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "tipo"))
                .andExpect(model().attribute("erro", containsString("tipo de movimentação")));
    }

    @Test
    @DisplayName("observação com mais de 255 caracteres é recusada")
    void observacaoLongaRecusada() throws Exception {
        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "ENTRADA").param("quantidade", "1")
                        .param("observacao", "o".repeat(256)))
                .andExpect(model().attributeHasFieldErrors("movimentacaoForm", "observacao"));
    }

    @Test
    @DisplayName("regra de negócio violada (saída acima do saldo) volta à tela de estoque com a mensagem")
    void saidaAcimaDoSaldoVoltaComMensagem() throws Exception {
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RegraNegocioException("Saída de 50 unidade(s) maior que o saldo atual (10). Nada foi registrado."));

        mvc.perform(movimentar().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "50"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attribute("erro",
                        "Saída de 50 unidade(s) maior que o saldo atual (10). Nada foi registrado."))
                .andExpect(model().attributeExists("produtos", "movimentacoes"));
    }

    @Test
    @DisplayName("produto de outra loja (IDOR) na movimentação responde 404")
    void produtoDeOutraLojaDa404() throws Exception {
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));

        mvc.perform(movimentar().param("produtoId", "999").param("tipo", "ENTRADA").param("quantidade", "1"))
                .andExpect(status().isNotFound());
    }

    // ----- B2: filtros do histórico -----

    @Test
    @DisplayName("filtros produto, tipo, de e ate chegam ao service (com a loja da sessão) e voltam ao model para repor os campos")
    void filtrosDoHistorico() throws Exception {
        mvc.perform(get("/estoque").with(logadoComo(7))
                        .param("produto", "4").param("tipo", "saida").param("de", "2026-01-05")
                        .param("ate", "2026-02-10").param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtroProduto", 4))
                .andExpect(model().attribute("filtroTipo", "SAIDA"))
                .andExpect(model().attribute("filtroDe", "2026-01-05"))
                .andExpect(model().attribute("filtroAte", "2026-02-10"))
                .andExpect(model().attribute("filtrosAtivos", true))
                .andExpect(model().attribute("parametrosFiltros",
                        "&produto=4&tipo=SAIDA&de=2026-01-05&ate=2026-02-10"));

        verify(estoqueService).listarMovimentacoes(eq(7),
                eq(new MovimentacaoFiltro(4, TipoMovimentacao.SAIDA,
                        LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 10))),
                eq(2), anyInt());
    }

    @Test
    @DisplayName("filtros inválidos são ignorados: sem 500, sem erro, tela normal e model sem filtro ativo")
    void filtrosInvalidosSaoIgnorados() throws Exception {
        mvc.perform(get("/estoque").with(logadoComo(7))
                        .param("produto", "abc").param("tipo", "TRANSFERENCIA")
                        .param("de", "31/12/2026").param("ate", "2026-02-30"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque"))
                .andExpect(model().attribute("filtrosAtivos", false))
                .andExpect(model().attribute("filtroTipo", ""))
                .andExpect(model().attribute("filtroDe", ""))
                .andExpect(model().attribute("filtroAte", ""))
                .andExpect(model().attribute("parametrosFiltros", ""));
        verify(estoqueService).listarMovimentacoes(eq(7), eq(MovimentacaoFiltro.VAZIO), eq(1), anyInt());
    }

    // ----- B4: exportação CSV -----

    private MovimentacaoEstoque movimentacao(String produto, String observacao) {
        Usuario loja = Fabrica.usuario(7);
        Produto p =
                Fabrica.produto(4, produto, 10, 2, loja);
        p.setCategoria(Fabrica.categoria(3, "Papelaria", loja));
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(p);
        mov.setTipo(TipoMovimentacao.SAIDA);
        mov.setQuantidade(3);
        mov.setQuantidadeAnterior(10);
        mov.setQuantidadeNova(7);
        mov.setObservacao(observacao);
        mov.setCriadoEm(LocalDateTime.of(2026, 3, 9, 14, 5, 7));
        return mov;
    }

    @Test
    @DisplayName("exportar.csv: UTF-8 com BOM, separador ';', cabeçalhos em português, anexo com data e filtros repassados")
    void exportarCsvFormato() throws Exception {
        when(estoqueService.listarParaExportar(eq(7), any(MovimentacaoFiltro.class)))
                .thenReturn(new PageImpl<>(List.of(movimentacao("Caneta", "Venda balcão"))));

        byte[] corpo = mvc.perform(get("/estoque/exportar.csv").with(logadoComo(7))
                        .param("produto", "4").param("tipo", "SAIDA").param("de", "2026-03-01").param("ate", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(header()
                        .string("Content-Disposition",
                                matchesPattern("attachment; filename=\"movimentacoes-\\d{4}-\\d{2}-\\d{2}\\.csv\"")))
                .andExpect(header()
                        .string("Content-Type", containsString("text/csv")))
                .andExpect(header()
                        .string("Content-Type", containsString("charset=UTF-8")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(corpo).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String texto = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8);
        String[] linhas = texto.split("\r\n");
        assertThat(linhas).containsExactly(
                "Data e hora;Código do produto;Produto;Categoria;Tipo;Quantidade;Saldo anterior;Saldo novo;Observação",
                "09/03/2026 14:05:07;4;Caneta;Papelaria;Saída;3;10;7;Venda balcão");
        verify(estoqueService).listarParaExportar(eq(7), eq(new MovimentacaoFiltro(4, TipoMovimentacao.SAIDA,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))));
    }

    @Test
    @DisplayName("exportar.csv protege fórmulas e escapa aspas, ';' e quebras de linha nos textos do usuário")
    void exportarCsvProtegeEEscapa() throws Exception {
        when(estoqueService.listarParaExportar(eq(7), any(MovimentacaoFiltro.class)))
                .thenReturn(new PageImpl<>(List.of(
                        movimentacao("=HYPERLINK(\"http://x\")", "linha1\nlinha2; \"aspas\""),
                        movimentacao("@SOMA(A1)", "-2+3"))));

        byte[] corpo = mvc.perform(get("/estoque/exportar.csv").with(logadoComo(7)))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8);

        assertThat(texto)
                // produto começa com '=': ganha apóstrofo; as aspas internas são dobradas e a célula vai entre aspas
                .contains(";\"'=HYPERLINK(\"\"http://x\"\")\";")
                // observação com quebra de linha, ';' e aspas
                .contains(";\"linha1\nlinha2; \"\"aspas\"\"\"\r\n")
                .contains(";'@SOMA(A1);")
                .contains(";'-2+3\r\n");
    }

    @Test
    @DisplayName("exportação cortada pelo limite avisa no cabeçalho X-Exportacao-Truncada")
    void exportarCsvTruncadoAvisa() throws Exception {
        when(estoqueService.listarParaExportar(eq(7), any(MovimentacaoFiltro.class)))
                .thenReturn(new PageImpl<>(
                        List.of(movimentacao("Caneta", null)),
                        PageRequest.of(0, 1), 50_000));

        mvc.perform(get("/estoque/exportar.csv").with(logadoComo(7)))
                .andExpect(header()
                        .string("X-Exportacao-Truncada", "true"));
    }
}
