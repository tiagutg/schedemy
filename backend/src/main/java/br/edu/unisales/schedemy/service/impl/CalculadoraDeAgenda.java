package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.BloqueioPeriodoRepository;
import br.edu.unisales.schedemy.repository.DisponibilidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Responde se um horario esta livre para um usuario. E a unica fonte dessa regra:
 * a validacao ao salvar um agendamento e a sugestao de horarios livres (RF 14) usam
 * esta mesma classe, entao nunca divergem.
 *
 * - Professor/coordenador: o horario precisa caber numa disponibilidade ativa do dia (RN 15),
 *   nao pode cair num bloqueio de periodo (RF 04/RF 05) e nao pode chocar com outra reuniao (RN 01).
 * - Demais usuarios (aluno): apenas nao pode chocar com outra reuniao (RN 01).
 *
 * Os horarios sao tratados em minutos desde 00:00 (0 a 1440) para representar o fim do dia.
 */
@Component
@RequiredArgsConstructor
public class CalculadoraDeAgenda {

    private static final int FIM_DO_DIA = 24 * 60;

    private final DisponibilidadeRepository disponibilidadeRepository;
    private final BloqueioPeriodoRepository bloqueioPeriodoRepository;
    private final AgendamentoRepository agendamentoRepository;

    /** Por que o usuario nao pode participar no horario. */
    public enum TipoImpedimento {
        FORA_DA_DISPONIBILIDADE,
        BLOQUEADO,
        REUNIAO_CONFLITANTE
    }

    /** Impedimento encontrado; {@code conflitante} so e preenchido para REUNIAO_CONFLITANTE. */
    public record Impedimento(Usuario usuario, TipoImpedimento tipo, Agendamento conflitante) {
    }

    /** Agenda de um usuario num dia, carregada uma unica vez e reaproveitada em varias consultas. */
    public record AgendaDoDia(Usuario usuario, List<Intervalo> janelas, List<Intervalo> bloqueios,
                              List<Agendamento> reunioes) {
    }

    record Intervalo(int inicio, int fim) {
        boolean sobrepoe(int outroInicio, int outroFim) {
            return inicio < outroFim && outroInicio < fim;
        }

        boolean contem(int outroInicio, int outroFim) {
            return inicio <= outroInicio && outroFim <= fim;
        }
    }

    public static boolean ehDocente(Usuario usuario) {
        return usuario.getTipoUsuario() == TipoUsuario.PROFESSOR || usuario.getTipoUsuario() == TipoUsuario.COORDENADOR;
    }

    /** Dia da semana no padrao do banco: 1 = domingo ... 7 = sabado. */
    public static int diaSemanaDoBanco(LocalDate data) {
        return data.getDayOfWeek().getValue() % 7 + 1;
    }

    /** Carrega disponibilidades, bloqueios e reunioes ativas do usuario na data. */
    public AgendaDoDia carregar(Usuario usuario, LocalDate data, Long agendamentoIgnoradoId) {
        List<Intervalo> janelas = List.of();
        List<Intervalo> bloqueios = List.of();
        if (ehDocente(usuario)) {
            int diaSemana = diaSemanaDoBanco(data);
            janelas = disponibilidadeRepository.findByUsuarioIdAndAtivoTrue(usuario.getId()).stream()
                    .filter(d -> d.getDiaSemana() != null && d.getDiaSemana() == diaSemana)
                    .map(d -> new Intervalo(minutos(d.getHorarioInicio()), minutos(d.getHorarioFim())))
                    .toList();
            bloqueios = bloqueioPeriodoRepository.buscarQueImpactamData(usuario.getId(), data, diaSemana).stream()
                    .map(CalculadoraDeAgenda::intervaloDoBloqueio)
                    .toList();
        }
        List<Agendamento> reunioes = agendamentoRepository
                .buscarAgendamentosAtivosDoUsuarioNaData(usuario.getId(), data, agendamentoIgnoradoId);
        return new AgendaDoDia(usuario, janelas, bloqueios, reunioes);
    }

    /** Primeiro impedimento do usuario no intervalo, ou vazio se o horario estiver livre. */
    public Optional<Impedimento> verificar(AgendaDoDia agenda, LocalTime inicio, LocalTime fim) {
        return verificar(agenda, minutos(inicio), minutosDoFim(inicio, fim));
    }

    public Optional<Impedimento> verificar(Usuario usuario, LocalDate data, LocalTime inicio, LocalTime fim,
                                           Long agendamentoIgnoradoId) {
        return verificar(carregar(usuario, data, agendamentoIgnoradoId), inicio, fim);
    }

