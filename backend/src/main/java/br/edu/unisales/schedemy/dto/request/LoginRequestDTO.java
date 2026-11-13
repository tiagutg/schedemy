package br.edu.unisales.schedemy.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credenciais para obter o token de acesso.")
public record LoginRequestDTO(
        @Schema(example = "kevin.aluno@unisales.edu.br")
        @NotBlank(message = "email e obrigatorio")
        @Size(max = 150)
        String email,

        @Schema(example = "Senha123@")
        @NotBlank(message = "senha e obrigatoria")
        @Size(max = 100)
        String senha
) {
    /** Nao deixa a senha aparecer em logs ou mensagens de erro. */
    @Override
    public String toString() {
        return "LoginRequestDTO[email=" + email + ", senha=***]";
    }
}
