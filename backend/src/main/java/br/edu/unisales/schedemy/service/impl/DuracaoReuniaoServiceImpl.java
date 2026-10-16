package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.dto.request.DuracaoReuniaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.DuracaoReuniaoResponseDTO;
import br.edu.unisales.schedemy.exception.RecursoDuplicadoException;
import br.edu.unisales.schedemy.exception.RecursoNaoEncontradoException;
import br.edu.unisales.schedemy.mapper.DuracaoReuniaoMapper;
import br.edu.unisales.schedemy.repository.DuracaoReuniaoRepository;
import br.edu.unisales.schedemy.service.DuracaoReuniaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DuracaoReuniaoServiceImpl implements DuracaoReuniaoService {

    private final DuracaoReuniaoRepository duracaoReuniaoRepository;
    private final DuracaoReuniaoMapper duracaoReuniaoMapper;

    @Override
    public DuracaoReuniaoResponseDTO criar(DuracaoReuniaoRequestDTO dto) {
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
        duracaoReuniaoRepository.delete(entidade);
    }

    private DuracaoReuniao buscarEntidadePorId(Long id) {
        return duracaoReuniaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.paraEntidade("Duracao de reuniao", id));
    }
}