package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import br.edu.unisales.schedemy.repository.SolicitacaoEdicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Tarefas periodicas do ciclo de vida da reuniao (RN 08 - atualizacao automatica de status):
 * - PENDENTE/AGUARDANDO_RESPOSTA cujo horario de inicio chegou sem confirmacao passa a CANCELADO (RN 18);
 * - CONFIRMADO cujo horario comecou passa a EM_ANDAMENTO;
 * - CONFIRMADO/EM_ANDAMENTO cujo horario terminou passa a CONCLUIDO e o aluno e convidado a avaliar (RF 24);
 * - lembrete aos participantes que aceitaram, nas 24 horas antes do inicio (RN 11).
 *
 * Desligue com schedemy.rotina-agenda.ativa=false (ex.: em testes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "schedemy.rotina-agenda.ativa", havingValue = "true", matchIfMissing = true)
public class RotinaDeAgenda {
    private static final int HORAS_ANTES_DO_LEMBRETE = 24;
    private static final Set<StatusAgendamento> ACOMPANHADOS = EnumSet.of(
            StatusAgendamento.PENDENTE, StatusAgendamento.AGUARDANDO_RESPOSTA,
            StatusAgendamento.CONFIRMADO, StatusAgendamento.EM_ANDAMENTO);

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final Notificador notificador;
    private final SolicitacaoEdicaoRepository solicitacaoEdicaoRepository;

    @Scheduled(initialDelayString = "${schedemy.rotina-agenda.atraso-inicial:PT30S}",
            fixedDelayString = "${schedemy.rotina-agenda.intervalo:PT5M}")
    @Transactional
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        int expiradas = 0;
        int iniciadas = 0;
        int concluidas = 0;
        int lembretes = 0;
        int pedidosExpirados = 0;
        // Reunioes ativas ate amanha: as que ja comecaram/terminaram e as que comecam nas proximas 24 horas.
        for (Agendamento a : agendamentoRepository.findByStatusInAndDataReuniaoLessThanEqual(
                ACOMPANHADOS, agora.toLocalDate().plusDays(1))) {
            LocalDateTime inicio = LocalDateTime.of(a.getDataReuniao(), a.getHorarioInicio());
            LocalDateTime fim = fimDaReuniao(a, inicio);

            // RF 21/RN 14 - pedido de alteracao sem resposta ate 24 h antes: expira e a reuniao volta ao que era.
            if (a.getStatus() == StatusAgendamento.AGUARDANDO_RESPOSTA
                    && !inicio.isAfter(agora.plusHours(SolicitacaoEdicaoServiceImpl.HORAS_MINIMAS))) {
                pedidosExpirados += expirarPedido(a, agora);
            }

            switch (a.getStatus()) {
                case PENDENTE, AGUARDANDO_RESPOSTA -> {
                    // RN 18 - sem um docente e um aluno confirmados ate o inicio, a reuniao nao acontece.
                    if (!inicio.isAfter(agora)) {
                        a.setStatus(StatusAgendamento.CANCELADO);
                        agendamentoRepository.save(a);
                        notificador.naoConfirmada(a);
                        expiradas++;
                    }
                }
                case CONFIRMADO, EM_ANDAMENTO -> {
                    if (!fim.isAfter(agora)) {
                        a.setStatus(StatusAgendamento.CONCLUIDO);
                        agendamentoRepository.save(a);
                        notificador.avaliacaoPendente(a);
                        concluidas++;
                    } else if (!inicio.isAfter(agora)) {
                        if (a.getStatus() == StatusAgendamento.CONFIRMADO) {
                            a.setStatus(StatusAgendamento.EM_ANDAMENTO);
                            agendamentoRepository.save(a);
                            iniciadas++;
                        }
                    } else if (a.getStatus() == StatusAgendamento.CONFIRMADO
                            && !inicio.isAfter(agora.plusHours(HORAS_ANTES_DO_LEMBRETE))) {
                        lembretes += lembrar(a);
                    }
                }
                default -> {
                    // CANCELADO e CONCLUIDO nao sao consultados.
                }
            }
        }
        if (expiradas + iniciadas + concluidas + lembretes + pedidosExpirados > 0) {
            log.info("Rotina de agenda: {} expirada(s) sem confirmacao, {} iniciada(s), {} concluida(s), {} lembrete(s), "
                            + "{} pedido(s) de alteracao expirado(s).",
                    expiradas, iniciadas, concluidas, lembretes, pedidosExpirados);
        }
    }

    /**
     * Encerra o pedido pendente como EXPIRADA e devolve a reuniao ao status pelas respostas (RN 18), normalmente
     * CONFIRMADO; quem pediu e os docentes sao avisados. Sem pedido pendente, so corrige o status.
     */
    private int expirarPedido(Agendamento a, LocalDateTime agora) {
        a.setStatus(AgendamentoServiceImpl.statusPelasRespostas(a.getParticipantes()));
        agendamentoRepository.save(a);
        return solicitacaoEdicaoRepository.findFirstByAgendamentoIdAndStatus(a.getId(), StatusSolicitacao.PENDENTE)
                .map(s -> {
                    s.setStatus(StatusSolicitacao.EXPIRADA);
                    s.setRespondidoEm(agora);
                    solicitacaoEdicaoRepository.save(s);
                    notificador.pedidoEncerrado(a, s.getSolicitante(), null, true);
                    return 1;
                })
                .orElse(0);
    }

    /** Fim da reuniao; se passar da meia-noite (fim "antes" do inicio), termina no dia seguinte. */
    private static LocalDateTime fimDaReuniao(Agendamento a, LocalDateTime inicio) {
        if (a.getHorarioFim() == null) {
            return inicio;
        }
        LocalDateTime fim = LocalDateTime.of(a.getDataReuniao(), a.getHorarioFim());
        return fim.isAfter(inicio) ? fim : fim.plusDays(1);
    }

    private int lembrar(Agendamento agendamento) {
        int enviados = 0;
        for (AgendamentoParticipante p : agendamento.getParticipantes()) {
            if (p.getStatusResposta() == StatusResposta.ACEITO
                    && !notificacaoRepository.existsByAgendamentoIdAndUsuarioIdAndTipoEvento(
                    agendamento.getId(), p.getUsuario().getId(), Notificador.LEMBRETE)) {
                notificador.lembrete(agendamento, p.getUsuario());
                enviados++;
            }
        }
        return enviados;
    }
}
