package br.edu.unisales.schedemy.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Faz o 401 sair no mesmo formato (ErrorResponseDTO) das demais respostas de erro (RNF 17).
 *
 * Substitui o entry point padrao, que responde sem corpo e so com o cabecalho WWW-Authenticate
 * (no Basic Auth, esse cabecalho fazia o navegador abrir a janela nativa de login).
 * Mesmo esquema do AcessoNegadoJsonHandler: delega ao GlobalExceptionHandler.
 */
@Component
public class NaoAutenticadoJsonEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public NaoAutenticadoJsonEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {
        resolver.resolveException(request, response, null, ex);
    }
}
