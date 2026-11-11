package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.dto.request.AgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.CancelamentoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RemarcacaoRequestDTO;
import br.edu.unisales.schedemy.dto.request.RespostaAgendamentoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.HorariosLivresResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AgendamentoService {

    // Cadastro de agendamento.
    AgendamentoResponseDTO criar(AgendamentoRequestDTO dto);

    // Visualizacao detalhada.
    AgendamentoResponseDTO buscarPorId(Long id);

    // RF 14 - horarios de inicio em que todos os usuarios informados estao livres.
    HorariosLivresResponseDTO buscarHorariosLivres(LocalDate data, Long idDuracao, List<Long> idsUsuarios,
                                                   Long ignorarAgendamentoId);

    // Listagem, filtro e ordenacao dos agendamentos de um usuario.
    // "busca" procura no topico e no nome dos envolvidos.
    Page<AgendamentoResponseDTO> listarPorUsuario(Long usuarioId, List<StatusAgendamento> status,
                                                  LocalDate dataInicio, LocalDate dataFim, String busca,
                                                  Pageable pageable);

    // Listagem administrativa de todos os agendamentos (uso de recepcionista/coordenacao).
    Page<AgendamentoResponseDTO> listarTodos(List<StatusAgendamento> status, LocalDate dataInicio, LocalDate dataFim,
                                             String busca, Pageable pageable);

    // Edicao permitida enquanto o agendamento ainda estiver pendente.
    AgendamentoResponseDTO atualizar(Long id, AgendamentoRequestDTO dto);

    // Aceitar ou recusar um agendamento.
    AgendamentoResponseDTO responder(Long id, RespostaAgendamentoRequestDTO dto);

    // Cancelamento de agendamento.
    AgendamentoResponseDTO cancelar(Long id, CancelamentoRequestDTO dto);

    // Remocao definitiva, permitida apenas para agendamentos pendentes ou ja cancelados.
    // RF 28 - cancela a reuniao original e cria a nova ligada a ela, na mesma transacao.
    AgendamentoResponseDTO remarcar(Long id, RemarcacaoRequestDTO dto);

    void deletar(Long id);
}