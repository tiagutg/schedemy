package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    @Query("""
            SELECT n FROM Notificacao n
            WHERE (:usuarioId IS NULL OR n.usuario.id = :usuarioId)
              AND n.canalEnvio = :canal
              AND (:status IS NULL OR n.statusEnvio = :status)
            """)
    Page<Notificacao> buscarComFiltros(@Param("usuarioId") Long usuarioId,
                                       @Param("canal") CanalEnvio canal,
                                       @Param("status") StatusEnvio status,
                                       Pageable pageable);

    /** RF 23 - fila de e-mail: os mais antigos primeiro, ja com o destinatario e a reuniao. */
    @Query("""
            SELECT n FROM Notificacao n
            JOIN FETCH n.usuario
            LEFT JOIN FETCH n.agendamento
            WHERE n.canalEnvio = br.edu.unisales.schedemy.domain.enums.CanalEnvio.EMAIL
              AND n.statusEnvio = br.edu.unisales.schedemy.domain.enums.StatusEnvio.PENDENTE
            ORDER BY n.id
            """)
    List<Notificacao> buscarEmailsPendentes(Pageable pageable);

    /** RF 23 - e-mail que ficou na fila alem do prazo (servidor fora do ar, envio desligado) nao sai mais. */
    @Modifying
    @Query("""
            UPDATE Notificacao n SET n.statusEnvio = br.edu.unisales.schedemy.domain.enums.StatusEnvio.FALHA
            WHERE n.canalEnvio = br.edu.unisales.schedemy.domain.enums.CanalEnvio.EMAIL
              AND n.statusEnvio = br.edu.unisales.schedemy.domain.enums.StatusEnvio.PENDENTE
              AND n.criadoEm < :limite
            """)
    int expirarEmailsPendentes(@Param("limite") LocalDateTime limite);

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
