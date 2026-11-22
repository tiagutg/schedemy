package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.LogAuditoria;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.dto.response.LogAuditoriaResponseDTO;
import br.edu.unisales.schedemy.exception.RegraDeNegocioException;
import br.edu.unisales.schedemy.repository.LogAuditoriaRepository;
import br.edu.unisales.schedemy.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditoriaServiceImpl implements AuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    /**
     * Periodo por dia (fim inclusivo). Sem acoes, todas; tabela vazia, todas (o PostgreSQL nao aceita parametro de
     * texto nulo sem tipo, por isso "" em vez de null). A ordem e fixa (mais recente primeiro).
     */
    @Override
    public Page<LogAuditoriaResponseDTO> listar(Long idUsuario, Boolean somenteSistema, List<AcaoAuditoria> acoes,
                                                String tabela, Long idRegistro, LocalDate dataInicio, LocalDate dataFim,
                                                Pageable pageable) {
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new RegraDeNegocioException("A data final deve ser igual ou posterior a data inicial.");
        }
        List<AcaoAuditoria> filtroAcoes = acoes == null || acoes.isEmpty() ? Arrays.asList(AcaoAuditoria.values()) : acoes;
        LocalDateTime desde = dataInicio == null ? null : dataInicio.atStartOfDay();
        LocalDateTime ate = dataFim == null ? null : dataFim.plusDays(1).atStartOfDay();
        return logAuditoriaRepository.buscarComFiltros(idUsuario, Boolean.TRUE.equals(somenteSistema), filtroAcoes,
                        tabela == null ? "" : tabela.trim(), idRegistro, desde, ate,
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()))
                .map(AuditoriaServiceImpl::paraResponseDTO);
    }

    static LogAuditoriaResponseDTO paraResponseDTO(LogAuditoria l) {
        Usuario u = l.getUsuario();
        return new LogAuditoriaResponseDTO(l.getId(),
                u == null ? null : u.getId(),
                u == null ? null : u.getNome(),
                u == null ? null : u.getTipoUsuario(),
                l.getAcao(), l.getTabelaAfetada(), l.getIdTabelaAfetada(), l.getDetalhes(), l.getIpOrigem(),
                l.getCriadoEm());
    }
}
