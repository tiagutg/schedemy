package br.edu.unisales.schedemy.dto.request;

import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Justificativa obrigatoria para alteracoes em agendamento (RF 11)")
public record RegistroMotivoRequestDTO(
        @Schema(example = "1")
        @NotNull(message = "idAgendamento e obrigatorio")
        Long idAgendamento,

        @Schema(description = "Quem registrou o motivo", example = "2")
        @NotNull(message = "idUsuario e obrigatorio")
        Long idUsuario,

        @Schema(example = "CANCELAMENTO")
        @NotNull(message = "tipoOperacao e obrigatorio")
        TipoOperacaoMotivo tipoOperacao,

        @Schema(example = "Imprevisto pessoal.")
        @NotBlank(message = "justificativa e obrigatoria")
        @Size(max = 200, message = "justificativa deve ter no maximo 200 caracteres")
        String justificativa
) {
}
