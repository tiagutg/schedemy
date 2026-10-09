package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;

import java.time.LocalDateTime;

public record ParticipanteResponseDTO(
        Long idUsuario,
        String nomeUsuario,
        String emailUsuario,
        PapelParticipante papel,
        StatusResposta statusResposta,
        LocalDateTime dataResposta
) {
}