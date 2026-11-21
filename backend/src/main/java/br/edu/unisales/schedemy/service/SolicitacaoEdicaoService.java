package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.RespostaSolicitacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.SolicitacaoEdicaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.SolicitacaoEdicaoResponseDTO;

import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** RF 21/22, RN 12 - pedido de alteracao de reuniao confirmada, decidido por um docente. */
public interface SolicitacaoEdicaoService {
    SolicitacaoEdicaoResponseDTO solicitar(Long idAgendamento, SolicitacaoEdicaoRequestDTO dto);

    List<SolicitacaoEdicaoResponseDTO> listarDoAgendamento(Long idAgendamento);

    /** RF 22 - pedidos que o logado acompanha (docente e aluno: das proprias reunioes; recepcao/admin: todos). */
    Page<SolicitacaoEdicaoResponseDTO> listar(List<StatusSolicitacao> status, Pageable pageable);

    SolicitacaoEdicaoResponseDTO responder(Long idSolicitacao, RespostaSolicitacaoRequestDTO dto);

    SolicitacaoEdicaoResponseDTO cancelar(Long idSolicitacao);
}
