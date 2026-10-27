package org.example.stockhubspring.controller;

import org.example.stockhubspring.exception.RecursoNaoEncontradoException;
import org.example.stockhubspring.exception.RegraNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Erros dos endpoints JSON: sempre {"ok":false,"erro":"..."}, sem redirect, sem HTML e sem
 * stack trace. Vale só para estes controllers e tem prioridade sobre o GlobalExceptionHandler (das telas).
 */
@RestControllerAdvice(assignableTypes = {ApiBuscaController.class, ApiDashboardController.class,
        EstoqueJsonController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExcecoes {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExcecoes.class);

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraNegocio(RegraNegocioException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridade(DataIntegrityViolationException ex) {
        LOG.warn("Violação de integridade na API: {}", ex.getMostSpecificCause().getMessage());
        return erro(HttpStatus.CONFLICT,
                "Não foi possível concluir a operação: o registro entra em conflito com outro já existente ou em uso.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> bloqueio(PessimisticLockingFailureException ex) {
        return erro(HttpStatus.CONFLICT, "Outra operação está mexendo nos mesmos dados agora. Tente de novo em instantes.");
    }

    /** Última defesa: nunca devolve detalhes internos. Exceções do próprio Spring MVC mantêm o status delas. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception ex) {
        if (ex instanceof ErrorResponse resposta) {
            HttpStatusCode status = resposta.getStatusCode();
            return erro(status, status.is4xxClientError() ? "Requisição inválida." : "Erro interno. Tente novamente.");
        }
        LOG.error("Erro inesperado na API", ex);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente em instantes.");
    }

    private static ResponseEntity<Map<String, Object>> erro(HttpStatusCode status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("ok", false);
        corpo.put("erro", mensagem);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(corpo);
    }
}
