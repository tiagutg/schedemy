package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.UsuarioRequestDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.UsuarioMapper;
import br.edu.unisales.schedemy.repository.AgendamentoParticipanteRepository;
import br.edu.unisales.schedemy.repository.LogAuditoriaRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** RNF 19 - o cadastro de usuarios entra na trilha de auditoria; com trilha, o usuario so e desativado. */
class UsuarioServiceImplTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final AgendamentoParticipanteRepository participanteRepository = mock(AgendamentoParticipanteRepository.class);
    private final LogAuditoriaRepository logAuditoriaRepository = mock(LogAuditoriaRepository.class);
    private final Auditoria auditoria = mock(Auditoria.class);
    private final UsuarioAutenticado usuarioAutenticado = mock(UsuarioAutenticado.class);
    @SuppressWarnings("deprecation")
    private final UsuarioServiceImpl service = new UsuarioServiceImpl(usuarioRepository, participanteRepository,
            new UsuarioMapper(), NoOpPasswordEncoder.getInstance(), usuarioAutenticado, auditoria,
            logAuditoriaRepository);

    private Usuario pedro;

    @BeforeEach
    void preparar() {
        pedro = Usuario.builder().id(2L).idMicrosoft("ms-2").nome("Pedro").email("pedro@unisales.edu.br")
                .telefoneWhatsapp("27999990000").tipoUsuario(TipoUsuario.ALUNO).matriculaRa("20261002")
                .departamento("SI").senha("x").ativo(true).build();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(pedro));
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).then(returnsFirstArg());
    }

    private UsuarioRequestDTO edicao(String email, String telefone, TipoUsuario tipo, String senha, Boolean ativo) {
        return new UsuarioRequestDTO(null, "Pedro", email, senha, telefone, tipo, "20261002", null, "SI", ativo);
    }

    @Test
    @DisplayName("edicao registra so os nomes dos campos (sem o e-mail/telefone novos) e o perfil antigo e novo")
    void edicao() {
        service.atualizar(2L, edicao("pedro.novo@unisales.edu.br", "27988887777", TipoUsuario.PROFESSOR, null, null));

        ArgumentCaptor<String> detalhes = ArgumentCaptor.forClass(String.class);
        verify(auditoria).registrar(eq(AcaoAuditoria.USUARIO_EDITADO), eq(2L), detalhes.capture());
        assertThat(detalhes.getValue()).contains("email", "telefone", "perfil (ALUNO -> PROFESSOR)")
                .doesNotContain("pedro.novo", "27988887777");
        verify(auditoria, never()).registrar(eq(AcaoAuditoria.USUARIO_SENHA_ALTERADA), any(), any());
    }

    @Test
    @DisplayName("desativar e trocar a senha viram acoes proprias; sem mudanca de dados, sem USUARIO_EDITADO")
    void desativacaoESenha() {
        service.atualizar(2L, edicao("pedro@unisales.edu.br", "27999990000", TipoUsuario.ALUNO, "NovaSenha1", false));

        verify(auditoria).registrar(eq(AcaoAuditoria.USUARIO_DESATIVADO), eq(2L), anyString());
        verify(auditoria).registrar(eq(AcaoAuditoria.USUARIO_SENHA_ALTERADA), eq(2L), anyString());
        verify(auditoria, never()).registrar(eq(AcaoAuditoria.USUARIO_EDITADO), any(), any());
    }

    @Test
    @DisplayName("usuario com acoes na trilha nao e excluido (so desativado)")
    void naoExcluiComTrilha() {
        when(logAuditoriaRepository.existsByUsuarioId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.deletar(2L)).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("auditoria");
        verify(usuarioRepository, never()).delete(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    @DisplayName("usuario sem historico e excluido e a exclusao entra na trilha")
    void excluiSemHistorico() {
        service.deletar(2L);

        verify(auditoria).registrar(AcaoAuditoria.USUARIO_REMOVIDO, 2L, "Pedro (ALUNO)");
        verify(usuarioRepository).delete(pedro);
    }

    @Test
    @DisplayName("ninguem remove o proprio cadastro")
    void naoRemoveOProprio() {
        when(usuarioAutenticado.id()).thenReturn(2L);

        assertThatThrownBy(() -> service.deletar(2L)).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("proprio cadastro");
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("ninguem desativa o proprio cadastro nem muda o proprio perfil; os demais dados podem mudar")
    void naoSeTrancaParaFora() {
        when(usuarioAutenticado.id()).thenReturn(2L);

        assertThatThrownBy(() -> service.atualizar(2L, edicao("pedro@unisales.edu.br", "27999990000", TipoUsuario.ALUNO, null, false)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("desativar");
        pedro.setAtivo(true);
        assertThatThrownBy(() -> service.atualizar(2L, edicao("pedro@unisales.edu.br", "27999990000", TipoUsuario.ADMIN, null, null)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("perfil");
        pedro.setTipoUsuario(TipoUsuario.ALUNO);
        service.atualizar(2L, edicao("pedro.novo@unisales.edu.br", "27999990000", TipoUsuario.ALUNO, null, null));
        verify(usuarioRepository).save(pedro);
    }
}