    private Optional<Impedimento> verificar(AgendaDoDia agenda, int inicio, int fim) {
        Usuario usuario = agenda.usuario();
        if (ehDocente(usuario)) {
            // RN 15 - precisa caber inteiro numa disponibilidade ativa do dia.
            if (agenda.janelas().stream().noneMatch(j -> j.contem(inicio, fim))) {
                return Optional.of(new Impedimento(usuario, TipoImpedimento.FORA_DA_DISPONIBILIDADE, null));
            }
            // RF 04/RF 05 - periodo bloqueado pelo docente.
            if (agenda.bloqueios().stream().anyMatch(b -> b.sobrepoe(inicio, fim))) {
                return Optional.of(new Impedimento(usuario, TipoImpedimento.BLOQUEADO, null));
            }
        }
        // RN 01 - sem sobreposicao com outra reuniao ativa.
        return agenda.reunioes().stream()
                .filter(r -> intervaloDaReuniao(r).sobrepoe(inicio, fim))
                .findFirst()
                .map(r -> new Impedimento(usuario, TipoImpedimento.REUNIAO_CONFLITANTE, r));
    }

    /**
     * RF 14 - horarios de inicio (no passo informado) em que TODOS os usuarios estao livres
     * para uma reuniao com a duracao informada. Nao oferece horarios anteriores a {@code naoAntesDe}.
     */
    public List<LocalTime> horariosLivres(List<Usuario> usuarios, LocalDate data, int duracaoMinutos,
                                          int passoMinutos, Long agendamentoIgnoradoId, LocalTime naoAntesDe) {
        if (usuarios.isEmpty() || duracaoMinutos <= 0 || passoMinutos <= 0) {
            return List.of();
        }
        List<AgendaDoDia> agendas = usuarios.stream()
                .map(u -> carregar(u, data, agendamentoIgnoradoId))
                .toList();
        int minimo = naoAntesDe == null ? 0 : arredondarParaCima(minutos(naoAntesDe), passoMinutos);

        List<LocalTime> livres = new ArrayList<>();
        for (int inicio = minimo; inicio + duracaoMinutos <= FIM_DO_DIA; inicio += passoMinutos) {
            int fim = inicio + duracaoMinutos;
            int inicioCandidato = inicio;
            boolean todosLivres = agendas.stream().allMatch(a -> verificar(a, inicioCandidato, fim).isEmpty());
            if (todosLivres) {
                livres.add(LocalTime.of(inicio / 60, inicio % 60));
            }
        }
        return livres;
    }

    /** RF 06 - o bloqueio cobre a reuniao (data, recorrencia e horario)? */
    public boolean bloqueioAfetaReuniao(BloqueioPeriodo bloqueio, Agendamento reuniao) {
        LocalDate data = reuniao.getDataReuniao();
        if (data.isBefore(bloqueio.getDataInicio()) || data.isAfter(bloqueio.getDataFim())) {
            return false;
        }
        if (Boolean.TRUE.equals(bloqueio.getERecorrente())
                && !Integer.valueOf(diaSemanaDoBanco(data)).equals(bloqueio.getDiaSemanaRecorrente())) {
            return false;
        }
        Intervalo r = intervaloDaReuniao(reuniao);
        return intervaloDoBloqueio(bloqueio).sobrepoe(r.inicio(), r.fim());
    }

    /** Bloqueio sem horario cobre o dia inteiro; sem fim, vai ate o fim do dia. */
    private static Intervalo intervaloDoBloqueio(BloqueioPeriodo b) {
        int inicio = b.getHorarioInicio() == null ? 0 : minutos(b.getHorarioInicio());
        int fim = b.getHorarioFim() == null ? FIM_DO_DIA : minutosDoFim(b.getHorarioInicio(), b.getHorarioFim());
        return new Intervalo(inicio, fim);
    }

    private static Intervalo intervaloDaReuniao(Agendamento a) {
        return new Intervalo(minutos(a.getHorarioInicio()), minutosDoFim(a.getHorarioInicio(), a.getHorarioFim()));
    }

    private static int minutos(LocalTime hora) {
        return hora.getHour() * 60 + hora.getMinute();
    }

    /** Um fim "menor ou igual" ao inicio (ex.: 00:00) significa que vai ate o fim do dia. */
    private static int minutosDoFim(LocalTime inicio, LocalTime fim) {
        int m = minutos(fim);
        return inicio != null && m <= minutos(inicio) ? FIM_DO_DIA : m;
    }

    private static int arredondarParaCima(int valor, int passo) {
        return (valor + passo - 1) / passo * passo;
    }
}
