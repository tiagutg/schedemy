package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.TipoUsuario;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String telefoneWhatsapp,
        TipoUsuario tipoUsuario,
        String matriculaRa,
        String numIdentificacao,
        String departamento,
        Boolean ativo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}