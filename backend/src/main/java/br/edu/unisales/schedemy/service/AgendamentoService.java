package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.dto.request.AgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RespostaAgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AgendamentoService {

    // Cadastro de agendamento.
    AgendamentoResponseDTO criar(AgendamentoRequestDTO dto);

    // Visualizacao detalhada.
    AgendamentoResponseDTO buscarPorId(Long id);

    // Listagem, filtro e ordenacao dos agendamentos de um usuario.
    Page<AgendamentoResponseDTO> listarPorUsuario(Long usuarioId, StatusAgendamento status,
                                                  LocalDate dataInicio, LocalDate dataFim, Pageable pageable);

    // Listagem administrativa de todos os agendamentos (uso de recepcionista/coordenacao).
    Page<AgendamentoResponseDTO> listarTodos(StatusAgendamento status, LocalDate dataInicio, LocalDate dataFim,
                                             Pageable pageable);

    // Edicao permitida enquanto o agendamento ainda estiver pendente.
    AgendamentoResponseDTO atualizar(Long id, AgendamentoRequestDTO dto);

    // Aceitar ou recusar um agendamento.
    AgendamentoResponseDTO responder(Long id, RespostaAgendamentoRequestDTO dto);

    // Cancelamento de agendamento.
    AgendamentoResponseDTO cancelar(Long id, CancelamentoRequestDTO dto);

    // Remocao definitiva, permitida apenas para agendamentos pendentes ou ja cancelados.
    void deletar(Long id);
}