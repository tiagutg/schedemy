package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.security.UsuarioAutenticado;

import br.edu.unisales.schedemy.domain.entity.*;
import br.edu.unisales.schedemy.domain.enums.*;
import br.edu.unisales.schedemy.dto.request.AgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RespostaAgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.HorariosLivresResponseDTO;
import br.edu.unisales.schedemy.exception.AcessoNegadoNegocioException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.AgendamentoMapper;
import br.edu.unisales.schedemy.repository.*;
import br.edu.unisales.schedemy.service.AgendamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementacao das regras de negocio (RN) relacionadas ao ciclo de vida do agendamento,
 * conforme o levantamento de requisitos do Schedemy.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgendamentoServiceImpl implements AgendamentoService {

    private static final int LIMITE_MAX_DIAS_ANTECEDENCIA = 30;   // RN 16
    private static final int LIMITE_MIN_HORAS_CANCELAMENTO = 1;   // RN 05
    private static final int LIMITE_MIN_HORAS_EDICAO = 24;        // RN 14
    private static final int LIMITE_MAX_AGENDAMENTOS_ALUNO_24H = 2; // RN 13
    private static final int LIMITE_MAX_DURACAO_MINUTOS = 60;     // RN 17
    private static final int PASSO_HORARIOS_LIVRES_MINUTOS = 15;  // RF 14

    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DuracaoReuniaoRepository duracaoReuniaoRepository;
    private final SalaVirtualRepository salaVirtualRepository;
    private final RegistroMotivoRepository registroMotivoRepository;
    private final AgendamentoMapper agendamentoMapper;
    private final UsuarioAutenticado usuarioAutenticado;
    private final CalculadoraDeAgenda calculadoraDeAgenda;
    private final Notificador notificador;
    private final NotificacaoRepository notificacaoRepository;

    @Override
    public AgendamentoResponseDTO criar(AgendamentoRequestDTO dto) {
        Usuario organizador = buscarUsuario(dto.idOrganizador());
        // RF 13 - a recepcao agenda em nome de alunos e docentes; os demais perfis, so em nome proprio.
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()
                && !usuarioAutenticado.id().equals(organizador.getId())) {
            throw new AcessoNegadoNegocioException("Voce so pode criar agendamentos em seu proprio nome.");
        }
        DuracaoReuniao duracao = buscarDuracao(dto.idDuracao());

        validarDuracao(duracao);
        validarAntecedenciaMaxima(dto.dataReuniao());

        List<Usuario> convidados = dto.idsConvidados().stream().map(this::buscarUsuario).toList();
        validarComposicaoDeParticipantes(organizador, convidados);

        LocalTime horarioFim = dto.horarioInicio().plusMinutes(duracao.getMinutos());
        validarHorarioLivreParaTodos(organizador, convidados, dto.dataReuniao(), dto.horarioInicio(), horarioFim, null);

        if (organizador.getTipoUsuario() == TipoUsuario.ALUNO) {
            validarLimiteDeAgendamentosDoAluno(organizador.getId());
        }

        Agendamento agendamento = Agendamento.builder()
                .organizador(organizador)
                .duracao(duracao)
                .dataReuniao(dto.dataReuniao())
                .horarioInicio(dto.horarioInicio())
                .formato(dto.formato())
                .topico(dto.topico())
                .resumo(dto.resumo())
                .status(StatusAgendamento.PENDENTE)
                .build();

        if (dto.idAgendamentoOrigem() != null) {
            agendamento.setAgendamentoOrigem(buscarAgendamento(dto.idAgendamentoOrigem())); // RF 28
        }

        // RN 09 - o organizador sempre participa como ORGANIZADOR, ja aceito.
        AgendamentoParticipante organizadorParticipante = AgendamentoParticipante.builder()
                .usuario(organizador)
                .papel(PapelParticipante.ORGANIZADOR)
                .statusResposta(StatusResposta.ACEITO)
                .dataResposta(LocalDateTime.now())
                .build();
        agendamento.adicionarParticipante(organizadorParticipante);

        for (Usuario convidado : convidados) {
            AgendamentoParticipante participante = AgendamentoParticipante.builder()
                    .usuario(convidado)
                    .papel(PapelParticipante.CONVIDADO)
                    .statusResposta(StatusResposta.PENDENTE)
                    .build();
            agendamento.adicionarParticipante(participante);
        }

        agendamento = agendamentoRepository.save(agendamento);

        if (agendamento.getFormato() != FormatoReuniao.PRESENCIAL) {
            criarSalaVirtual(agendamento, dto.linkReuniao()); // RF 27
        }

        notificador.novoAgendamento(agendamento, usuarioAutenticado.obter()); // RF 23
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    @Override
    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(Long id) {
        Agendamento agendamento = buscarAgendamento(id);
        usuarioAutenticado.exigirParticipanteOuGestor(agendamento);
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    @Override
    @Transactional(readOnly = true)
    public HorariosLivresResponseDTO buscarHorariosLivres(LocalDate data, Long idDuracao, List<Long> idsUsuarios,
                                                          Long ignorarAgendamentoId) {
        DuracaoReuniao duracao = buscarDuracao(idDuracao);
        validarDuracao(duracao);
        if (ignorarAgendamentoId != null) {
            usuarioAutenticado.exigirParticipanteOuGestor(buscarAgendamento(ignorarAgendamentoId));
        }
        List<Usuario> usuarios = idsUsuarios.stream().distinct().map(this::buscarUsuario).toList();

        LocalDate hoje = LocalDate.now();
        // RN 16 - fora da janela de agendamento nao ha horario a oferecer.
        boolean foraDoPrazo = data.isBefore(hoje)
                || ChronoUnit.DAYS.between(hoje, data) > LIMITE_MAX_DIAS_ANTECEDENCIA;
        // RN 15 - sem professor/coordenador nao ha disponibilidade cadastrada para basear a sugestao.
        if (foraDoPrazo || usuarios.stream().noneMatch(CalculadoraDeAgenda::ehDocente)) {
            return new HorariosLivresResponseDTO(data, duracao.getMinutos(), List.of());
        }

        // RF 14 - so os horarios livres para TODOS; nunca expoe reunioes ou bloqueios de ninguem.
        LocalTime naoAntesDe = data.equals(hoje) ? LocalTime.now() : null;
        List<LocalTime> horarios = calculadoraDeAgenda.horariosLivres(usuarios, data, duracao.getMinutos(),
                PASSO_HORARIOS_LIVRES_MINUTOS, ignorarAgendamentoId, naoAntesDe);
        return new HorariosLivresResponseDTO(data, duracao.getMinutos(), horarios);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgendamentoResponseDTO> listarPorUsuario(Long usuarioId, List<StatusAgendamento> status,
                                                         LocalDate dataInicio, LocalDate dataFim, String busca,
                                                         Pageable pageable) {
        usuarioAutenticado.exigirProprioOuAdmin(usuarioId,
                "Voce so pode listar os seus proprios agendamentos.");
        buscarUsuario(usuarioId);
        return agendamentoRepository.buscarPorUsuarioComFiltros(usuarioId, statusOuTodos(status), dataInicio, dataFim,
                        termoDeBusca(busca), pageable)
                .map(agendamentoMapper::paraResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgendamentoResponseDTO> listarTodos(List<StatusAgendamento> status, LocalDate dataInicio, LocalDate dataFim,
                                                    String busca, Pageable pageable) {
        // RF 20/RF 21 - a recepcao acompanha todos os agendamentos para poder cancela-los ou altera-los.
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()) {
            throw new AcessoNegadoNegocioException("Somente a recepcao e a administracao podem listar todos os agendamentos.");
        }
        return agendamentoRepository.buscarComFiltros(statusOuTodos(status), dataInicio, dataFim, termoDeBusca(busca), pageable)
                .map(agendamentoMapper::paraResponseDTO);
    }

    /** Sem busca, usa "" (casa com tudo): o PostgreSQL nao aceita LOWER(null) sem tipo. */
    private static String termoDeBusca(String busca) {
        return busca == null ? "" : busca.trim();
    }

    /** Sem filtro de status, considera todos (ex.: "aguardando resposta" = PENDENTE + AGUARDANDO_RESPOSTA). */
    private static List<StatusAgendamento> statusOuTodos(List<StatusAgendamento> status) {
        return status == null || status.isEmpty() ? List.of(StatusAgendamento.values()) : status;
    }

    @Override
    public AgendamentoResponseDTO atualizar(Long id, AgendamentoRequestDTO dto) {
        Agendamento agendamento = buscarAgendamento(id);
        // RF 21 - o organizador ou a recepcao alteram reunioes ainda nao aceitas.
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()
                && !usuarioAutenticado.id().equals(agendamento.getOrganizador().getId())) {
            throw new AcessoNegadoNegocioException("Somente o organizador pode editar este agendamento.");
        }

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new RegraDeNegocioException(
                    "Somente agendamentos com status PENDENTE podem ser editados diretamente. " +
                            "Para reunioes ja confirmadas, utilize a remarcacao.");
        }
        validarAntecedenciaMinima(agendamento.getDataReuniao(), agendamento.getHorarioInicio(),
                LIMITE_MIN_HORAS_EDICAO, "editado"); // RN 14

        DuracaoReuniao duracao = buscarDuracao(dto.idDuracao());
        validarDuracao(duracao);
        validarAntecedenciaMaxima(dto.dataReuniao());

        List<Usuario> convidados = dto.idsConvidados().stream().map(this::buscarUsuario).toList();
        validarComposicaoDeParticipantes(agendamento.getOrganizador(), convidados);

        LocalTime horarioFim = dto.horarioInicio().plusMinutes(duracao.getMinutos());
        validarHorarioLivreParaTodos(agendamento.getOrganizador(), convidados, dto.dataReuniao(), dto.horarioInicio(),
                horarioFim, agendamento.getId());

        agendamento.setDuracao(duracao);
        agendamento.setDataReuniao(dto.dataReuniao());
        agendamento.setHorarioInicio(dto.horarioInicio());
        agendamento.setFormato(dto.formato());
        agendamento.setTopico(dto.topico());
        agendamento.setResumo(dto.resumo());

        // Recria a lista de participantes conforme os novos convidados informados.
        agendamento.getParticipantes().removeIf(p -> p.getPapel() == PapelParticipante.CONVIDADO);
        for (Usuario convidado : convidados) {
            agendamento.adicionarParticipante(AgendamentoParticipante.builder()
                    .usuario(convidado)
                    .papel(PapelParticipante.CONVIDADO)
                    .statusResposta(StatusResposta.PENDENTE)
                    .build());
        }
        sincronizarSalaVirtual(agendamento, dto.linkReuniao());

        agendamento = agendamentoRepository.save(agendamento);
        notificador.alteracao(agendamento, usuarioAutenticado.obter()); // RF 23
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    @Override
    public AgendamentoResponseDTO responder(Long id, RespostaAgendamentoRequestDTO dto) {
        Agendamento agendamento = buscarAgendamento(id);
        Usuario usuario = usuarioAutenticado.obter();
        if (!usuario.getId().equals(dto.idUsuario())) {
            throw new AcessoNegadoNegocioException("Voce so pode responder a um agendamento em seu proprio nome.");
        }

        AgendamentoParticipante participante = agendamento.getParticipantes().stream()
                .filter(p -> p.getUsuario().getId().equals(usuario.getId()))
                .findFirst()
                .orElseThrow(() -> new AcessoNegadoNegocioException(
                        "O usuario informado nao e participante deste agendamento."));

        if (Boolean.FALSE.equals(dto.aceitar()) && (dto.motivo() == null || dto.motivo().isBlank())) {
            throw new RegraDeNegocioException("O motivo e obrigatorio ao recusar um agendamento."); // RF 11
        }

        participante.setStatusResposta(Boolean.TRUE.equals(dto.aceitar()) ? StatusResposta.ACEITO : StatusResposta.RECUSADO);
        participante.setDataResposta(LocalDateTime.now());

        if (Boolean.FALSE.equals(dto.aceitar())) {
            registrarMotivo(agendamento, usuario, TipoOperacaoMotivo.RECUSA_AGENDAMENTO, dto.motivo());
        }

        StatusAgendamento statusAnterior = agendamento.getStatus();
        recalcularStatus(agendamento);
        agendamento = agendamentoRepository.save(agendamento);
        notificador.resposta(agendamento, usuario, Boolean.TRUE.equals(dto.aceitar()), statusAnterior); // RF 23
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    @Override
    public AgendamentoResponseDTO cancelar(Long id, CancelamentoRequestDTO dto) {
        Agendamento agendamento = buscarAgendamento(id);
        Usuario usuario = usuarioAutenticado.obter();
        if (!usuario.getId().equals(dto.idUsuario())) {
            throw new AcessoNegadoNegocioException("Voce so pode cancelar um agendamento em seu proprio nome.");
        }
        // RF 20 - participantes ou a recepcao; o motivo fica registrado em nome de quem cancelou.
        usuarioAutenticado.exigirParticipanteOuGestor(agendamento);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO || agendamento.getStatus() == StatusAgendamento.CONCLUIDO) {
            throw new RegraDeNegocioException("Nao e possivel cancelar um agendamento que ja esta " + agendamento.getStatus() + ".");
        }
        if (!Boolean.TRUE.equals(dto.confirmar())) {
            throw new RegraDeNegocioException("E necessario confirmar explicitamente o cancelamento."); // RNF 10
        }
        validarAntecedenciaMinima(agendamento.getDataReuniao(), agendamento.getHorarioInicio(),
                LIMITE_MIN_HORAS_CANCELAMENTO, "cancelado"); // RN 05

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        registrarMotivo(agendamento, usuario, TipoOperacaoMotivo.CANCELAMENTO, dto.motivo()); // RF 11

        agendamento = agendamentoRepository.save(agendamento);
        // Na remarcacao (RF 28) os envolvidos ja receberam o convite da nova reuniao: nao avisa o cancelamento da antiga.
        if (!agendamentoRepository.existsByAgendamentoOrigemIdAndStatusNot(agendamento.getId(), StatusAgendamento.CANCELADO)) {
            notificador.cancelamento(agendamento, usuario); // RF 23
        }
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    @Override
    public void deletar(Long id) {
        Agendamento agendamento = buscarAgendamento(id);
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE && agendamento.getStatus() != StatusAgendamento.CANCELADO) {
            throw new RegraDeNegocioException(
                    "Somente agendamentos PENDENTES ou CANCELADOS podem ser removidos definitivamente. " +
                            "Para os demais casos, utilize o cancelamento.");
        }
        notificacaoRepository.removerDoAgendamento(agendamento.getId());
        agendamentoRepository.delete(agendamento);
    }

    // ------------------------------------------------------------------
    // Regras de negocio auxiliares
    // ------------------------------------------------------------------

    /** RN 02/RN 03/RN 04 - a reuniao precisa de pelo menos um aluno e um professor/coordenador entre os envolvidos. */
    private void validarComposicaoDeParticipantes(Usuario organizador, List<Usuario> convidados) {
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
    private void validarHorarioLivreParaTodos(Usuario organizador, List<Usuario> convidados, LocalDate data,
                                              LocalTime inicio, LocalTime fim, Long agendamentoIgnoradoId) {
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
    private List<Usuario> envolvidos(Usuario organizador, List<Usuario> convidados) {
        Map<Long, Usuario> porId = new LinkedHashMap<>();
        porId.put(organizador.getId(), organizador);
        convidados.forEach(c -> porId.putIfAbsent(c.getId(), c));
        return List.copyOf(porId.values());
    }

    /** RN 13 - um aluno pode ter no maximo 2 agendamentos ativos criados nas ultimas 24 horas. */
    private void validarLimiteDeAgendamentosDoAluno(Long alunoId) {
        long total = agendamentoRepository.contarAgendamentosDoAlunoDesde(alunoId, LocalDateTime.now().minusHours(24));
        if (total >= LIMITE_MAX_AGENDAMENTOS_ALUNO_24H) {
            throw new RegraDeNegocioException(
                    "Limite de %d agendamentos por periodo de 24 horas atingido para este aluno."
                            .formatted(LIMITE_MAX_AGENDAMENTOS_ALUNO_24H));
        }
    }

    /** RN 16 - a reuniao nao pode ser marcada com mais de 30 dias de antecedencia. */
    private void validarAntecedenciaMaxima(LocalDate dataReuniao) {
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
    private void validarAntecedenciaMinima(LocalDate data, LocalTime horario, int horasMinimas, String acao) {
        LocalDateTime momentoReuniao = LocalDateTime.of(data, horario);
        long horasRestantes = ChronoUnit.HOURS.between(LocalDateTime.now(), momentoReuniao);
        if (horasRestantes < horasMinimas) {
            throw new RegraDeNegocioException(
                    "O agendamento so pode ser %s com pelo menos %d hora(s) de antecedencia.".formatted(acao, horasMinimas));
        }
    }

    /** RN 17 - duracao maxima permitida por reuniao. */
    private void validarDuracao(DuracaoReuniao duracao) {
        if (Boolean.FALSE.equals(duracao.getAtivo())) {
            throw new RegraDeNegocioException("A duracao selecionada nao esta mais ativa no sistema.");
        }
        if (duracao.getMinutos() > LIMITE_MAX_DURACAO_MINUTOS) {
            throw new RegraDeNegocioException(
                    "A duracao maxima permitida por reuniao e de %d minutos.".formatted(LIMITE_MAX_DURACAO_MINUTOS));
        }
    }

    /** RN 18 - confirma a reuniao quando ao menos um docente e um aluno aceitam; cancela se um docente recusa. */
    private void recalcularStatus(Agendamento agendamento) {
        List<AgendamentoParticipante> participantes = agendamento.getParticipantes();

        boolean docenteRecusou = participantes.stream().anyMatch(p ->
                CalculadoraDeAgenda.ehDocente(p.getUsuario()) && p.getStatusResposta() == StatusResposta.RECUSADO);
        if (docenteRecusou) {
            agendamento.setStatus(StatusAgendamento.CANCELADO);
            return;
        }

        boolean temDocenteAceito = participantes.stream().anyMatch(p ->
                CalculadoraDeAgenda.ehDocente(p.getUsuario()) && p.getStatusResposta() == StatusResposta.ACEITO);
        boolean temAlunoAceito = participantes.stream().anyMatch(p ->
                p.getUsuario().getTipoUsuario() == TipoUsuario.ALUNO && p.getStatusResposta() == StatusResposta.ACEITO);

        if (temDocenteAceito && temAlunoAceito) {
            agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        } else {
            agendamento.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
        }
    }

    /**
     * RF 27 - sala virtual para reunioes online/hibridas: usa o link informado pelo organizador
     * (Teams, Meet...) ou gera um automaticamente.
     */
    private void criarSalaVirtual(Agendamento agendamento, String linkInformado) {
        SalaVirtual sala = SalaVirtual.builder()
                .agendamento(agendamento)
                .linkTeams(linkOuSimulado(linkInformado))
                .build();
        salaVirtualRepository.save(sala);
        agendamento.setSalaVirtual(sala);
    }

    /** Mantem a sala virtual coerente com o formato depois de uma edicao. */
    private void sincronizarSalaVirtual(Agendamento agendamento, String linkInformado) {
        SalaVirtual sala = agendamento.getSalaVirtual();
        if (agendamento.getFormato() == FormatoReuniao.PRESENCIAL) {
            agendamento.setSalaVirtual(null); // orphanRemoval apaga a sala
        } else if (sala == null) {
            criarSalaVirtual(agendamento, linkInformado);
        } else if (linkInformado != null && !linkInformado.isBlank()) {
            sala.setLinkTeams(linkInformado.trim());
        }
    }

    private static String linkOuSimulado(String linkInformado) {
        if (linkInformado != null && !linkInformado.isBlank()) {
            return linkInformado.trim();
        }
        return "https://teams.microsoft.com/l/meetup-join/schedemy/%s".formatted(UUID.randomUUID());
    }

    /** RF 11 - toda operacao sensivel (cancelamento, recusa) precisa registrar uma justificativa. */
    private void registrarMotivo(Agendamento agendamento, Usuario usuario, TipoOperacaoMotivo tipo, String justificativa) {
        RegistroMotivo registro = RegistroMotivo.builder()
                .agendamento(agendamento)
                .usuario(usuario)
                .tipoOperacao(tipo)
                .justificativa(justificativa)
                .build();
        registroMotivoRepository.save(registro);
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }

    private DuracaoReuniao buscarDuracao(Long id) {
        return duracaoReuniaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Duracao de reuniao", id));
    }

    private Agendamento buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", id));
    }
}