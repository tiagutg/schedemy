package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
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

    @Scheduled(initialDelayString = "${schedemy.rotina-agenda.atraso-inicial:PT30S}",
            fixedDelayString = "${schedemy.rotina-agenda.intervalo:PT5M}")
    @Transactional
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        int expiradas = 0;
        int iniciadas = 0;
        int concluidas = 0;
        int lembretes = 0;
        // Reunioes ativas ate amanha: as que ja comecaram/terminaram e as que comecam nas proximas 24 horas.
        for (Agendamento a : agendamentoRepository.findByStatusInAndDataReuniaoLessThanEqual(
                ACOMPANHADOS, agora.toLocalDate().plusDays(1))) {
            LocalDateTime inicio = LocalDateTime.of(a.getDataReuniao(), a.getHorarioInicio());
            LocalDateTime fim = fimDaReuniao(a, inicio);

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
        if (expiradas + iniciadas + concluidas + lembretes > 0) {
            log.info("Rotina de agenda: {} expirada(s) sem confirmacao, {} iniciada(s), {} concluida(s), {} lembrete(s).",
                    expiradas, iniciadas, concluidas, lembretes);
        }
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
