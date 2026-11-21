package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.SolicitacaoEdicao;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitacaoEdicaoRepository extends JpaRepository<SolicitacaoEdicao, Long> {

    /** Id da reuniao do pedido, sem carregar as entidades: o service trava a reuniao antes de le-la (RN 01). */
    @Query("SELECT s.agendamento.id FROM SolicitacaoEdicao s WHERE s.id = :id")
    Optional<Long> buscarIdDoAgendamento(@Param("id") Long id);

    /** RF 21 - pedidos da reuniao, do mais recente para o mais antigo. */
    List<SolicitacaoEdicao> findByAgendamentoIdOrderByCriadoEmDesc(Long agendamentoId);

    /** RF 21 - so existe um pedido PENDENTE por reuniao. */
    Optional<SolicitacaoEdicao> findFirstByAgendamentoIdAndStatus(Long agendamentoId, StatusSolicitacao status);

    boolean existsByAgendamentoIdAndStatus(Long agendamentoId, StatusSolicitacao status);

    /**
     * Encerra os pedidos PENDENTE da reuniao (ela foi cancelada ou remarcada): passam ao status informado.
     * Devolve quantos mudaram.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("""
            UPDATE SolicitacaoEdicao s SET s.status = :novoStatus, s.respondidoEm = :quando
            WHERE s.agendamento.id = :agendamentoId
              AND s.status = br.edu.unisales.schedemy.domain.enums.StatusSolicitacao.PENDENTE
            """)
    int encerrarPendentes(@Param("agendamentoId") Long agendamentoId,
                          @Param("novoStatus") StatusSolicitacao novoStatus,
                          @Param("quando") LocalDateTime quando);
}
