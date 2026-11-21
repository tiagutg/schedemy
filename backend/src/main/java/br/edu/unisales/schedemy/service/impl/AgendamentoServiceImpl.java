package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.security.UsuarioAutenticado;

import br.edu.unisales.schedemy.domain.entity.*;
import br.edu.unisales.schedemy.domain.enums.*;
import br.edu.unisales.schedemy.dto.request.AgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RemarcacaoRequestDTO;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementacao das regras de negocio (RN) relacionadas ao ciclo de vida do agendamento,
 * conforme o levantamento de requisitos do Schedemy.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgendamentoServiceImpl implements AgendamentoService {

    private static final int LIMITE_MIN_HORAS_CANCELAMENTO = 1;   // RN 05
    private static final int LIMITE_MIN_HORAS_EDICAO = 24;        // RN 14
    private static final int LIMITE_MAX_AGENDAMENTOS_ALUNO_24H = 2; // RN 13
    private static final int PASSO_HORARIOS_LIVRES_MINUTOS = 15;  // RF 14
    /** Depois de confirmada, a reuniao ainda recebe respostas dos convidados que nao responderam (RN 18). */
    private static final Set<StatusAgendamento> STATUS_QUE_ACEITAM_RESPOSTA = EnumSet.of(
            StatusAgendamento.PENDENTE, StatusAgendamento.AGUARDANDO_RESPOSTA, StatusAgendamento.CONFIRMADO);
    /** Remarcacao - reunioes ainda nao iniciadas e nao canceladas. */
    private static final Set<StatusAgendamento> STATUS_QUE_PODEM_SER_REMARCADOS = STATUS_QUE_ACEITAM_RESPOSTA;
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_BR = DateTimeFormatter.ofPattern("HH:mm");

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
    private final RegrasDeAgendamento regras;
    private final SolicitacaoEdicaoRepository solicitacaoEdicaoRepository;
    private final Auditoria auditoria;

    /**
     * RN 01 - READ COMMITTED (padrao do PostgreSQL; o MySQL usa REPEATABLE READ): depois de esperar a trava dos
     * envolvidos, a checagem de conflito precisa enxergar a reuniao que a outra transacao acabou de gravar.
     * Em REPEATABLE READ o MySQL continuaria lendo a "foto" tirada na primeira consulta da transacao.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgendamentoResponseDTO criar(AgendamentoRequestDTO dto) {
        Agendamento criado = novoAgendamento(dto, null);
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_CRIADO, criado.getId(), Auditoria.descrever(criado)); // RNF 19
        return agendamentoMapper.paraResponseDTO(criado);
    }

    /**
     * Regras de criacao (compartilhadas por criar e remarcar).
     * @param agendamentoIgnoradoId reuniao que nao conta como conflito (a original, na remarcacao)
     */
    private Agendamento novoAgendamento(AgendamentoRequestDTO dto, Long agendamentoIgnoradoId) {
        Usuario organizador = buscarUsuarioAtivo(dto.idOrganizador());
        // RF 13 - a recepcao agenda em nome de alunos e docentes; os demais perfis, so em nome proprio.
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()
                && !usuarioAutenticado.id().equals(organizador.getId())) {
            throw new AcessoNegadoNegocioException("Voce so pode criar agendamentos em seu proprio nome.");
        }
        DuracaoReuniao duracao = buscarDuracao(dto.idDuracao());

        validarDuracao(duracao);
        validarAntecedenciaMaxima(dto.dataReuniao());
        validarInicioNoFuturo(dto.dataReuniao(), dto.horarioInicio());

        List<Usuario> convidados = dto.idsConvidados().stream().map(this::buscarUsuarioAtivo).toList();
        validarComposicaoDeParticipantes(organizador, convidados);

        LocalTime horarioFim = dto.horarioInicio().plusMinutes(duracao.getMinutos());
        validarHorarioLivreParaTodos(organizador, convidados, dto.dataReuniao(), dto.horarioInicio(), horarioFim,
                agendamentoIgnoradoId);

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
            agendamento.setAgendamentoOrigem(buscarAgendamento(dto.idAgendamentoOrigem())); // remarcacao
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
        return agendamento;
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
        List<Usuario> usuarios = idsUsuarios.stream().distinct().map(this::buscarUsuarioAtivo).toList();

        LocalDate hoje = LocalDate.now();
        // RN 16 - fora da janela de agendamento nao ha horario a oferecer.
        boolean foraDoPrazo = data.isBefore(hoje)
                || ChronoUnit.DAYS.between(hoje, data) > RegrasDeAgendamento.LIMITE_MAX_DIAS_ANTECEDENCIA;
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
    @Transactional(isolation = Isolation.READ_COMMITTED) // RN 01 - ver criar()
    public AgendamentoResponseDTO atualizar(Long id, AgendamentoRequestDTO dto) {
        Agendamento agendamento = travarAgendamento(id);
        // RF 21 - o organizador ou a recepcao alteram reunioes ainda nao aceitas.
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()
                && !usuarioAutenticado.id().equals(agendamento.getOrganizador().getId())) {
            throw new AcessoNegadoNegocioException("Somente o organizador pode editar este agendamento.");
        }

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new RegraDeNegocioException(
                    "Somente agendamentos com status PENDENTE podem ser editados diretamente. " +
                            "Para reunioes ja confirmadas, peca a alteracao (o docente aceita ou recusa).");
        }
        validarAntecedenciaMinima(agendamento.getDataReuniao(), agendamento.getHorarioInicio(),
                LIMITE_MIN_HORAS_EDICAO, "editado"); // RN 14

        DuracaoReuniao duracao = buscarDuracao(dto.idDuracao());
        validarDuracao(duracao);
        validarAntecedenciaMaxima(dto.dataReuniao());
        validarInicioNoFuturo(dto.dataReuniao(), dto.horarioInicio());

        List<Usuario> convidados = dto.idsConvidados().stream().map(this::buscarUsuarioAtivo).toList();
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
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_EDITADO, agendamento.getId(), Auditoria.descrever(agendamento)); // RNF 19
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

        // RF 19 - so convidados respondem; o organizador ja entra como ACEITO (RN 09) e desiste pelo cancelamento (RN 05).
        if (participante.getPapel() == PapelParticipante.ORGANIZADOR) {
            throw new RegraDeNegocioException(
                    "O organizador nao responde ao proprio convite. Para desistir da reuniao, cancele o agendamento.");
        }
        // Reuniao cancelada, em andamento ou concluida nao pode voltar a mudar de status por uma resposta.
        if (!STATUS_QUE_ACEITAM_RESPOSTA.contains(agendamento.getStatus())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel responder a um agendamento com status " + agendamento.getStatus() + ".");
        }
        if (!LocalDateTime.of(agendamento.getDataReuniao(), agendamento.getHorarioInicio()).isAfter(LocalDateTime.now())) {
            throw new RegraDeNegocioException("O horario desta reuniao ja passou; nao e mais possivel responder ao convite.");
        }

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
        // RF 21 - com pedido de alteracao pendente, a reuniao continua "Aguardando resposta" ate o docente decidir;
        // se a resposta cancelar a reuniao (RN 18), o pedido deixa de valer.
        if (statusAnterior == StatusAgendamento.AGUARDANDO_RESPOSTA) {
            if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
                encerrarPedidosDeAlteracao(agendamento);
            } else {
                agendamento.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
            }
        }
        agendamento = agendamentoRepository.save(agendamento);
        notificador.resposta(agendamento, usuario, Boolean.TRUE.equals(dto.aceitar()), statusAnterior); // RF 23
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_RESPONDIDO, agendamento.getId(), // RNF 19
                (Boolean.TRUE.equals(dto.aceitar()) ? "Aceitou: " : "Recusou: ") + Auditoria.descrever(agendamento));
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    @Override
    public AgendamentoResponseDTO cancelar(Long id, CancelamentoRequestDTO dto) {
        Agendamento agendamento = travarAgendamento(id);
        Usuario usuario = usuarioAutenticado.obter();
        if (!usuario.getId().equals(dto.idUsuario())) {
            throw new AcessoNegadoNegocioException("Voce so pode cancelar um agendamento em seu proprio nome.");
        }
        // RF 20 - participantes ou a recepcao; o motivo fica registrado em nome de quem cancelou.
        usuarioAutenticado.exigirParticipanteOuGestor(agendamento);

        validarCancelamento(agendamento, dto.confirmar(), "cancelado");

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        registrarMotivo(agendamento, usuario, TipoOperacaoMotivo.CANCELAMENTO, dto.motivo()); // RF 11
        encerrarPedidosDeAlteracao(agendamento);

        agendamento = agendamentoRepository.save(agendamento);
        // Na remarcacao os envolvidos ja receberam o convite da nova reuniao: nao avisa o cancelamento da antiga.
        if (!agendamentoRepository.existsByAgendamentoOrigemIdAndStatusNot(agendamento.getId(), StatusAgendamento.CANCELADO)) {
            notificador.cancelamento(agendamento, usuario); // RF 23
        }
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_CANCELADO, agendamento.getId(), // RNF 19
                Auditoria.descrever(agendamento) + " - motivo: " + dto.motivo());
        return agendamentoMapper.paraResponseDTO(agendamento);
    }

    /**
     * Remarcacao atomica (base do RF 08/09): cancela a reuniao original (com o motivo - RF 11) e cria a nova ligada a ela.
     * Tudo na mesma transacao: se a nova reuniao for recusada por qualquer regra, a original continua como estava.
     * READ COMMITTED pelo mesmo motivo de criar() (RN 01).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgendamentoResponseDTO remarcar(Long id, RemarcacaoRequestDTO dto) {
        Agendamento original = travarAgendamento(id);
        // Como na edicao: o organizador remarca; a recepcao e a administracao, por ele.
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()
                && !usuarioAutenticado.id().equals(original.getOrganizador().getId())) {
            throw new AcessoNegadoNegocioException("Somente o organizador pode remarcar este agendamento.");
        }
        if (!STATUS_QUE_PODEM_SER_REMARCADOS.contains(original.getStatus())) {
            throw new RegraDeNegocioException("Nao e possivel remarcar um agendamento que esta " + original.getStatus() + ".");
        }
        validarCancelamento(original, dto.confirmar(), "remarcado");

        original.setStatus(StatusAgendamento.CANCELADO);
        encerrarPedidosDeAlteracao(original);
        registrarMotivo(original, usuarioAutenticado.obter(), TipoOperacaoMotivo.CANCELAMENTO,
                "Remarcada para %s às %s: %s".formatted(dto.dataReuniao().format(DATA_BR),
                        dto.horarioInicio().format(HORA_BR), dto.motivo().trim()));
        agendamentoRepository.save(original);

        AgendamentoRequestDTO dadosNovos = new AgendamentoRequestDTO(original.getOrganizador().getId(),
                dto.idsConvidados(), dto.idDuracao(), dto.dataReuniao(), dto.horarioInicio(), dto.formato(),
                dto.topico(), dto.resumo(), original.getId(), dto.linkReuniao());
        // A original deixa de existir para a checagem de conflito: a nova pode ocupar o mesmo horario (ou parte dele).
        // Os envolvidos recebem o aviso de remarcacao da nova reuniao, nao o de cancelamento da original.
        Agendamento nova = novoAgendamento(dadosNovos, original.getId());
        // RNF 19 - a original (cancelada) aponta para a nova, e a nova nasce como remarcacao da original.
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_REMARCADO, original.getId(),
                "Remarcada para a reuniao %d (%s) - motivo: %s".formatted(nova.getId(), Auditoria.descrever(nova), dto.motivo()));
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_CRIADO, nova.getId(),
                "Remarcacao da reuniao %d: %s".formatted(original.getId(), Auditoria.descrever(nova)));
        return agendamentoMapper.paraResponseDTO(nova);
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
        // RNF 19 - o registro some, a trilha fica (id_tabela_afetada nao tem FK).
        auditoria.registrar(AcaoAuditoria.AGENDAMENTO_REMOVIDO, agendamento.getId(), Auditoria.descrever(agendamento));
        agendamentoRepository.delete(agendamento);
    }

    // ------------------------------------------------------------------
    // Regras de negocio auxiliares
    // ------------------------------------------------------------------

    /** Regras comuns ao cancelamento e a remarcacao (que cancela a reuniao original). */
    private void validarCancelamento(Agendamento agendamento, Boolean confirmar, String acao) {
        if (agendamento.getStatus() == StatusAgendamento.CANCELADO || agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                || agendamento.getStatus() == StatusAgendamento.EM_ANDAMENTO) {
            throw new RegraDeNegocioException("Nao e possivel cancelar um agendamento que ja esta " + agendamento.getStatus() + ".");
        }
        if (!Boolean.TRUE.equals(confirmar)) {
            throw new RegraDeNegocioException("E necessario confirmar explicitamente o cancelamento."); // RNF 10
        }
        validarAntecedenciaMinima(agendamento.getDataReuniao(), agendamento.getHorarioInicio(),
                LIMITE_MIN_HORAS_CANCELAMENTO, acao); // RN 05
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

    private void recalcularStatus(Agendamento agendamento) {
        agendamento.setStatus(statusPelasRespostas(agendamento.getParticipantes()));
    }

    /**
     * RN 18 (Documento de Requisitos v2.7.0) - a reuniao acontece quando pelo menos um professor/coordenador
     * e pelo menos um aluno aceitam. A recusa de parte dos alunos (ou de parte dos docentes) nao impede a
     * reuniao; ela so e cancelada quando TODOS os docentes ou TODOS os alunos recusam.
     *
     * RNF 13 - enquanto nao confirma nem cancela, continua PENDENTE (decisao de 12/11/2026). As respostas
     * nao levam mais a AGUARDANDO_RESPOSTA: esse status fica para a solicitacao de edicao (RF 21) e a
     * sugestao de remarcacao (RF 08) esperando resposta. Assim a edicao direta (RN 14) segue valendo ate
     * a reuniao ser confirmada.
     */
    static StatusAgendamento statusPelasRespostas(List<AgendamentoParticipante> participantes) {
        List<AgendamentoParticipante> docentes = participantes.stream()
                .filter(p -> CalculadoraDeAgenda.ehDocente(p.getUsuario())).toList();
        List<AgendamentoParticipante> alunos = participantes.stream()
                .filter(p -> p.getUsuario().getTipoUsuario() == TipoUsuario.ALUNO).toList();

        if (todosRecusaram(docentes) || todosRecusaram(alunos)) {
            return StatusAgendamento.CANCELADO;
        }
        boolean temDocenteAceito = docentes.stream().anyMatch(p -> p.getStatusResposta() == StatusResposta.ACEITO);
        boolean temAlunoAceito = alunos.stream().anyMatch(p -> p.getStatusResposta() == StatusResposta.ACEITO);
        return temDocenteAceito && temAlunoAceito ? StatusAgendamento.CONFIRMADO : StatusAgendamento.PENDENTE;
    }

    private static boolean todosRecusaram(List<AgendamentoParticipante> grupo) {
        return !grupo.isEmpty() && grupo.stream().allMatch(p -> p.getStatusResposta() == StatusResposta.RECUSADO);
    }

    /**
     * RF 27 - sala virtual para reunioes online/hibridas com o link informado pelo organizador (Teams, Meet...).
     * Ate existir a integracao com o Microsoft Teams, sem link informado nao ha sala: antes era gerado um
     * endereco do Teams que nao existe e era mostrado aos participantes como se fosse a sala real.
     */
    private void criarSalaVirtual(Agendamento agendamento, String linkInformado) {
        if (linkInformado == null || linkInformado.isBlank()) {
            return;
        }
        SalaVirtual sala = SalaVirtual.builder()
                .agendamento(agendamento)
                .linkTeams(linkInformado.trim())
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

    /** RF 21 - reuniao cancelada (ou remarcada): o pedido de alteracao pendente deixa de valer. */
    private void encerrarPedidosDeAlteracao(Agendamento agendamento) {
        solicitacaoEdicaoRepository.encerrarPendentes(agendamento.getId(), StatusSolicitacao.CANCELADA, LocalDateTime.now());
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

    // ------------------------------------------------------------------
    // Regras comuns ao agendamento e ao pedido de alteracao (RF 21): RegrasDeAgendamento
    // ------------------------------------------------------------------

    private void validarComposicaoDeParticipantes(Usuario organizador, List<Usuario> convidados) {
        regras.validarComposicaoDeParticipantes(organizador, convidados);
    }

    private void validarHorarioLivreParaTodos(Usuario organizador, List<Usuario> convidados, LocalDate data,
                                              LocalTime inicio, LocalTime fim, Long agendamentoIgnoradoId) {
        regras.validarHorarioLivreParaTodos(organizador, convidados, data, inicio, fim, agendamentoIgnoradoId);
    }

    private List<Usuario> envolvidos(Usuario organizador, List<Usuario> convidados) {
        return RegrasDeAgendamento.envolvidos(organizador, convidados);
    }

    private void validarAntecedenciaMaxima(LocalDate dataReuniao) {
        regras.validarAntecedenciaMaxima(dataReuniao);
    }

    private void validarAntecedenciaMinima(LocalDate data, LocalTime horario, int horasMinimas, String acao) {
        regras.validarAntecedenciaMinima(data, horario, horasMinimas, acao);
    }

    private void validarDuracao(DuracaoReuniao duracao) {
        regras.validarDuracao(duracao);
    }

    private void validarInicioNoFuturo(LocalDate data, LocalTime horario) {
        regras.validarInicioNoFuturo(data, horario);
    }

    private Usuario buscarUsuario(Long id) {
        return regras.buscarUsuario(id);
    }

    private Usuario buscarUsuarioAtivo(Long id) {
        return regras.buscarUsuarioAtivo(id);
    }

    private DuracaoReuniao buscarDuracao(Long id) {
        return regras.buscarDuracao(id);
    }

    /**
     * Trava e so depois le o agendamento (ver AgendamentoRepository.travarPorId): a leitura vem depois da espera,
     * entao enxerga o que a outra transacao gravou.
     */
    private Agendamento travarAgendamento(Long id) {
        agendamentoRepository.travarPorId(id);
        return buscarAgendamento(id);
    }

    private Agendamento buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", id));
    }
}