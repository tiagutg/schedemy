package br.edu.unisales.schedemy.dto.request;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Dados para criacao/atualizacao de um agendamento de reuniao.")
public record AgendamentoRequestDTO(
        @Schema(description = "ID do usuario organizador (quem esta solicitando)", example = "1")
        @NotNull(message = "idOrganizador e obrigatorio")
        Long idOrganizador,

        @Schema(description = "IDs dos demais convidados da reuniao (professores/coordenadores/alunos)")
        @NotEmpty(message = "e necessario informar ao menos um convidado")
        List<Long> idsConvidados,

        @Schema(description = "ID da duracao de reuniao pre-cadastrada", example = "3")
        @NotNull(message = "idDuracao e obrigatorio")
        Long idDuracao,

        @Schema(example = "2026-10-15")
        @NotNull(message = "dataReuniao e obrigatorio")
        @FutureOrPresent(message = "dataReuniao deve ser hoje ou uma data futura")
        LocalDate dataReuniao,

        @Schema(example = "14:00:00")
        @NotNull(message = "horarioInicio e obrigatorio")
        LocalTime horarioInicio,

        @Schema(example = "PRESENCIAL")
        @NotNull(message = "formato e obrigatorio")
        FormatoReuniao formato,

        @Schema(example = "Duvidas sobre TCC")
        @NotBlank(message = "topico e obrigatorio")
        @Size(max = 200)
        String topico,

        @Schema(example = "Discutir cronograma de entrega do trabalho de conclusao de curso.")
        @Size(max = 500)
        String resumo,

        @Schema(description = "ID do agendamento anterior, quando reutilizado", example = "null")
        Long idAgendamentoOrigem
) {
}