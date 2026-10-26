package org.example.stockhubspring.exception;

/**
 * Violação de uma regra de negócio (ex.: saída maior que o saldo). A mensagem é amigável
 * e pode ser mostrada ao usuário.
 */
public class RegraNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
