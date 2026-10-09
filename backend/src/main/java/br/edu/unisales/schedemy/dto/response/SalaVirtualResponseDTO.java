package br.edu.unisales.schedemy.dto.response;

import java.time.LocalDateTime;

public record SalaVirtualResponseDTO(
        Long id,
        String linkTeams,
        LocalDateTime criadoEm
) {
}