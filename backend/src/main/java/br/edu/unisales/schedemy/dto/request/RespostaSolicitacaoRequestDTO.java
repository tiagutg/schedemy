package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** RF 22 - o docente aceita ou recusa o pedido de alteracao; ao recusar, o motivo e obrigatorio (RF 11). */
@Schema(description = "Resposta do docente ao pedido de alteracao (RF 22).")
public record RespostaSolicitacaoRequestDTO(
        @Schema(example = "true")
        @NotNull(message = "aceitar e obrigatorio")
        Boolean aceitar,

        @Schema(description = "Obrigatorio ao recusar", example = "Tenho banca nesse horario")
        @Size(max = 500, message = "motivo deve ter no maximo 500 caracteres")
        String motivo
) {
}
