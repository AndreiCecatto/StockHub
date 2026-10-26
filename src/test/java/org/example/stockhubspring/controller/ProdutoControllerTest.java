package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ProdutoFiltro;
import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.MovimentacaoEstoque;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.model.NivelEstoque;
import org.example.stockhubspring.service.ProdutoService.ResultadoExclusao;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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

class ProdutoControllerTest extends ControllerTestBase {

    @BeforeEach
    void stubs() {
        when(produtoService.listarPagina(any(), any())).thenReturn(new PageImpl<>(List.of()));
    }

    /** Requisição de cadastro válida; os testes alteram só o campo que querem quebrar. */
    private MockHttpServletRequestBuilder cadastroValido() {
        return cadastro("nome", "Caneta");
    }

    /** Cadastro válido, trocando o valor de UM campo (param() acumularia dois valores, então monta do zero). */
    private MockHttpServletRequestBuilder cadastro(String campo, String valor) {
        java.util.Map<String, String> campos = new java.util.LinkedHashMap<>();
        campos.put("nome", "Caneta");
        campos.put("descricao", "Azul");
        campos.put("preco", "5.90");
        campos.put("quantidade", "10");
        campos.put("estoqueMinimo", "2");
        campos.put("categoriaId", "3");
        campos.put(campo, valor);
        MockHttpServletRequestBuilder requisicao = post("/produtos").with(logadoComo(7)).with(csrf());
        campos.forEach(requisicao::param);
        return requisicao;
    }

    @Test
    @DisplayName("listagem usa a loja logada e normaliza filtros (ordenação fora da lista branca, tamanho máximo)")
    void listagemNormalizaFiltros() throws Exception {
        mvc.perform(get("/produtos").with(logadoComo(7))
                        .param("ordenacao", "nome; drop table produto").param("direcao", "xx")
                        .param("tamanho", "100000").param("pagina", "-3").param("nivelEstoque", "baixo")
                        .param("busca", "  caneta "))
                .andExpect(status().isOk())
                .andExpect(view().name("produtos"))
                .andExpect(model().attribute("ordenacao", "id"))
                .andExpect(model().attribute("direcao", "desc"))
                .andExpect(model().attribute("tamanho", ProdutoFiltro.TAMANHO_MAXIMO))
                .andExpect(model().attribute("busca", "caneta"))
                .andExpect(model().attribute("nivelEstoque", "baixo"));

        ArgumentCaptor<ProdutoFiltro> filtro = ArgumentCaptor.forClass(ProdutoFiltro.class);
        verify(produtoService).listarPagina(filtro.capture(), eq(7));
        assertThat(filtro.getValue().nivel()).isEqualTo(NivelEstoque.BAIXO);
        assertThat(filtro.getValue().pagina()).isEqualTo(1);
    }

    @Test
    @DisplayName("parâmetros de filtro são devolvidos codificados para os links de paginação (sem quebrar a URL)")
    void parametrosDeFiltroCodificados() throws Exception {
        mvc.perform(get("/produtos").with(logadoComo(7)).param("busca", "a&b=c <x>"))
                .andExpect(model().attribute("parametrosFiltros",
                        containsString("busca=a%26b%3Dc+%3Cx%3E")));
    }

    @Test
    @DisplayName("acao=novo e acao=editar redirecionam para as rotas novas")
    void acoesAntigasRedirecionam() throws Exception {
        mvc.perform(get("/produtos").with(logadoComo(7)).param("acao", "novo"))
                .andExpect(redirectedUrl("/produtos/novo"));
        mvc.perform(get("/produtos").with(logadoComo(7)).param("acao", "editar").param("id", "4"))
                .andExpect(redirectedUrl("/produtos/4/editar"));
        mvc.perform(get("/produtos").with(logadoComo(7)).param("acao", "visualizar").param("id", "4"))
                .andExpect(redirectedUrl("/produtos/4"));
    }

