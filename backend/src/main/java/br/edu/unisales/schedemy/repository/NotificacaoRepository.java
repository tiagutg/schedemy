package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    @Query("""
            SELECT n FROM Notificacao n
            WHERE (:usuarioId IS NULL OR n.usuario.id = :usuarioId)
              AND (:status IS NULL OR n.statusEnvio = :status)
            """)
    Page<Notificacao> buscarComFiltros(@Param("usuarioId") Long usuarioId,
                                       @Param("status") StatusEnvio status,
                                       Pageable pageable);
}
