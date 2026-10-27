package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.MovimentoDiario;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.Categoria;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.TipoMovimentacao;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.testsupport.Fabrica;
import org.example.stockhubspring.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contratos JSON da interface interativa: busca, movimentação por JSON e movimentos do painel. */
class ApiJsonWebTest extends ControllerTestBase {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private Produto produto(int id, String nome, int saldo, int minimo) {
        Usuario loja = Fabrica.usuario(7);
        Produto p = Fabrica.produto(id, nome, saldo, minimo, loja);
        Categoria c = Fabrica.categoria(3, "Papelaria", loja);
        p.setCategoria(c);
        return p;
    }

    @BeforeEach
    void buscaSemResultadoPorPadrao() {
        when(produtoService.buscarTolerante(any(), anyInt(), any()))
                .thenReturn(new ProdutoService.ResultadoBusca(List.of(), 0));
    }

    private MockHttpServletRequestBuilder movimentarJson() {
        return post("/estoque/movimentar").with(logadoComo(7)).with(csrf()).accept(JSON);
    }

    // ----- GET /api/busca -----

    @Test
    @DisplayName("busca: devolve array com o formato do contrato e consulta só a loja da sessão")
    void buscaFormato() throws Exception {
        when(produtoService.buscarTolerante(eq("cane"), eq(8), eq(7))).thenReturn(new ProdutoService.ResultadoBusca(
                List.of(produto(4, "Caneta azul", 3, 5), produto(5, "Caneca", 0, 2),
                        produto(6, "Canela", 50, 5)), 2));

        mvc.perform(get("/api/busca").param("q", "cane").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].nome").value("Caneta azul"))
                .andExpect(jsonPath("$[0].categoria").value("Papelaria"))
                .andExpect(jsonPath("$[0].saldo").value(3))
                .andExpect(jsonPath("$[0].minimo").value(5))
                .andExpect(jsonPath("$[0].nivel").value("BAIXO"))
                .andExpect(jsonPath("$[0].url").value("/produtos/4"))
                .andExpect(jsonPath("$[1].nivel").value("SEM_ESTOQUE"))
                .andExpect(jsonPath("$[2].nivel").value("NORMAL"))
                .andExpect(jsonPath("$[0].aproximado").value(false))
                .andExpect(jsonPath("$[1].aproximado").value(false))
                .andExpect(jsonPath("$[2].aproximado").value(true));
    }

    @Test
    @DisplayName("busca: limite ausente vale 8; limite informado é repassado; limite inválido volta ao padrão (sem 400)")
    void buscaLimite() throws Exception {
        mvc.perform(get("/api/busca").param("q", "ab").with(logadoComo(7))).andExpect(status().isOk());
        verify(produtoService).buscarTolerante("ab", 8, 7);

        mvc.perform(get("/api/busca").param("q", "ab").param("limite", "15").with(logadoComo(7)))
                .andExpect(status().isOk());
        verify(produtoService).buscarTolerante("ab", 15, 7);

        mvc.perform(get("/api/busca").param("q", "ab").param("limite", "abc").with(logadoComo(7)))
                .andExpect(status().isOk());
        verify(produtoService, org.mockito.Mockito.times(2)).buscarTolerante("ab", 8, 7);
    }

