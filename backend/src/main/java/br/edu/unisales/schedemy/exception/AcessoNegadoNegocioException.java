package br.edu.unisales.schedemy.exception;

// Lancada quando uma regra de autorizacao de negocio impede a operacao (HTTP 403).
public class AcessoNegadoNegocioException extends RuntimeException {
    public AcessoNegadoNegocioException(String mensagem) {
        super(mensagem);
    }
}