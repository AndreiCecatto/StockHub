package org.example.stockhubspring.controller;

import org.example.stockhubspring.dto.AcaoItemNfe;
import org.example.stockhubspring.dto.ConfirmacaoNfeForm;
import org.example.stockhubspring.dto.PendenteNfe;
import org.example.stockhubspring.dto.RevisaoNfe;
import org.example.stockhubspring.dto.RevisaoNfe.ItemRevisao;
import org.example.stockhubspring.dto.RevisaoNfe.MotivoSugestao;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.NotaFiscal;
import org.example.stockhubspring.model.SituacaoItemNfe;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.nfe.NfeParser;
import org.example.stockhubspring.nfe.NotaNfe;
import org.example.stockhubspring.service.ImportacaoNfeService.DetalheNota;
import org.example.stockhubspring.testsupport.Fabrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Rotas /compras/nfe: login, CSRF, limite de tamanho, limite de abuso (429) e contrato das telas. */
class NfeControllerTest extends ControllerTestBase {

    @org.springframework.beans.factory.annotation.Autowired
    private org.example.stockhubspring.nfe.NfeLimitador limitador;

    @org.junit.jupiter.api.BeforeEach
    void zerarLimites() {
        // o contexto é compartilhado entre as classes de teste: cada teste começa com os contadores zerados
        limitador.reiniciar();
    }

    private static final UUID TOKEN = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String BASE = "/compras/nfe";

    private static MockMultipartFile arquivo(byte[] conteudo) {
        return new MockMultipartFile("arquivo", "nota.xml", "text/xml", conteudo);
    }

