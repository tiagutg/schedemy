package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.CancelamentoDeAfetadasRequestDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.exception.ReunioesAfetadasException;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.DisponibilidadeRepository;
import br.edu.unisales.schedemy.repository.RegistroMotivoRepository;
import br.edu.unisales.schedemy.repository.SolicitacaoEdicaoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * RF 06/RF 07/RF 10 - reunioes afetadas por uma mudanca na agenda do docente (decisao de 17/11/2026, no lugar
 * da sugestao de remarcacao do RF 08/09).
 *
 * Editar, desativar ou remover uma disponibilidade e criar ou editar um bloqueio podem deixar reunioes marcadas
 * fora da agenda. O front pede a previa (nada e gravado); se houver reunioes afetadas, mostra a lista e pede um
 * motivo (RF 11). Ao salvar com a confirmacao, as afetadas sao canceladas na mesma transacao da mudanca e os
 * envolvidos recebem o aviso (tela + e-mail, RF 23) com o link "Agendar de novo" (RF 28).
 * Sem a confirmacao (ou com reunioes que o docente nao viu), a mudanca e recusada: nada e cancelado sem ele ver.
 *
 * Afetada: reuniao futura PENDENTE, AGUARDANDO_RESPOSTA ou CONFIRMADO de que o docente participa (organizador ou
 * convidado) e que fica fora da disponibilidade ou dentro do bloqueio. EM_ANDAMENTO nunca e afetada. Reuniao com
 * varios docentes e cancelada inteira. A regra de horario e a da CalculadoraDeAgenda.
 */
@Component
@RequiredArgsConstructor
public class ImpactoNaAgenda {

    /** Reunioes que ainda nao comecaram e nao foram canceladas. */
    static final Set<StatusAgendamento> STATUS_AFETAVEIS = EnumSet.of(
            StatusAgendamento.PENDENTE, StatusAgendamento.AGUARDANDO_RESPOSTA, StatusAgendamento.CONFIRMADO);
    /** Disponibilidade e semanal: vale para qualquer data futura. */
    private static final LocalDate SEM_LIMITE = LocalDate.of(9999, 12, 31);
    /** RN 05 - prazo do cancelamento comum; aqui so avisa na previa, a reuniao e cancelada mesmo assim. */
    private static final int HORAS_AVISO_CANCELAMENTO = 1;

    private final AgendamentoRepository agendamentoRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroMotivoRepository registroMotivoRepository;
    private final CalculadoraDeAgenda calculadoraDeAgenda;
    private final Notificador notificador;
    private final UsuarioAutenticado usuarioAutenticado;
    private final SolicitacaoEdicaoRepository solicitacaoEdicaoRepository;

    /**
     * Reunioes que cabiam na janela {@code atual} e nao cabem em nenhuma janela do docente depois da mudanca.
     *
     * @param atual  a disponibilidade como esta gravada
     * @param depois como ela ficara (objeto nao gravado), ou null quando for removida
     */
    public List<Agendamento> afetadasPelaDisponibilidade(Disponibilidade atual, Disponibilidade depois) {
        // Criar ou reativar uma janela so acrescenta horarios: nunca afeta reunioes.
        if (!Boolean.TRUE.equals(atual.getAtivo())) {
            return List.of();
        }
        Usuario docente = atual.getUsuario();
        List<Disponibilidade> janelasDepois = new ArrayList<>(disponibilidadeRepository
                .findByUsuarioIdAndAtivoTrue(docente.getId()).stream()
                .filter(d -> !Objects.equals(d.getId(), atual.getId()))
                .toList());
        if (depois != null && depois.getUsuario() != null && docente.getId().equals(depois.getUsuario().getId())) {
            janelasDepois.add(depois);
        }
        return futurasDoDocente(docente, LocalDate.now(), SEM_LIMITE).stream()
                .filter(a -> calculadoraDeAgenda.cabeEmAlgumaJanela(a, List.of(atual)))
                .filter(a -> !calculadoraDeAgenda.cabeEmAlgumaJanela(a, janelasDepois))
                .toList();
    }

