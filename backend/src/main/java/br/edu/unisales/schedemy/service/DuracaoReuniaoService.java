package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.DuracaoReuniaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.DuracaoReuniaoResponseDTO;

import java.util.List;

public interface DuracaoReuniaoService {
    DuracaoReuniaoResponseDTO criar(DuracaoReuniaoRequestDTO dto);

    DuracaoReuniaoResponseDTO buscarPorId(Long id);

    List<DuracaoReuniaoResponseDTO> listarAtivas();

    List<DuracaoReuniaoResponseDTO> listarTodas();

    DuracaoReuniaoResponseDTO atualizar(Long id, DuracaoReuniaoRequestDTO dto);

    void deletar(Long id);
}