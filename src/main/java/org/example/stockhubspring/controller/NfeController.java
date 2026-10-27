package org.example.stockhubspring.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.stockhubspring.dto.ConfirmacaoNfeForm;
import org.example.stockhubspring.dto.RevisaoNfe;
import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.example.stockhubspring.model.NotaFiscal;
import org.example.stockhubspring.model.NotaFiscalItem;
import org.example.stockhubspring.model.SituacaoItemNfe;
import org.example.stockhubspring.model.Usuario;
import org.example.stockhubspring.nfe.NfeLimitador;
import org.example.stockhubspring.nfe.NfeParser;
import org.example.stockhubspring.service.CategoriaService;
import org.example.stockhubspring.service.ImportacaoNfeService;
import org.example.stockhubspring.service.ImportacaoNfeService.DetalheNota;
import org.example.stockhubspring.service.ProdutoService;
import org.example.stockhubspring.service.UsuarioAtualService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Compras: importação de NF-e (XML). Só recebe, valida o formato e redireciona; as regras ficam no
 * {@link ImportacaoNfeService}. Tudo é escopado pela loja da sessão (RN-01).
 *
 * <p>Upload: o token CSRF NÃO vai na URL. O front envia por {@code fetch} com {@code FormData} e o cabeçalho
 * {@code X-CSRF-TOKEN}, mais {@code X-Requested-With: XMLHttpRequest} e {@code Accept: application/json}; a
 * resposta é JSON ({@code ok}, {@code redirect} ou {@code erro}).
 */
@Controller
@RequestMapping("/compras/nfe")
public class NfeController {

    private static final int TAMANHO_PAGINA = 20;
    private static final String LISTA = "/compras/nfe";

    private final ImportacaoNfeService importacaoService;
    private final CategoriaService categoriaService;
    private final ProdutoService produtoService;
    private final UsuarioAtualService usuarioAtualService;
    private final ErrosFormulario errosFormulario;
    private final NfeLimitador limitador;

    public NfeController(ImportacaoNfeService importacaoService,
                         CategoriaService categoriaService,
                         ProdutoService produtoService,
                         UsuarioAtualService usuarioAtualService,
                         ErrosFormulario errosFormulario,
                         NfeLimitador limitador) {
        this.importacaoService = importacaoService;
        this.categoriaService = categoriaService;
        this.produtoService = produtoService;
        this.usuarioAtualService = usuarioAtualService;
        this.errosFormulario = errosFormulario;
        this.limitador = limitador;
    }

    /** Só os campos esperados entram no formulário de confirmação (nada de propriedades inesperadas). */
    @InitBinder("confirmacaoNfeForm")
    void restringirCampos(WebDataBinder binder) {
        binder.setAllowedFields("confirmouDestinatario", "item[*].acao", "item[*].produtoId", "item[*].nome",
                "item[*].categoriaId", "item[*].preco");
    }

    /** Notas importadas (mais recentes primeiro), importações pendentes e o formulário de envio. */
    @GetMapping
    public String listar(@RequestParam(defaultValue = "1") int pagina, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        Page<NotaFiscal> notas = importacaoService.listarNotas(usuario.getCodigo(), pagina, TAMANHO_PAGINA);
        model.addAttribute("notas", notas.getContent());
        model.addAttribute("paginaAtual", notas.getNumber() + 1);
        model.addAttribute("totalPaginas", Math.max(1, notas.getTotalPages()));
        model.addAttribute("totalNotas", notas.getTotalElements());
        model.addAttribute("pendentes", importacaoService.listarPendentes(usuario.getCodigo()));
        model.addAttribute("tamanhoMaximoMb", NfeParser.TAMANHO_MAXIMO_BYTES / (1024 * 1024));
        return "nfe-lista";
    }

