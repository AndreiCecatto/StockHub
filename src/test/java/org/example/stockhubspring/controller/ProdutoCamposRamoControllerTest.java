package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.ProdutoForm;
import org.example.stockhubspring.model.Produto;
import org.example.stockhubspring.model.Subtipo;
import org.example.stockhubspring.service.CamposRamo;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Controller de produto com os campos por ramo (AD-17): modelo camposRamo, binding e segurança. */
class ProdutoCamposRamoControllerTest extends ControllerTestBase {

    private static final CamposRamo.Permitidos FARMACIA = CamposRamo.permitidos(Subtipo.FARMACIA);

    @Test
    @DisplayName("formulário novo expõe camposRamo da loja")
    void novoExpoeCamposRamo() throws Exception {
        when(produtoService.camposPermitidos(any())).thenReturn(FARMACIA);

        mvc.perform(get("/produtos/novo").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attribute("camposRamo", FARMACIA));
    }

    @Test
    @DisplayName("edição e detalhe expõem camposRamo, com produto antigo (campos null)")
    void editarEDetalheExpoemCamposRamo() throws Exception {
        Produto produto = Fabrica.produto(4, "Dipirona", 3, 1, Fabrica.usuario(7));
        produto.setCategoria(Fabrica.categoria(3, "Remédios", Fabrica.usuario(7)));
        when(produtoService.buscarPorId(4, 7)).thenReturn(produto);
        when(produtoService.camposPermitidos(any())).thenReturn(FARMACIA);

        mvc.perform(get("/produtos/4/editar").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("camposRamo", FARMACIA));
        mvc.perform(get("/produtos/4").with(logadoComo(7)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("camposRamo", FARMACIA));
    }

    @Test
    @DisplayName("loja sem subtipo recebe camposRamo vazio")
    void semSubtipoCamposVazios() throws Exception {
        when(produtoService.camposPermitidos(any())).thenReturn(CamposRamo.Permitidos.NENHUM);

        mvc.perform(get("/produtos/novo").with(logadoComo(7)))
                .andExpect(model().attribute("camposRamo", CamposRamo.Permitidos.NENHUM));
    }

    @Test
    @DisplayName("POST com CSRF liga os campos por ramo no ProdutoForm e redireciona")
    void postLigaCamposNoForm() throws Exception {
        mvc.perform(post("/produtos").with(logadoComo(7)).with(csrf())
                        .param("nome", "Dipirona").param("preco", "5.90").param("quantidade", "10")
                        .param("estoqueMinimo", "2").param("categoriaId", "3")
                        .param("lote", "  L-77 ").param("dataValidade", "2030-05-20")
                        .param("tamanho", "").param("numeroSerie", "SN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produtos"));

        ArgumentCaptor<ProdutoForm> captor = ArgumentCaptor.forClass(ProdutoForm.class);
        verify(produtoService).salvar(captor.capture(), any());
        ProdutoForm form = captor.getValue();
        assertThat(form.getLote()).isEqualTo("L-77");
        assertThat(form.getDataValidade()).isEqualTo(LocalDate.of(2030, 5, 20));
        assertThat(form.getTamanho()).isNull();
        assertThat(form.getNumeroSerie()).isEqualTo("SN");
    }

    @Test
    @DisplayName("POST sem CSRF segue recusado (403) mesmo com campos por ramo")
    void postSemCsrfRecusado() throws Exception {
        mvc.perform(post("/produtos").with(logadoComo(7))
                        .param("nome", "Dipirona").param("lote", "L1"))
                .andExpect(status().isForbidden());
        verify(produtoService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("sem login, o formulário redireciona ao login")
    void semLoginRedireciona() throws Exception {
        mvc.perform(get("/produtos/novo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("data de validade inválida volta ao formulário com camposRamo, sem chamar o service")
    void validadeInvalidaVoltaAoFormulario() throws Exception {
        when(produtoService.camposPermitidos(any())).thenReturn(FARMACIA);

        mvc.perform(post("/produtos").with(logadoComo(7)).with(csrf())
                        .param("nome", "Dipirona").param("preco", "5.90").param("quantidade", "1")
                        .param("estoqueMinimo", "2").param("categoriaId", "3")
                        .param("dataValidade", "nao-e-data"))
                .andExpect(status().isOk())
                .andExpect(view().name("produto-form"))
                .andExpect(model().attribute("camposRamo", FARMACIA));
        verify(produtoService, never()).salvar(any(), any());
    }

    @Test
    @DisplayName("texto com <script> no lote é só repassado ao service (a saída é escapada na JSP)")
    void scriptNoLoteNaoQuebra() throws Exception {
        mvc.perform(post("/produtos").with(logadoComo(7)).with(csrf())
                        .param("nome", "X1").param("preco", "1").param("quantidade", "0")
                        .param("estoqueMinimo", "0").param("categoriaId", "3")
                        .param("lote", "<script>alert(1)</script>"))
                .andExpect(status().is3xxRedirection());
    }
}
