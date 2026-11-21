package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.SolicitacaoEdicao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.RespostaSolicitacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.SolicitacaoEdicaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.SolicitacaoEdicaoResponseDTO;
import br.edu.unisales.schedemy.exception.AcessoNegadoNegocioException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.SolicitacaoEdicaoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.RegistroMotivoRepository;
import br.edu.unisales.schedemy.repository.SolicitacaoEdicaoRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import br.edu.unisales.schedemy.service.SolicitacaoEdicaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF 21/22, RN 12 - pedido de alteracao de reuniao ja confirmada (decisoes de 21/11/2026):
 * - pede: aluno participante, ou a recepcao/administracao em nome dele; o docente que quiser mudar cancela e
 *   usa o "Agendar de novo";
 * - so reuniao CONFIRMADO, com pelo menos 24 h ate o horario original (RN 14), e um pedido por vez. A reuniao
 *   fica AGUARDANDO_RESPOSTA e continua valendo no horario original;
 * - decide: o primeiro docente participante que responder (ou a administracao). Aceito, a reuniao passa a ter os
 *   dados novos (as regras de agendamento sao conferidas de novo); recusado, com motivo, continua como estava;
 * - sem resposta ate 24 h antes do horario original, o pedido expira (RotinaDeAgenda).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SolicitacaoEdicaoServiceImpl implements SolicitacaoEdicaoService {

    /** RN 14 - prazo do pedido e da resposta, contado ate o horario original. */
    static final int HORAS_MINIMAS = 24;

    private final SolicitacaoEdicaoRepository solicitacaoEdicaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final RegistroMotivoRepository registroMotivoRepository;
    private final RegrasDeAgendamento regras;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Notificador notificador;
    private final SolicitacaoEdicaoMapper mapper;

    /** RN 01 - READ COMMITTED como no agendamento: a checagem de horario livre enxerga o que acabou de ser gravado. */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SolicitacaoEdicaoResponseDTO solicitar(Long idAgendamento, SolicitacaoEdicaoRequestDTO dto) {
        Agendamento agendamento = travarAgendamento(idAgendamento);
        Usuario solicitante = usuarioAutenticado.obter();
        boolean gestor = usuarioAutenticado.podeGerirAgendamentosDeTerceiros();
        boolean alunoParticipante = solicitante.getTipoUsuario() == TipoUsuario.ALUNO && participa(agendamento, solicitante);
        if (!gestor && !alunoParticipante) {
            throw new AcessoNegadoNegocioException(
                    "Somente alunos participantes da reuniao ou a recepcao podem pedir a alteracao.");
        }

        switch (agendamento.getStatus()) {
            case CONFIRMADO -> { }
            case PENDENTE -> throw new RegraDeNegocioException(
                    "Esta reuniao ainda nao foi confirmada: edite o agendamento diretamente.");
            case AGUARDANDO_RESPOSTA -> throw new RegraDeNegocioException(
                    "Ja existe um pedido de alteracao aguardando a resposta do docente.");
            default -> throw new RegraDeNegocioException(
                    "Nao e possivel pedir alteracao de uma reuniao com status " + agendamento.getStatus() + ".");
        }
        if (solicitacaoEdicaoRepository.existsByAgendamentoIdAndStatus(agendamento.getId(), StatusSolicitacao.PENDENTE)) {
            throw new RegraDeNegocioException("Ja existe um pedido de alteracao aguardando a resposta do docente.");
        }
        regras.validarAntecedenciaMinima(agendamento.getDataReuniao(), agendamento.getHorarioInicio(),
                HORAS_MINIMAS, "alterado"); // RN 14

        Proposta proposta = validarProposta(agendamento, dto.idsConvidados(), dto.idDuracao(), dto);
        if (nadaMuda(agendamento, proposta, dto)) {
            throw new RegraDeNegocioException("O pedido nao muda nada na reuniao.");
        }

        SolicitacaoEdicao solicitacao = SolicitacaoEdicao.builder()
                .agendamento(agendamento)
                .solicitante(solicitante)
                .novaDuracao(proposta.duracao())
                .novaData(dto.dataReuniao())
                .novoHorarioInicio(dto.horarioInicio())
                .novoFormato(dto.formato())
                .novoTopico(dto.topico().trim())
                .novoResumo(dto.resumo())
                .motivo(dto.motivo().trim())
                .novosConvidados(new LinkedHashSet<>(proposta.convidados()))
                .build();
        solicitacao = solicitacaoEdicaoRepository.save(solicitacao);

        // RN 10/RNF 13 - "Aguardando resposta": a reuniao continua valendo no horario original.
        agendamento.setStatus(StatusAgendamento.AGUARDANDO_RESPOSTA);
        agendamentoRepository.save(agendamento);
        registrar(agendamento, solicitante, TipoOperacaoMotivo.SOLICITACAO_EDICAO, dto.motivo().trim()); // RF 11
        notificador.pedidoDeAlteracao(agendamento, solicitante,
                Notificador.quando(dto.dataReuniao(), dto.horarioInicio())); // RF 23
        return mapper.paraResponseDTO(solicitacao);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitacaoEdicaoResponseDTO> listarDoAgendamento(Long idAgendamento) {
        Agendamento agendamento = agendamentoRepository.findById(idAgendamento)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", idAgendamento));
        usuarioAutenticado.exigirParticipanteOuGestor(agendamento); // RNF 15
        return solicitacaoEdicaoRepository.findByAgendamentoIdOrderByCriadoEmDesc(idAgendamento).stream()
                .map(mapper::paraResponseDTO)
                .toList();
    }

    /** RF 22/RN 12 - o primeiro docente que responder decide. READ COMMITTED pelo mesmo motivo de solicitar(). */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SolicitacaoEdicaoResponseDTO responder(Long idSolicitacao, RespostaSolicitacaoRequestDTO dto) {
        SolicitacaoEdicao solicitacao = travarSolicitacao(idSolicitacao);
        Agendamento agendamento = solicitacao.getAgendamento();
        Usuario docente = usuarioAutenticado.obter();
        if (!usuarioAutenticado.ehAdmin() && !(CalculadoraDeAgenda.ehDocente(docente) && participa(agendamento, docente))) {
            throw new AcessoNegadoNegocioException(
                    "Somente os professores e coordenadores da reuniao podem responder ao pedido de alteracao.");
        }
        exigirPendente(solicitacao);
        if (!LocalDateTime.of(agendamento.getDataReuniao(), agendamento.getHorarioInicio())
                .isAfter(LocalDateTime.now().plusHours(HORAS_MINIMAS))) {
            throw new RegraDeNegocioException(
                    "O prazo para responder terminou: faltam menos de %d horas para a reuniao.".formatted(HORAS_MINIMAS));
        }

        if (Boolean.TRUE.equals(dto.aceitar())) {
            aplicar(solicitacao, docente);
        } else {
            String motivo = dto.motivo() == null ? "" : dto.motivo().trim();
            if (motivo.isEmpty()) {
                throw new RegraDeNegocioException("O motivo e obrigatorio ao recusar o pedido de alteracao."); // RF 11
            }
            solicitacao.setStatus(StatusSolicitacao.RECUSADA);
            solicitacao.setRespondidoEm(LocalDateTime.now());
            agendamento.setStatus(AgendamentoServiceImpl.statusPelasRespostas(agendamento.getParticipantes()));
            agendamentoRepository.save(agendamento);
            registrar(agendamento, docente, TipoOperacaoMotivo.RECUSA_SOLICITACAO_EDICAO, motivo);
            notificador.alteracaoRecusada(agendamento, solicitacao.getSolicitante(), docente, motivo); // RF 23
        }
        return mapper.paraResponseDTO(solicitacaoEdicaoRepository.save(solicitacao));
    }

    /** Quem pediu (ou a recepcao/administracao) desiste; a reuniao continua como estava. */
    @Override
    public SolicitacaoEdicaoResponseDTO cancelar(Long idSolicitacao) {
        SolicitacaoEdicao solicitacao = travarSolicitacao(idSolicitacao);
        Usuario usuario = usuarioAutenticado.obter();
        if (!usuarioAutenticado.podeGerirAgendamentosDeTerceiros()
                && !usuario.getId().equals(solicitacao.getSolicitante().getId())) {
            throw new AcessoNegadoNegocioException("Somente quem fez o pedido pode desistir dele.");
        }
        exigirPendente(solicitacao);
        Agendamento agendamento = solicitacao.getAgendamento();
        solicitacao.setStatus(StatusSolicitacao.CANCELADA);
        solicitacao.setRespondidoEm(LocalDateTime.now());
        agendamento.setStatus(AgendamentoServiceImpl.statusPelasRespostas(agendamento.getParticipantes()));
        agendamentoRepository.save(agendamento);
        notificador.pedidoEncerrado(agendamento, solicitacao.getSolicitante(), usuario, false); // RF 23
        return mapper.paraResponseDTO(solicitacaoEdicaoRepository.save(solicitacao));
    }

    // ------------------------------------------------------------------

    private record Proposta(DuracaoReuniao duracao, List<Usuario> convidados, LocalTime horarioFim) {
    }

    /**
     * As mesmas regras de criar/editar (RN 01, 02, 03, 15, 16, 17 e inicio no futuro), conferidas ao pedir e de
     * novo ao aceitar. O organizador nao muda; a propria reuniao nao conta como conflito.
     */
    private Proposta validarProposta(Agendamento agendamento, List<Long> idsConvidados, Long idDuracao,
                                     SolicitacaoEdicaoRequestDTO dto) {
        return validarProposta(agendamento, idsConvidados, regras.buscarDuracao(idDuracao),
                dto.dataReuniao(), dto.horarioInicio());
    }

    private Proposta validarProposta(Agendamento agendamento, List<Long> idsConvidados, DuracaoReuniao duracao,
                                     java.time.LocalDate data, LocalTime inicio) {
        regras.validarDuracao(duracao);
        regras.validarAntecedenciaMaxima(data);
        regras.validarInicioNoFuturo(data, inicio);
        Usuario organizador = regras.buscarUsuarioAtivo(agendamento.getOrganizador().getId());
        List<Usuario> convidados = idsConvidados.stream()
                .distinct()
                .filter(id -> !id.equals(organizador.getId()))
                .map(regras::buscarUsuarioAtivo)
                .toList();
        regras.validarComposicaoDeParticipantes(organizador, convidados);
        LocalTime fim = inicio.plusMinutes(duracao.getMinutos());
        regras.validarHorarioLivreParaTodos(organizador, convidados, data, inicio, fim, agendamento.getId());
        return new Proposta(duracao, convidados, fim);
    }

    private static boolean nadaMuda(Agendamento a, Proposta p, SolicitacaoEdicaoRequestDTO dto) {
        Set<Long> atuais = a.getParticipantes().stream()
                .filter(x -> x.getPapel() == PapelParticipante.CONVIDADO)
                .map(x -> x.getUsuario().getId()).collect(Collectors.toSet());
        Set<Long> novos = p.convidados().stream().map(Usuario::getId).collect(Collectors.toSet());
        return a.getDataReuniao().equals(dto.dataReuniao())
                && a.getHorarioInicio().equals(dto.horarioInicio())
                && a.getDuracao().getId().equals(p.duracao().getId())
                && a.getFormato() == dto.formato()
                && a.getTopico().equals(dto.topico().trim())
                && Objects.equals(vazioParaNulo(a.getResumo()), vazioParaNulo(dto.resumo()))
                && atuais.equals(novos);
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /**
     * Aceito: confere as regras de novo (a agenda pode ter mudado desde o pedido) e aplica. Quem continua na
     * reuniao mantem a resposta; convidado novo responde ao convite; quem saiu deixa de participar. O docente que
     * aceitou e o aluno que pediu ficam como ACEITO. O status segue o RN 18.
     */
    private void aplicar(SolicitacaoEdicao s, Usuario docente) {
        Agendamento a = s.getAgendamento();
        List<Long> idsConvidados = s.getNovosConvidados().stream().map(Usuario::getId).toList();
        Proposta proposta = validarProposta(a, idsConvidados, s.getNovaDuracao(), s.getNovaData(), s.getNovoHorarioInicio());

        a.setDuracao(proposta.duracao());
        a.setDataReuniao(s.getNovaData());
        a.setHorarioInicio(s.getNovoHorarioInicio());
        a.setFormato(s.getNovoFormato());
        a.setTopico(s.getNovoTopico());
        a.setResumo(s.getNovoResumo());
        if (s.getNovoFormato() == FormatoReuniao.PRESENCIAL) {
            a.setSalaVirtual(null); // orphanRemoval apaga a sala; on-line sem sala segue "ainda sem link" (RF 27)
        }

        Set<Long> ficam = proposta.convidados().stream().map(Usuario::getId).collect(Collectors.toSet());
        List<Usuario> removidos = new ArrayList<>();
        a.getParticipantes().removeIf(p -> {
            boolean sai = p.getPapel() == PapelParticipante.CONVIDADO && !ficam.contains(p.getUsuario().getId());
            if (sai) {
                removidos.add(p.getUsuario());
            }
            return sai;
        });
        Set<Long> jaEstao = a.getParticipantes().stream().map(p -> p.getUsuario().getId()).collect(Collectors.toSet());
        for (Usuario convidado : proposta.convidados()) {
            if (!jaEstao.contains(convidado.getId())) {
                a.adicionarParticipante(AgendamentoParticipante.builder()
                        .usuario(convidado)
                        .papel(PapelParticipante.CONVIDADO)
                        .statusResposta(StatusResposta.PENDENTE)
                        .build());
            }
        }
        LocalDateTime agora = LocalDateTime.now();
        for (AgendamentoParticipante p : a.getParticipantes()) {
            Long id = p.getUsuario().getId();
            if (id.equals(docente.getId()) || id.equals(s.getSolicitante().getId())) {
                p.setStatusResposta(StatusResposta.ACEITO);
                p.setDataResposta(agora);
            }
        }
        a.setStatus(AgendamentoServiceImpl.statusPelasRespostas(a.getParticipantes()));
        agendamentoRepository.save(a);

        s.setStatus(StatusSolicitacao.ACEITA);
        s.setRespondidoEm(agora);
        notificador.alteracaoAceita(a, docente, removidos); // RF 23
    }

    private static void exigirPendente(SolicitacaoEdicao s) {
        if (s.getStatus() != StatusSolicitacao.PENDENTE) {
            throw new RegraDeNegocioException("Este pedido de alteracao ja foi encerrado (" + s.getStatus() + ").");
        }
    }

    private static boolean participa(Agendamento agendamento, Usuario usuario) {
        return agendamento.getParticipantes().stream()
                .anyMatch(p -> p.getUsuario() != null && p.getUsuario().getId().equals(usuario.getId()));
    }

    private void registrar(Agendamento agendamento, Usuario usuario, TipoOperacaoMotivo tipo, String justificativa) {
        registroMotivoRepository.save(RegistroMotivo.builder()
                .agendamento(agendamento)
                .usuario(usuario)
                .tipoOperacao(tipo)
                .justificativa(justificativa)
                .build());
    }

    /** RN 01 - trava a reuniao (ordem de sempre: reuniao, depois usuarios) e so entao le. */
    private Agendamento travarAgendamento(Long id) {
        agendamentoRepository.travarPorId(id);
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", id));
    }

    /** Trava a reuniao do pedido antes de ler o pedido: duas respostas simultaneas acontecem uma depois da outra. */
    private SolicitacaoEdicao travarSolicitacao(Long id) {
        Long idAgendamento = solicitacaoEdicaoRepository.buscarIdDoAgendamento(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Pedido de alteracao", id));
        agendamentoRepository.travarPorId(idAgendamento);
        return solicitacaoEdicaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Pedido de alteracao", id));
    }
}
