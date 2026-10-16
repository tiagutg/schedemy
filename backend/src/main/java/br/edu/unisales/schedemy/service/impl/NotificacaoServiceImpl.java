package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.dto.request.NotificacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.NotificacaoResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.NotificacaoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacaoServiceImpl implements NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final NotificacaoMapper notificacaoMapper;

    @Override
    public NotificacaoResponseDTO criar(NotificacaoRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.idUsuario())
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", dto.idUsuario()));

        Agendamento agendamento = null;
        if (dto.idAgendamento() != null) {
            agendamento = agendamentoRepository.findById(dto.idAgendamento())
                    .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", dto.idAgendamento()));
        }

        Notificacao entidade = notificacaoMapper.paraEntidade(dto, usuario, agendamento);
        entidade = notificacaoRepository.save(entidade);
        return notificacaoMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacaoResponseDTO buscarPorId(Long id) {
        return notificacaoMapper.paraResponseDTO(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificacaoResponseDTO> listar(Long usuarioId, StatusEnvio status, Pageable pageable) {
        return notificacaoRepository.buscarComFiltros(usuarioId, status, pageable)
                .map(notificacaoMapper::paraResponseDTO);
    }

    @Override
    public NotificacaoResponseDTO marcarComoEnviada(Long id) {
        Notificacao entidade = buscarEntidadePorId(id);

        if (entidade.getStatusEnvio() == StatusEnvio.ENVIADO) {
            throw new RegraDeNegocioException("Esta notificacao ja consta como enviada.");
        }

        entidade.setStatusEnvio(StatusEnvio.ENVIADO);
        entidade.setEnviadoEm(LocalDateTime.now());
        entidade = notificacaoRepository.save(entidade);
        return notificacaoMapper.paraResponseDTO(entidade);
    }

    @Override
    public void deletar(Long id) {
        notificacaoRepository.delete(buscarEntidadePorId(id));
    }

    private Notificacao buscarEntidadePorId(Long id) {
        return notificacaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Notificacao", id));
    }
}
