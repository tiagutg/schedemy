package br.edu.unisales.schedemy.dto.request;

import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para enfileirar uma notificacao (RF 23)")
public record NotificacaoRequestDTO(
        @Schema(example = "2")
        @NotNull(message = "idUsuario e obrigatorio")
        Long idUsuario,

        @Schema(description = "Opcional: lembretes gerais nao estao presos a um agendamento", example = "1")
        Long idAgendamento,

        @Schema(example = "EMAIL")
        CanalEnvio canalEnvio,

        @Schema(example = "AGENDAMENTO_CRIADO")
        @NotBlank(message = "tipoEvento e obrigatorio")
        @Size(max = 50, message = "tipoEvento deve ter no maximo 50 caracteres")
        String tipoEvento,

        @Schema(example = "Sua reuniao foi confirmada para 01/12 as 09:30.")
        @NotBlank(message = "mensagem e obrigatoria")
        @Size(max = 200, message = "mensagem deve ter no maximo 200 caracteres")
        String mensagem
) {
}
