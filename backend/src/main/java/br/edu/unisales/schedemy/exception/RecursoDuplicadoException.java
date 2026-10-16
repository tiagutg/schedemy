package br.edu.unisales.schedemy.exception;

/**
 * Lancada ao tentar violar uma restricao de unicidade do dominio
 * (ex.: e-mail ja cadastrado). Traduzida para HTTP 409 (Conflict).
 */
public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
