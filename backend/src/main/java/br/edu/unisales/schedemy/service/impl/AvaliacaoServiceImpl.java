package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.AvaliacaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.request.AvaliacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AvaliacaoResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoDuplicadoException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.AvaliacaoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.AvaliacaoReuniaoRepository;
import br.edu.unisales.schedemy.repository.UsuarioRepository;
import br.edu.unisales.schedemy.service.AvaliacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AvaliacaoServiceImpl implements AvaliacaoService {

    private final AvaliacaoReuniaoRepository avaliacaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AvaliacaoMapper avaliacaoMapper;

    @Override
    public AvaliacaoResponseDTO criar(AvaliacaoRequestDTO dto) {
        Agendamento agendamento = buscarAgendamento(dto.idAgendamento());
        Usuario aluno = buscarUsuario(dto.idAluno());

        validarPapelAluno(aluno);
        validarReuniaoConcluida(agendamento);
        validarParticipacao(agendamento, aluno);

        if (avaliacaoRepository.existsByAgendamentoIdAndAlunoId(agendamento.getId(), aluno.getId())) {
            throw new RecursoDuplicadoException("Este aluno ja avaliou esta reuniao.");
        }

        AvaliacaoReuniao entidade = avaliacaoMapper.paraEntidade(dto, agendamento, aluno);
        entidade = avaliacaoRepository.save(entidade);
        return avaliacaoMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public AvaliacaoResponseDTO buscarPorId(Long id) {
        return avaliacaoMapper.paraResponseDTO(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvaliacaoResponseDTO> listarPorAluno(Long alunoId, Pageable pageable) {
        return avaliacaoRepository.findByAlunoId(alunoId, pageable)
                .map(avaliacaoMapper::paraResponseDTO);
    }

    @Override
    public AvaliacaoResponseDTO atualizar(Long id, AvaliacaoRequestDTO dto) {
        AvaliacaoReuniao entidade = buscarEntidadePorId(id);

        // O vinculo aluno/agendamento e imutavel: trocar qualquer um dos dois
        // criaria outra avaliacao, nao a edicao desta.
        if (!entidade.getAgendamento().getId().equals(dto.idAgendamento())
                || !entidade.getAluno().getId().equals(dto.idAluno())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel trocar o agendamento ou o aluno de uma avaliacao ja registrada.");
        }

        avaliacaoMapper.atualizarEntidade(entidade, dto);
        entidade = avaliacaoRepository.save(entidade);
        return avaliacaoMapper.paraResponseDTO(entidade);
    }

    @Override
    public void deletar(Long id) {
        avaliacaoRepository.delete(buscarEntidadePorId(id));
    }

    /** RF 24 - a avaliacao e do aluno. */
    private void validarPapelAluno(Usuario usuario) {
        if (usuario.getTipoUsuario() != TipoUsuario.ALUNO) {
            throw new RegraDeNegocioException("Somente usuarios do tipo ALUNO podem avaliar uma reuniao.");
        }
    }

    /** Caso de uso 5.6.13 - so reunioes concluidas podem ser avaliadas. */
    private void validarReuniaoConcluida(Agendamento agendamento) {
        if (agendamento.getStatus() != StatusAgendamento.CONCLUIDO) {
            throw new RegraDeNegocioException(
                    "Somente reunioes com status CONCLUIDO podem ser avaliadas. Status atual: "
                            + agendamento.getStatus() + ".");
        }
    }

    /** O aluno so avalia reuniao da qual participou (RNF 15). */
    private void validarParticipacao(Agendamento agendamento, Usuario aluno) {
        boolean participou = agendamento.getParticipantes().stream()
                .map(AgendamentoParticipante::getUsuario)
                .anyMatch(u -> u.getId().equals(aluno.getId()));
        if (!participou) {
            throw new RegraDeNegocioException("O aluno informado nao participou desta reuniao.");
        }
    }

    private Agendamento buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Agendamento", id));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Usuario", id));
    }

    private AvaliacaoReuniao buscarEntidadePorId(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Avaliacao", id));
    }
}
