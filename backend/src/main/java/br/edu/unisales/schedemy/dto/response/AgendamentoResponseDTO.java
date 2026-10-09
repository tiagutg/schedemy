package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AgendamentoResponseDTO(
        Long id,
        Long idOrganizador,
        String nomeOrganizador,
        Integer duracaoMinutos,
        Long idAgendamentoOrigem,
        LocalDate dataReuniao,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        FormatoReuniao formato,
        String topico,
        String resumo,
        StatusAgendamento status,
        List<ParticipanteResponseDTO> participantes,
        SalaVirtualResponseDTO salaVirtual,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}