    /** Reunioes do docente cobertas pelo bloqueio (como ficara depois de criar ou editar; objeto nao gravado). */
    public List<Agendamento> afetadasPeloBloqueio(BloqueioPeriodo bloqueio) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = bloqueio.getDataInicio().isBefore(hoje) ? hoje : bloqueio.getDataInicio();
        return futurasDoDocente(bloqueio.getUsuario(), inicio, bloqueio.getDataFim()).stream()
                .filter(a -> calculadoraDeAgenda.bloqueioAfetaReuniao(bloqueio, a))
                .toList();
    }

    /**
     * Confere a confirmacao do docente e cancela as reunioes afetadas. Chame dentro da transacao que grava a
     * mudanca na agenda, ANTES de grava-la (a lista e calculada com a agenda ainda como esta no banco).
     *
     * RN 01 - travas na ordem de sempre: primeiro as reunioes, depois o docente. Os ids das reunioes futuras do
     * docente sao lidos sem carregar as entidades, travados, e so entao a lista e calculada: enxerga o que outra
     * transacao acabou de gravar. Com o docente travado, ninguem consegue agendar na janela que esta mudando.
     *
     * @param afetadas calcula a lista (chamado depois das travas)
     */
    public void cancelarAfetadas(Usuario docente, Supplier<List<Agendamento>> afetadas,
                                 CancelamentoDeAfetadasRequestDTO confirmacao) {
        List<Long> travadas = agendamentoRepository.buscarIdsDoParticipanteDesde(
                docente.getId(), LocalDate.now(), STATUS_AFETAVEIS);
        travadas.forEach(agendamentoRepository::travarPorId);
        usuarioRepository.travarParaAgendamento(List.of(docente.getId()));

        List<Agendamento> lista = afetadas.get();
        if (lista.isEmpty()) {
            return;
        }
        Set<Long> vistas = confirmacao == null || confirmacao.idsAgendamentos() == null
                ? Set.of() : new HashSet<>(confirmacao.idsAgendamentos());
        Set<Long> travadasSet = new HashSet<>(travadas);
        boolean todasConfirmadas = lista.stream().allMatch(a -> vistas.contains(a.getId()) && travadasSet.contains(a.getId()));
        if (!todasConfirmadas) {
            throw new ReunioesAfetadasException(vistas.isEmpty()
                    ? "Esta mudanca na agenda cancela reunioes ja marcadas. Revise a lista e confirme o cancelamento informando o motivo."
                    : "A lista de reunioes afetadas mudou desde a previa. Revise a lista e confirme de novo.",
                    paraResposta(lista, docente));
        }
        String motivo = confirmacao.motivo() == null ? "" : confirmacao.motivo().trim();
        if (motivo.isEmpty()) {
            throw new RegraDeNegocioException("Informe o motivo do cancelamento das reunioes afetadas.");
        }

        Usuario autor = usuarioAutenticado.obter();
        for (Agendamento reuniao : lista) {
            // RN 05 nao se aplica: a reuniao deixou de caber na agenda do docente, mesmo a menos de 1 h.
            reuniao.setStatus(StatusAgendamento.CANCELADO);
            registroMotivoRepository.save(RegistroMotivo.builder() // RF 11
                    .agendamento(reuniao)
                    .usuario(autor)
                    .tipoOperacao(TipoOperacaoMotivo.CANCELAMENTO)
                    .justificativa("Agenda de %s mudou: %s".formatted(docente.getNome(), motivo))
                    .build());
            agendamentoRepository.save(reuniao);
            // RF 21 - um pedido de alteracao pendente deixa de valer junto com a reuniao.
            solicitacaoEdicaoRepository.encerrarPendentes(reuniao.getId(), StatusSolicitacao.CANCELADA, LocalDateTime.now());
            notificador.canceladaPorMudancaDeAgenda(reuniao, docente, autor, motivo); // RF 23
        }
    }

    /** RF 07 - aluno(s), topico, data e horario; outros docentes (a reuniao cai para todos) e o aviso do RN 05. */
    public List<ReuniaoAfetadaResponseDTO> paraResposta(List<Agendamento> reunioes, Usuario docente) {
        LocalDateTime limiteAviso = LocalDateTime.now().plusHours(HORAS_AVISO_CANCELAMENTO);
        return reunioes.stream().map(a -> {
            List<Usuario> envolvidos = a.getParticipantes().stream()
                    .map(AgendamentoParticipante::getUsuario)
                    .filter(Objects::nonNull)
                    .toList();
            return new ReuniaoAfetadaResponseDTO(
                    a.getId(),
                    a.getTopico(),
                    a.getDataReuniao(),
                    a.getHorarioInicio(),
                    a.getHorarioFim(),
                    a.getStatus(),
                    envolvidos.stream().filter(u -> u.getTipoUsuario() == TipoUsuario.ALUNO).map(Usuario::getNome).toList(),
                    envolvidos.stream()
                            .filter(u -> CalculadoraDeAgenda.ehDocente(u) && !u.getId().equals(docente.getId()))
                            .map(Usuario::getNome).toList(),
                    LocalDateTime.of(a.getDataReuniao(), a.getHorarioInicio()).isBefore(limiteAviso));
        }).toList();
    }

    /** Reunioes do docente que ainda vao comecar, nos status afetaveis. */
    private List<Agendamento> futurasDoDocente(Usuario docente, LocalDate dataInicio, LocalDate dataFim) {
        if (dataFim.isBefore(dataInicio)) {
            return List.of();
        }
        LocalDateTime agora = LocalDateTime.now();
        return agendamentoRepository
                .buscarDoParticipanteNoPeriodo(docente.getId(), dataInicio, dataFim, STATUS_AFETAVEIS).stream()
                .filter(a -> LocalDateTime.of(a.getDataReuniao(), a.getHorarioInicio()).isAfter(agora))
                .toList();
    }
}
