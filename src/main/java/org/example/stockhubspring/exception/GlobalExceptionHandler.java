package org.example.stockhubspring.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Converte exceções em respostas amigáveis, sem stack trace:
 * regra de negócio vira mensagem (flash "erro") na página de origem; recurso inexistente vira 404.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Só caminhos simples: evita {variáveis} de template e redirecionamento para outro site.
    private static final Pattern CAMINHO_SEGURO = Pattern.compile("^/[A-Za-z0-9/_.-]*$");
    private static final String DESTINO_PADRAO = "/dashboard";

    @ExceptionHandler(RegraNegocioException.class)
    public String regraNegocio(RegraNegocioException ex, HttpServletRequest request, RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", ex.getMessage());
        return "redirect:" + destinoDeRetorno(request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public void naoEncontrado(RecursoNaoEncontradoException ex, HttpServletResponse response) throws IOException {
        // O /error do Spring Boot monta a página (sem stack trace, ver server.error.* nas configs).
        response.sendError(HttpServletResponse.SC_NOT_FOUND, ex.getMessage());
    }

    /** Bloqueio/deadlock de linha (ex.: duas confirmações de NF-e mexendo nos mesmos produtos): pedir para tentar de novo. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public String bloqueio(PessimisticLockingFailureException ex, HttpServletRequest request, RedirectAttributes redirect) {
        LOG.warn("Falha de bloqueio em {}", request.getRequestURI());
        redirect.addFlashAttribute("erro", "Outra operação está mexendo nos mesmos dados agora. Tente de novo em instantes.");
        return "redirect:" + destinoDeRetorno(request);
    }

    /** Última defesa: restrição do banco violada (ex.: nome duplicado numa corrida entre dois usuários). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integridade(DataIntegrityViolationException ex, HttpServletRequest request, RedirectAttributes redirect) {
        LOG.warn("Violação de integridade em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        redirect.addFlashAttribute("erro",
                "Não foi possível concluir a operação: o registro entra em conflito com outro já existente ou em uso.");
        return "redirect:" + destinoDeRetorno(request);
    }

    /**
     * Arquivo maior que o limite do upload (spring.servlet.multipart.max-file-size): mensagem amigável.
     * Pedido por fetch (X-Requested-With) recebe JSON 413; formulário comum volta com a mensagem (flash).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object uploadGrande(HttpServletRequest request, RedirectAttributes redirect) {
        return erroDeUpload(request, redirect, HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo passa do limite de 2 MB.");
    }

    /** Envio sem o campo do arquivo. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public Object semArquivo(HttpServletRequest request, RedirectAttributes redirect) {
        return erroDeUpload(request, redirect, HttpStatus.BAD_REQUEST, "Selecione o arquivo XML da NF-e.");
    }

    private Object erroDeUpload(HttpServletRequest request, RedirectAttributes redirect, HttpStatus status,
                                String mensagem) {
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("ok", false);
            corpo.put("erro", mensagem);
            return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(corpo);
        }
        redirect.addFlashAttribute("erro", mensagem);
        return "redirect:" + destinoDeRetorno(request);
    }

    /** Volta para a página de onde veio o pedido (Referer do mesmo site); senão, para o painel. */
    private String destinoDeRetorno(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return DESTINO_PADRAO;
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return DESTINO_PADRAO;
            }
            String caminho = uri.getPath();
            String contexto = request.getContextPath();
            if (caminho != null && !contexto.isEmpty() && caminho.startsWith(contexto)) {
                caminho = caminho.substring(contexto.length());
            }
            if (caminho == null || caminho.startsWith("//") || !CAMINHO_SEGURO.matcher(caminho).matches()) {
                return DESTINO_PADRAO;
            }
            return caminho;
        } catch (IllegalArgumentException e) {
            return DESTINO_PADRAO;
        }
    }
}
