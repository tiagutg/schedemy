package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;

import java.time.LocalDateTime;

/**
 * Participante como os demais envolvidos o veem. Sem e-mail (RNF 18, minimizacao): os participantes nao precisam
 * do contato uns dos outros; o cadastro completo fica com a administracao e a recepcao (GET /usuarios).
 */
public record ParticipanteResponseDTO(
        Long idUsuario,
        String nomeUsuario,
        PapelParticipante papel,
        StatusResposta statusResposta,
        LocalDateTime dataResposta
) {
}