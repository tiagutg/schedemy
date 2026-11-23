package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoDeAfetadasRequestDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.exception.ReunioesAfetadasException;
import br.edu.unisales.schedemy.mapper.BloqueioPeriodoMapper;
import br.edu.unisales.schedemy.repository.BloqueioPeriodoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Bloqueio de agenda (RF 04/05): so de docente, periodo e horario coerentes. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BloqueioPeriodoServiceImplTest {

    private static final long ID_USUARIO = 2L;
    private static final LocalDate DIA = LocalDate.of(2026, 12, 1);

    @Mock private BloqueioPeriodoRepository bloqueioPeriodoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioAutenticado usuarioAutenticado;
    @Mock private ImpactoNaAgenda impactoNaAgenda;

    private BloqueioPeriodoServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new BloqueioPeriodoServiceImpl(bloqueioPeriodoRepository, usuarioRepository,
                new BloqueioPeriodoMapper(), usuarioAutenticado, impactoNaAgenda, mock(Auditoria.class));
        usuarioDoTipo(TipoUsuario.PROFESSOR);
        when(bloqueioPeriodoRepository.save(any())).then(returnsFirstArg());
    }

    private Usuario usuarioDoTipo(TipoUsuario tipo) {
        Usuario u = Usuario.builder().id(ID_USUARIO).nome("Usuario").tipoUsuario(tipo).build();
        when(usuarioRepository.findById(ID_USUARIO)).thenReturn(Optional.of(u));
        return u;
    }

    private static BloqueioPeriodoRequestDTO pedido(String inicio, String fim) {
        return new BloqueioPeriodoRequestDTO(ID_USUARIO, DIA, DIA,
                inicio == null ? null : LocalTime.parse(inicio), fim == null ? null : LocalTime.parse(fim),
                false, null, "Congresso", null);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TipoUsuario.class, names = {"PROFESSOR", "COORDENADOR"})
    @DisplayName("docente pode ter bloqueio")
    void docentePode(TipoUsuario tipo) {
        usuarioDoTipo(tipo);

        service.criar(pedido("13:00", "18:00"));

        verify(bloqueioPeriodoRepository).save(any());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = TipoUsuario.class, names = {"ALUNO", "RECEPCIONISTA", "ADMIN"})
    @DisplayName("quem nao e docente nao tem bloqueio de agenda")
    void naoDocenteNaoPode(TipoUsuario tipo) {
        usuarioDoTipo(tipo);

        assertThatThrownBy(() -> service.criar(pedido(null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("PROFESSOR ou COORDENADOR");
        verify(bloqueioPeriodoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({"14:00, 10:00", "10:00, 10:00"})
    @DisplayName("recusa fim igual ou anterior ao inicio")
    void recusaHorarioInvertido(String inicio, String fim) {
        assertThatThrownBy(() -> service.criar(pedido(inicio, fim)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("posterior");
        verify(bloqueioPeriodoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "inicio={0}, fim={1}")
    @CsvSource(value = {"NULL, NULL", "13:00, NULL", "NULL, 12:00"}, nullValues = "NULL")
    @DisplayName("horarios em branco continuam valendo (dia inteiro / ate o fim do dia / desde o inicio do dia)")
    void aceitaHorariosEmBranco(String inicio, String fim) {
        service.criar(pedido(inicio, fim));

        verify(bloqueioPeriodoRepository).save(any());
    }

    @Test
    @DisplayName("RF 05: so o bloqueio recorrente pode ficar sem data de fim")
    void semDataDeFim() {
        BloqueioPeriodoRequestDTO unicoSemFim = new BloqueioPeriodoRequestDTO(ID_USUARIO, DIA, null,
                null, null, false, null, "Congresso", null);
        assertThatThrownBy(() -> service.criar(unicoSemFim))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data de fim");
        verify(bloqueioPeriodoRepository, never()).save(any());

        service.criar(new BloqueioPeriodoRequestDTO(ID_USUARIO, DIA, null,
                LocalTime.of(14, 0), LocalTime.of(16, 0), true, 3, "Colegiado", null));
        verify(bloqueioPeriodoRepository).save(any());
    }

    @Test
    @DisplayName("edicao tambem valida horario e papel")
    void edicaoValida() {
        BloqueioPeriodo existente = BloqueioPeriodo.builder().id(5L)
                .usuario(usuarioDoTipo(TipoUsuario.PROFESSOR)).dataInicio(DIA).dataFim(DIA).build();
        when(bloqueioPeriodoRepository.findById(5L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.atualizar(5L, pedido("18:00", "08:00")))
                .isInstanceOf(RegraDeNegocioException.class);

        usuarioDoTipo(TipoUsuario.ALUNO);
        assertThatThrownBy(() -> service.atualizar(5L, pedido(null, null)))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(bloqueioPeriodoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF 06 - criar confere e cancela as afetadas antes de gravar; a previa nao grava")
    void criarConfereAfetadas() {
        CancelamentoDeAfetadasRequestDTO confirmacao = new CancelamentoDeAfetadasRequestDTO(List.of(30L), "Congresso");
        BloqueioPeriodoRequestDTO dto = new BloqueioPeriodoRequestDTO(ID_USUARIO, DIA, DIA, null, null, false, null,
                "Congresso", confirmacao);

        service.previaDaCriacao(dto);
        verify(impactoNaAgenda, never()).cancelarAfetadas(any(), any(), any());
        verify(bloqueioPeriodoRepository, never()).save(any());

        service.criar(dto);
        InOrder ordem = inOrder(impactoNaAgenda, bloqueioPeriodoRepository);
        ordem.verify(impactoNaAgenda).cancelarAfetadas(any(), any(), eq(confirmacao));
        ordem.verify(bloqueioPeriodoRepository).save(any());
    }

    @Test
    @DisplayName("RF 06 - sem a confirmacao das afetadas, o bloqueio nao e gravado")
    void criarRecusadoNaoGrava() {
        doThrow(new ReunioesAfetadasException("afeta", List.of()))
                .when(impactoNaAgenda).cancelarAfetadas(any(), any(), any());

        assertThatThrownBy(() -> service.criar(pedido(null, null))).isInstanceOf(ReunioesAfetadasException.class);
        verify(bloqueioPeriodoRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF 06 - remover bloqueio nunca afeta reunioes")
    void removerNaoConfere() {
        BloqueioPeriodo existente = BloqueioPeriodo.builder().id(5L)
                .usuario(usuarioDoTipo(TipoUsuario.PROFESSOR)).dataInicio(DIA).dataFim(DIA).build();
        when(bloqueioPeriodoRepository.findById(5L)).thenReturn(Optional.of(existente));

        service.deletar(5L);

        verify(impactoNaAgenda, never()).cancelarAfetadas(any(), any(), any());
        verify(bloqueioPeriodoRepository).delete(existente);
    }
}
