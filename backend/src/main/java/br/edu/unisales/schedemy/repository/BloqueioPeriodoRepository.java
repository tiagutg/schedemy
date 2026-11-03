package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BloqueioPeriodoRepository extends JpaRepository<BloqueioPeriodo, Long> {
    List<BloqueioPeriodo> findByUsuarioId(Long usuarioId);

    // RF 04/RF 05 - bloqueios do usuario que cobrem a data; o recorrente so vale no dia da semana configurado
    // (1 = domingo ... 7 = sabado). O filtro por horario fica na CalculadoraDeAgenda.
    @Query("""
            SELECT b FROM BloqueioPeriodo b
            WHERE b.usuario.id = :usuarioId
              AND b.dataInicio <= :data AND b.dataFim >= :data
              AND (b.eRecorrente = false OR b.diaSemanaRecorrente = :diaSemana)
            """)
    List<BloqueioPeriodo> buscarQueImpactamData(@Param("usuarioId") Long usuarioId,
                                                @Param("data") LocalDate data,
                                                @Param("diaSemana") Integer diaSemana);

    @Query("""
            SELECT b FROM BloqueioPeriodo b
            WHERE (:usuarioId IS NULL OR b.usuario.id = :usuarioId)
              AND (CAST(:dataInicio AS LocalDate) IS NULL OR b.dataFim >= :dataInicio)
              AND (CAST(:dataFim AS LocalDate) IS NULL OR b.dataInicio <= :dataFim)
            """)
    Page<BloqueioPeriodo> buscarComFiltros(@Param("usuarioId") Long usuarioId,
                                           @Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim,
                                           Pageable pageable);
}