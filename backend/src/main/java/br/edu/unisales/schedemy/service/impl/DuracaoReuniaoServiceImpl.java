package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.dto.request.DuracaoReuniaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.DuracaoReuniaoResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoDuplicadoException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.mapper.DuracaoReuniaoMapper;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.DuracaoReuniaoRepository;
import br.edu.unisales.schedemy.service.DuracaoReuniaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class DuracaoReuniaoServiceImpl implements DuracaoReuniaoService {

    /** Valores aceitos pela restricao CHECK da tabela duracao_reuniao (RN 17: maximo de 60 minutos). */
    private static final Set<Integer> MINUTOS_PERMITIDOS = Set.of(10, 15, 20, 30, 45, 60);

    private final DuracaoReuniaoRepository duracaoReuniaoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final DuracaoReuniaoMapper duracaoReuniaoMapper;

    @Override
    public DuracaoReuniaoResponseDTO criar(DuracaoReuniaoRequestDTO dto) {
        validarMinutos(dto.minutos());
        if (duracaoReuniaoRepository.existsByMinutos(dto.minutos())) {
            throw new RecursoDuplicadoException("Ja existe uma duracao cadastrada com " + dto.minutos() + " minutos.");
        }
        DuracaoReuniao entidade = duracaoReuniaoMapper.paraEntidade(dto);
        entidade = duracaoReuniaoRepository.save(entidade);
        return duracaoReuniaoMapper.paraResponseDTO(entidade);
    }

    @Override
    @Transactional(readOnly = true)
    public DuracaoReuniaoResponseDTO buscarPorId(Long id) {
        return duracaoReuniaoMapper.paraResponseDTO(buscarEntidadePorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DuracaoReuniaoResponseDTO> listarAtivas() {
        return duracaoReuniaoRepository.findByAtivoTrue().stream()
                .map(duracaoReuniaoMapper::paraResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DuracaoReuniaoResponseDTO> listarTodas() {
        return duracaoReuniaoRepository.findAll().stream()
                .map(duracaoReuniaoMapper::paraResponseDTO)
                .toList();
    }

    @Override
    public DuracaoReuniaoResponseDTO atualizar(Long id, DuracaoReuniaoRequestDTO dto) {
        DuracaoReuniao entidade = buscarEntidadePorId(id);
        validarMinutos(dto.minutos());
        duracaoReuniaoRepository.findByMinutos(dto.minutos()).ifPresent(outra -> {
            if (!outra.getId().equals(id)) {
                throw new RecursoDuplicadoException("Ja existe outra duracao cadastrada com " + dto.minutos() + " minutos.");
            }
        });
        duracaoReuniaoMapper.atualizarEntidade(entidade, dto);
        entidade = duracaoReuniaoRepository.save(entidade);
        return duracaoReuniaoMapper.paraResponseDTO(entidade);
    }

    @Override
    public void deletar(Long id) {
        DuracaoReuniao entidade = buscarEntidadePorId(id);
        if (agendamentoRepository.existsByDuracaoId(id)) {
            throw new RegraDeNegocioException(
                    "Esta duracao ja foi usada em agendamentos e nao pode ser excluida. Desative-a (ativo = false).");
        }
        duracaoReuniaoRepository.delete(entidade);
    }

    private void validarMinutos(Integer minutos) {
        if (!MINUTOS_PERMITIDOS.contains(minutos)) {
            throw new RegraDeNegocioException("Duracao invalida. Valores permitidos: 10, 15, 20, 30, 45 ou 60 minutos.");
        }
    }

    private DuracaoReuniao buscarEntidadePorId(Long id) {
        return duracaoReuniaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Duracao de reuniao", id));
    }
}