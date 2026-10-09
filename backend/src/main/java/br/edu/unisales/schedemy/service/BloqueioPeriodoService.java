package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.BloqueioPeriodoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface BloqueioPeriodoService {
    BloqueioPeriodoResponseDTO criar(BloqueioPeriodoRequestDTO dto);

    BloqueioPeriodoResponseDTO buscarPorId(Long id);

    Page<BloqueioPeriodoResponseDTO> listar(Long usuarioId, LocalDate dataInicio, LocalDate dataFim, Pageable pageable);

    BloqueioPeriodoResponseDTO atualizar(Long id, BloqueioPeriodoRequestDTO dto);

    void deletar(Long id);

    // Lista agendamentos do professor/coordenador impactados pelo bloqueio de periodo.
    List<AgendamentoResponseDTO> listarAgendamentosImpactados(Long id);
}