package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    @Query("""
            SELECT n FROM Notificacao n
            WHERE (:usuarioId IS NULL OR n.usuario.id = :usuarioId)
              AND (:status IS NULL OR n.statusEnvio = :status)
            """)
    Page<Notificacao> buscarComFiltros(@Param("usuarioId") Long usuarioId,
                                       @Param("status") StatusEnvio status,
                                       Pageable pageable);

    boolean existsByAgendamentoIdAndUsuarioIdAndTipoEvento(Long agendamentoId, Long usuarioId, String tipoEvento);

    /** Avisos do sistema: PENDENTE = nao lido, ENVIADO = lido. */
    @Modifying
    @Query("""
            UPDATE Notificacao n SET n.statusEnvio = br.edu.unisales.schedemy.domain.enums.StatusEnvio.ENVIADO,
                                     n.enviadoEm = :agora
            WHERE n.usuario.id = :usuarioId
              AND n.canalEnvio = br.edu.unisales.schedemy.domain.enums.CanalEnvio.SISTEMA
              AND n.statusEnvio = br.edu.unisales.schedemy.domain.enums.StatusEnvio.PENDENTE
            """)
    int marcarTodasComoLidas(@Param("usuarioId") Long usuarioId, @Param("agora") LocalDateTime agora);

    /** A FK de notificacao nao apaga em cascata: os avisos saem antes da remocao do agendamento. */
    @Modifying
    @Query("DELETE FROM Notificacao n WHERE n.agendamento.id = :agendamentoId")
    int removerDoAgendamento(@Param("agendamentoId") Long agendamentoId);
}
