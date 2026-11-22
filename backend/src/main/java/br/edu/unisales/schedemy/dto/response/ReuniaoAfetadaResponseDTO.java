package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** RF 07 - reuniao que sera cancelada por uma mudanca na agenda do docente. */
@Schema(description = "Reuniao afetada por uma mudanca na agenda do docente.")
public record ReuniaoAfetadaResponseDTO(
        Long id,
        String topico,
        LocalDate dataReuniao,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        StatusAgendamento status,
        @Schema(description = "Nomes dos alunos envolvidos")
        List<String> alunos,
        @Schema(description = "Outros professores/coordenadores da reuniao: ela e cancelada inteira para todos")
        List<String> outrosDocentes,
        @Schema(description = "RN 05 - comeca em menos de 1 hora; e cancelada mesmo assim")
        boolean menosDeUmaHora
) {
}
