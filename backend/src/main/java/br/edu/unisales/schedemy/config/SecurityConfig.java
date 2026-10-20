package br.edu.unisales.schedemy.config;

import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Autenticacao (Basic Auth) e autorizacao por papel da Schedemy API.
 *
 * As regras abaixo espelham a matriz de acesso do Docume   nto de Definicao de
 * Requisitos v3.1.0 (secao 5.3 "Relacao Entre Atores e Casos de Uso", RN 03 e
 * RN 06), a mesma aplicada no front-end em src/utils/permissoes.js.
 *
 * Resumo:
 * - ALUNO          agendamentos (criar/ver/cancelar/responder) e avaliacoes
 * - PROFESSOR      o mesmo do coordenador (a secao 5.3 trata os dois como um ator)
 * - COORDENADOR    + disponibilidades, bloqueios, duracoes e notificacoes
 * - RECEPCIONISTA  agendamentos, sem aceitar/recusar convite (RN 03)
 * - ADMIN          acesso total
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String ADMIN = "ADMIN";
    private static final String COORDENADOR = "COORDENADOR";
    private static final String PROFESSOR = "PROFESSOR";
    private static final String RECEPCIONISTA = "RECEPCIONISTA";
    private static final String ALUNO = "ALUNO";

    /** Perfis que gerenciam agenda e parametros academicos (RN 06, RN 17). */
    private static final String[] DOCENTES = { PROFESSOR, COORDENADOR, ADMIN };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Documentacao OpenAPI/Swagger e Health livre
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // --- Usuarios ---
                        // A LEITURA fica aberta a qualquer autenticado de proposito: o login
                        // valida a credencial consultando este endpoint e a escolha de
                        // convidados depende dele (RF 14 e RF 33). So a gestao e restrita.
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/**").authenticated()
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole(ADMIN)

                        // --- Duracoes de reuniao (RF 01, RN 17) ---
                        // Todos consultam para montar o agendamento; so a docencia altera.
                        .requestMatchers(HttpMethod.GET, "/api/v1/duracoes-reuniao", "/api/v1/duracoes-reuniao/**").authenticated()
                        .requestMatchers("/api/v1/duracoes-reuniao", "/api/v1/duracoes-reuniao/**").hasAnyRole(DOCENTES)

                        // --- Disponibilidades (RF 01-03, RF 12 e RN 06) ---
                        // RN 06 restringe quem DEFINE os horarios; a consulta e necessaria
                        // para exibir os intervalos livres dos convidados (RF 14).
                        .requestMatchers(HttpMethod.GET, "/api/v1/disponibilidades", "/api/v1/disponibilidades/**").authenticated()
                        .requestMatchers("/api/v1/disponibilidades", "/api/v1/disponibilidades/**").hasAnyRole(DOCENTES)

                        // --- Bloqueios de periodo (RF 04-07) ---
                        // Gestao de agenda propria da docencia, inclusive a consulta.
                        .requestMatchers("/api/v1/bloqueios-periodo", "/api/v1/bloqueios-periodo/**").hasAnyRole(DOCENTES)

                        // --- Agendamentos ---
                        // RN 03: a recepcao nao e convidada, entao nao aceita/recusa convites.
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/agendamentos/*/resposta")
                        .hasAnyRole(ALUNO, PROFESSOR, COORDENADOR, ADMIN)
                        // Remocao definitiva nao consta nos casos de uso: so administracao.
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/agendamentos/**").hasRole(ADMIN)
                        // Listagem global: visao administrativa. Os demais perfis usam
                        // /agendamentos/usuarios/{id} (somente o proprio id - RNF 15).
                        .requestMatchers(HttpMethod.GET, "/api/v1/agendamentos").hasRole(ADMIN)
                        // Criar, consultar, editar e cancelar: RF 13, 15-18, 20, 21.
                        // A posse do recurso e verificada nos services (UsuarioAutenticado.java).
                        .requestMatchers("/api/v1/agendamentos", "/api/v1/agendamentos/**").authenticated()

                        // --- Avaliacoes (RF 24) ---
                        // Somente o aluno avalia; a docencia acompanha (RNF 11).
                        .requestMatchers(HttpMethod.POST, "/api/v1/avaliacoes").hasAnyRole(ALUNO, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/avaliacoes", "/api/v1/avaliacoes/**")
                        .hasAnyRole(ALUNO, PROFESSOR, COORDENADOR, ADMIN)
                        .requestMatchers("/api/v1/avaliacoes", "/api/v1/avaliacoes/**").hasRole(ADMIN)

                        // --- Notificacoes (RF 23) ---
                        // O envio e do ator Sistema; a tela e um log operacional.
                        .requestMatchers(HttpMethod.GET, "/api/v1/notificacoes", "/api/v1/notificacoes/**").hasAnyRole(DOCENTES)
                        .requestMatchers("/api/v1/notificacoes", "/api/v1/notificacoes/**").hasRole(ADMIN)

                        // --- Registros de motivo (RF 11) ---
                        // Acompanham cancelamento, edicao e remarcacao, feitos por todos os perfis.
                        .requestMatchers("/api/v1/registros-motivo", "/api/v1/registros-motivo/**").authenticated()

                        // Qualquer outro endpoint da API exige ao menos autenticacao
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