    private static MockMultipartFile xmlPequeno() {
        return arquivo("<NFe/>".getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartHttpServletRequestBuilder envio(MockMultipartFile arquivo) {
        return multipart(BASE + "/enviar").file(arquivo);
    }

    private static RevisaoNfe revisaoDeExemplo() {
        NotaNfe nota = new NotaNfe("4".repeat(44), 55, 1, 1234, "VENDA", LocalDateTime.of(2026, 3, 10, 10, 30),
                new NotaNfe.Pessoa("12345678000195", "FORNECEDOR", "FANTASIA", "RS"),
                new NotaNfe.Pessoa("98765432000198", "LOJA", null, "RS"),
                new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50.00"), true, true,
                List.of());
        ItemRevisao item = new ItemRevisao(1, "P1", null, "PRODUTO", "22021000", "5102", "1102", "UN",
                new BigDecimal("10.0000"), new BigDecimal("5.0000000000"), new BigDecimal("50.00"), null,
                new BigDecimal("5.0000"), SituacaoItemNfe.IMPORTADO, false, null, List.of(), null, null, null,
                MotivoSugestao.NENHUM, false, BigDecimal.ZERO);
        return new RevisaoNfe(TOKEN, LocalDateTime.of(2026, 3, 10, 11, 0), nota, NotaNfe.AVISO_NAO_VERIFICADA,
                List.of(), false, null, false, 1, List.of(item));
    }

    private static NotaFiscal notaSalva(int id) {
        NotaFiscal nota = new NotaFiscal();
        ReflectionTestUtils.setField(nota, "id", id);
        return nota;
    }

    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("segurança")
    class Seguranca {

        @ParameterizedTest(name = "GET {0} sem login redireciona para /login")
        @ValueSource(strings = {BASE, BASE + "/1", BASE + "/importacoes/11111111-2222-3333-4444-555555555555"})
        void paginasExigemLogin(String caminho) throws Exception {
            mvc.perform(get(caminho)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
            verifyNoInteractions(importacaoNfeService);
        }

        @Test
        @DisplayName("POST de envio sem login (mesmo com CSRF) redireciona para /login")
        void envioExigeLogin() throws Exception {
            mvc.perform(envio(xmlPequeno()).with(csrf()))
                    .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
            verifyNoInteractions(importacaoNfeService);
        }

        @Test
        @DisplayName("fetch sem login recebe 401 em JSON")
        void envioSemLoginJson() throws Exception {
            mvc.perform(envio(xmlPequeno()).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json"))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.ok").value(false));
        }

        @Test
        @DisplayName("upload sem token CSRF é 403, e o serviço nem é chamado")
        void envioSemCsrf() throws Exception {
            mvc.perform(envio(xmlPequeno()).with(logadoComo(41))).andExpect(status().isForbidden());
            verifyNoInteractions(importacaoNfeService);
        }

        @Test
        @DisplayName("fetch sem token CSRF recebe 403 em JSON")
        void envioSemCsrfJson() throws Exception {
            mvc.perform(envio(xmlPequeno()).with(logadoComo(41))
                            .header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.ok").value(false));
            verifyNoInteractions(importacaoNfeService);
        }

        @ParameterizedTest(name = "POST {0} sem token CSRF é 403")
        @ValueSource(strings = {BASE + "/importacoes/11111111-2222-3333-4444-555555555555/confirmar",
                BASE + "/importacoes/11111111-2222-3333-4444-555555555555/cancelar"})
        void acoesSemCsrf(String caminho) throws Exception {
            mvc.perform(post(caminho).with(logadoComo(41))).andExpect(status().isForbidden());
            verifyNoInteractions(importacaoNfeService);
        }

        @ParameterizedTest(name = "GET {0} não existe: ações só por POST")
        @ValueSource(strings = {BASE + "/enviar", BASE + "/importacoes/11111111-2222-3333-4444-555555555555/confirmar",
                BASE + "/importacoes/11111111-2222-3333-4444-555555555555/cancelar"})
        void acoesSoPorPost(String caminho) throws Exception {
            mvc.perform(get(caminho).with(logadoComo(41))).andExpect(status().is4xxClientError());
            verifyNoInteractions(importacaoNfeService);
        }
    }

    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("listagem")
    class Listagem {

        @Test
        @DisplayName("mostra as notas da loja da sessão, as pendentes e o limite de tamanho")
        void lista() throws Exception {
            NotaFiscal nota = notaSalva(9);
            when(importacaoNfeService.listarNotas(eq(42), eq(1), any(Integer.class)))
                    .thenReturn(new PageImpl<>(List.of(nota)));
            PendenteNfe pendente = new PendenteNfe(TOKEN, "4".repeat(44), LocalDateTime.now(), LocalDateTime.now());
            when(importacaoNfeService.listarPendentes(42)).thenReturn(List.of(pendente));

            mvc.perform(get(BASE).with(logadoComo(42)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("nfe-lista"))
                    .andExpect(model().attribute("notas", List.of(nota)))
                    .andExpect(model().attribute("pendentes", List.of(pendente)))
                    .andExpect(model().attribute("paginaAtual", 1))
                    .andExpect(model().attribute("totalPaginas", 1))
                    .andExpect(model().attribute("totalNotas", 1L))
                    .andExpect(model().attribute("tamanhoMaximoMb", 2));
        }
    }

    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("envio do XML")
    class Envio {

        @Test
        @DisplayName("formulário comum: envia os bytes com a loja da sessão e redireciona para a revisão")
        void envioOk() throws Exception {
            byte[] conteudo = "<NFe/>".getBytes(StandardCharsets.UTF_8);
            when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class))).thenReturn(TOKEN);

            mvc.perform(envio(arquivo(conteudo)).with(logadoComo(43)).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(BASE + "/importacoes/" + TOKEN));

            ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
            ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
            verify(importacaoNfeService).enviar(bytes.capture(), usuario.capture());
            assertThat(bytes.getValue()).isEqualTo(conteudo);
            assertThat(usuario.getValue().getCodigo()).isEqualTo(43);
        }

        @Test
        @DisplayName("fetch (X-Requested-With): responde JSON com o endereço da revisão")
        void envioAjaxOk() throws Exception {
            when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class))).thenReturn(TOKEN);

            mvc.perform(envio(xmlPequeno()).with(logadoComo(43)).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest").header("Accept", "application/json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ok").value(true))
                    .andExpect(jsonPath("$.redirect").value(BASE + "/importacoes/" + TOKEN));
        }

