package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.response.BloqueioPeriodoResponseDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
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

    /** RF 06/RF 07 - reunioes que o bloqueio novo cancelaria, sem gravar nada. */
    List<ReuniaoAfetadaResponseDTO> previaDaCriacao(BloqueioPeriodoRequestDTO dto);

    /** RF 06/RF 07 - reunioes que a edicao do bloqueio cancelaria, sem gravar nada. */
    List<ReuniaoAfetadaResponseDTO> previaDaEdicao(Long id, BloqueioPeriodoRequestDTO dto);
}