package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.dto.request.CancelamentoDeAfetadasRequestDTO;
import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.dto.response.DisponibilidadeResponseDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DisponibilidadeService {
    DisponibilidadeResponseDTO criar(DisponibilidadeRequestDTO dto);

    DisponibilidadeResponseDTO buscarPorId(Long id);

    Page<DisponibilidadeResponseDTO> listar(Long usuarioId, Integer diaSemana, Boolean ativo, Pageable pageable);

    DisponibilidadeResponseDTO atualizar(Long id, DisponibilidadeRequestDTO dto);

    void deletar(Long id);

    /** RF 03/RF 06 - remove e cancela as reunioes afetadas confirmadas pelo docente. */
    void remover(Long id, CancelamentoDeAfetadasRequestDTO confirmacao);

    /** RF 06/RF 07 - reunioes que a edicao cancelaria, sem gravar nada. */
    List<ReuniaoAfetadaResponseDTO> previaDaEdicao(Long id, DisponibilidadeRequestDTO dto);

    /** RF 06/RF 07 - reunioes que a remocao cancelaria, sem gravar nada. */
    List<ReuniaoAfetadaResponseDTO> previaDaRemocao(Long id);
}