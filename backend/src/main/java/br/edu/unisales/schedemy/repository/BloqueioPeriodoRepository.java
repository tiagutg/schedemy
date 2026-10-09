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

    @Query("""
            SELECT b FROM BloqueioPeriodo b
            WHERE b.usuario.id = :usuarioId
              AND b.dataInicio <= :data AND b.dataFim >= :data
            """)
    List<BloqueioPeriodo> buscarQueImpactamData(@Param("usuarioId") Long usuarioId, @Param("data") LocalDate data);

    @Query("""
            SELECT b FROM BloqueioPeriodo b
            WHERE (:usuarioId IS NULL OR b.usuario.id = :usuarioId)
              AND (:dataInicio IS NULL OR b.dataFim >= :dataInicio)
              AND (:dataFim IS NULL OR b.dataInicio <= :dataFim)
            """)
    Page<BloqueioPeriodo> buscarComFiltros(@Param("usuarioId") Long usuarioId,
                                           @Param("dataInicio") LocalDate dataInicio,
                                           @Param("dataFim") LocalDate dataFim,
                                           Pageable pageable);
}