package br.edu.unisales.schedemy.config;

import static org.springframework.security.config.Customizer.withDefaults;
import br.edu.unisales.schedemy.security.ConversorDeToken;
import br.edu.unisales.schedemy.security.ExigeAceiteDoTermoFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.ExceptionTranslationFilter;

/**
 * Autenticacao (token de acesso JWT, emitido em POST /api/v1/auth/login) e autorizacao por
 * papel da Schedemy API. O token e validado pelo JwtConfig e convertido no usuario pelo
 * ConversorDeToken (papel lido do banco a cada requisicao).
 *
 * As regras abaixo seguem o Documento de Definicao de Requisitos v3.1.0 (atores
 * citados nos RFs, RN 03, RN 06 e RNF 15) e sao as mesmas aplicadas no front-end
 * em src/utils/permissoes.ts.
 *
 * Resumo:
 * - TODOS          os proprios avisos (notificacoes do sistema)
 * - ALUNO          agendamentos (criar/ver/cancelar/responder) e avaliacoes
 * - PROFESSOR      o mesmo do coordenador (os RFs sempre citam "professor ou coordenador")
 * - COORDENADOR    + disponibilidades, bloqueios e notificacoes
 * - RECEPCIONISTA  agendamentos de todos (criar por terceiros, ver, editar, cancelar),
 *                  sem aceitar/recusar convite (RN 03)
 * - ADMIN          acesso total (inclusive as duracoes de reuniao, definidas pela instituicao - RN 17)
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
    public SecurityFilterChain filterChain(HttpSecurity http, AccessDeniedHandler accessDeniedHandler,
                                           AuthenticationEntryPoint naoAutenticado,
                                           ConversorDeToken conversorDeToken) throws Exception {
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
                        // Login: troca e-mail e senha pelo token de acesso.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        // --- LGPD (RNF 18) ---
                        // O termo e a politica sao publicos (/privacidade); cada um ve e da o proprio aceite.
                        // Sem o aceite da versao em vigor, o ExigeAceiteDoTermoFilter barra as demais rotas.
                        .requestMatchers(HttpMethod.GET, "/api/v1/termos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/consentimentos/meu").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/consentimentos").authenticated()
                        .requestMatchers("/api/v1/consentimentos", "/api/v1/consentimentos/**").hasRole(ADMIN)

                        // --- Usuarios ---
                        // Todos leem o proprio cadastro (login) e o resumo para escolher convidados
                        // (RF 14 e RF 33). O cadastro completo (e-mail, telefone) fica com a
                        // administracao e a recepcao, que agenda por terceiros (RNF 18).
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/me", "/api/v1/usuarios/resumo").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/**").hasAnyRole(RECEPCIONISTA, ADMIN)
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole(ADMIN)

                        // --- Duracoes de reuniao (RF 01, RN 17) ---
                        // Todos consultam para montar o agendamento. A lista vale para o sistema inteiro e e
                        // "definida pela instituicao" (RN 17; RF 01 fala em duracoes "disponibilizadas pelo
                        // sistema"): so a administracao altera.
                        .requestMatchers(HttpMethod.GET, "/api/v1/duracoes-reuniao", "/api/v1/duracoes-reuniao/**").authenticated()
                        .requestMatchers("/api/v1/duracoes-reuniao", "/api/v1/duracoes-reuniao/**").hasRole(ADMIN)

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
                        // RF 21: o aluno (ou a recepcao por ele) pede a alteracao de reuniao confirmada.
                        // A consulta dos pedidos (GET) cai na regra geral de agendamentos.
                        .requestMatchers(HttpMethod.POST, "/api/v1/agendamentos/*/solicitacoes-edicao")
                        .hasAnyRole(ALUNO, RECEPCIONISTA, ADMIN)
                        // Remocao definitiva nao consta nos casos de uso: so administracao.
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/agendamentos/**").hasRole(ADMIN)
                        // Listagem global: a recepcao agenda, altera e cancela por terceiros (RF 13, 20, 21).
                        // Os demais perfis usam /agendamentos/usuarios/{id} (somente o proprio id - RNF 15).
                        .requestMatchers(HttpMethod.GET, "/api/v1/agendamentos").hasAnyRole(RECEPCIONISTA, ADMIN)
                        // Criar, consultar, editar e cancelar: RF 13, 15-18, 20, 21. Inclui
                        // GET /agendamentos/horarios-livres (RF 14), que nao expoe a agenda de ninguem.
                        // A posse do recurso e verificada nos services (UsuarioAutenticado.java).
                        .requestMatchers("/api/v1/agendamentos", "/api/v1/agendamentos/**").authenticated()

                        // --- Pedidos de alteracao (RF 21/22, RN 12) ---
                        // So professores e coordenadores decidem; quem pediu (ou a recepcao) desiste.
                        // A lista (GET) e de todos os perfis: o escopo pelo logado e aplicado no service.
                        .requestMatchers(HttpMethod.GET, "/api/v1/solicitacoes-edicao").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/solicitacoes-edicao/*/resposta")
                        .hasAnyRole(PROFESSOR, COORDENADOR, ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/solicitacoes-edicao/*/cancelamento")
                        .hasAnyRole(ALUNO, RECEPCIONISTA, ADMIN)
                        .requestMatchers("/api/v1/solicitacoes-edicao", "/api/v1/solicitacoes-edicao/**").hasRole(ADMIN)

                        // --- Avaliacoes (RF 24) ---
                        // Somente o aluno avalia; a docencia acompanha (RNF 11).
                        .requestMatchers(HttpMethod.POST, "/api/v1/avaliacoes").hasAnyRole(ALUNO, ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/v1/avaliacoes", "/api/v1/avaliacoes/**")
                        .hasAnyRole(ALUNO, PROFESSOR, COORDENADOR, ADMIN)
                        .requestMatchers("/api/v1/avaliacoes", "/api/v1/avaliacoes/**").hasRole(ADMIN)

                        // --- Notificacoes (RF 23) ---
                        // O sistema gera os avisos; cada usuario le e marca como lidos os SEUS
                        // (o escopo e verificado no service). Criar, enviar e remover: administracao.
                        .requestMatchers(HttpMethod.GET, "/api/v1/notificacoes", "/api/v1/notificacoes/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/notificacoes/lidas", "/api/v1/notificacoes/*/lida").authenticated()
                        .requestMatchers("/api/v1/notificacoes", "/api/v1/notificacoes/**").hasRole(ADMIN)

                        // --- Auditoria (RNF 19) ---
                        // A trilha tem dados pessoais e IP: so a administracao consulta.
                        .requestMatchers("/api/v1/auditoria", "/api/v1/auditoria/**").hasRole(ADMIN)

                        // --- Registros de motivo (RF 11) ---
                        // Somente leitura (escopo no service): o motivo e gravado pela propria operacao. Nao ha
                        // POST avulso, que deixaria qualquer participante forjar uma recusa no historico.
                        .requestMatchers(HttpMethod.GET, "/api/v1/registros-motivo/**").authenticated()
                        .requestMatchers("/api/v1/registros-motivo", "/api/v1/registros-motivo/**").hasRole(ADMIN)

                        // Qualquer outro endpoint da API exige ao menos autenticacao
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Sem Basic Auth: a senha so trafega no login. Token ausente, invalido ou vencido -> 401 em JSON.
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeToken))
                        .authenticationEntryPoint(naoAutenticado))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(accessDeniedHandler))
                // RNF 18 - depois do ExceptionTranslationFilter, para o 403 (TERMO_PENDENTE) sair em JSON.
                .addFilterAfter(new ExigeAceiteDoTermoFilter(), ExceptionTranslationFilter.class);

        return http.build();
    }
}
