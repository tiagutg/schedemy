package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Dados para cadastro/atualizacao de uma duracao de reuniao permitida")
public record DuracaoReuniaoRequestDTO(
        @Schema(example = "30", allowableValues = {"10", "15", "20", "30", "45", "60"})
        @NotNull(message = "minutos e obrigatorio")
        Integer minutos,

        Boolean ativo
) {
}