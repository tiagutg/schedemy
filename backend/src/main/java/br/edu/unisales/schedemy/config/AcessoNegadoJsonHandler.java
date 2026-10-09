package br.edu.unisales.schedemy.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Faz o 403 vindo das regras do filtro de seguranca sair no mesmo formato
 * (ErrorResponseDTO) das demais respostas de erro da API.
 *
 * A negacao acontece antes do DispatcherServlet, entao o @RestControllerAdvice
 * nao seria acionado e a resposta iria sem corpo. Em vez de montar o JSON aqui,
 * delegamos ao resolver do MVC, que cai no GlobalExceptionHandler ja existente
 * (tratarAcessoNegadoSpring) e mantem a mensagem em um lugar so (RNF 17).
 */
@Component
public class AcessoNegadoJsonHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public AcessoNegadoJsonHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
