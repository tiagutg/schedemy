package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.dto.request.NotificacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.NotificacaoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacaoService {
    NotificacaoResponseDTO criar(NotificacaoRequestDTO dto);

    NotificacaoResponseDTO buscarPorId(Long id);

    Page<NotificacaoResponseDTO> listar(Long usuarioId, StatusEnvio status, Pageable pageable);

    NotificacaoResponseDTO marcarComoEnviada(Long id);

    void deletar(Long id);
}
