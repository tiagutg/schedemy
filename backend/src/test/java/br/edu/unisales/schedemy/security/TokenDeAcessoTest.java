package br.edu.unisales.schedemy.security;

import br.edu.unisales.schedemy.config.JwtConfig;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Item 2.3 - token de acesso: emissao, validacao e conversao no usuario (sem Spring). */
class TokenDeAcessoTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-caracteres";

    private final JwtConfig config = new JwtConfig(SEGREDO);
    private final JwtDecoder decoder = config.jwtDecoder();
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ConversorDeToken conversor = new ConversorDeToken(usuarioRepository);
    private final EmissorDeToken emissor = new EmissorDeToken(config.jwtEncoder(), 480);

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = Usuario.builder().id(7L).email("aluno@unisales.edu.br").senha("$2a$10$hashqualquer")
                .tipoUsuario(TipoUsuario.ALUNO).ativo(true).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("Token emitido e aceito: papel lido do cadastro e nome = e-mail")
    void tokenValido() {
        var emitido = emissor.emitir(usuario);
        Jwt jwt = decoder.decode(emitido.token());

        assertThat(jwt.getSubject()).isEqualTo("7");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.EMISSOR);
        assertThat(emitido.expiraEm()).isEqualTo(jwt.getExpiresAt());
        // O token nao carrega e-mail, senha nem papel.
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "senha", "papel", "scope");

        var autenticacao = conversor.convert(jwt);
        assertThat(autenticacao.getName()).isEqualTo("aluno@unisales.edu.br");
        assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ALUNO");
    }

    @Test
    @DisplayName("Perfil trocado depois do login vale na hora")
    void papelVemDoBanco() {
        Jwt jwt = decoder.decode(emissor.emitir(usuario).token());
        usuario.setTipoUsuario(TipoUsuario.PROFESSOR);

        assertThat(conversor.convert(jwt).getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PROFESSOR");
    }

    @Test
    @DisplayName("Usuario desativado depois do login perde a sessao")
    void usuarioInativo() {
        Jwt jwt = decoder.decode(emissor.emitir(usuario).token());
        usuario.setAtivo(false);

        assertThatThrownBy(() -> conversor.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    @DisplayName("Senha trocada derruba os tokens emitidos com a senha antiga")
    void senhaTrocada() {
        Jwt jwt = decoder.decode(emissor.emitir(usuario).token());
        usuario.setSenha("$2a$10$outrohash");

        assertThatThrownBy(() -> conversor.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    @DisplayName("Usuario removido do banco: 401")
    void usuarioRemovido() {
        Jwt jwt = decoder.decode(emissor.emitir(usuario).token());
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversor.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    @DisplayName("Token vencido e recusado")
    void tokenVencido() {
        Clock ontem = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        var vencido = new EmissorDeToken(config.jwtEncoder(), Duration.ofHours(8), ontem).emitir(usuario);

        assertThatThrownBy(() -> decoder.decode(vencido.token())).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token assinado com outro segredo e recusado")
    void outroSegredo() {
        var outraApi = new JwtConfig("outro-segredo-bem-diferente-com-32-caracteres");
        String token = new EmissorDeToken(outraApi.jwtEncoder(), 480).emitir(usuario).token();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Segredo curto demais impede a API de subir")
    void segredoCurto() {
        assertThatThrownBy(() -> new JwtConfig("curto")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Sem segredo configurado, a API sorteia um e continua funcionando")
    void semSegredo() {
        var sorteado = new JwtConfig("");
        String token = new EmissorDeToken(sorteado.jwtEncoder(), 480).emitir(usuario).token();

        assertThat(sorteado.jwtDecoder().decode(token).getSubject()).isEqualTo("7");
        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }
}
