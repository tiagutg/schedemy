package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.TipoUsuario;

/**
 * Dados minimos de um usuario para escolher convidados e popular filtros (RF 14, RF 33).
 * Sem e-mail, telefone ou numero de identificacao: o cadastro completo so e lido pela
 * administracao e pela recepcao (RNF 18).
 */
public record UsuarioResumoResponseDTO(
        Long id,
        String nome,
        TipoUsuario tipoUsuario,
        String matriculaRa,
        String departamento,
        Boolean ativo
) {
}
