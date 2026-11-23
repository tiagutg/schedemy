package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.LogAuditoria;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    /** O usuario e autor de alguma linha da trilha (nao pode ser excluido, so desativado). */
    boolean existsByUsuarioId(Long usuarioId);

    /**
     * RNF 19 - consulta da administracao, da mais recente para a mais antiga. Filtros opcionais: autor (ou so as
     * acoes do sistema), perfil do autor, acoes, tabela ("" = todas, como no termoDeBusca), registro e periodo [desde, ate).
     */
    @Query(value = """
            SELECT l FROM LogAuditoria l
            LEFT JOIN FETCH l.usuario u
            WHERE (:idUsuario IS NULL OR u.id = :idUsuario)
              AND (:somenteSistema = false OR l.usuario IS NULL)
              AND (:tipo IS NULL OR u.tipoUsuario = :tipo)
              AND l.acao IN :acoes
              AND (:tabela = '' OR l.tabelaAfetada = :tabela)
              AND (:idRegistro IS NULL OR l.idTabelaAfetada = :idRegistro)
              AND (CAST(:desde AS LocalDateTime) IS NULL OR l.criadoEm >= :desde)
              AND (CAST(:ate AS LocalDateTime) IS NULL OR l.criadoEm < :ate)
            ORDER BY l.criadoEm DESC, l.id DESC
            """, countQuery = """
            SELECT COUNT(l) FROM LogAuditoria l
            LEFT JOIN l.usuario u
            WHERE (:idUsuario IS NULL OR u.id = :idUsuario)
              AND (:somenteSistema = false OR l.usuario IS NULL)
              AND (:tipo IS NULL OR u.tipoUsuario = :tipo)
              AND l.acao IN :acoes
              AND (:tabela = '' OR l.tabelaAfetada = :tabela)
              AND (:idRegistro IS NULL OR l.idTabelaAfetada = :idRegistro)
              AND (CAST(:desde AS LocalDateTime) IS NULL OR l.criadoEm >= :desde)
              AND (CAST(:ate AS LocalDateTime) IS NULL OR l.criadoEm < :ate)
            """)
    Page<LogAuditoria> buscarComFiltros(@Param("idUsuario") Long idUsuario,
                                        @Param("somenteSistema") boolean somenteSistema,
                                        @Param("tipo") TipoUsuario tipo,
                                        @Param("acoes") Collection<AcaoAuditoria> acoes,
                                        @Param("tabela") String tabela,
                                        @Param("idRegistro") Long idRegistro,
                                        @Param("desde") LocalDateTime desde,
                                        @Param("ate") LocalDateTime ate,
                                        Pageable pageable);
}
