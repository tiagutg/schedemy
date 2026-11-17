package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.BloqueioPeriodoRepository;
import br.edu.unisales.schedemy.repository.DisponibilidadeRepository;
import br.edu.unisales.schedemy.service.impl.CalculadoraDeAgenda.TipoImpedimento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regras de "horario livre" usadas tanto na validacao ao salvar quanto na sugestao de
 * horarios (RF 14). Roda sem banco: os repositorios sao mocks.
 */
@ExtendWith(MockitoExtension.class)
// Cada teste estuba so os usuarios que interessam; os demais devolvem listas vazias.
@MockitoSettings(strictness = Strictness.LENIENT)
class CalculadoraDeAgendaTest {

    // Terca-feira: dia 3 no padrao do banco (1 = domingo ... 7 = sabado).
    private static final LocalDate TERCA = LocalDate.of(2026, 11, 3);
    private static final int DIA_TERCA = 3;

    @Mock private DisponibilidadeRepository disponibilidadeRepository;
    @Mock private BloqueioPeriodoRepository bloqueioPeriodoRepository;
    @Mock private AgendamentoRepository agendamentoRepository;
    @InjectMocks private CalculadoraDeAgenda calculadora;

    private final Usuario professor = usuario(1L, TipoUsuario.PROFESSOR);
    private final Usuario coordenador = usuario(2L, TipoUsuario.COORDENADOR);
    private final Usuario aluno = usuario(3L, TipoUsuario.ALUNO);

    @Test
    @DisplayName("Dia da semana segue o padrao do banco")
    void diaSemanaDoBanco() {
        assertThat(CalculadoraDeAgenda.diaSemanaDoBanco(TERCA)).isEqualTo(DIA_TERCA);
        assertThat(CalculadoraDeAgenda.diaSemanaDoBanco(LocalDate.of(2026, 11, 1))).isEqualTo(1); // domingo
        assertThat(CalculadoraDeAgenda.diaSemanaDoBanco(LocalDate.of(2026, 11, 7))).isEqualTo(7); // sabado
    }

    @Test
    @DisplayName("RN 15: docente livre dentro da disponibilidade do dia")
    void docenteLivreDentroDaDisponibilidade() {
        disponivel(professor, DIA_TERCA, "08:00", "12:00");

        assertThat(calculadora.verificar(professor, TERCA, hora("10:00"), hora("10:30"), null)).isEmpty();
    }

    @Test
    @DisplayName("RN 15: fora da disponibilidade, ou em outro dia da semana, nao esta livre")
    void docenteForaDaDisponibilidade() {
        disponivel(professor, DIA_TERCA, "08:00", "12:00");
        disponivel(professor, DIA_TERCA + 1, "13:00", "18:00");

        assertThat(calculadora.verificar(professor, TERCA, hora("11:45"), hora("12:15"), null))
                .get().extracting(CalculadoraDeAgenda.Impedimento::tipo)
                .isEqualTo(TipoImpedimento.FORA_DA_DISPONIBILIDADE);
        assertThat(calculadora.verificar(professor, TERCA, hora("14:00"), hora("14:30"), null))
                .get().extracting(CalculadoraDeAgenda.Impedimento::tipo)
                .isEqualTo(TipoImpedimento.FORA_DA_DISPONIBILIDADE);
    }

    @Test
    @DisplayName("RF 04: bloqueio com horario impede so o intervalo bloqueado")
    void bloqueioComHorario() {
        disponivel(professor, DIA_TERCA, "08:00", "18:00");
        bloqueado(professor, bloqueio(TERCA, TERCA, "14:00", "16:00"));

        assertThat(calculadora.verificar(professor, TERCA, hora("15:30"), hora("16:30"), null))
                .get().extracting(CalculadoraDeAgenda.Impedimento::tipo)
                .isEqualTo(TipoImpedimento.BLOQUEADO);
        assertThat(calculadora.verificar(professor, TERCA, hora("16:00"), hora("16:30"), null)).isEmpty();
    }

    @Test
    @DisplayName("RF 04: bloqueio sem horario cobre o dia inteiro")
    void bloqueioDiaInteiro() {
        disponivel(professor, DIA_TERCA, "08:00", "18:00");
        bloqueado(professor, bloqueio(TERCA.minusDays(1), TERCA.plusDays(2), null, null));

        assertThat(calculadora.verificar(professor, TERCA, hora("08:00"), hora("08:30"), null))
                .get().extracting(CalculadoraDeAgenda.Impedimento::tipo)
                .isEqualTo(TipoImpedimento.BLOQUEADO);
    }

    @Test
    @DisplayName("RN 01: reuniao ativa no intervalo impede o horario e informa qual e")
    void reuniaoConflitante() {
        disponivel(professor, DIA_TERCA, "08:00", "18:00");
        Agendamento existente = reuniao(TERCA, "10:00", 30);
        reunioes(professor, existente);

        var impedimento = calculadora.verificar(professor, TERCA, hora("10:15"), hora("10:45"), null);

        assertThat(impedimento).get().extracting(CalculadoraDeAgenda.Impedimento::tipo)
                .isEqualTo(TipoImpedimento.REUNIAO_CONFLITANTE);
        assertThat(impedimento.get().conflitante()).isSameAs(existente);
        // Encostar no fim da reuniao anterior nao e sobreposicao.
        assertThat(calculadora.verificar(professor, TERCA, hora("10:30"), hora("11:00"), null)).isEmpty();
    }

