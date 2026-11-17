package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.config.JwtConfig;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.LoginRequestDTO;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.EmissorDeToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Item 2.3 - login por e-mail e senha devolvendo token (sem Spring). */
class AutenticacaoServiceImplTest {

    private static final String EMAIL = "kevin.aluno@unisales.edu.br";
    private static final String SENHA = "Senha123@";

    // Custo 4: so para o teste ser rapido; a API usa o padrao (10).
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final JwtConfig config = new JwtConfig("segredo-de-teste-com-pelo-menos-32-caracteres");
    private final AutenticacaoServiceImpl service = new AutenticacaoServiceImpl(
            usuarioRepository, encoder, new EmissorDeToken(config.jwtEncoder(), 480));

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = Usuario.builder().id(3L).email(EMAIL).senha(encoder.encode(SENHA))
                .tipoUsuario(TipoUsuario.ALUNO).ativo(true).build();
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("Credenciais corretas devolvem token Bearer valido para o usuario")
    void loginValido() {
        var resposta = service.entrar(new LoginRequestDTO(EMAIL, SENHA));

        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.expiraEm()).isNotNull();
        assertThat(config.jwtDecoder().decode(resposta.token()).getSubject()).isEqualTo("3");
    }

    @Test
    @DisplayName("Espacos em volta do e-mail sao ignorados")
    void emailComEspacos() {
        assertThat(service.entrar(new LoginRequestDTO("  " + EMAIL + " ", SENHA)).token()).isNotBlank();
    }

    @Test
    @DisplayName("Senha errada: 401 com mensagem generica")
    void senhaErrada() {
        assertThatThrownBy(() -> service.entrar(new LoginRequestDTO(EMAIL, "errada")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AutenticacaoServiceImpl.CREDENCIAIS_INVALIDAS);
    }

    @Test
    @DisplayName("E-mail inexistente: mesma resposta da senha errada")
    void emailInexistente() {
        assertThatThrownBy(() -> service.entrar(new LoginRequestDTO("ninguem@unisales.edu.br", SENHA)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AutenticacaoServiceImpl.CREDENCIAIS_INVALIDAS);
    }

    @Test
    @DisplayName("Usuario inativo nao entra, mesmo com a senha certa")
    void usuarioInativo() {
        usuario.setAtivo(false);
        assertThatThrownBy(() -> service.entrar(new LoginRequestDTO(EMAIL, SENHA)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage(AutenticacaoServiceImpl.CREDENCIAIS_INVALIDAS);
    }

    @Test
    @DisplayName("Senha acima do limite do BCrypt (72 bytes) e senha errada, nao erro 500")
    void senhaLongaDemais() {
        assertThatThrownBy(() -> service.entrar(new LoginRequestDTO(EMAIL, "x".repeat(100))))
                .isInstanceOf(BadCredentialsException.class);
    }
}
