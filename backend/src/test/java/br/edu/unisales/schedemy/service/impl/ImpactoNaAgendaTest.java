package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.CancelamentoDeAfetadasRequestDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.exception.ReunioesAfetadasException;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.BloqueioPeriodoRepository;
import br.edu.unisales.schedemy.repository.DisponibilidadeRepository;
import br.edu.unisales.schedemy.repository.RegistroMotivoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * RF 06/RF 07/RF 10 - reunioes afetadas por mudanca na agenda do docente e o cancelamento confirmado.
 * Roda sem banco; a CalculadoraDeAgenda e a de verdade (a regra de horario e dela).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImpactoNaAgendaTest {

    private static final LocalDate DIA = LocalDate.now().plusDays(7);
    private static final int DIA_SEMANA = CalculadoraDeAgenda.diaSemanaDoBanco(DIA);

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private DisponibilidadeRepository disponibilidadeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RegistroMotivoRepository registroMotivoRepository;
    @Mock private Notificador notificador;
    @Mock private UsuarioAutenticado usuarioAutenticado;

    private ImpactoNaAgenda impacto;

    private final Usuario docente = usuario(2L, "Rômulo Ferreira", TipoUsuario.PROFESSOR);
    private final Usuario outroDocente = usuario(3L, "Marcelo Souza", TipoUsuario.COORDENADOR);
    private final Usuario aluno = usuario(1L, "Kevin Lins", TipoUsuario.ALUNO);
    private final List<Agendamento> doDocente = new ArrayList<>();
    private long proximoId = 100;

    @BeforeEach
    void preparar() {
        CalculadoraDeAgenda calculadora = new CalculadoraDeAgenda(disponibilidadeRepository,
                mock(BloqueioPeriodoRepository.class), agendamentoRepository);
        impacto = new ImpactoNaAgenda(agendamentoRepository, disponibilidadeRepository, usuarioRepository,
                registroMotivoRepository, calculadora, notificador, usuarioAutenticado);
        when(agendamentoRepository.buscarDoParticipanteNoPeriodo(eq(docente.getId()), any(), any(), any()))
                .thenAnswer(c -> doDocente.stream()
                        .filter(a -> !a.getDataReuniao().isBefore(c.getArgument(1, LocalDate.class))
                                && !a.getDataReuniao().isAfter(c.getArgument(2, LocalDate.class)))
                        .toList());
        when(agendamentoRepository.buscarIdsDoParticipanteDesde(eq(docente.getId()), any(), any()))
                .thenAnswer(c -> doDocente.stream().map(Agendamento::getId).toList());
        when(usuarioAutenticado.obter()).thenReturn(docente);
    }

    // ------------------------------------------------------------------
    // Disponibilidade
    // ------------------------------------------------------------------

    @Test
    @DisplayName("encurtar a janela afeta so as reunioes que ficam de fora")
    void encurtarJanela() {
        Disponibilidade atual = janelaGravada(1L, "08:00", "12:00");
        Agendamento dentro = reuniao(DIA, "09:00", aluno, docente);
        Agendamento fora = reuniao(DIA, "10:30", aluno, docente);

        assertThat(impacto.afetadasPelaDisponibilidade(atual, janela(DIA_SEMANA, "08:00", "10:00", true)))
                .containsExactly(fora);
        assertThat(dentro.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
    }

    @Test
    @DisplayName("aumentar a janela ou reativar uma janela inativa nunca afeta reunioes")
    void aumentarOuReativar() {
        Disponibilidade atual = janelaGravada(1L, "08:00", "12:00");
        reuniao(DIA, "09:00", aluno, docente);

        assertThat(impacto.afetadasPelaDisponibilidade(atual, janela(DIA_SEMANA, "07:00", "14:00", true))).isEmpty();

        atual.setAtivo(false);
        assertThat(impacto.afetadasPelaDisponibilidade(atual, janela(DIA_SEMANA, "08:00", "12:00", true))).isEmpty();
        // so a primeira consulta (janela ativa) foi ao banco
        verify(agendamentoRepository, times(1)).buscarDoParticipanteNoPeriodo(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("desativar, remover ou trocar o dia afeta as reunioes da janela; as de outra janela nao")
    void desativarRemoverOuTrocarDia() {
        Disponibilidade atual = janelaGravada(1L, "08:00", "12:00");
        Disponibilidade tarde = janela(DIA_SEMANA, "14:00", "18:00", true);
        tarde.setId(2L);
        when(disponibilidadeRepository.findByUsuarioIdAndAtivoTrue(docente.getId())).thenReturn(List.of(atual, tarde));
        Agendamento manha = reuniao(DIA, "09:00", aluno, docente);
        reuniao(DIA, "15:00", aluno, docente);

        assertThat(impacto.afetadasPelaDisponibilidade(atual, janela(DIA_SEMANA, "08:00", "12:00", false)))
                .containsExactly(manha);
        assertThat(impacto.afetadasPelaDisponibilidade(atual, null)).containsExactly(manha);
        assertThat(impacto.afetadasPelaDisponibilidade(atual, janela(DIA_SEMANA % 7 + 1, "08:00", "12:00", true)))
                .containsExactly(manha);
    }

    @Test
    @DisplayName("passar a janela para outro docente afeta as reunioes do docente atual")
    void trocarDocente() {
        Disponibilidade atual = janelaGravada(1L, "08:00", "12:00");
        Agendamento manha = reuniao(DIA, "09:00", aluno, docente);
        Disponibilidade depois = janela(DIA_SEMANA, "08:00", "12:00", true);
        depois.setUsuario(outroDocente);

        assertThat(impacto.afetadasPelaDisponibilidade(atual, depois)).containsExactly(manha);
    }

    @Test
    @DisplayName("reuniao que ja comecou (ainda sem a rotina passar para EM_ANDAMENTO) nao e afetada")
    void jaComecou() {
        LocalDateTime ha5min = LocalDateTime.now().minusMinutes(5);
        Disponibilidade atual = janelaGravada(1L, "00:00", "23:59");
        atual.setDiaSemana(CalculadoraDeAgenda.diaSemanaDoBanco(ha5min.toLocalDate()));
        reuniao(ha5min.toLocalDate(), ha5min.toLocalTime().withSecond(0).withNano(0).toString(), aluno, docente);

        assertThat(impacto.afetadasPelaDisponibilidade(atual, null)).isEmpty();
    }

    @Test
    @DisplayName("so busca reunioes futuras nos status PENDENTE, AGUARDANDO_RESPOSTA e CONFIRMADO")
    void statusAfetaveis() {
        impacto.afetadasPelaDisponibilidade(janelaGravada(1L, "08:00", "12:00"), null);

        verify(agendamentoRepository).buscarDoParticipanteNoPeriodo(eq(docente.getId()), eq(LocalDate.now()), any(),
                eq(ImpactoNaAgenda.STATUS_AFETAVEIS));
        assertThat(ImpactoNaAgenda.STATUS_AFETAVEIS).containsExactlyInAnyOrder(StatusAgendamento.PENDENTE,
                StatusAgendamento.AGUARDANDO_RESPOSTA, StatusAgendamento.CONFIRMADO);
    }

    // ------------------------------------------------------------------
    // Bloqueio
    // ------------------------------------------------------------------

    @Test
    @DisplayName("bloqueio afeta as reunioes que cobre; periodo no passado comeca a contar de hoje")
    void bloqueio() {
        Agendamento tarde = reuniao(DIA, "15:00", aluno, docente);
        reuniao(DIA, "09:00", aluno, docente);
        BloqueioPeriodo b = BloqueioPeriodo.builder().usuario(docente)
                .dataInicio(LocalDate.now().minusDays(3)).dataFim(DIA)
                .horarioInicio(LocalTime.of(13, 0)).horarioFim(LocalTime.of(18, 0)).eRecorrente(false).build();

        assertThat(impacto.afetadasPeloBloqueio(b)).containsExactly(tarde);
        verify(agendamentoRepository).buscarDoParticipanteNoPeriodo(docente.getId(), LocalDate.now(), DIA,
                ImpactoNaAgenda.STATUS_AFETAVEIS);
    }

    @Test
    @DisplayName("bloqueio todo no passado nao consulta nada")
    void bloqueioNoPassado() {
        BloqueioPeriodo b = BloqueioPeriodo.builder().usuario(docente)
                .dataInicio(LocalDate.now().minusDays(9)).dataFim(LocalDate.now().minusDays(2)).eRecorrente(false).build();

        assertThat(impacto.afetadasPeloBloqueio(b)).isEmpty();
        verify(agendamentoRepository, never()).buscarDoParticipanteNoPeriodo(anyLong(), any(), any(), any());
    }

    // ------------------------------------------------------------------
    // Confirmacao e cancelamento
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sem confirmacao, recusa com a lista e nao cancela nada")
    void semConfirmacao() {
        Agendamento a = reuniao(DIA, "10:00", aluno, docente);

        assertThatThrownBy(() -> impacto.cancelarAfetadas(docente, () -> List.of(a), null))
                .isInstanceOfSatisfying(ReunioesAfetadasException.class, e ->
                        assertThat(e.getReunioes()).extracting(ReuniaoAfetadaResponseDTO::id).containsExactly(a.getId()));
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
        verifyNoInteractions(notificador, registroMotivoRepository);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmacao sem uma das afetadas (surgiu depois da previa) recusa tudo")
    void confirmacaoIncompleta() {
        Agendamento vista = reuniao(DIA, "10:00", aluno, docente);
        Agendamento nova = reuniao(DIA, "11:00", aluno, docente);

        assertThatThrownBy(() -> impacto.cancelarAfetadas(docente, () -> List.of(vista, nova),
                new CancelamentoDeAfetadasRequestDTO(List.of(vista.getId()), "Colegiado")))
                .isInstanceOf(ReunioesAfetadasException.class)
                .hasMessageContaining("mudou");
        assertThat(vista.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
        verifyNoInteractions(notificador);
    }

    @Test
    @DisplayName("afetada que nao foi travada (gravada depois da trava) tambem recusa")
    void afetadaNaoTravada() {
        Agendamento a = reuniao(DIA, "10:00", aluno, docente);
        when(agendamentoRepository.buscarIdsDoParticipanteDesde(eq(docente.getId()), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> impacto.cancelarAfetadas(docente, () -> List.of(a),
                new CancelamentoDeAfetadasRequestDTO(List.of(a.getId()), "Colegiado")))
                .isInstanceOf(ReunioesAfetadasException.class);
    }

    @Test
    @DisplayName("motivo e obrigatorio quando ha reunioes afetadas (RF 11)")
    void motivoObrigatorio() {
        Agendamento a = reuniao(DIA, "10:00", aluno, docente);

        assertThatThrownBy(() -> impacto.cancelarAfetadas(docente, () -> List.of(a),
                new CancelamentoDeAfetadasRequestDTO(List.of(a.getId()), "  ")))
                .isInstanceOf(RegraDeNegocioException.class)
                .isNotInstanceOf(ReunioesAfetadasException.class)
                .hasMessageContaining("motivo");
        assertThat(a.getStatus()).isEqualTo(StatusAgendamento.PENDENTE);
    }

    @Test
    @DisplayName("confirmada: cancela (mesmo a menos de 1 h e com varios docentes), registra o motivo e avisa")
    void cancelaConfirmadas() {
        Agendamento logo = reuniao(LocalDate.now(), null, aluno, docente, outroDocente);
        logo.setStatus(StatusAgendamento.CONFIRMADO);
        Usuario admin = usuario(9L, "Admin", TipoUsuario.ADMIN);
        when(usuarioAutenticado.obter()).thenReturn(admin);

        impacto.cancelarAfetadas(docente, () -> List.of(logo),
                new CancelamentoDeAfetadasRequestDTO(List.of(logo.getId(), 999L), " Colegiado "));

        assertThat(logo.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        ArgumentCaptor<RegistroMotivo> registro = ArgumentCaptor.forClass(RegistroMotivo.class);
        verify(registroMotivoRepository).save(registro.capture());
        assertThat(registro.getValue().getTipoOperacao()).isEqualTo(TipoOperacaoMotivo.CANCELAMENTO);
        assertThat(registro.getValue().getUsuario()).isSameAs(admin);
        assertThat(registro.getValue().getJustificativa()).isEqualTo("Agenda de Rômulo Ferreira mudou: Colegiado");
        verify(agendamentoRepository).save(logo);
        verify(notificador).canceladaPorMudancaDeAgenda(logo, docente, admin, "Colegiado");
    }

    @Test
    @DisplayName("RN 01: trava as reunioes do docente, depois o docente, e so entao calcula a lista")
    @SuppressWarnings("unchecked")
    void ordemDasTravas() {
        Agendamento a = reuniao(DIA, "10:00", aluno, docente);
        Agendamento b = reuniao(DIA, "11:00", aluno, docente);
        Supplier<List<Agendamento>> afetadas = mock(Supplier.class);
        when(afetadas.get()).thenReturn(List.of());

        impacto.cancelarAfetadas(docente, afetadas, null);

        InOrder ordem = inOrder(agendamentoRepository, usuarioRepository, afetadas);
        ordem.verify(agendamentoRepository).travarPorId(a.getId());
        ordem.verify(agendamentoRepository).travarPorId(b.getId());
        ordem.verify(usuarioRepository).travarParaAgendamento(List.of(docente.getId()));
        ordem.verify(afetadas).get();
        verifyNoInteractions(notificador, registroMotivoRepository);
    }

    @Test
    @DisplayName("RF 07: lista com alunos, outros docentes e o aviso de menos de 1 h (RN 05)")
    void paraResposta() {
        Agendamento logo = reuniao(LocalDate.now(), null, aluno, docente, outroDocente);
        Agendamento depois = reuniao(DIA, "10:00", aluno, docente);

        List<ReuniaoAfetadaResponseDTO> lista = impacto.paraResposta(List.of(logo, depois), docente);

        assertThat(lista.get(0).alunos()).containsExactly("Kevin Lins");
        assertThat(lista.get(0).outrosDocentes()).containsExactly("Marcelo Souza");
        assertThat(lista.get(0).menosDeUmaHora()).isTrue();
        assertThat(lista.get(1).outrosDocentes()).isEmpty();
        assertThat(lista.get(1).menosDeUmaHora()).isFalse();
        assertThat(lista.get(1).horarioFim()).isEqualTo(LocalTime.of(10, 30));
    }

    // ------------------------------------------------------------------

    private static Usuario usuario(Long id, String nome, TipoUsuario tipo) {
        return Usuario.builder().id(id).nome(nome).tipoUsuario(tipo).ativo(true).build();
    }

    /** Janela do docente (como o service monta a "depois", sem id). */
    private Disponibilidade janela(int diaSemana, String inicio, String fim, boolean ativa) {
        return Disponibilidade.builder().usuario(docente).diaSemana(diaSemana).horarioInicio(LocalTime.parse(inicio))
                .horarioFim(LocalTime.parse(fim)).ativo(ativa).build();
    }

    /** Janela gravada do docente; e a unica ativa dele, salvo se o teste estubar outras. */
    private Disponibilidade janelaGravada(long id, String inicio, String fim) {
        Disponibilidade d = janela(DIA_SEMANA, inicio, fim, true);
        d.setId(id);
        when(disponibilidadeRepository.findByUsuarioIdAndAtivoTrue(docente.getId())).thenReturn(List.of(d));
        return d;
    }

    /**
     * Reuniao de 30 min organizada pelo primeiro usuario; horario nulo = daqui a 30 min (menos de 1 h).
     * Entra na lista de reunioes do docente.
     */
    private Agendamento reuniao(LocalDate data, String horario, Usuario organizador, Usuario... convidados) {
        LocalTime inicio = horario != null ? LocalTime.parse(horario)
                : LocalDateTime.now().plusMinutes(30).toLocalTime().withSecond(0).withNano(0);
        LocalDate dia = horario != null ? data : LocalDateTime.now().plusMinutes(30).toLocalDate();
        Agendamento a = Agendamento.builder()
                .id(proximoId++)
                .organizador(organizador)
                .duracao(DuracaoReuniao.builder().minutos(30).build())
                .dataReuniao(dia)
                .horarioInicio(inicio)
                .topico("TCC")
                .status(StatusAgendamento.PENDENTE)
                .build();
        a.getParticipantes().add(AgendamentoParticipante.builder().agendamento(a).usuario(organizador)
                .papel(PapelParticipante.ORGANIZADOR).build());
        for (Usuario c : convidados) {
            a.getParticipantes().add(AgendamentoParticipante.builder().agendamento(a).usuario(c)
                    .papel(PapelParticipante.CONVIDADO).build());
        }
        doDocente.add(a);
        return a;
    }
}
