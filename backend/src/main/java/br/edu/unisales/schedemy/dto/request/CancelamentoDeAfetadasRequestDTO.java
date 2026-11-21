package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * RF 06/RF 10/RF 11 - confirmacao do docente para cancelar as reunioes afetadas por uma mudanca na agenda
 * (editar, desativar ou remover disponibilidade; criar ou editar bloqueio). Os ids sao os que a previa mostrou:
 * se a mudanca afetar alguma reuniao fora desta lista, a API recusa (422) e devolve a lista atualizada.
 */
@Schema(description = "Confirmacao do cancelamento das reunioes afetadas por uma mudanca na agenda do docente.")
public record CancelamentoDeAfetadasRequestDTO(
        @Schema(description = "Ids das reunioes afetadas que o docente viu na previa e aceita cancelar", example = "[12, 15]")
        List<Long> idsAgendamentos,

        @Schema(description = "Motivo do cancelamento (obrigatorio quando ha reunioes afetadas); vai no aviso aos envolvidos",
                example = "Reuniao do colegiado")
        @Size(max = 500, message = "motivo deve ter no maximo 500 caracteres")
        String motivo
) {
}
