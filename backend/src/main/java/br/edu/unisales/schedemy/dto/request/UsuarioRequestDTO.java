package br.edu.unisales.schedemy.dto.request;

import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para cadastro/atualizacao de usuario")
public record UsuarioRequestDTO(
        @Schema(description = "Identificador da conta Microsoft vinculada. Opcional: no cadastro "
                + "manual o vinculo ainda nao existe (RF 29), entao o sistema gera um valor "
                + "provisorio. No update, se vier vazio, o valor atual e preservado.",
                example = "111111111")
        @Size(max = 255, message = "idMicrosoft deve ter no maximo 255 caracteres")
        String idMicrosoft,

        @Schema(example = "Nome")
        @NotBlank(message = "nome e obrigatorio")
        @Size(min = 3, max = 150, message = "nome deve ter entre 3 e 150 caracteres")
        String nome,

        @Schema(example = "user@email.com")
        @NotBlank(message = "email e obrigatorio")
        @Email(message = "email deve ser valido")
        @Size(max = 150)
        String email,

        @Schema(example = "Senha123@")
        @Size(min = 6, max = 100, message = "senha deve ter entre 6 e 100 caracteres")
        String senha, //

        @Schema(example = "2799989999")
        @Pattern(regexp = "^$|^[0-9]{10,20}$", message = "telefoneWhatsapp deve conter apenas numeros (10 a 20 digitos)")
        String telefoneWhatsapp,

        @Schema(example = "USUARIO")
        @NotNull(message = "tipoUsuario e obrigatorio")
        TipoUsuario tipoUsuario,

        @Schema(example = "2222222222")
        String matriculaRa,

        @Schema(example = "PROF-0098")
        String numIdentificacao,

        @Schema(example = "Ciencia da Computacao")
        String departamento,

        Boolean ativo
) {
}