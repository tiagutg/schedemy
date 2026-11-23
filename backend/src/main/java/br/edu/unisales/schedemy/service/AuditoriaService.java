package br.edu.unisales.schedemy.service;

import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.dto.response.LogAuditoriaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/** RNF 19 - consulta da trilha de auditoria (so administracao). */
public interface AuditoriaService {
    Page<LogAuditoriaResponseDTO> listar(Long idUsuario, Boolean somenteSistema, TipoUsuario tipoUsuario, List<AcaoAuditoria> acoes,
                                         String tabela, Long idRegistro, LocalDate dataInicio, LocalDate dataFim,
                                         Pageable pageable);
}
