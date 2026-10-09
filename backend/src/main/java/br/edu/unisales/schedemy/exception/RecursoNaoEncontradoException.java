package br.edu.unisales.schedemy.exception;

/**
 * Lancada quando uma entidade solicitada (por id) nao existe na base.
 * Traduzida pelo GlobalExceptionHandler para HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException paraId(String entidade, Object id) {
        return new RecursoNaoEncontradoException(entidade + " nao encontrado(a) com id: " + id);
    }

    public static RecursoNaoEncontradoException paraEntidade(String entidade, Long id) {
        return new RecursoNaoEncontradoException(entidade + " nao encontrado(a) com id: " + id);
    }
}