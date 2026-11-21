package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** RF 21/22 - pedido de alteracao e a reuniao como ficaria. */
public record SolicitacaoEdicaoResponseDTO(
        Long id,
        Long idAgendamento,
        Long idSolicitante,
        String nomeSolicitante,
        StatusSolicitacao status,
        LocalDate novaData,
        LocalTime novoHorarioInicio,
        LocalTime novoHorarioFim,
        Long idNovaDuracao,
        Integer novaDuracaoMinutos,
        FormatoReuniao novoFormato,
        String novoTopico,
        String novoResumo,
        List<ConvidadoProposto> novosConvidados,
        String motivo,
        LocalDateTime criadoEm,
        LocalDateTime respondidoEm
) {
    /** Sem e-mail: so o necessario para mostrar quem entra na reuniao (RNF 18). */
    public record ConvidadoProposto(Long id, String nome, TipoUsuario tipoUsuario) {
    }
}
