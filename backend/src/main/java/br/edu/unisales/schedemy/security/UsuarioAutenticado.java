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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Acesso ao usuario autenticado na requisicao e verificacoes de "dono" do recurso.
 *
 * A identidade usada nas regras de autorizacao SEMPRE vem do login (token de acesso),
 * nunca de ids enviados no corpo da requisicao (RNF 15).
 */
@Component
@RequiredArgsConstructor
public class UsuarioAutenticado {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_RECEPCIONISTA = "ROLE_RECEPCIONISTA";
    private static final String ATRIBUTO_USUARIO = UsuarioAutenticado.class.getName() + ".usuario";

    private final UsuarioRepository usuarioRepository;

    private Authentication autenticacao() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AcessoNegadoNegocioException("Usuario nao autenticado.");
        }
        return auth;
    }

    /**
     * Usuario dono da credencial usada na requisicao.
     *
     * Fica guardado na propria requisicao HTTP: as varias verificacoes de uma mesma chamada
     * (id(), exigirProprioOuAdmin, exigirParticipante...) consultam o banco uma vez so.
     * Fora de uma requisicao HTTP (ex.: testes), consulta sempre.
     */
    public Usuario obter() {
        String email = autenticacao().getName();
        RequestAttributes requisicao = RequestContextHolder.getRequestAttributes();
        if (requisicao != null
                && requisicao.getAttribute(ATRIBUTO_USUARIO, RequestAttributes.SCOPE_REQUEST) instanceof Usuario guardado
                && email.equals(guardado.getEmail())) {
            return guardado;
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new AcessoNegadoNegocioException("Usuario autenticado nao encontrado."));
        if (requisicao != null) {
            requisicao.setAttribute(ATRIBUTO_USUARIO, usuario, RequestAttributes.SCOPE_REQUEST);
        }
        return usuario;
    }

    /** Chamado pelo ConversorDeToken, que ja leu o cadastro ao validar o token. */
    static void guardarNaRequisicao(Usuario usuario) {
        RequestAttributes requisicao = RequestContextHolder.getRequestAttributes();
        if (requisicao != null) {
            requisicao.setAttribute(ATRIBUTO_USUARIO, usuario, RequestAttributes.SCOPE_REQUEST);
        }
    }

    public Long id() {
        return obter().getId();
    }

    public boolean ehAdmin() {
        return temPapel(ROLE_ADMIN);
    }

    /**
     * RF 13, RF 20, RF 21 - a recepcao (e a administracao) agenda, altera e cancela reunioes
     * de terceiros, sem participar delas (RN 03).
     */
    public boolean podeGerirAgendamentosDeTerceiros() {
        return temPapel(ROLE_ADMIN) || temPapel(ROLE_RECEPCIONISTA);
    }

    private boolean temPapel(String papel) {
        return autenticacao().getAuthorities().stream()
                .anyMatch(a -> papel.equals(a.getAuthority()));
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
        exigirParticipante(agendamento);
    }

    /** RNF 15 com a excecao da recepcao, que acompanha todos os agendamentos (RF 20, RF 21). */
    public void exigirParticipanteOuGestor(Agendamento agendamento) {
        if (podeGerirAgendamentosDeTerceiros()) {
            return;
        }
        exigirParticipante(agendamento);
    }

    private void exigirParticipante(Agendamento agendamento) {
        Long logado = id();
        boolean envolvido = agendamento.getOrganizador().getId().equals(logado)
                || agendamento.getParticipantes().stream()
                .anyMatch(p -> p.getUsuario().getId().equals(logado));
        if (!envolvido) {
            throw new AcessoNegadoNegocioException("Voce nao participa deste agendamento.");
        }
    }
}