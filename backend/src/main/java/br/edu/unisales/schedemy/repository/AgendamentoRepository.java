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
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    @Query("""
            SELECT DISTINCT a FROM Agendamento a
            LEFT JOIN a.participantes p
            WHERE (a.organizador.id = :usuarioId OR p.usuario.id = :usuarioId)
              AND (:status IS NULL OR a.status = :status)
              AND (:dataInicio IS NULL OR a.dataReuniao >= :dataInicio)
              AND (:dataFim IS NULL OR a.dataReuniao <= :dataFim)
            """)
    Page<Agendamento> buscarPorUsuarioComFiltros(@Param("usuarioId") Long usuarioId,
                                                 @Param("status") StatusAgendamento status,
                                                 @Param("dataInicio") LocalDate dataInicio,
                                                 @Param("dataFim") LocalDate dataFim,
                                                 Pageable pageable);

    @Query("""
            SELECT a FROM Agendamento a
            WHERE (:status IS NULL OR a.status = :status)
              AND (:dataInicio IS NULL OR a.dataReuniao >= :dataInicio)
              AND (:dataFim IS NULL OR a.dataReuniao <= :dataFim)
            """)
    Page<Agendamento> buscarComFiltros(@Param("status") StatusAgendamento status,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim,
                                       Pageable pageable);

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

    @Query("""
            SELECT a FROM Agendamento a
            WHERE a.organizador.id = :professorId
              AND a.dataReuniao BETWEEN :dataInicio AND :dataFim
              AND a.status IN (br.edu.unisales.schedemy.domain.enums.StatusAgendamento.PENDENTE,
                                br.edu.unisales.schedemy.domain.enums.StatusAgendamento.CONFIRMADO)
            """)
    List<Agendamento> buscarImpactadosPorPeriodo(@Param("professorId") Long professorId,
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