    @Test
    @DisplayName("formulário novo mostra um ProdutoForm vazio com as categorias da loja")
    void formularioNovo() throws Exception {
        mvc.perform(get("/produtos/novo").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attributeExists("produto", "categorias"));
        verify(categoriaService).listarParaSelecao(7);
    }

    @Test
    @DisplayName("produto inexistente ou de outra loja responde 404 no detalhe")
    void detalheDeOutraLojaDa404() throws Exception {
        when(produtoService.buscarPorId(99, 7)).thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));
        mvc.perform(get("/produtos/99").with(logadoComo(7)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("produto inexistente ou de outra loja responde 404 na edição")
    void edicaoDeOutraLojaDa404() throws Exception {
        when(produtoService.buscarPorId(99, 7)).thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));
        mvc.perform(get("/produtos/99/editar").with(logadoComo(7)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("cadastro válido chama o service com a loja da sessão e redireciona com mensagem")
    void cadastroValidoRedireciona() throws Exception {
        mvc.perform(cadastroValido())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("mensagem", "Produto cadastrado."));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<ProdutoForm> form = ArgumentCaptor.forClass(ProdutoForm.class);
        verify(produtoService).salvar(form.capture(), usuario.capture());
        assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
        assertThat(form.getValue().getNome()).isEqualTo("Caneta");
    }

    @Test
    @DisplayName("edição (com id) mostra a mensagem de atualização")
    void edicaoMostraMensagemDeAtualizacao() throws Exception {
        mvc.perform(cadastro("id", "5"))
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("mensagem", "Produto atualizado."));
    }

    @Test
    @DisplayName("o dono do produto nunca vem do POST: parâmetros usuario.codigo e ativo são ignorados")
    void donoNaoVemDoPost() throws Exception {
        mvc.perform(cadastro("usuario.codigo", "99").param("usuarioCodigo", "99").param("ativo", "false"))
                .andExpect(redirectedUrl("/produtos"));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(produtoService).salvar(any(), usuario.capture());
        assertThat(usuario.getValue().getCodigo()).isEqualTo(7);
    }

    @Test
    @DisplayName("nome vazio volta ao mesmo formulário com erro no campo e não chama o service")
    void nomeVazioVoltaComErro() throws Exception {
        mvc.perform(cadastro("nome", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attributeHasFieldErrors("produto", "nome"))
                .andExpect(model().attribute("errosCampos", hasKey("nome")))
                .andExpect(model().attributeExists("categorias", "erro"));
        verify(produtoService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("nome com 1 caractere ou com 121 caracteres é recusado (faixa 2 a 120)")
    void nomeForaDaFaixaRecusado() throws Exception {
        mvc.perform(cadastro("nome", "a"))
                .andExpect(model().attributeHasFieldErrors("produto", "nome"));
        mvc.perform(cadastro("nome", "a".repeat(121)))
                .andExpect(model().attributeHasFieldErrors("produto", "nome"));
        mvc.perform(cadastro("nome", "a".repeat(120)))
                .andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @DisplayName("preço negativo, com 3 casas ou acima de 9.999.999,99 é recusado")
    void precoInvalidoRecusado() throws Exception {
        mvc.perform(cadastro("preco", "-1"))
                .andExpect(model().attributeHasFieldErrors("produto", "preco"));
        mvc.perform(cadastro("preco", "1.999"))
                .andExpect(model().attributeHasFieldErrors("produto", "preco"));
        mvc.perform(cadastro("preco", "10000000.00"))
                .andExpect(model().attributeHasFieldErrors("produto", "preco"));
        verify(produtoService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("preço zero e preço máximo são aceitos")
    void precoNosLimitesAceito() throws Exception {
        mvc.perform(cadastro("preco", "0")).andExpect(redirectedUrl("/produtos"));
        mvc.perform(cadastro("preco", "9999999.99")).andExpect(redirectedUrl("/produtos"));
    }

    @Test
    @DisplayName("preço com letras dá mensagem amigável em português, não texto técnico")
    void precoComLetrasMensagemAmigavel() throws Exception {
        mvc.perform(cadastro("preco", "abc"))
                .andExpect(view().name("produto-form"))
                .andExpect(model().attributeHasFieldErrors("produto", "preco"))
                .andExpect(model().attribute("erro", containsString("valor numérico válido")));
    }

    @Test
    @DisplayName("quantidade negativa e estoque mínimo negativo são recusados")
    void quantidadesNegativasRecusadas() throws Exception {
        mvc.perform(cadastro("quantidade", "-5"))
                .andExpect(model().attributeHasFieldErrors("produto", "quantidade"));
        mvc.perform(cadastro("estoqueMinimo", "-1"))
                .andExpect(model().attributeHasFieldErrors("produto", "estoqueMinimo"));
        verify(produtoService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("quantidade acima do limite do int dá erro de formulário em vez de 500")
    void quantidadeGigantesca() throws Exception {
        mvc.perform(cadastro("quantidade", "99999999999999"))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attributeHasFieldErrors("produto", "quantidade"));
    }

    @Test
    @DisplayName("cadastro: estoque inicial acima de 1.000.000 é recusado no campo quantidade; 1.000.000 é aceito")
    void estoqueInicialAcimaDoLimiteRecusadoNoCadastro() throws Exception {
        mvc.perform(cadastro("quantidade", "1000001"))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attributeHasFieldErrors("produto", "quantidade"))
                .andExpect(model().attribute("errosCampos", hasKey("quantidade")))
                .andExpect(model().attribute("erro", containsString("1.000.000")));
        verify(produtoService, never()).salvar(any(), any());

        mvc.perform(cadastro("quantidade", "1000000"))
                .andExpect(redirectedUrl("/produtos"));
        verify(produtoService).salvar(any(), any());
    }

    @Test
    @DisplayName("edição: a quantidade é ignorada, então o saldo atual acima de 1.000.000 (campo somente leitura) não bloqueia")
    void edicaoNaoValidaOLimiteDoEstoqueInicial() throws Exception {
        mvc.perform(cadastro("quantidade", "1500000000").param("id", "5"))
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("mensagem", "Produto atualizado."));
    }

    @Test
    @DisplayName("sem categoria selecionada é recusado")
    void semCategoriaRecusado() throws Exception {
        mvc.perform(post("/produtos").with(logadoComo(7)).with(csrf())
                        .param("nome", "Caneta").param("preco", "1").param("estoqueMinimo", "0"))
                .andExpect(view().name("produto-form"))
                .andExpect(model().attributeHasFieldErrors("produto", "categoriaId"));
    }

    @Test
    @DisplayName("descrição com mais de 500 caracteres é recusada")
    void descricaoLongaRecusada() throws Exception {
        mvc.perform(cadastro("descricao", "d".repeat(501)))
                .andExpect(model().attributeHasFieldErrors("produto", "descricao"));
    }

    @Test
    @DisplayName("nome com <script> passa a validação e chega ao service como texto puro")
    void nomeComScriptChegaComoTextoPuro() throws Exception {
        mvc.perform(cadastro("nome", "<script>alert(1)</script>"))
                .andExpect(redirectedUrl("/produtos"));
        ArgumentCaptor<ProdutoForm> form = ArgumentCaptor.forClass(ProdutoForm.class);
        verify(produtoService).salvar(form.capture(), any());
        assertThat(form.getValue().getNome()).isEqualTo("<script>alert(1)</script>");
    }

    @Test
    @DisplayName("regra de negócio violada volta ao formulário com a mensagem do service")
    void regraDeNegocioVoltaAoFormulario() throws Exception {
        when(produtoService.salvar(any(), any()))
                .thenThrow(new RegraNegocioException("Já existe um produto ativo chamado \"Caneta\" na sua loja."));

        mvc.perform(cadastroValido())
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attribute("erro", "Já existe um produto ativo chamado \"Caneta\" na sua loja."))
                .andExpect(model().attributeExists("categorias"));
    }

    @Test
    @DisplayName("excluir produto sem histórico informa 'excluído'")
    void excluirInformaExcluido() throws Exception {
        when(produtoService.excluir(5, 7)).thenReturn(ResultadoExclusao.EXCLUIDO);
        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("mensagem", "Produto excluído."));
    }

    @Test
    @DisplayName("excluir produto com histórico informa 'inativado'")
    void excluirInformaInativado() throws Exception {
        when(produtoService.excluir(5, 7)).thenReturn(ResultadoExclusao.INATIVADO);
        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(redirectedUrl("/produtos"))
                .andExpect(flash().attribute("mensagem",
                        containsString("inativado")));
    }

    @Test
    @DisplayName("excluir produto de outra loja (IDOR) responde 404 e usa o código da sessão")
    void excluirDeOutraLojaDa404() throws Exception {
        when(produtoService.excluir(5, 7)).thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));
        mvc.perform(post("/produtos/5/excluir").with(logadoComo(7)).with(csrf()))
                .andExpect(status().isNotFound());
        verify(produtoService).excluir(5, 7);
    }

    @Test
    @DisplayName("id que não é número no caminho responde erro de cliente, não 500")
    void idNaoNumericoDa4xx() throws Exception {
        mvc.perform(get("/produtos/abc").with(logadoComo(7)))
                .andExpect(status().is4xxClientError());
    }

    // ----- B8: tamanho padrão -----

    @Test
    @DisplayName("sem o parâmetro tamanho, a listagem usa 25 produtos por página")
    void tamanhoPadraoDe25() throws Exception {
        mvc.perform(get("/produtos").with(logadoComo(7)))
                .andExpect(model().attribute("tamanho", 25));
        ArgumentCaptor<ProdutoFiltro> filtro = ArgumentCaptor.forClass(ProdutoFiltro.class);
        verify(produtoService).listarPagina(filtro.capture(), eq(7));
        assertThat(filtro.getValue().tamanho()).isEqualTo(25);
    }

    // ----- B3: histórico no detalhe -----

    @Test
    @DisplayName("detalhe expõe movimentacoesProduto (as últimas 20 do produto, da loja da sessão)")
    void detalheComMovimentacoes() throws Exception {
        Produto produto = Fabrica.produto(4, "Caneta", 10, 2, Fabrica.usuario(7));
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setQuantidadeAnterior(5);
        mov.setQuantidadeNova(10);
        when(produtoService.buscarPorId(4, 7)).thenReturn(produto);
        when(estoqueService.ultimasDoProduto(4, 7, 20)).thenReturn(List.of(mov));

        mvc.perform(get("/produtos/4").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-detalhe"))
                .andExpect(model().attribute("produto", produto))
                .andExpect(model().attribute("movimentacoesProduto", List.of(mov)));
    }

    @Test
    @DisplayName("detalhe de produto de outra loja responde 404 e nem consulta o histórico")
    void detalheDeOutraLojaNaoConsultaHistorico() throws Exception {
        when(produtoService.buscarPorId(99, 7)).thenThrow(new RecursoNaoEncontradoException("Produto não encontrado."));
        mvc.perform(get("/produtos/99").with(logadoComo(7)))
                .andExpect(status().isNotFound());
        verify(estoqueService, never()).ultimasDoProduto(any(), any(), anyInt());
    }

    // ----- B4: exportação CSV -----

    private Produto produtoParaCsv(String nome, String descricao, int quantidade) {
        Usuario loja = Fabrica.usuario(7);
        Produto produto = Fabrica.produto(4, nome, quantidade, 2, loja);
        produto.setDescricao(descricao);
        produto.setPreco(new java.math.BigDecimal("1234.5"));
        produto.setCategoria(Fabrica.categoria(3, "Papelaria", loja));
        return produto;
    }

    @Test
    @DisplayName("exportar.csv: BOM UTF-8, ';', cabeçalhos em português, decimal com vírgula e anexo com data")
    void exportarCsvFormato() throws Exception {
        when(produtoService.listarParaExportar(any(), eq(7)))
                .thenReturn(new PageImpl<>(List.of(produtoParaCsv("Caneta", "Azul", 3))));

        byte[] corpo = mvc.perform(get("/produtos/exportar.csv").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        matchesPattern("attachment; filename=\"produtos-\\d{4}-\\d{2}-\\d{2}\\.csv\"")))
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Type", containsString("charset=UTF-8")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(corpo).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        String texto = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8);
        assertThat(texto.split("\r\n")).containsExactly(
                "Código;Nome;Descrição;Categoria;Preço (R$);Quantidade;Estoque mínimo;Nível de estoque;Valor em estoque (R$)",
                "4;Caneta;Azul;Papelaria;1234,50;3;2;Estoque normal;3703,50");
    }

    @Test
    @DisplayName("exportar.csv respeita busca, categoria, nível e ordenação da listagem, sem paginação, só da loja da sessão")
    void exportarCsvRespeitaFiltros() throws Exception {
        when(produtoService.listarParaExportar(any(), eq(7))).thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/produtos/exportar.csv").with(logadoComo(7))
                        .param("busca", " caneta ").param("categoriaId", "3").param("nivelEstoque", "baixo")
                        .param("ordenacao", "nome").param("direcao", "asc")
                        .param("pagina", "9").param("tamanho", "1"))
                .andExpect(status().isOk());

        ArgumentCaptor<ProdutoFiltro> filtro = ArgumentCaptor.forClass(ProdutoFiltro.class);
        verify(produtoService).listarParaExportar(filtro.capture(), eq(7));
        assertThat(filtro.getValue().busca()).isEqualTo("caneta");
        assertThat(filtro.getValue().categoriaId()).isEqualTo(3);
        assertThat(filtro.getValue().nivel()).isEqualTo(NivelEstoque.BAIXO);
        assertThat(filtro.getValue().ordenacao()).isEqualTo("nome");
        assertThat(filtro.getValue().direcao()).isEqualTo("asc");
    }

    @Test
    @DisplayName("exportar.csv protege fórmulas (= + - @ tab CR) e escapa aspas, ';' e quebras nos textos")
    void exportarCsvProtegeEEscapa() throws Exception {
        when(produtoService.listarParaExportar(any(), eq(7))).thenReturn(new PageImpl<>(List.of(
                produtoParaCsv("=1+1", "+cmd|' /C calc'!A0", 1),
                produtoParaCsv("-5", "@SUM(A1)", 1),
                produtoParaCsv("\t=x", "\r=y", 1),
                produtoParaCsv("Caneta \"azul\"; 2 un", "linha1\nlinha2", 1))));

        byte[] corpo = mvc.perform(get("/produtos/exportar.csv").with(logadoComo(7)))
                .andReturn().getResponse().getContentAsByteArray();
        String texto = new String(corpo, 3, corpo.length - 3, StandardCharsets.UTF_8);

        assertThat(texto)
                .contains("4;'=1+1;'+cmd|' /C calc'!A0;Papelaria;")
                .contains("4;'-5;'@SUM(A1);Papelaria;")
                .contains("4;'\t=x;\"'\r=y\";Papelaria;")
                .contains("4;\"Caneta \"\"azul\"\"; 2 un\";\"linha1\nlinha2\";Papelaria;");
    }

    @Test
    @DisplayName("exportar.csv não é capturado por /produtos/{id} (não vira erro de id inválido)")
    void exportarCsvNaoEUmId() throws Exception {
        when(produtoService.listarParaExportar(any(), eq(7))).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/produtos/exportar.csv").with(logadoComo(7)))
                .andExpect(status().isOk());
        verify(produtoService, never()).buscarPorId(any(), any());
    }
}
