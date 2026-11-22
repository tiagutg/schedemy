package br.edu.unisales.schedemy.dto.response;

import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;

import java.time.LocalDateTime;

/** RNF 19 - linha da trilha de auditoria. Autor nulo = acao automatica do sistema. */
public record LogAuditoriaResponseDTO(
        Long id,
        Long idUsuario,
        String nomeUsuario,
        TipoUsuario tipoUsuario,
        AcaoAuditoria acao,
        String tabelaAfetada,
        Long idRegistro,
        String detalhes,
        String ipOrigem,
        LocalDateTime criadoEm
) {
}
