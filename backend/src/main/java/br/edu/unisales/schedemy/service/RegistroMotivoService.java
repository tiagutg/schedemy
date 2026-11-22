package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.response.RegistroMotivoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegistroMotivoService {
    RegistroMotivoResponseDTO buscarPorId(Long id);

    Page<RegistroMotivoResponseDTO> listarPorAgendamento(Long agendamentoId, Pageable pageable);
}
