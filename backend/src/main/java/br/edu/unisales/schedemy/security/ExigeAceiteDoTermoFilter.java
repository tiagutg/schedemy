package br.edu.unisales.schedemy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * RNF 18 - quem ainda nao aceitou a versao em vigor do termo so le o proprio cadastro, le o termo e aceita.
 * O ConversorDeToken marca a autenticacao com a autoridade TERMO_PENDENTE; aqui a requisicao e recusada com
 * TermoPendenteException, que o ExceptionTranslationFilter entrega ao AcessoNegadoJsonHandler (403 em JSON).
 *
 * Nao e @Component de proposito: so existe dentro da cadeia do Spring Security (SecurityConfig), e nao como filtro
 * avulso do servlet.
 */
public class ExigeAceiteDoTermoFilter extends OncePerRequestFilter {

    public static final String TERMO_PENDENTE = "TERMO_PENDENTE";

    /** "METODO caminho" liberados mesmo com o termo pendente. */
    private static final Set<String> LIBERADOS = Set.of(
            "GET /api/v1/usuarios/me",
            "GET /api/v1/termos",
            "GET /api/v1/consentimentos/meu",
            "POST /api/v1/consentimentos");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean pendente = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> TERMO_PENDENTE.equals(a.getAuthority()));
        if (pendente && !LIBERADOS.contains(request.getMethod() + " " + caminho(request))) {
            throw new TermoPendenteException();
        }
        chain.doFilter(request, response);
    }

    private static String caminho(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        String semContexto = contexto != null && !contexto.isEmpty() && uri.startsWith(contexto)
                ? uri.substring(contexto.length()) : uri;
        return semContexto.length() > 1 && semContexto.endsWith("/")
                ? semContexto.substring(0, semContexto.length() - 1) : semContexto;
    }
}
