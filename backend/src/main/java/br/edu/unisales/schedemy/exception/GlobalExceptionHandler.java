package br.edu.unisales.schedemy.exception;

import br.edu.unisales.schedemy.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento centralizado de excecoes da API. Converte excecoes de dominio e do framework
 * em respostas HTTP padronizadas (ErrorResponseDTO), evitando vazamento de stacktraces.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> tratarNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> tratarRegraDeNegocio(RegraDeNegocioException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negocio violada", ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorResponseDTO> tratarDuplicado(RecursoDuplicadoException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage(), request);
    }

    @ExceptionHandler(AcessoNegadoNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> tratarAcessoNegadoNegocio(AcessoNegadoNegocioException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> tratarAcessoNegadoSpring(AccessDeniedException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.FORBIDDEN, "Acesso negado",
                "Voce nao tem permissao para executar esta operacao.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> tratarIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Violacao de integridade no banco de dados na rota {}: ", request.getRequestURI(), ex);
        return construirResposta(HttpStatus.CONFLICT, "Violacao de integridade de dados",
                "A operacao viola uma restricao de integridade do banco de dados (ex.: chave unica ou vinculo existente).", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.put(erro.getField(), erro.getDefaultMessage());
        }
        ErrorResponseDTO corpo = ErrorResponseDTO.comCampos(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validacao",
                "Um ou mais campos sao invalidos.",
                request.getRequestURI(),
                erros
        );
        return ResponseEntity.badRequest().body(corpo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> tratarIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Requisicao invalida", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> tratarGenerico(Exception ex, HttpServletRequest request) {
        // Imprime a causa exata e a stacktrace no console do IntelliJ
        log.error("Erro inesperado nao tratado na rota {}: ", request.getRequestURI(), ex);
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado ao processar a requisicao.", request);
    }

    private ResponseEntity<ErrorResponseDTO> construirResposta(HttpStatus status, String erro, String mensagem, HttpServletRequest request) {
        ErrorResponseDTO corpo = ErrorResponseDTO.simples(status.value(), erro, mensagem, request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}