    /** Envio por formulário comum (sem JavaScript): sucesso redireciona à revisão; erro volta à lista com mensagem. */
    @PostMapping("/enviar")
    public Object enviar(@RequestParam("arquivo") MultipartFile arquivo, HttpServletRequest request,
                         RedirectAttributes redirect) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        ResponseEntity<Map<String, Object>> barrado = barrar(usuario, request, true);
        if (barrado != null) {
            return barrado;
        }
        try {
            UUID token = receber(arquivo, usuario);
            return "redirect:/compras/nfe/importacoes/" + token;
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:" + LISTA;
        } finally {
            limitador.sairProcessamento();
        }
    }

    /** Envio por fetch/FormData (cabeçalho X-Requested-With): responde JSON {ok, redirect} ou {ok:false, erro}. */
    @PostMapping(value = "/enviar", headers = "X-Requested-With=XMLHttpRequest")
    public ResponseEntity<Map<String, Object>> enviarAjax(@RequestParam("arquivo") MultipartFile arquivo,
                                                          HttpServletRequest request) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        ResponseEntity<Map<String, Object>> barrado = barrar(usuario, request, true);
        if (barrado != null) {
            return barrado;
        }
        try {
            UUID token = receber(arquivo, usuario);
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("ok", true);
            corpo.put("redirect", "/compras/nfe/importacoes/" + token);
            return json(HttpStatus.OK, corpo);
        } catch (RegraNegocioException e) {
            return erroJson(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        } finally {
            limitador.sairProcessamento();
        }
    }

    /** Revisão da importação pendente: cabeçalho, avisos, itens com sugestão de vínculo e decisões iniciais. */
    @GetMapping("/importacoes/{token}")
    public Object revisar(@PathVariable String token, HttpServletRequest request, Model model,
                          RedirectAttributes redirect) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        UUID id = interpretar(token);
        ResponseEntity<Map<String, Object>> barrado = barrar(usuario, request, false);
        if (barrado != null) {
            return paraNavegacao(barrado, redirect);
        }
        try {
            RevisaoNfe revisao = importacaoService.revisar(id, usuario);
            carregarRevisao(model, revisao, usuario, importacaoService.formularioInicial(revisao));
            return "nfe-revisao";
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:" + LISTA;
        } finally {
            limitador.sairProcessamento();
        }
    }

    @PostMapping("/importacoes/{token}/confirmar")
    public Object confirmar(@PathVariable String token,
                            @Valid @ModelAttribute("confirmacaoNfeForm") ConfirmacaoNfeForm form,
                            BindingResult resultado,
                            HttpServletRequest request,
                            Model model,
                            RedirectAttributes redirect) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        UUID id = interpretar(token);
        ResponseEntity<Map<String, Object>> barrado = barrar(usuario, request, false);
        if (barrado != null) {
            return paraNavegacao(barrado, redirect);
        }
        try {
            if (resultado.hasErrors()) {
                // Volta à MESMA tela de revisão, com os erros e as escolhas do dono
                RevisaoNfe revisao = importacaoService.revisar(id, usuario);
                errosFormulario.publicar(resultado, model);
                carregarRevisao(model, revisao, usuario, form);
                return "nfe-revisao";
            }
            NotaFiscal nota = importacaoService.confirmar(id, form, usuario);
            redirect.addFlashAttribute("mensagem", "Nota importada: os itens escolhidos entraram no estoque.");
            return "redirect:/compras/nfe/" + nota.getId();
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/compras/nfe/importacoes/" + id;
        } finally {
            limitador.sairProcessamento();
        }
    }

    @PostMapping("/importacoes/{token}/cancelar")
    public String cancelar(@PathVariable String token, RedirectAttributes redirect) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        try {
            importacaoService.cancelar(interpretar(token), usuario);
            redirect.addFlashAttribute("mensagem", "Importação cancelada. Nada foi gravado no estoque.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + LISTA;
    }

    /** Detalhe da nota importada: itens, produtos vinculados e movimentações geradas. */
    @GetMapping("/{id}")
    public String detalhe(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioAtualService.getUsuarioAtual();
        DetalheNota detalhe = importacaoService.detalhe(id, usuario.getCodigo());
        List<NotaFiscalItem> itens = detalhe.itens();
        model.addAttribute("nota", detalhe.nota());
        model.addAttribute("itens", itens);
        model.addAttribute("itensImportados", contar(itens, SituacaoItemNfe.IMPORTADO));
        model.addAttribute("itensIgnorados", contar(itens, SituacaoItemNfe.IGNORADO));
        model.addAttribute("itensFracionados", contar(itens, SituacaoItemNfe.FRACIONADO));
        return "nfe-detalhe";
    }

    // -------------------------------------------------------------------------------------------

    private UUID receber(MultipartFile arquivo, Usuario usuario) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Selecione o arquivo XML da NF-e.");
        }
        // confere o tamanho ANTES de ler o conteúdo para a memória
        if (arquivo.getSize() > NfeParser.TAMANHO_MAXIMO_BYTES) {
            throw new RegraNegocioException("O arquivo passa do limite de 2 MB.");
        }
        try {
            return importacaoService.enviar(arquivo.getBytes(), usuario);
        } catch (IOException e) {
            throw new RegraNegocioException(NfeParser.MENSAGEM_INVALIDA);
        }
    }

    private void carregarRevisao(Model model, RevisaoNfe revisao, Usuario usuario, ConfirmacaoNfeForm form) {
        model.addAttribute("revisao", revisao);
        model.addAttribute("token", revisao.token().toString());
        model.addAttribute("categorias", categoriaService.listarParaSelecao(usuario.getCodigo()));
        model.addAttribute("produtos", produtoService.listarParaSelecao(usuario.getCodigo()));
        model.addAttribute("confirmacaoNfeForm", form);
    }

    /** Token que não é um UUID responde "não encontrado", igual a um token de outra loja. */
    private static UUID interpretar(String token) {
        try {
            return UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new RecursoNaoEncontradoException("Importação não encontrada.");
        }
    }

    private static long contar(List<NotaFiscalItem> itens, SituacaoItemNfe situacao) {
        return itens.stream().filter(i -> i.getSituacao() == situacao).count();
    }

    /**
     * Aplica os limites de abuso (IP, global, loja) e reserva uma vaga de processamento. Devolve a resposta 429 se
     * algum falhar, ou null se passou (e então o chamador DEVE liberar a vaga no finally).
     */
    private ResponseEntity<Map<String, Object>> barrar(Usuario usuario, HttpServletRequest request, boolean envio) {
        boolean passou = limitador.tentarIp(request.getRemoteAddr())
                && limitador.tentarGlobal()
                && (envio ? limitador.tentar(usuario.getCodigo()) : limitador.tentarRevisao(usuario.getCodigo()));
        if (!passou) {
            return limiteExcedido(usuario, "Muitas requisições em pouco tempo. Aguarde alguns minutos e tente de novo.");
        }
        if (!limitador.entrarProcessamento()) {
            return limiteExcedido(usuario, "O servidor está ocupado lendo outras notas. Tente de novo em instantes.");
        }
        return null;
    }

    /**
     * Páginas navegadas pelo usuário (revisão e confirmação) não devem mostrar JSON cru no 429: voltam à lista com a
     * mensagem. O Retry-After se perde, mas o texto já diz para aguardar.
     */
    private static Object paraNavegacao(ResponseEntity<Map<String, Object>> barrado, RedirectAttributes redirect) {
        Object erro = barrado.getBody() == null ? null : barrado.getBody().get("erro");
        redirect.addFlashAttribute("erro", erro == null ? "Muitas requisições em pouco tempo." : erro.toString());
        return "redirect:" + LISTA;
    }

    private ResponseEntity<Map<String, Object>> limiteExcedido(Usuario usuario, String mensagem) {
        long espera = limitador.segundosParaLiberar(usuario.getCodigo());
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("ok", false);
        corpo.put("erro", mensagem);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(espera))
                .contentType(MediaType.APPLICATION_JSON).body(corpo);
    }

    private static ResponseEntity<Map<String, Object>> erroJson(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("ok", false);
        corpo.put("erro", mensagem);
        return json(status, corpo);
    }

    private static ResponseEntity<Map<String, Object>> json(HttpStatus status, Map<String, Object> corpo) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(corpo);
    }
}
