package br.edu.unisales.schedemy.exception;

/**
 * Lancada quando uma operacao viola uma regra de negocio do dominio.
 * Traduzida pelo GlobalExceptionHandler para HTTP 422 (Unprocessable Entity).
 */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
