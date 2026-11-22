package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.repository.DuracaoReuniaoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras que valem para qualquer reuniao proposta: ao criar, editar ou remarcar (AgendamentoServiceImpl) e ao
 * pedir ou aceitar a alteracao de uma reuniao confirmada (SolicitacaoEdicaoServiceImpl, RF 21/22).
 * O horario livre continua sendo da CalculadoraDeAgenda.
 */
@Component
@RequiredArgsConstructor
public class RegrasDeAgendamento {

    static final int LIMITE_MAX_DIAS_ANTECEDENCIA = 30;   // RN 16
    static final int LIMITE_MAX_DURACAO_MINUTOS = 60;     // RN 17

    private final UsuarioRepository usuarioRepository;
    private final DuracaoReuniaoRepository duracaoReuniaoRepository;
    private final CalculadoraDeAgenda calculadoraDeAgenda;

    /** RN 02/RN 03/RN 04 - a reuniao precisa de pelo menos um aluno e um professor/coordenador entre os envolvidos. */
    public void validarComposicaoDeParticipantes(Usuario organizador, List<Usuario> convidados) {
        if (convidados.isEmpty()) {
            throw new RegraDeNegocioException("E necessario informar ao menos um convidado para o agendamento.");
        }
        // RN 03 - recepcao e administracao agendam para outros, mas nao participam da reuniao.
        for (Usuario envolvido : envolvidos(organizador, convidados)) {
            if (envolvido.getTipoUsuario() != TipoUsuario.ALUNO && !CalculadoraDeAgenda.ehDocente(envolvido)) {
                throw new RegraDeNegocioException(
                        "O usuario '%s' nao pode participar de reunioes: apenas alunos, professores e coordenadores."
                                .formatted(envolvido.getNome()));
            }
        }
        boolean temAluno = organizador.getTipoUsuario() == TipoUsuario.ALUNO
                || convidados.stream().anyMatch(u -> u.getTipoUsuario() == TipoUsuario.ALUNO);
        boolean temDocente = organizador.getTipoUsuario() == TipoUsuario.PROFESSOR
                || organizador.getTipoUsuario() == TipoUsuario.COORDENADOR
                || convidados.stream().anyMatch(u -> u.getTipoUsuario() == TipoUsuario.PROFESSOR
                || u.getTipoUsuario() == TipoUsuario.COORDENADOR);
        if (!temAluno || !temDocente) {
            throw new RegraDeNegocioException(
                    "O agendamento deve envolver pelo menos um ALUNO e um PROFESSOR/COORDENADOR.");
        }
    }

    /**
     * RN 01/RN 15/RF 04/RF 14 - o horario precisa estar livre para TODOS os envolvidos (organizador e convidados):
     * docentes dentro da disponibilidade e fora de bloqueios; ninguem com outra reuniao no intervalo.
     * Usa a mesma CalculadoraDeAgenda da sugestao de horarios livres.
     */
    public void validarHorarioLivreParaTodos(Usuario organizador, List<Usuario> convidados, LocalDate data,
                                              LocalTime inicio, LocalTime fim, Long agendamentoIgnoradoId) {
        // RN 01 - sem a trava, duas requisicoes simultaneas passariam pela checagem antes de qualquer uma gravar.
        usuarioRepository.travarParaAgendamento(envolvidos(organizador, convidados).stream().map(Usuario::getId).toList());
        for (Usuario envolvido : envolvidos(organizador, convidados)) {
            calculadoraDeAgenda.verificar(envolvido, data, inicio, fim, agendamentoIgnoradoId)
                    .ifPresent(impedimento -> {
                        if (impedimento.tipo() == CalculadoraDeAgenda.TipoImpedimento.REUNIAO_CONFLITANTE) {
                            Agendamento existente = impedimento.conflitante();
                            throw new RegraDeNegocioException(
                                    "O usuario '%s' ja possui um agendamento conflitante em %s entre %s e %s."
                                            .formatted(envolvido.getNome(), data, existente.getHorarioInicio(),
                                                    existente.getHorarioFim()));
                        }
                        // Indisponibilidade ou bloqueio: o motivo do bloqueio nao e revelado.
                        throw new RegraDeNegocioException("O %s %s nao esta disponivel neste horario."
                                .formatted(envolvido.getTipoUsuario().name().toLowerCase(), envolvido.getNome()));
                    });
        }
    }

    /** Organizador seguido dos convidados, sem repetir usuarios. */
    public static List<Usuario> envolvidos(Usuario organizador, List<Usuario> convidados) {
        Map<Long, Usuario> porId = new LinkedHashMap<>();
        porId.put(organizador.getId(), organizador);
        convidados.forEach(c -> porId.putIfAbsent(c.getId(), c));
        return List.copyOf(porId.values());
    }

    /** RN 16 - a reuniao nao pode ser marcada com mais de 30 dias de antecedencia. */
    public void validarAntecedenciaMaxima(LocalDate dataReuniao) {
        long dias = ChronoUnit.DAYS.between(LocalDate.now(), dataReuniao);
        if (dias < 0) {
            throw new RegraDeNegocioException("A data da reuniao nao pode estar no passado.");
        }
        if (dias > LIMITE_MAX_DIAS_ANTECEDENCIA) {
            throw new RegraDeNegocioException(
                    "Agendamentos so podem ser feitos com ate %d dias de antecedencia.".formatted(LIMITE_MAX_DIAS_ANTECEDENCIA));
        }
    }

    /** RN 05/RN 14 - exige antecedencia minima para cancelar ou editar um agendamento. */
    public void validarAntecedenciaMinima(LocalDate data, LocalTime horario, int horasMinimas, String acao) {
        LocalDateTime momentoReuniao = LocalDateTime.of(data, horario);
        long horasRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), momentoReuniao);
        if (horasRestantes < horasMinimas) {
            throw new RegraDeNegocioException(
                    "O agendamento so pode ser %s com pelo menos %d hora(s) de antecedencia.".formatted(acao, horasMinimas));
        }
    }

    /** RN 17 - duracao maxima permitida por reuniao. */
    public void validarDuracao(DuracaoReuniao duracao) {
        if (Boolean.FALSE.equals(duracao.getAtivo())) {
            throw new RegraDeNegocioException("A duracao selecionada nao esta mais ativa no sistema.");
        }
        if (duracao.getMinutos() > LIMITE_MAX_DURACAO_MINUTOS) {
            throw new RegraDeNegocioException(
                    "A duracao maxima permitida por reuniao e de %d minutos.".formatted(LIMITE_MAX_DURACAO_MINUTOS));
        }
    }

    /** O horario de inicio nao pode estar no passado (a validacao de data sozinha aceitava "hoje, 08:00" as 15:00). */
    public void validarInicioNoFuturo(LocalDate data, LocalTime horario) {
        if (!LocalDateTime.of(data, horario).isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException("O horario de inicio da reuniao ja passou.");
        }
    }

    public Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }

    /** Organizador e convidados precisam estar ativos: um usuario inativo nao consegue entrar para responder. */
    public Usuario buscarUsuarioAtivo(Long id) {
        Usuario usuario = buscarUsuario(id);
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException(
                    "O usuario '%s' esta inativo e nao pode participar de reunioes.".formatted(usuario.getNome()));
        }
        return usuario;
    }

    public DuracaoReuniao buscarDuracao(Long id) {
        return duracaoReuniaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Duracao de reuniao", id));
    }
}
