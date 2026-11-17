package br.edu.unisales.schedemy.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Token de acesso: envie em toda requisicao no cabecalho 'Authorization: Bearer <token>'.")
public record TokenResponseDTO(
        @Schema(description = "JWT assinado pela API")
        String token,

        @Schema(example = "Bearer")
        String tipo,

        @Schema(description = "Instante de expiracao (UTC); depois disso e preciso entrar de novo",
                example = "2026-11-12T23:00:00Z")
        Instant expiraEm
) {
}
