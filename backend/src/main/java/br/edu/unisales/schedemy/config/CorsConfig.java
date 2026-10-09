package br.edu.unisales.schedemy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**Libera o consumo da API por aplicacoes front-end externas,
 * ja preparando o backend para a proxima etapa do projeto.
 * As origens permitidas sao configuraveis via variavel de ambiente CORS_ALLOWED_ORIGINS.
 * Em desenvolvimento, libera as portas padrao de Vite/React/Angular/Vue.
 **/
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String origensEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        List<String> origens = origensEnv != null && !origensEnv.isBlank()
                ? List.of(origensEnv.split(","))
                : List.of("http://localhost:3000", "http://localhost:5173", "http://localhost:4200", "http://127.0.0.1:3000", "http://127.0.0.1:5173");

        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origens);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }
}