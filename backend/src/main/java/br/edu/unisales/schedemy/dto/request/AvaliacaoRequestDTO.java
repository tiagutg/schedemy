package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para registro de avaliacao de uma reuniao concluida (RF 24)")
public record AvaliacaoRequestDTO(
        @Schema(example = "1")
        @NotNull(message = "idAgendamento e obrigatorio")
        Long idAgendamento,

        @Schema(example = "2")
        @NotNull(message = "idAluno e obrigatorio")
        Long idAluno,

        @Schema(description = "Nota de 0 a 5", example = "5")
        @NotNull(message = "nota e obrigatoria")
        @Min(value = 0, message = "nota deve ser no minimo 0")
        @Max(value = 5, message = "nota deve ser no maximo 5")
        Integer nota,

        @Schema(example = "Atendimento objetivo e esclarecedor.")
        @Size(max = 200, message = "comentario deve ter no maximo 200 caracteres")
        String comentario
) {
}
