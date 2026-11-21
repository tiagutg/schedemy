package br.edu.unisales.schedemy.security;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.repository.ConsentimentoTermoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.impl.TermoDePrivacidade;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforma o token validado em usuario autenticado, consultando o cadastro a cada requisicao.
 *
 * O papel vem do banco, nao do token: desativar o usuario, trocar o perfil ou a senha vale na
 * hora, sem esperar o token expirar (o Basic Auth tambem consultava o banco em toda chamada).
 * O usuario lido fica guardado na requisicao para o UsuarioAutenticado nao consultar de novo.
 */
@Component
@RequiredArgsConstructor
public class ConversorDeToken implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String SESSAO_INVALIDA = "Sessao expirada ou invalida. Entre novamente.";

    private final UsuarioRepository usuarioRepository;
    private final ConsentimentoTermoRepository consentimentoTermoRepository;
    private final TermoDePrivacidade termoDePrivacidade;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id = idDoToken(jwt);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new InvalidBearerTokenException(SESSAO_INVALIDA));
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new InvalidBearerTokenException(SESSAO_INVALIDA);
        }
        // Senha trocada (pelo admin, por exemplo) derruba as sessoes abertas com a senha antiga.
        if (!EmissorDeToken.versaoDaSenha(usuario).equals(jwt.getClaimAsString(EmissorDeToken.CLAIM_VERSAO_SENHA))) {
            throw new InvalidBearerTokenException(SESSAO_INVALIDA);
        }

        UsuarioAutenticado.guardarNaRequisicao(usuario);
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + usuario.getTipoUsuario().name()));
        // RNF 18 - sem o aceite da versao em vigor do termo, o ExigeAceiteDoTermoFilter barra quase tudo.
        if (!consentimentoTermoRepository.existsByUsuarioIdAndVersaoTermo(usuario.getId(), termoDePrivacidade.versao())) {
            autoridades.add(new SimpleGrantedAuthority(ExigeAceiteDoTermoFilter.TERMO_PENDENTE));
        }
        // O nome da autenticacao e o e-mail, como era no Basic Auth (UsuarioAutenticado.obter).
        return new JwtAuthenticationToken(jwt, autoridades, usuario.getEmail());
    }

    private static Long idDoToken(Jwt jwt) {
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InvalidBearerTokenException(SESSAO_INVALIDA);
        }
    }
}
