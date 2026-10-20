package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.security.UsuarioAutenticado;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.RegistroMotivoRequestDTO;
import br.edu.unisales.schedemy.dto.response.RegistroMotivoResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.mapper.RegistroMotivoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.RegistroMotivoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.RegistroMotivoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistroMotivoServiceImpl implements RegistroMotivoService {

    private final RegistroMotivoRepository registroMotivoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroMotivoMapper registroMotivoMapper;
    private final UsuarioAutenticado usuarioAutenticado;

    @Override
    public RegistroMotivoResponseDTO criar(RegistroMotivoRequestDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(dto.idAgendamento())
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", dto.idAgendamento()));
        Usuario usuario = usuarioRepository.findById(dto.idUsuario())
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", dto.idUsuario()));

        usuarioAutenticado.exigirProprioOuAdmin(usuario.getId(), "Voce so pode registrar motivos em seu proprio nome.");
        usuarioAutenticado.exigirParticipanteOuAdmin(agendamento);

        RegistroMotivo entidade = registroMotivoMapper.paraEntidade(dto, agendamento, usuario);
        entidade = registroMotivoRepository.save(entidade);
        return registroMotivoMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistroMotivoResponseDTO buscarPorId(Long id) {
        RegistroMotivo entidade = registroMotivoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("RegistroMotivo", id));
        usuarioAutenticado.exigirParticipanteOuAdmin(entidade.getAgendamento());
        return registroMotivoMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroMotivoResponseDTO> listarPorAgendamento(Long agendamentoId, Pageable pageable) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", agendamentoId));
        usuarioAutenticado.exigirParticipanteOuAdmin(agendamento);
        return registroMotivoRepository.findByAgendamentoId(agendamentoId, pageable)
                .map(registroMotivoMapper::paraResponseDTO);
    }
}
