package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

@Schema(description = "Dados para cadastro/atualizacao de disponibilidade semanal de professor/coordenador")
public record DisponibilidadeRequestDTO(
        @Schema(description = "ID do usuario (professor ou coordenador)", example = "2")
        @NotNull(message = "idUsuario e obrigatorio")
        Long idUsuario,

        @Schema(description = "Dia da semana: 1=domingo ... 7=sabado", example = "4")
        @NotNull(message = "diaSemana e obrigatorio")
        @Min(value = 1, message = "diaSemana deve estar entre 1 e 7")
        @Max(value = 7, message = "diaSemana deve estar entre 1 e 7")
        Integer diaSemana,

        @Schema(example = "08:00:00")
        @NotNull(message = "horarioInicio e obrigatorio")
        LocalTime horarioInicio,

        @Schema(example = "12:00:00")
        @NotNull(message = "horarioFim e obrigatorio")
        LocalTime horarioFim,

        Boolean ativo
) {
}