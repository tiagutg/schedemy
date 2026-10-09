package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Dados para bloqueio de periodo na agenda.")
public record BloqueioPeriodoRequestDTO(
        @Schema(example = "2")
        @NotNull(message = "idUsuario e obrigatorio")
        Long idUsuario,

        @Schema(example = "2026-10-10")
        @NotNull(message = "dataInicio e obrigatorio")
        LocalDate dataInicio,

        @Schema(example = "2026-10-10")
        @NotNull(message = "dataFim e obrigatorio")
        LocalDate dataFim,

        @Schema(example = "13:00:00")
        LocalTime horarioInicio,

        @Schema(example = "18:00:00")
        LocalTime horarioFim,

        @Schema(description = "Indica se o bloqueio se repete semanalmente")
        Boolean eRecorrente,

        @Schema(description = "Dia da semana do bloqueio recorrente (1=domingo ... 7=sabado)", example = "3")
        @Min(value = 1, message = "diaSemanaRecorrente deve estar entre 1 e 7")
        @Max(value = 7, message = "diaSemanaRecorrente deve estar entre 1 e 7")
        Integer diaSemanaRecorrente,

        @Schema(example = "Congresso academico")
        String motivo
) {
}