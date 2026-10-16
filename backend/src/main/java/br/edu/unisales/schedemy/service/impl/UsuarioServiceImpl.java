package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.UsuarioRequestDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResponseDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.exception.RecursoDuplicadoException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.mapper.UsuarioMapper;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    /** Marca usuarios cadastrados manualmente, ainda sem conta Microsoft vinculada. */
    public static final String PREFIXO_VINCULO_PENDENTE = "pendente-sso:";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UsuarioResponseDTO criar(UsuarioRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com o email: " + dto.email());
        }

        // Validação da senha na criação.
        if (dto.senha() == null || dto.senha().isBlank()) {
            throw new RegraDeNegocioException("A senha é obrigatoria para a criação de usuário.");
        }

        boolean temVinculoMicrosoft = dto.idMicrosoft() != null && !dto.idMicrosoft().isBlank();
        if (temVinculoMicrosoft && usuarioRepository.existsByIdMicrosoft(dto.idMicrosoft())) {
            throw new RecursoDuplicadoException("Ja existe um usuario cadastrado com este idMicrosoft.");
        }

        Usuario usuario = usuarioMapper.paraEntidade(dto);

        // Criptografa a senha antes e persistir no banco.
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        if (!temVinculoMicrosoft) {
            usuario.setIdMicrosoft(gerarVinculoProvisorio());
        }
        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.paraResponseDTO(usuario);
    }

    /**
     * A coluna id_microsoft e NOT NULL e UNIQUE, mas no cadastro manual o vinculo com a
     * conta Microsoft ainda nao existe (ele so aparece no login via SSO - RF 29/RF 31).
     * Gera um marcador provisorio, proposital e facil de identificar: quando o SSO entrar,
     * estes registros precisam ser reconciliados com o idMicrosoft real.
     */
    private String gerarVinculoProvisorio() {
        return PREFIXO_VINCULO_PENDENTE + UUID.randomUUID();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return usuarioMapper.paraResponseDTO(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listar(TipoUsuario tipo, String nome, Boolean ativo, Pageable pageable) {
        return usuarioRepository.buscarComFiltros(tipo, nome, ativo, pageable)
                .map(usuarioMapper::paraResponseDTO);
    }

    @Override
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        usuarioRepository.findByEmail(dto.email()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new RecursoDuplicadoException("Ja existe outro usuario cadastrado com o email: " + dto.email());
            }
        });

        usuarioMapper.atualizarEntidade(usuario, dto);

        // Se uma nova senha foi enviada como atualização, criptografa e atualiza.
        if (dto.senha() != null && !dto.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }

        usuario = usuarioRepository.save(usuario);
        return usuarioMapper.paraResponseDTO(usuario);
    }

    @Override
    public void deletar(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }
}