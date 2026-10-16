package br.edu.unisales.schedemy.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BloqueioPeriodoResponseDTO(
        Long id,
        Long idUsuario,
        String nomeUsuario,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        Boolean eRecorrente,
        Integer diaSemanaRecorrente,
        String motivo,
        Boolean sincronizadoOutlook,
        LocalDateTime criadoEm
) {
}