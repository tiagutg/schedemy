package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.dto.request.NotificacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.NotificacaoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificacaoService {
    NotificacaoResponseDTO criar(NotificacaoRequestDTO dto);

    NotificacaoResponseDTO buscarPorId(Long id);

    /** canal nulo = SISTEMA (os avisos que a tela mostra); EMAIL lista a fila de e-mail (RF 23). */
    Page<NotificacaoResponseDTO> listar(Long usuarioId, CanalEnvio canal, StatusEnvio status, Pageable pageable);

    NotificacaoResponseDTO marcarComoEnviada(Long id);

    // Avisos do sistema: o dono marca como lidos (PENDENTE -> ENVIADO).
    NotificacaoResponseDTO marcarComoLida(Long id);

    int marcarTodasComoLidas();

    void deletar(Long id);
}
