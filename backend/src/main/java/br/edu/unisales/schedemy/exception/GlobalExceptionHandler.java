package br.edu.unisales.schedemy.exception;

import br.edu.unisales.schedemy.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    /** RNF 17 - falha de autenticacao com mensagem clara (chega aqui pelo NaoAutenticadoJsonEntryPoint). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> tratarNaoAutenticado(AuthenticationException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.UNAUTHORIZED, "Nao autenticado",
                "E-mail ou senha invalidos, ou sessao expirada. Entre novamente.", request);
    }

    /** Corpo ausente, JSON malformado ou valor invalido (ex.: enum desconhecido, data em formato errado). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> tratarCorpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "O corpo da requisicao esta ausente ou tem um formato/valor invalido.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> tratarRotaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.NOT_FOUND, "Recurso nao encontrado",
                "O endereco requisitado nao existe.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex,
                                                                     HttpServletRequest request) {
        return construirResposta(HttpStatus.METHOD_NOT_ALLOWED, "Metodo nao permitido",
                "O metodo %s nao e suportado neste endereco.".formatted(ex.getMethod()), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> tratarMidiaNaoSuportada(HttpMediaTypeNotSupportedException ex,
                                                                    HttpServletRequest request) {
        return construirResposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteudo nao suportado",
                "Envie o corpo como application/json.", request);
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

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponseDTO> tratarParametroInvalido(Exception ex, HttpServletRequest request) {
        String parametro = ex instanceof MissingServletRequestParameterException faltando
                ? faltando.getParameterName()
                : ((MethodArgumentTypeMismatchException) ex).getName();
        return construirResposta(HttpStatus.BAD_REQUEST, "Requisicao invalida",
                "Parametro '%s' ausente ou invalido.".formatted(parametro), request);
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