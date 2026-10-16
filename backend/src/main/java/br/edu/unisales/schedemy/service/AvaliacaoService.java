package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.AvaliacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AvaliacaoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AvaliacaoService {
    AvaliacaoResponseDTO criar(AvaliacaoRequestDTO dto);

    AvaliacaoResponseDTO buscarPorId(Long id);

    Page<AvaliacaoResponseDTO> listarPorAluno(Long alunoId, Pageable pageable);

    AvaliacaoResponseDTO atualizar(Long id, AvaliacaoRequestDTO dto);

    void deletar(Long id);
}
