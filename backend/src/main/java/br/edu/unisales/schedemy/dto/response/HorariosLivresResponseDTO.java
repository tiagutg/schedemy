package br.edu.unisales.schedemy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Horarios de inicio em que todos os usuarios informados estao livres (RF 14).")
public record HorariosLivresResponseDTO(
        LocalDate data,
        Integer duracaoMinutos,
        @Schema(description = "Horarios de inicio, em passos de 15 minutos", example = "[\"08:00:00\", \"08:15:00\"]")
        List<LocalTime> horarios
) {
}
