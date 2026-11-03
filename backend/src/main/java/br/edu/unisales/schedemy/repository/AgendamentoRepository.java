package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    @Query("""
            SELECT DISTINCT a FROM Agendamento a
            LEFT JOIN a.participantes p
            WHERE (a.organizador.id = :usuarioId OR p.usuario.id = :usuarioId)
              AND a.status IN :status
              AND (CAST(:dataInicio AS LocalDate) IS NULL OR a.dataReuniao >= :dataInicio)
              AND (CAST(:dataFim AS LocalDate) IS NULL OR a.dataReuniao <= :dataFim)
              AND (LOWER(a.topico) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR EXISTS (SELECT 1 FROM AgendamentoParticipante b
                              WHERE b.agendamento = a AND LOWER(b.usuario.nome) LIKE LOWER(CONCAT('%', :busca, '%'))))
            """)
    Page<Agendamento> buscarPorUsuarioComFiltros(@Param("usuarioId") Long usuarioId,
                                                 @Param("status") Collection<StatusAgendamento> status,
                                                 @Param("dataInicio") LocalDate dataInicio,
                                                 @Param("dataFim") LocalDate dataFim,
                                                 @Param("busca") String busca,
                                                 Pageable pageable);

    // "busca" vazia casa com tudo; com texto, procura no topico e no nome de qualquer envolvido.
    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.status IN :status
              AND (CAST(:dataInicio AS LocalDate) IS NULL OR a.dataReuniao >= :dataInicio)
              AND (CAST(:dataFim AS LocalDate) IS NULL OR a.dataReuniao <= :dataFim)
              AND (LOWER(a.topico) LIKE LOWER(CONCAT('%', :busca, '%'))
                   OR EXISTS (SELECT 1 FROM AgendamentoParticipante b
                              WHERE b.agendamento = a AND LOWER(b.usuario.nome) LIKE LOWER(CONCAT('%', :busca, '%'))))
            """)
    Page<Agendamento> buscarComFiltros(@Param("status") Collection<StatusAgendamento> status,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim,
                                       @Param("busca") String busca,
                                       Pageable pageable);

    /** RF 28 - existe uma remarcacao ativa desta reuniao? */
    boolean existsByAgendamentoOrigemIdAndStatusNot(Long agendamentoOrigemId, StatusAgendamento status);

    /** Reunioes num status ate a data informada (rotina de conclusao e lembretes). */
    List<Agendamento> findByStatusAndDataReuniaoLessThanEqual(StatusAgendamento status, LocalDate data);

    @Query("""
            SELECT a FROM Agendamento a
            JOIN a.participantes p
            WHERE p.usuario.id = :usuarioId
              AND a.dataReuniao = :data
              AND a.status <> br.edu.unisales.schedemy.domain.enums.StatusAgendamento.CANCELADO
              AND (:agendamentoIgnoradoId IS NULL OR a.id <> :agendamentoIgnoradoId)
            """)
    List<Agendamento> buscarAgendamentosAtivosDoUsuarioNaData(@Param("usuarioId") Long usuarioId,
                                                              @Param("data") LocalDate data,
                                                              @Param("agendamentoIgnoradoId") Long agendamentoIgnoradoId);

    // RF 06 - reunioes ativas em que o usuario participa (como organizador ou convidado) no periodo.
    // Recorrencia e horario do bloqueio sao aplicados depois, na CalculadoraDeAgenda.
    @Query("""
            SELECT DISTINCT a FROM Agendamento a
            JOIN a.participantes p
            WHERE p.usuario.id = :usuarioId
              AND a.dataReuniao BETWEEN :dataInicio AND :dataFim
              AND a.status NOT IN (br.edu.unisales.schedemy.domain.enums.StatusAgendamento.CANCELADO,
                                   br.edu.unisales.schedemy.domain.enums.StatusAgendamento.CONCLUIDO)
            ORDER BY a.dataReuniao, a.horarioInicio
            """)
    List<Agendamento> buscarImpactadosPorPeriodo(@Param("usuarioId") Long usuarioId,
                                                 @Param("dataInicio") LocalDate dataInicio,
                                                 @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT COUNT(a) FROM Agendamento a
            WHERE a.organizador.id = :alunoId
              AND a.criadoEm >= :desde
              AND a.status <> br.edu.unisales.schedemy.domain.enums.StatusAgendamento.CANCELADO
            """)
    long contarAgendamentosDoAlunoDesde(@Param("alunoId") Long alunoId, @Param("desde") LocalDateTime desde);
}