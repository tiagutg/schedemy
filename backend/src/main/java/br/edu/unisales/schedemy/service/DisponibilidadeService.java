package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.dto.response.DisponibilidadeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DisponibilidadeService {
    DisponibilidadeResponseDTO criar(DisponibilidadeRequestDTO dto);

    DisponibilidadeResponseDTO buscarPorId(Long id);

    Page<DisponibilidadeResponseDTO> listar(Long usuarioId, Integer diaSemana, Boolean ativo, Pageable pageable);

    DisponibilidadeResponseDTO atualizar(Long id, DisponibilidadeRequestDTO dto);

    void deletar(Long id);
}