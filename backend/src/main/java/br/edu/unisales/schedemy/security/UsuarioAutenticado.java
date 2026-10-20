package br.edu.unisales.schedemy.security;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.exception.AcessoNegadoNegocioException;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acesso ao usuario autenticado na requisicao e verificacoes de "dono" do recurso.
 *
 * A identidade usada nas regras de autorizacao SEMPRE vem do login (Basic Auth),
 * nunca de ids enviados no corpo da requisicao (RNF 15).
 */
@Component
@RequiredArgsConstructor
public class UsuarioAutenticado {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UsuarioRepository usuarioRepository;

    private Authentication autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AcessoNegadoNegocioException("Usuario nao autenticado.");
        }
        return auth;
    }

    /** Usuario dono da credencial usada na requisicao. */
    public Usuario obter() {
        String email = autenticacao().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AcessoNegadoNegocioException("Usuario autenticado nao encontrado."));
    }

    public Long id() {
        return obter().getId();
    }

    public boolean ehAdmin() {
        return autenticacao().getAuthorities().stream()
                .anyMatch(a -> ROLE_ADMIN.equals(a.getAuthority()));
    }

    /** Permite a operacao apenas se o id informado for o do usuario logado (ou se for ADMIN). */
    public void exigirProprioOuAdmin(Long idUsuario, String mensagem) {
        if (ehAdmin()) {
            return;
        }
        if (idUsuario == null || !id().equals(idUsuario)) {
            throw new AcessoNegadoNegocioException(mensagem);
        }
    }

    /**
     * Resolve o filtro de usuario de uma listagem: ADMIN usa o que pediu (inclusive "todos");
     * os demais so enxergam os proprios registros.
     */
    public Long escopoDoUsuario(Long idSolicitado) {
        if (ehAdmin()) {
            return idSolicitado;
        }
        Long logado = id();
        if (idSolicitado != null && !logado.equals(idSolicitado)) {
            throw new AcessoNegadoNegocioException("Voce so pode consultar os seus proprios registros.");
        }
        return logado;
    }

    /** RNF 15 - so o organizador, os participantes (ou ADMIN) acessam o agendamento. */
    public void exigirParticipanteOuAdmin(Agendamento agendamento) {
        if (ehAdmin()) {
            return;
        }
        Long logado = id();
        boolean envolvido = agendamento.getOrganizador().getId().equals(logado)
                || agendamento.getParticipantes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(logado));
        if (!envolvido) {
            throw new AcessoNegadoNegocioException("Voce nao participa deste agendamento.");
        }
    }
}