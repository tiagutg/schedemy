package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RNF 18 - aceite da versao do termo que o usuario leu (precisa ser a versao em vigor). */
public record AceiteTermoRequestDTO(
        @Schema(example = "2026-11-21")
        @NotBlank(message = "versao e obrigatoria")
        @Size(max = 20, message = "versao deve ter no maximo 20 caracteres")
        String versao
) {
}