        @Test
        @DisplayName("token CSRF no cabeçalho X-CSRF-TOKEN (não na URL) é aceito no upload")
        void csrfNoCabecalho() throws Exception {
            when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class))).thenReturn(TOKEN);

            mvc.perform(envio(xmlPequeno()).with(logadoComo(43))
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                    .csrf().asHeader())
                            .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("arquivo acima de 2 MB é recusado no controller, sem chegar ao serviço")
        void arquivoGrande() throws Exception {
            byte[] grande = new byte[NfeParser.TAMANHO_MAXIMO_BYTES + 1];

            mvc.perform(envio(arquivo(grande)).with(logadoComo(43)).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attribute("erro", containsString("2 MB")));
            verifyNoInteractions(importacaoNfeService);
        }

        @Test
        @DisplayName("fetch com arquivo acima de 2 MB recebe 422 em JSON")
        void arquivoGrandeAjax() throws Exception {
            byte[] grande = new byte[NfeParser.TAMANHO_MAXIMO_BYTES + 1];

            mvc.perform(envio(arquivo(grande)).with(logadoComo(43)).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.erro", containsString("2 MB")));
            verifyNoInteractions(importacaoNfeService);
        }

        @Test
        @DisplayName("arquivo vazio volta à listagem com mensagem")
        void arquivoVazio() throws Exception {
            mvc.perform(envio(arquivo(new byte[0])).with(logadoComo(43)).with(csrf()))
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attribute("erro", containsString("Selecione")));
            verifyNoInteractions(importacaoNfeService);
        }

        @Test
        @DisplayName("nota recusada pelo serviço volta à listagem com a mensagem (flash erro)")
        void recusadaPeloServico() throws Exception {
            when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class)))
                    .thenThrow(new RegraNegocioException("NFC-e (modelo 65) não é aceita: envie uma NF-e modelo 55."));

            mvc.perform(envio(xmlPequeno()).with(logadoComo(43)).with(csrf()))
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attribute("erro", containsString("NFC-e")));
        }

        @Test
        @DisplayName("fetch: nota recusada responde 422 com {ok:false, erro}")
        void recusadaPeloServicoAjax() throws Exception {
            when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class)))
                    .thenThrow(new RegraNegocioException("Esta nota já foi importada em 10/03/2026 10:30."));

            mvc.perform(envio(xmlPequeno()).with(logadoComo(43)).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.erro").value("Esta nota já foi importada em 10/03/2026 10:30."));
        }

        @Test
        @DisplayName("sem o campo do arquivo: mensagem amigável, nunca erro técnico")
        void semCampoArquivo() throws Exception {
            mvc.perform(multipart(BASE + "/enviar").with(logadoComo(43)).with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("erro", containsString("Selecione o arquivo")));
            mvc.perform(multipart(BASE + "/enviar").with(logadoComo(43)).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.ok").value(false));
        }

        @Test
        @DisplayName("abuso: o 21º envio em 10 minutos da mesma loja recebe 429 em JSON com Retry-After")
        void limiteDeAbuso() throws Exception {
            when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class))).thenReturn(TOKEN);
            for (int i = 0; i < 20; i++) {
                mvc.perform(envio(xmlPequeno()).with(logadoComo(99)).with(csrf())
                                .header("X-Requested-With", "XMLHttpRequest"))
                        .andExpect(status().isOk());
            }

            mvc.perform(envio(xmlPequeno()).with(logadoComo(99)).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"))
                    .andExpect(jsonPath("$.ok").value(false))
                    .andExpect(jsonPath("$.erro", containsString("Muitas requisições")));
            // também para o formulário comum
            mvc.perform(envio(xmlPequeno()).with(logadoComo(99)).with(csrf()))
                    .andExpect(status().isTooManyRequests());

            verify(importacaoNfeService, times(20)).enviar(any(byte[].class), any(Usuario.class));
            // outra loja não é afetada
            mvc.perform(envio(xmlPequeno()).with(logadoComo(98)).with(csrf())
                            .header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isOk());
        }
    }

    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("revisão")
    class Revisao {

        @Test
        @DisplayName("publica no model a revisão, categorias, produtos, token e o formulário com decisões iniciais")
        void revisao() throws Exception {
            RevisaoNfe revisao = revisaoDeExemplo();
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class))).thenReturn(revisao);
            ConfirmacaoNfeForm inicial = new ConfirmacaoNfeForm();
            when(importacaoNfeService.formularioInicial(revisao)).thenReturn(inicial);
            when(categoriaService.listarParaSelecao(44)).thenReturn(List.of(Fabrica.categoria(1, "Geral",
                    Fabrica.usuario(44))));
            when(produtoService.listarParaSelecao(44)).thenReturn(List.of());

            mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(44)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("nfe-revisao"))
                    .andExpect(model().attribute("revisao", revisao))
                    .andExpect(model().attribute("token", TOKEN.toString()))
                    .andExpect(model().attribute("confirmacaoNfeForm", inicial))
                    .andExpect(model().attributeExists("categorias", "produtos"));
            verify(categoriaService).listarParaSelecao(44);
            verify(produtoService).listarParaSelecao(44);
        }

        @Test
        @DisplayName("importação expirada ou concluída volta à listagem com a mensagem")
        void revisaoRecusada() throws Exception {
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class)))
                    .thenThrow(new RegraNegocioException("Esta importação expirou (30 minutos). Envie o arquivo novamente."));

            mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(44)))
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attribute("erro", containsString("expirou")));
        }

        @Test
        @DisplayName("token de outra loja ou inexistente responde 404")
        void tokenAlheio() throws Exception {
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Importação não encontrada."));

            mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(44))).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("token que não é UUID responde 404 (igual a token alheio)")
        void tokenMalformado() throws Exception {
            mvc.perform(get(BASE + "/importacoes/nao-e-uuid").with(logadoComo(44))).andExpect(status().isNotFound());
            mvc.perform(post(BASE + "/importacoes/nao-e-uuid/cancelar").with(logadoComo(44)).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("confirmação")
    class Confirmacao {

        private String caminho() {
            return BASE + "/importacoes/" + TOKEN + "/confirmar";
        }

        @Test
        @DisplayName("lê só as decisões por item e a confirmação do destinatário, e redireciona para a nota")
        void confirma() throws Exception {
            when(importacaoNfeService.confirmar(eq(TOKEN), any(ConfirmacaoNfeForm.class), any(Usuario.class)))
                    .thenReturn(notaSalva(77));

            mvc.perform(post(caminho()).with(logadoComo(45)).with(csrf())
                            .param("confirmouDestinatario", "true")
                            .param("item[1].acao", "VINCULAR").param("item[1].produtoId", "5")
                            .param("item[2].acao", "CRIAR").param("item[2].nome", "Produto novo")
                            .param("item[2].categoriaId", "3").param("item[2].preco", "9.90")
                            .param("item[3].acao", "IGNORAR"))
                    .andExpect(redirectedUrl(BASE + "/77"))
                    .andExpect(flash().attributeExists("mensagem"));

            ArgumentCaptor<ConfirmacaoNfeForm> form = ArgumentCaptor.forClass(ConfirmacaoNfeForm.class);
            verify(importacaoNfeService).confirmar(eq(TOKEN), form.capture(), any(Usuario.class));
            assertThat(form.getValue().isConfirmouDestinatario()).isTrue();
            assertThat(form.getValue().getItem()).containsOnlyKeys(1, 2, 3);
            assertThat(form.getValue().getItem().get(1).getAcao()).isEqualTo(AcaoItemNfe.VINCULAR);
            assertThat(form.getValue().getItem().get(1).getProdutoId()).isEqualTo(5);
            assertThat(form.getValue().getItem().get(2).getNome()).isEqualTo("Produto novo");
            assertThat(form.getValue().getItem().get(2).getCategoriaId()).isEqualTo(3);
            assertThat(form.getValue().getItem().get(2).getPreco()).isEqualByComparingTo("9.90");
            assertThat(form.getValue().getItem().get(3).getAcao()).isEqualTo(AcaoItemNfe.IGNORAR);
        }

        @Test
        @DisplayName("campos fora do contrato (dados da nota vindos do navegador) são ignorados")
        void camposInesperados() throws Exception {
            when(importacaoNfeService.confirmar(eq(TOKEN), any(ConfirmacaoNfeForm.class), any(Usuario.class)))
                    .thenReturn(notaSalva(78));

            mvc.perform(post(caminho()).with(logadoComo(45)).with(csrf())
                            .param("confirmouDestinatario", "true")
                            .param("item[1].acao", "IGNORAR")
                            .param("item[1].quantidade", "999999").param("item[1].valorTotal", "1")
                            .param("chaveAcesso", "x"))
                    .andExpect(status().is3xxRedirection());

            ArgumentCaptor<ConfirmacaoNfeForm> form = ArgumentCaptor.forClass(ConfirmacaoNfeForm.class);
            verify(importacaoNfeService).confirmar(eq(TOKEN), form.capture(), any(Usuario.class));
            assertThat(form.getValue().getItem().get(1).getAcao()).isEqualTo(AcaoItemNfe.IGNORAR);
        }

        @Test
        @DisplayName("campo inválido volta à revisão com erro e o serviço não confirma")
        void formularioInvalido() throws Exception {
            RevisaoNfe revisao = revisaoDeExemplo();
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class))).thenReturn(revisao);

            mvc.perform(post(caminho()).with(logadoComo(45)).with(csrf())
                            .param("confirmouDestinatario", "true")
                            .param("item[1].acao", "TROCAR").param("item[1].preco", "abc"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("nfe-revisao"))
                    .andExpect(model().attributeExists("erro", "errosCampos", "revisao", "categorias", "produtos"))
                    .andExpect(model().attribute("errosCampos", hasKey("item[1].acao")));
            verify(importacaoNfeService, never()).confirmar(any(), any(), any());
        }

        @Test
        @DisplayName("nome com mais de 120 caracteres é recusado na validação")
        void nomeGrande() throws Exception {
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class))).thenReturn(revisaoDeExemplo());

            mvc.perform(post(caminho()).with(logadoComo(45)).with(csrf())
                            .param("confirmouDestinatario", "true")
                            .param("item[1].acao", "CRIAR").param("item[1].nome", "x".repeat(121)))
                    .andExpect(view().name("nfe-revisao"))
                    .andExpect(model().attribute("errosCampos", hasKey("item[1].nome")));
            verify(importacaoNfeService, never()).confirmar(any(), any(), any());
        }

        @Test
        @DisplayName("regra de negócio (ex.: saldo estourado) volta à revisão com a mensagem")
        void regraDeNegocio() throws Exception {
            when(importacaoNfeService.confirmar(eq(TOKEN), any(ConfirmacaoNfeForm.class), any(Usuario.class)))
                    .thenThrow(new RegraNegocioException("Item 2 (CABO): O saldo do produto não pode passar de 2.000.000.000 unidades. Nada foi importado."));

            mvc.perform(post(caminho()).with(logadoComo(45)).with(csrf())
                            .param("confirmouDestinatario", "true").param("item[1].acao", "IGNORAR"))
                    .andExpect(redirectedUrl(BASE + "/importacoes/" + TOKEN))
                    .andExpect(flash().attribute("erro", containsString("Item 2")));
        }

        @Test
        @DisplayName("token de outra loja responde 404 e nada é confirmado")
        void tokenAlheio() throws Exception {
            when(importacaoNfeService.confirmar(eq(TOKEN), any(ConfirmacaoNfeForm.class), any(Usuario.class)))
                    .thenThrow(new RecursoNaoEncontradoException("Importação não encontrada."));

            mvc.perform(post(caminho()).with(logadoComo(45)).with(csrf())
                            .param("confirmouDestinatario", "true").param("item[1].acao", "IGNORAR"))
                    .andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------------------------------

    @Nested
    @DisplayName("cancelamento e detalhe")
    class CancelamentoEDetalhe {

        @Test
        @DisplayName("cancelar por POST volta à listagem com mensagem")
        void cancelar() throws Exception {
            mvc.perform(post(BASE + "/importacoes/" + TOKEN + "/cancelar").with(logadoComo(46)).with(csrf()))
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attributeExists("mensagem"));
            verify(importacaoNfeService).cancelar(eq(TOKEN), any(Usuario.class));
        }

        @Test
        @DisplayName("cancelar importação já confirmada mostra o erro")
        void cancelarConfirmada() throws Exception {
            org.mockito.Mockito.doThrow(new RegraNegocioException("Esta importação já foi confirmada e não pode ser cancelada."))
                    .when(importacaoNfeService).cancelar(eq(TOKEN), any(Usuario.class));

            mvc.perform(post(BASE + "/importacoes/" + TOKEN + "/cancelar").with(logadoComo(46)).with(csrf()))
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attribute("erro", containsString("já foi confirmada")));
        }

        @Test
        @DisplayName("detalhe da nota usa a loja da sessão e publica nota, itens e contagens")
        void detalhe() throws Exception {
            NotaFiscal nota = notaSalva(12);
            when(importacaoNfeService.detalhe(12, 47)).thenReturn(new DetalheNota(nota, List.of()));

            mvc.perform(get(BASE + "/12").with(logadoComo(47)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("nfe-detalhe"))
                    .andExpect(model().attribute("nota", nota))
                    .andExpect(model().attribute("itens", List.of()))
                    .andExpect(model().attribute("itensImportados", 0L))
                    .andExpect(model().attribute("itensIgnorados", 0L))
                    .andExpect(model().attribute("itensFracionados", 0L));
        }

        @Test
        @DisplayName("detalhe publica como foi pago e as parcelas para a tela")
        void detalheComCobranca() throws Exception {
            NotaFiscal nota = notaSalva(13);
            org.example.stockhubspring.model.NotaFiscalPagamento pagamento = new org.example.stockhubspring.model.NotaFiscalPagamento();
            pagamento.setDescricao("Boleto");
            org.example.stockhubspring.model.NotaFiscalParcela parcela = new org.example.stockhubspring.model.NotaFiscalParcela();
            parcela.setNumero(1);
            when(importacaoNfeService.detalhe(13, 47)).thenReturn(
                    new DetalheNota(nota, List.of(), List.of(pagamento), List.of(parcela)));

            mvc.perform(get(BASE + "/13").with(logadoComo(47)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("pagamentos", List.of(pagamento)))
                    .andExpect(model().attribute("parcelas", List.of(parcela)));
        }

        @Test
        @DisplayName("nota de outra loja responde 404")
        void detalheAlheio() throws Exception {
            when(importacaoNfeService.detalhe(12, 47)).thenThrow(new RecursoNaoEncontradoException("Nota não encontrada."));

            mvc.perform(get(BASE + "/12").with(logadoComo(47))).andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("a resposta de erro nunca ecoa o conteúdo do XML")
    void semEcoDoConteudo() throws Exception {
        when(importacaoNfeService.enviar(any(byte[].class), any(Usuario.class)))
                .thenThrow(new RegraNegocioException(NfeParser.MENSAGEM_INVALIDA));

        mvc.perform(envio(arquivo("<segredo>SENHA-123</segredo>".getBytes(StandardCharsets.UTF_8)))
                        .with(logadoComo(48)).with(csrf()).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("SENHA-123"))));
    }

    @Nested
    @DisplayName("limites das demais rotas e bloqueio de linha")
    class LimitesEBloqueio {

        @Test
        @DisplayName("revisão: a 61ª chamada da mesma loja em 10 minutos recebe 429")
        void limiteDeRevisao() throws Exception {
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class))).thenReturn(revisaoDeExemplo());
            for (int i = 0; i < 60; i++) {
                mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(61))).andExpect(status().isOk());
            }

            // navegação comum não recebe JSON cru: volta à lista com a mensagem
            mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(61)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl(BASE))
                    .andExpect(flash().attributeExists("erro"));
            verify(importacaoNfeService, times(60)).revisar(eq(TOKEN), any(Usuario.class));
        }

        @Test
        @DisplayName("confirmação com erro de formulário também conta no limite (ela reprocessa o XML)")
        void limiteDeConfirmacao() throws Exception {
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class))).thenReturn(revisaoDeExemplo());
            for (int i = 0; i < 60; i++) {
                mvc.perform(post(BASE + "/importacoes/" + TOKEN + "/confirmar").with(logadoComo(62)).with(csrf())
                        .param("item[1].acao", "TROCAR")).andExpect(status().isOk());
            }

            mvc.perform(post(BASE + "/importacoes/" + TOKEN + "/confirmar").with(logadoComo(62)).with(csrf())
                            .param("item[1].acao", "TROCAR"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("erro"));
        }

        @Test
        @DisplayName("limite por IP: lojas diferentes no mesmo IP dividem o limite")
        void limitePorIp() throws Exception {
            when(importacaoNfeService.revisar(eq(TOKEN), any(Usuario.class))).thenReturn(revisaoDeExemplo());
            for (int loja = 70; loja < 72; loja++) {
                for (int i = 0; i < 60; i++) {
                    mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(loja))).andExpect(status().isOk());
                }
            }

            // 120 chamadas já gastas pelo IP: outra loja, mesmo IP, é barrada
            mvc.perform(get(BASE + "/importacoes/" + TOKEN).with(logadoComo(72)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("erro"));
        }

        @Test
        @DisplayName("deadlock ou bloqueio de linha na confirmação vira mensagem para tentar de novo, sem erro técnico")
        void bloqueioDeLinha() throws Exception {
            when(importacaoNfeService.confirmar(eq(TOKEN), any(ConfirmacaoNfeForm.class), any(Usuario.class)))
                    .thenThrow(new org.springframework.dao.CannotAcquireLockException("detalhe interno do banco"));

            mvc.perform(post(BASE + "/importacoes/" + TOKEN + "/confirmar").with(logadoComo(63)).with(csrf())
                            .param("confirmouDestinatario", "true").param("item[1].acao", "IGNORAR"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("erro", containsString("Tente de novo")))
                    .andExpect(flash().attribute("erro", org.hamcrest.Matchers.not(containsString("detalhe interno"))));
        }
    }
}
