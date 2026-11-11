package br.edu.unisales.schedemy.dto.request;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * RF 28 - remarcacao: dados da nova reuniao + motivo do cancelamento da original.
 * O organizador e o mesmo da reuniao original e o vinculo (idAgendamentoOrigem) vem da URL.
 */
@Schema(description = "Dados para remarcar um agendamento: a original e cancelada e a nova e criada na mesma operacao.")
public record RemarcacaoRequestDTO(
        @Schema(description = "IDs dos convidados da nova reuniao (o organizador e o da reuniao original)")
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

        @Schema(description = "Link da sala online. Opcional (RF 27).", example = "https://meet.google.com/abc-defg-hij")
        @Size(max = 1000)
        @Pattern(regexp = "^\\s*$|^https?://\\S+$", message = "linkReuniao deve ser um endereco http(s) valido")
        String linkReuniao,

        @Schema(description = "Motivo da remarcacao; fica registrado no cancelamento da reuniao original (RF 11)",
                example = "Conflito com prova")
        @NotBlank(message = "motivo e obrigatorio para remarcacao")
        @Size(max = 400)
        String motivo,

        @Schema(description = "Confirmacao explicita: a reuniao original sera cancelada (RNF 10)", example = "true")
        @NotNull(message = "confirmar e obrigatorio")
        Boolean confirmar
) {
}
