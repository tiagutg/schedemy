package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Resposta de aceite/recusa a um agendamento, edicao ou remarcacao.")
public record RespostaAgendamentoRequestDTO(
        @Schema(description = "ID do usuario que esta respondendo", example = "2")
        @NotNull(message = "idUsuario e obrigatorio")
        Long idUsuario,

        @Schema(description = "true para aceitar, false para recusar")
        @NotNull(message = "aceitar e obrigatorio")
        Boolean aceitar,

        @Schema(description = "Motivo, obrigatorio em caso de recusa)", example = "Conflito de horario")
        @Size(max = 500)
        String motivo
) {
}