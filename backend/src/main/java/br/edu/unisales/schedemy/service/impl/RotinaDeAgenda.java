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

/**
 * Tarefas periodicas do ciclo de vida da reuniao:
 * - reuniao CONFIRMADA cujo horario ja terminou passa a CONCLUIDO e o aluno e convidado a avaliar (RF 24);
 * - lembrete aos participantes que aceitaram, nas 24 horas antes do inicio (RF 23).
 *
 * Desligue com schedemy.rotina-agenda.ativa=false (ex.: em testes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "schedemy.rotina-agenda.ativa", havingValue = "true", matchIfMissing = true)
public class RotinaDeAgenda {
    private static final int HORAS_ANTES_DO_LEMBRETE = 24;

    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final Notificador notificador;

    @Scheduled(initialDelayString = "${schedemy.rotina-agenda.atraso-inicial:PT30S}",
            fixedDelayString = "${schedemy.rotina-agenda.intervalo:PT5M}")
    @Transactional
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        int concluidas = 0;
        int lembretes = 0;
        // Confirmadas ate amanha: as que ja terminaram e as que comecam nas proximas 24 horas.
        for (Agendamento a : agendamentoRepository.findByStatusAndDataReuniaoLessThanEqual(
                StatusAgendamento.CONFIRMADO, agora.toLocalDate().plusDays(1))) {
            LocalDateTime inicio = LocalDateTime.of(a.getDataReuniao(), a.getHorarioInicio());
            LocalDateTime fim = a.getHorarioFim() == null ? inicio : LocalDateTime.of(a.getDataReuniao(), a.getHorarioFim());

            if (!fim.isAfter(agora)) {
                a.setStatus(StatusAgendamento.CONCLUIDO);
                agendamentoRepository.save(a);
                notificador.avaliacaoPendente(a);
                concluidas++;
            } else if (inicio.isAfter(agora) && !inicio.isAfter(agora.plusHours(HORAS_ANTES_DO_LEMBRETE))) {
                lembretes += lembrar(a);
            }
        }
        if (concluidas + lembretes > 0) {
            log.info("Rotina de agenda: {} reuniao(oes) concluida(s), {} lembrete(s) enviado(s).", concluidas, lembretes);
        }
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
