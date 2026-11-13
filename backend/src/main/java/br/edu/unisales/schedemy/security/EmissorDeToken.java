package br.edu.unisales.schedemy.security;

import br.edu.unisales.schedemy.config.JwtConfig;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;

/**
 * Emite o token de acesso (JWT) depois que o login confere e-mail e senha.
 *
 * Claims: sub = id do usuario, iss = "schedemy", iat/exp e "sv" (versao da senha).
 * O token nao leva papel nem dados pessoais: o ConversorDeToken le o cadastro a cada requisicao.
 */
@Component
public class EmissorDeToken {

    static final String CLAIM_VERSAO_SENHA = "sv";

    private final JwtEncoder encoder;
    private final Duration validade;
    private final Clock relogio;

    @Autowired
    public EmissorDeToken(JwtEncoder encoder, @Value("${schedemy.jwt.validade-minutos:480}") long validadeMinutos) {
        this(encoder, Duration.ofMinutes(validadeMinutos), Clock.systemUTC());
    }

    EmissorDeToken(JwtEncoder encoder, Duration validade, Clock relogio) {
        this.encoder = encoder;
        this.validade = validade;
        this.relogio = relogio;
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = relogio.instant().truncatedTo(ChronoUnit.SECONDS); // o JWT guarda segundos
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(CLAIM_VERSAO_SENHA, versaoDaSenha(usuario))
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }

    /**
     * Resumo curto do hash BCrypt da senha: muda quando a senha muda, sem expor o hash no token.
     * Ate o SSO (RF 29), usuario sem senha nao chega a receber token.
     */
    static String versaoDaSenha(Usuario usuario) {
        String hash = usuario.getSenha() == null ? "" : usuario.getSenha();
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(resumo, 12));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }
}
