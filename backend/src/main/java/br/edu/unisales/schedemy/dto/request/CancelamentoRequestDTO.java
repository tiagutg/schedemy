package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para cancelamento de agendamento.")
public record CancelamentoRequestDTO(
        @Schema(description = "ID do usuario que esta cancelando", example = "1")
        @NotNull(message = "idUsuario e obrigatorio")
        Long idUsuario,

        @Schema(description = "Motivo do cancelamento", example = "Imprevisto pessoal")
        @NotBlank(message = "motivo e obrigatorio para cancelamento")
        @Size(max = 500)
        String motivo,

        @Schema(description = "Confirmacao explicita do cancelamento", example = "true")
        @NotNull(message = "confirmar e obrigatorio")
        Boolean confirmar
) {
}