    @Test
    @DisplayName("busca: sem resultado ou sem q responde 200 com []")
    void buscaVazia() throws Exception {
        mvc.perform(get("/api/busca").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("busca e movimentos: anônimo recebe 401 em JSON, não redirect ao login")
    void anonimoRecebe401Json() throws Exception {
        mvc.perform(get("/api/busca").param("q", "caneta"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.erro").exists());
        mvc.perform(get("/api/dashboard/movimentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
        verify(produtoService, never()).buscarTolerante(any(), anyInt(), any());
    }

    // ----- POST /estoque/movimentar (JSON) -----

    @Test
    @DisplayName("movimentar JSON: sucesso 200 com saldo anterior/novo, nível e mensagem; usa a loja da sessão")
    void movimentarSucesso() throws Exception {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setId(99);
        mov.setProduto(produto(4, "Caneta azul", 5, 5));
        mov.setTipo(TipoMovimentacao.SAIDA);
        mov.setQuantidade(5);
        mov.setQuantidadeAnterior(10);
        mov.setQuantidadeNova(5);
        when(estoqueService.movimentar(eq(4), eq(TipoMovimentacao.SAIDA), eq(5), eq("Venda"), any(Usuario.class)))
                .thenReturn(mov);

        mvc.perform(movimentarJson().param("produtoId", "4").param("tipo", "SAIDA")
                        .param("quantidade", "5").param("observacao", "Venda"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.movimentacaoId").value(99))
                .andExpect(jsonPath("$.produtoId").value(4))
                .andExpect(jsonPath("$.nome").value("Caneta azul"))
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.quantidade").value(5))
                .andExpect(jsonPath("$.saldoAnterior").value(10))
                .andExpect(jsonPath("$.saldoNovo").value(5))
                .andExpect(jsonPath("$.nivel").value("BAIXO"))
                .andExpect(jsonPath("$.mensagem").value(containsString("passou de 10 para 5")));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(estoqueService).movimentar(eq(4), eq(TipoMovimentacao.SAIDA), eq(5), eq("Venda"), usuario.capture());
        org.assertj.core.api.Assertions.assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
    }

    @Test
    @DisplayName("movimentar JSON: o token CSRF no cabeçalho X-CSRF-TOKEN é aceito")
    void csrfPorCabecalho() throws Exception {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto(4, "Caneta", 5, 1));
        mov.setQuantidadeAnterior(1);
        mov.setQuantidadeNova(5);
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any())).thenReturn(mov);

        mvc.perform(post("/estoque/movimentar").with(logadoComo(7)).with(csrf().asHeader()).accept(JSON)
                        .param("produtoId", "4").param("tipo", "ENTRADA").param("quantidade", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @DisplayName("movimentar JSON: sem token CSRF responde 403 em JSON e não chama o service")
    void semCsrfRecusa() throws Exception {
        mvc.perform(post("/estoque/movimentar").with(logadoComo(7)).accept(JSON)
                        .param("produtoId", "4").param("tipo", "ENTRADA").param("quantidade", "4"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("movimentar JSON: anônimo (com CSRF) recebe 401 em JSON")
    void movimentarAnonimo() throws Exception {
        mvc.perform(post("/estoque/movimentar").with(csrf()).accept(JSON)
                        .param("produtoId", "4").param("tipo", "ENTRADA").param("quantidade", "4"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ok").value(false));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("movimentar JSON: regra de negócio violada vira 422 {ok:false, erro}")
    void regraViolada422() throws Exception {
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RegraNegocioException("Saída de 50 unidade(s) maior que o saldo atual (10). Nada foi registrado."));

        mvc.perform(movimentarJson().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "50"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.erro").value(containsString("maior que o saldo atual (10)")));
    }

    @Test
    @DisplayName("movimentar JSON: campos inválidos viram 400 com o mapa 'campos' e não chamam o service")
    void validacao400() throws Exception {
        mvc.perform(movimentarJson().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "-3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.erro").isNotEmpty())
                .andExpect(jsonPath("$.campos.quantidade").value(containsString("negativa")));
        mvc.perform(movimentarJson().param("produtoId", "4").param("tipo", "SAIDA").param("quantidade", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", hasKey("quantidade")));
        mvc.perform(movimentarJson())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", hasKey("produtoId")))
                .andExpect(jsonPath("$.campos", hasKey("tipo")));
        verify(estoqueService, never()).movimentar(any(), any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("movimentar JSON: produto inexistente ou de outra loja (IDOR) responde 404 em JSON")
    void produtoDeOutraLoja404() throws Exception {
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));

        mvc.perform(movimentarJson().param("produtoId", "999").param("tipo", "ENTRADA").param("quantidade", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.erro").value("Produto não encontrado."));
    }

    @Test
    @DisplayName("movimentar sem Accept JSON (nem Accept de navegador) segue com redirect e flash, como antes")
    void semAcceptJsonContinuaRedirect() throws Exception {
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setProduto(produto(4, "Caneta", 5, 1));
        mov.setQuantidadeAnterior(10);
        mov.setQuantidadeNova(15);
        when(estoqueService.movimentar(any(), any(), anyInt(), any(), any())).thenReturn(mov);

        String navegador = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
        for (MockHttpServletRequestBuilder pedido : List.of(
                post("/estoque/movimentar"),
                post("/estoque/movimentar").header("Accept", navegador),
                post("/estoque/movimentar").header("Accept", "*/*"))) {
            mvc.perform(pedido.with(logadoComo(7)).with(csrf())
                            .param("produtoId", "4").param("tipo", "ENTRADA").param("quantidade", "5"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/estoque"));
        }
    }

    // ----- GET /api/dashboard/movimentos -----

    @Test
    @DisplayName("movimentos: formato do contrato (dia, entradas, saidas, ajustes) e dias padrão 14")
    void movimentosFormato() throws Exception {
        when(dashboardService.movimentosPorDia(7, 14)).thenReturn(List.of(
                new MovimentoDiario("2026-10-25", 10, 4, 1), new MovimentoDiario("2026-10-26", 0, 0, 0)));

        mvc.perform(get("/api/dashboard/movimentos").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dia").value("2026-10-25"))
                .andExpect(jsonPath("$[0].entradas").value(10))
                .andExpect(jsonPath("$[0].saidas").value(4))
                .andExpect(jsonPath("$[0].ajustes").value(1))
                .andExpect(jsonPath("$[1].entradas").value(0));
    }

    @Test
    @DisplayName("movimentos: dias informado é repassado (o service ajusta aos limites); inválido volta ao padrão")
    void movimentosDias() throws Exception {
        mvc.perform(get("/api/dashboard/movimentos").param("dias", "30").with(logadoComo(7)))
                .andExpect(status().isOk());
        verify(dashboardService).movimentosPorDia(7, 30);

        mvc.perform(get("/api/dashboard/movimentos").param("dias", "9999").with(logadoComo(7)))
                .andExpect(status().isOk());
        verify(dashboardService).movimentosPorDia(7, 9999);

        mvc.perform(get("/api/dashboard/movimentos").param("dias", "x").with(logadoComo(7)))
                .andExpect(status().isOk());
        verify(dashboardService).movimentosPorDia(7, 14);
    }

    @Test
    @DisplayName("erro inesperado numa rota da API vira 500 em JSON sem detalhes internos")
    void erroInesperadoSemVazar() throws Exception {
        when(dashboardService.movimentosPorDia(anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("detalhe interno secreto"));

        mvc.perform(get("/api/dashboard/movimentos").with(logadoComo(7)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secreto"))));
    }
}
