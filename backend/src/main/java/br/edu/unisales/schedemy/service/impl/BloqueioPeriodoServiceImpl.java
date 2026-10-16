package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.BloqueioPeriodoResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.AgendamentoMapper;
import br.edu.unisales.schedemy.mapper.BloqueioPeriodoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.BloqueioPeriodoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.BloqueioPeriodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BloqueioPeriodoServiceImpl implements BloqueioPeriodoService {

    private final BloqueioPeriodoRepository bloqueioPeriodoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final BloqueioPeriodoMapper bloqueioPeriodoMapper;
    private final AgendamentoMapper agendamentoMapper;

    @Override
    public BloqueioPeriodoResponseDTO criar(BloqueioPeriodoRequestDTO dto) {
        Usuario usuario = buscarUsuario(dto.idUsuario());
        validarPeriodo(dto);

        BloqueioPeriodo entidade = bloqueioPeriodoMapper.paraEntidade(dto, usuario);
        entidade = bloqueioPeriodoRepository.save(entidade);
        return bloqueioPeriodoMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public BloqueioPeriodoResponseDTO buscarPorId(Long id) {
        return bloqueioPeriodoMapper.paraResponseDTO(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BloqueioPeriodoResponseDTO> listar(Long usuarioId, LocalDate dataInicio, LocalDate dataFim,
                                                   Pageable pageable) {
        return bloqueioPeriodoRepository.buscarComFiltros(usuarioId, dataInicio, dataFim, pageable)
                .map(bloqueioPeriodoMapper::paraResponseDTO);
    }

    @Override
    public BloqueioPeriodoResponseDTO atualizar(Long id, BloqueioPeriodoRequestDTO dto) {
        BloqueioPeriodo entidade = buscarEntidadePorId(id);
        Usuario usuario = buscarUsuario(dto.idUsuario());
        validarPeriodo(dto);

        bloqueioPeriodoMapper.atualizarEntidade(entidade, dto, usuario);
        entidade = bloqueioPeriodoRepository.save(entidade);
        return bloqueioPeriodoMapper.paraResponseDTO(entidade);
    }

    @Override
    public void deletar(Long id) {
        BloqueioPeriodo entidade = buscarEntidadePorId(id);
        bloqueioPeriodoRepository.delete(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarAgendamentosImpactados(Long id) {
        BloqueioPeriodo bloqueio = buscarEntidadePorId(id);
        List<Agendamento> impactados = agendamentoRepository.buscarImpactadosPorPeriodo(
                bloqueio.getUsuario().getId(), bloqueio.getDataInicio(), bloqueio.getDataFim());
        return impactados.stream().map(agendamentoMapper::paraResponseDTO).toList();
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