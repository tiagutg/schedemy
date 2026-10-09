package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.UsuarioRequestDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {
    public Usuario paraEntidade(UsuarioRequestDTO dto) {
        return Usuario.builder()
                .idMicrosoft(dto.idMicrosoft())
                .nome(dto.nome())
                .email(dto.email())
                .telefoneWhatsapp(dto.telefoneWhatsapp())
                .tipoUsuario(dto.tipoUsuario())
                .matriculaRa(dto.matriculaRa())
                .numIdentificacao(dto.numIdentificacao())
                .departamento(dto.departamento())
                .ativo(dto.ativo() == null ? Boolean.TRUE : dto.ativo())
                .build();
    }

    public void atualizarEntidade(Usuario usuario, UsuarioRequestDTO dto) {
        // Vazio significa "nao mexer": o vinculo com a conta Microsoft nao e editado pelo formulario.
        if (dto.idMicrosoft() != null && !dto.idMicrosoft().isBlank()) {
            usuario.setIdMicrosoft(dto.idMicrosoft());
        }
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setTelefoneWhatsapp(dto.telefoneWhatsapp());
        usuario.setTipoUsuario(dto.tipoUsuario());
        usuario.setMatriculaRa(dto.matriculaRa());
        usuario.setNumIdentificacao(dto.numIdentificacao());
        usuario.setDepartamento(dto.departamento());
        if (dto.ativo() != null) {
            usuario.setAtivo(dto.ativo());
        }
    }

    public UsuarioResponseDTO paraResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefoneWhatsapp(),
                usuario.getTipoUsuario(),
                usuario.getMatriculaRa(),
                usuario.getNumIdentificacao(),
                usuario.getDepartamento(),
                usuario.getAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }
}