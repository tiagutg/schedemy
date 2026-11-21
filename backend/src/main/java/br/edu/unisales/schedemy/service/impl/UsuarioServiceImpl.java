package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.repository.LogAuditoriaRepository;

import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.UsuarioRequestDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResponseDTO;
import br.edu.unisales.schedemy.dto.response.UsuarioResumoResponseDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.exception.RecursoDuplicadoException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.mapper.UsuarioMapper;
import br.edu.unisales.schedemy.repository.AgendamentoParticipanteRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import br.edu.unisales.schedemy.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioServiceImpl implements UsuarioService {

    /** Marca usuarios cadastrados manualmente, ainda sem conta Microsoft vinculada. */
    public static final String PREFIXO_VINCULO_PENDENTE = "pendente-sso:";

    private final UsuarioRepository usuarioRepository;
    private final AgendamentoParticipanteRepository agendamentoParticipanteRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioAutenticado usuarioAutenticado;
    private final Auditoria auditoria;
    private final LogAuditoriaRepository logAuditoriaRepository;

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
        auditoria.registrar(AcaoAuditoria.USUARIO_CRIADO, usuario.getId(), descrever(usuario)); // RNF 19
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
    public UsuarioResponseDTO buscarLogado() {
        return usuarioMapper.paraResponseDTO(usuarioAutenticado.obter());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> listar(TipoUsuario tipo, String nome, Boolean ativo, Pageable pageable) {
        return buscar(tipo, nome, ativo, pageable).map(usuarioMapper::paraResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResumoResponseDTO> listarResumo(TipoUsuario tipo, String nome, Boolean ativo, Pageable pageable) {
        return buscar(tipo, nome, ativo, pageable).map(usuarioMapper::paraResumoDTO);
    }

    private Page<Usuario> buscar(TipoUsuario tipo, String nome, Boolean ativo, Pageable pageable) {
        // Sem filtro de nome, busca por "" (aceita todos): o PostgreSQL nao aceita LOWER(null) sem tipo.
        String filtroNome = nome == null ? "" : nome;
        return usuarioRepository.buscarComFiltros(tipo, filtroNome, ativo, pageable);
    }

    @Override
    public UsuarioResponseDTO atualizar(Long id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarEntidadePorId(id);

        usuarioRepository.findByEmail(dto.email()).ifPresent(outro -> {
            if (!outro.getId().equals(id)) {
                throw new RecursoDuplicadoException("Ja existe outro usuario cadastrado com o email: " + dto.email());
            }
        });

        Usuario antes = copiaParaComparar(usuario);
        usuarioMapper.atualizarEntidade(usuario, dto);

        // Se uma nova senha foi enviada como atualização, criptografa e atualiza.
        boolean trocouSenha = dto.senha() != null && !dto.senha().isBlank();
        if (trocouSenha) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }

        usuario = usuarioRepository.save(usuario);
        registrarEdicao(antes, usuario, trocouSenha);
        return usuarioMapper.paraResponseDTO(usuario);
    }

    @Override
    public void deletar(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        // As FKs de agendamento, participante, motivo e avaliacao sao RESTRICT: com historico, so desativando.
        if (agendamentoParticipanteRepository.existsByUsuarioId(id)) {
            throw new RegraDeNegocioException(
                    "Este usuario participa de agendamentos e nao pode ser excluido. Desative o cadastro (ativo = false).");
        }
        // RNF 19 - a trilha de quem ja agiu no sistema (ao menos o login) e mantida: so desativando.
        if (logAuditoriaRepository.existsByUsuarioId(id)) {
            throw new RegraDeNegocioException(
                    "Este usuario ja tem acoes registradas na auditoria e nao pode ser excluido. Desative o cadastro (ativo = false).");
        }
        auditoria.registrar(AcaoAuditoria.USUARIO_REMOVIDO, usuario.getId(), descrever(usuario));
        usuarioRepository.delete(usuario);
    }

    /**
     * RNF 19 - edicao, (des)ativacao e troca de senha viram linhas separadas, para filtrar por acao. Nos detalhes vao
     * so os NOMES dos campos alterados (minimizacao de dados - RNF 18), com o perfil antigo e o novo.
     */
    private void registrarEdicao(Usuario antes, Usuario depois, boolean trocouSenha) {
        List<String> campos = new ArrayList<>();
        if (!Objects.equals(antes.getNome(), depois.getNome())) campos.add("nome");
        if (!Objects.equals(antes.getEmail(), depois.getEmail())) campos.add("email");
        if (!Objects.equals(antes.getTelefoneWhatsapp(), depois.getTelefoneWhatsapp())) campos.add("telefone");
        if (antes.getTipoUsuario() != depois.getTipoUsuario()) {
            campos.add("perfil (%s -> %s)".formatted(antes.getTipoUsuario(), depois.getTipoUsuario()));
        }
        if (!Objects.equals(antes.getMatriculaRa(), depois.getMatriculaRa())) campos.add("matricula");
        if (!Objects.equals(antes.getNumIdentificacao(), depois.getNumIdentificacao())) campos.add("identificacao");
        if (!Objects.equals(antes.getDepartamento(), depois.getDepartamento())) campos.add("departamento");
        if (!Objects.equals(antes.getIdMicrosoft(), depois.getIdMicrosoft())) campos.add("vinculo Microsoft");
        if (!campos.isEmpty()) {
            auditoria.registrar(AcaoAuditoria.USUARIO_EDITADO, depois.getId(),
                    depois.getNome() + " - alterou: " + String.join(", ", campos));
        }
        if (!Objects.equals(antes.getAtivo(), depois.getAtivo())) {
            auditoria.registrar(Boolean.TRUE.equals(depois.getAtivo()) ? AcaoAuditoria.USUARIO_ATIVADO
                    : AcaoAuditoria.USUARIO_DESATIVADO, depois.getId(), descrever(depois));
        }
        if (trocouSenha) {
            auditoria.registrar(AcaoAuditoria.USUARIO_SENHA_ALTERADA, depois.getId(), descrever(depois));
        }
    }

    private static Usuario copiaParaComparar(Usuario u) {
        return Usuario.builder().nome(u.getNome()).email(u.getEmail()).telefoneWhatsapp(u.getTelefoneWhatsapp())
                .tipoUsuario(u.getTipoUsuario()).matriculaRa(u.getMatriculaRa()).numIdentificacao(u.getNumIdentificacao())
                .departamento(u.getDepartamento()).idMicrosoft(u.getIdMicrosoft()).ativo(u.getAtivo()).build();
    }

    /** "Nome (PERFIL)": sem e-mail nem telefone na trilha (RNF 18). */
    private static String descrever(Usuario u) {
        return "%s (%s)".formatted(u.getNome(), u.getTipoUsuario());
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }
}