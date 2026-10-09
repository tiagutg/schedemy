package br.edu.unisales.schedemy.dto.response;

import java.time.LocalDateTime;

public record AvaliacaoResponseDTO(
        Long id,
        Long idAgendamento,
        String topicoAgendamento,
        Long idAluno,
        String nomeAluno,
        Integer nota,
        String comentario,
        LocalDateTime criadoEm
) {
}
