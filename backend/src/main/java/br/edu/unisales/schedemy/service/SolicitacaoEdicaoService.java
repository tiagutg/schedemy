package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.RespostaSolicitacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.SolicitacaoEdicaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.SolicitacaoEdicaoResponseDTO;

import java.util.List;

/** RF 21/22, RN 12 - pedido de alteracao de reuniao confirmada, decidido por um docente. */
public interface SolicitacaoEdicaoService {
    SolicitacaoEdicaoResponseDTO solicitar(Long idAgendamento, SolicitacaoEdicaoRequestDTO dto);

    List<SolicitacaoEdicaoResponseDTO> listarDoAgendamento(Long idAgendamento);

    SolicitacaoEdicaoResponseDTO responder(Long idSolicitacao, RespostaSolicitacaoRequestDTO dto);

    SolicitacaoEdicaoResponseDTO cancelar(Long idSolicitacao);
}
