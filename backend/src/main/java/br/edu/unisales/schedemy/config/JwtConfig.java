package br.edu.unisales.schedemy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Chave e validacao do token de acesso emitido pela propria API (HS256).
 *
 * Passo intermediario ate o login com a conta Microsoft (RNF 16, RF 31): quando o Entra ID
 * existir, basta trocar este decoder pelo do emissor da Microsoft (issuer-uri) e resolver o
 * usuario pelo oid no ConversorDeToken; o restante do SecurityConfig continua igual.
 */
@Slf4j
@Configuration
public class JwtConfig {

    /** Valor da claim "iss" dos tokens desta API. */
    public static final String EMISSOR = "schedemy";

    /** HS256 exige chave de pelo menos 256 bits. */
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    private final SecretKey chave;

    public JwtConfig(@Value("${schedemy.jwt.segredo:}") String segredo) {
        this.chave = new SecretKeySpec(bytesDoSegredo(segredo), "HmacSHA256");
    }

    private static byte[] bytesDoSegredo(String segredo) {
        if (segredo == null || segredo.isBlank()) {
            log.warn("SCHEDEMY_JWT_SEGREDO nao definido: usando um segredo sorteado nesta inicializacao. "
                    + "Os tokens deixam de valer quando a API reiniciar. Defina a variavel em producao.");
            byte[] sorteado = new byte[TAMANHO_MINIMO_SEGREDO];
            new SecureRandom().nextBytes(sorteado);
            return sorteado;
        }
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException(
                    "SCHEDEMY_JWT_SEGREDO precisa ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        return bytes;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(chave).algorithm(MacAlgorithm.HS256).build();
    }

    /** Confere assinatura, validade (exp/nbf, com 60 s de tolerancia) e o emissor. */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }
}
