package org.example.stockhubspring.exception;

/**
 * Registro inexistente ou de outra loja (RN-01): os dois casos respondem igual, para não
 * revelar que o registro existe.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
