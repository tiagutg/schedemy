package br.edu.unisales.schedemy.dto.response;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record DisponibilidadeResponseDTO(
        Long id,
        Long idUsuario,
        String nomeUsuario,
        Integer diaSemana,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        Boolean ativo,
        LocalDateTime criadoEm
) {
}