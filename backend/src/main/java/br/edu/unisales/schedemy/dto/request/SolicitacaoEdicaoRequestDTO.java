package br.edu.unisales.schedemy.dto.request;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** RF 21 - a reuniao confirmada como deveria ficar; o organizador nao muda. */
@Schema(description = "Pedido de alteracao de reuniao ja confirmada (RF 21): os dados como a reuniao deveria ficar.")
public record SolicitacaoEdicaoRequestDTO(
        @Schema(description = "Convidados (sem o organizador)", example = "[6]")
        @NotEmpty(message = "idsConvidados e obrigatorio")
        List<Long> idsConvidados,

        @Schema(example = "4")
        @NotNull(message = "idDuracao e obrigatorio")
        Long idDuracao,

        @Schema(example = "2026-12-01")
        @NotNull(message = "dataReuniao e obrigatoria")
        LocalDate dataReuniao,

        @Schema(example = "14:00:00")
        @NotNull(message = "horarioInicio e obrigatorio")
        LocalTime horarioInicio,

        @NotNull(message = "formato e obrigatorio")
        FormatoReuniao formato,

        @NotBlank(message = "topico e obrigatorio")
        @Size(max = 200, message = "topico deve ter no maximo 200 caracteres")
        String topico,

        @Size(max = 500, message = "resumo deve ter no maximo 500 caracteres")
        String resumo,

        @Schema(description = "Motivo do pedido (RF 11)", example = "Prova no mesmo horario")
        @NotBlank(message = "motivo e obrigatorio")
        @Size(max = 200, message = "motivo deve ter no maximo 200 caracteres")
        String motivo
) {
}
