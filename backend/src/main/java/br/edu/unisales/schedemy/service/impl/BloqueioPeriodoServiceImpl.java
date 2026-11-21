package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.security.UsuarioAutenticado;

import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.response.BloqueioPeriodoResponseDTO;
import br.edu.unisales.schedemy.dto.response.ReuniaoAfetadaResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.BloqueioPeriodoMapper;
import br.edu.unisales.schedemy.repository.BloqueioPeriodoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.BloqueioPeriodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BloqueioPeriodoServiceImpl implements BloqueioPeriodoService {

    private final BloqueioPeriodoRepository bloqueioPeriodoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BloqueioPeriodoMapper bloqueioPeriodoMapper;
    private final UsuarioAutenticado usuarioAutenticado;
    private final ImpactoNaAgenda impactoNaAgenda;

    /**
     * RF 04/RF 06 - as reunioes dentro do bloqueio sao canceladas na mesma transacao, se o docente confirmou
     * (ver ImpactoNaAgenda). Sem a confirmacao, recusa com a lista (422). READ COMMITTED como no agendamento (RN 01).
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BloqueioPeriodoResponseDTO criar(BloqueioPeriodoRequestDTO dto) {
        Usuario usuario = validarNovoEstado(dto);
        BloqueioPeriodo entidade = bloqueioPeriodoMapper.paraEntidade(dto, usuario);
        impactoNaAgenda.cancelarAfetadas(usuario, () -> impactoNaAgenda.afetadasPeloBloqueio(entidade), dto.cancelarAfetadas());
        return bloqueioPeriodoMapper.paraResponseDTO(bloqueioPeriodoRepository.save(entidade));
    }

    /** RF 06/RF 07 - reunioes que o bloqueio novo cancelaria, sem gravar nada. */
    @Override
    @Transactional(readOnly = true)
    public List<ReuniaoAfetadaResponseDTO> previaDaCriacao(BloqueioPeriodoRequestDTO dto) {
        Usuario usuario = validarNovoEstado(dto);
        return impactoNaAgenda.paraResposta(
                impactoNaAgenda.afetadasPeloBloqueio(bloqueioPeriodoMapper.paraEntidade(dto, usuario)), usuario);
    }

    /** RF 06/RF 07 - reunioes que a edicao cancelaria, sem gravar nada. */
    @Override
    @Transactional(readOnly = true)
    public List<ReuniaoAfetadaResponseDTO> previaDaEdicao(Long id, BloqueioPeriodoRequestDTO dto) {
        buscarEntidadeDoDono(id);
        Usuario usuario = validarNovoEstado(dto);
        return impactoNaAgenda.paraResposta(
                impactoNaAgenda.afetadasPeloBloqueio(bloqueioPeriodoMapper.paraEntidade(dto, usuario)), usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public BloqueioPeriodoResponseDTO buscarPorId(Long id) {
        BloqueioPeriodo bloqueio = buscarEntidadePorId(id);
        usuarioAutenticado.exigirProprioOuAdmin(bloqueio.getUsuario().getId(), "Voce so pode gerenciar os seus proprios bloqueios.");
        return bloqueioPeriodoMapper.paraResponseDTO(bloqueio);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BloqueioPeriodoResponseDTO> listar(Long usuarioId, LocalDate dataInicio, LocalDate dataFim,
                                                   Pageable pageable) {
        return bloqueioPeriodoRepository.buscarComFiltros(usuarioAutenticado.escopoDoUsuario(usuarioId), dataInicio, dataFim, pageable)
                .map(bloqueioPeriodoMapper::paraResponseDTO);
    }

    /**
     * RF 06 - vale o bloqueio como ficara: reunioes que ele passa a cobrir sao canceladas (com a confirmacao);
     * diminuir o periodo nao afeta nada. Mudar o docente afeta so a agenda do novo docente.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BloqueioPeriodoResponseDTO atualizar(Long id, BloqueioPeriodoRequestDTO dto) {
        BloqueioPeriodo entidade = buscarEntidadeDoDono(id);
        Usuario usuario = validarNovoEstado(dto);
        BloqueioPeriodo depois = bloqueioPeriodoMapper.paraEntidade(dto, usuario);
        impactoNaAgenda.cancelarAfetadas(usuario, () -> impactoNaAgenda.afetadasPeloBloqueio(depois), dto.cancelarAfetadas());

        bloqueioPeriodoMapper.atualizarEntidade(entidade, dto, usuario);
        return bloqueioPeriodoMapper.paraResponseDTO(bloqueioPeriodoRepository.save(entidade));
    }

    /** RF 06 - remover um bloqueio so libera horarios: nunca afeta reunioes. */
    @Override
    public void deletar(Long id) {
        bloqueioPeriodoRepository.delete(buscarEntidadeDoDono(id));
    }

    /** Regras comuns ao salvar e a previa; devolve o docente do bloqueio. */
    private Usuario validarNovoEstado(BloqueioPeriodoRequestDTO dto) {
        Usuario usuario = buscarUsuario(dto.idUsuario());
        usuarioAutenticado.exigirProprioOuAdmin(usuario.getId(), "Voce so pode gerenciar os seus proprios bloqueios.");
        validarPapelDocente(usuario);
        validarPeriodo(dto);
        return usuario;
    }

    private BloqueioPeriodo buscarEntidadeDoDono(Long id) {
        BloqueioPeriodo entidade = buscarEntidadePorId(id);
        usuarioAutenticado.exigirProprioOuAdmin(entidade.getUsuario().getId(), "Voce so pode gerenciar os seus proprios bloqueios.");
        return entidade;
    }

    private void validarPeriodo(BloqueioPeriodoRequestDTO dto) {
        if (dto.dataFim().isBefore(dto.dataInicio())) {
            throw new RegraDeNegocioException("A data de fim do bloqueio deve ser igual ou posterior a data de inicio.");
        }
        boolean recorrente = Boolean.TRUE.equals(dto.eRecorrente());
        if (recorrente && dto.diaSemanaRecorrente() == null) {
            throw new RegraDeNegocioException("diaSemanaRecorrente e obrigatorio quando o bloqueio e recorrente.");
        }
        if (!recorrente && dto.diaSemanaRecorrente() != null) {
            throw new RegraDeNegocioException("diaSemanaRecorrente so deve ser informado quando o bloqueio e recorrente.");
        }
        // Sem horario o bloqueio cobre o dia inteiro; sem fim, vai ate o fim do dia (CalculadoraDeAgenda).
        if (dto.horarioInicio() != null && dto.horarioFim() != null && !dto.horarioFim().isAfter(dto.horarioInicio())) {
            throw new RegraDeNegocioException(
                    "O horario de fim do bloqueio deve ser posterior ao horario de inicio. Para bloquear ate o fim do dia, deixe o horario de fim em branco.");
        }
    }

    /** RF 04 - bloqueio e da agenda do docente (mesma regra da disponibilidade, RN 06). */
    private void validarPapelDocente(Usuario usuario) {
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR && usuario.getTipoUsuario() != TipoUsuario.COORDENADOR) {
            throw new RegraDeNegocioException(
                    "Somente usuarios do tipo PROFESSOR ou COORDENADOR podem ter bloqueios de agenda.");
        }
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }

    private BloqueioPeriodo buscarEntidadePorId(Long id) {
        return bloqueioPeriodoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Bloqueio de periodo", id));
    }
}