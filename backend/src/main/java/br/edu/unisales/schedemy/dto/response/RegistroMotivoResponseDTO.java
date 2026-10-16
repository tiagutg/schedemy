package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;

import java.time.LocalDateTime;

public record RegistroMotivoResponseDTO(
        Long id,
        Long idAgendamento,
        Long idUsuario,
        String nomeUsuario,
        TipoOperacaoMotivo tipoOperacao,
        String justificativa,
        LocalDateTime criadoEm
) {
}
