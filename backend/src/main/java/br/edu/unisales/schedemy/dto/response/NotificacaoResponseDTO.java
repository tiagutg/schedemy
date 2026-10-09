package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;

import java.time.LocalDateTime;

public record NotificacaoResponseDTO(
        Long id,
        Long idUsuario,
        String nomeUsuario,
        Long idAgendamento,
        CanalEnvio canalEnvio,
        String tipoEvento,
        String mensagem,
        StatusEnvio statusEnvio,
        LocalDateTime enviadoEm,
        LocalDateTime criadoEm
) {
}