    @Test
    @DisplayName("Aluno nao tem disponibilidade: so conflita com outras reunioes")
    void alunoSoConflitaComReunioes() {
        assertThat(calculadora.verificar(aluno, TERCA, hora("22:00"), hora("22:30"), null)).isEmpty();
        verifyNoInteractions(disponibilidadeRepository, bloqueioPeriodoRepository);

        reunioes(aluno, reuniao(TERCA, "22:00", 60));
        assertThat(calculadora.verificar(aluno, TERCA, hora("22:15"), hora("22:45"), null))
                .get().extracting(CalculadoraDeAgenda.Impedimento::tipo)
                .isEqualTo(TipoImpedimento.REUNIAO_CONFLITANTE);
    }

    @Test
    @DisplayName("RF 14: horarios livres sao os que servem para TODOS os envolvidos")
    void horariosLivresParaTodos() {
        disponivel(professor, DIA_TERCA, "08:00", "10:00");
        disponivel(coordenador, DIA_TERCA, "09:00", "11:00");
        reunioes(aluno, reuniao(TERCA, "09:00", 30));

        List<LocalTime> livres = calculadora.horariosLivres(
                List.of(professor, coordenador, aluno), TERCA, 30, 15, null, null);

        // Interseccao 09:00-10:00, sem a reuniao do aluno das 09:00 as 09:30.
        assertThat(livres).containsExactly(hora("09:30"));
    }

    @Test
    @DisplayName("RF 14: respeita bloqueios e nao oferece horarios antes do minimo")
    void horariosLivresComBloqueioEMinimo() {
        disponivel(professor, DIA_TERCA, "08:00", "10:00");
        bloqueado(professor, bloqueio(TERCA, TERCA, "08:00", "08:30"));

        List<LocalTime> livres = calculadora.horariosLivres(
                List.of(professor), TERCA, 30, 15, null, hora("08:40"));

        // Minimo arredondado para 08:45; o bloqueio ja terminou.
        assertThat(livres).containsExactly(hora("08:45"), hora("09:00"), hora("09:15"), hora("09:30"));
    }

    @Test
    @DisplayName("RF 14: sem disponibilidade no dia, nenhum horario")
    void horariosLivresSemDisponibilidade() {
        assertThat(calculadora.horariosLivres(List.of(professor), TERCA, 30, 15, null, null)).isEmpty();
    }

    @Test
    @DisplayName("RF 06: bloqueio recorrente so afeta reunioes no dia da semana configurado")
    void bloqueioRecorrenteAfetaReuniao() {
        BloqueioPeriodo todaTerca = bloqueio(TERCA.minusWeeks(1), TERCA.plusWeeks(3), "13:00", "18:00");
        todaTerca.setERecorrente(true);
        todaTerca.setDiaSemanaRecorrente(DIA_TERCA);

        assertThat(calculadora.bloqueioAfetaReuniao(todaTerca, reuniao(TERCA, "14:00", 30))).isTrue();
        assertThat(calculadora.bloqueioAfetaReuniao(todaTerca, reuniao(TERCA.plusDays(1), "14:00", 30))).isFalse();
        assertThat(calculadora.bloqueioAfetaReuniao(todaTerca, reuniao(TERCA, "10:00", 30))).isFalse();
        assertThat(calculadora.bloqueioAfetaReuniao(todaTerca, reuniao(TERCA.plusWeeks(4), "14:00", 30))).isFalse();
    }

    // ------------------------------------------------------------------

    private static Usuario usuario(Long id, TipoUsuario tipo) {
        return Usuario.builder().id(id).nome(tipo.name() + " " + id).tipoUsuario(tipo).build();
    }

    private static LocalTime hora(String hhmm) {
        return LocalTime.parse(hhmm);
    }

    private void disponivel(Usuario docente, int diaSemana, String inicio, String fim) {
        List<Disponibilidade> atuais = new java.util.ArrayList<>(
                disponibilidadeRepository.findByUsuarioIdAndAtivoTrue(docente.getId()));
        atuais.add(Disponibilidade.builder().usuario(docente).diaSemana(diaSemana)
                .horarioInicio(hora(inicio)).horarioFim(hora(fim)).build());
        when(disponibilidadeRepository.findByUsuarioIdAndAtivoTrue(docente.getId())).thenReturn(atuais);
    }

    private void bloqueado(Usuario docente, BloqueioPeriodo bloqueio) {
        when(bloqueioPeriodoRepository.buscarQueImpactamData(eq(docente.getId()), any(), anyInt()))
                .thenReturn(List.of(bloqueio));
    }

    private void reunioes(Usuario usuario, Agendamento... agendamentos) {
        when(agendamentoRepository.buscarAgendamentosAtivosDoUsuarioNaData(eq(usuario.getId()), any(), any()))
                .thenReturn(List.of(agendamentos));
    }

    private static BloqueioPeriodo bloqueio(LocalDate inicio, LocalDate fim, String horaInicio, String horaFim) {
        return BloqueioPeriodo.builder()
                .dataInicio(inicio).dataFim(fim)
                .horarioInicio(horaInicio == null ? null : hora(horaInicio))
                .horarioFim(horaFim == null ? null : hora(horaFim))
                .build();
    }

    private static Agendamento reuniao(LocalDate data, String inicio, int minutos) {
        return Agendamento.builder()
                .dataReuniao(data)
                .horarioInicio(hora(inicio))
                .duracao(DuracaoReuniao.builder().minutos(minutos).build())
                .build();
    }
}
