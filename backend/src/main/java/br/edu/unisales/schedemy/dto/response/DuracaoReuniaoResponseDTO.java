package br.edu.unisales.schedemy.dto.response;

public record DuracaoReuniaoResponseDTO(
        Long id,
        Integer minutos,
        Boolean ativo
) {
}