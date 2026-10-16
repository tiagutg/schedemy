package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.UsuarioRequestDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {
    UsuarioResponseDTO criar(UsuarioRequestDTO dto);

    UsuarioResponseDTO buscarPorId(Long id);

    Page<UsuarioResponseDTO> listar(TipoUsuario tipo, String nome, Boolean ativo, Pageable pageable);

    UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto);

    void deletar(Long id);
}