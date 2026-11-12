package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    /**
     * Janelas ativas do mesmo docente e dia que se sobrepoem a [inicio, fim). Janelas que so encostam
     * (08:00-12:00 e 12:00-14:00) nao contam. idIgnorado exclui a propria janela na edicao (-1 na criacao).
     */
    @Query("""
            SELECT COUNT(d) > 0 FROM Disponibilidade d
            WHERE d.usuario.id = :usuarioId
              AND d.diaSemana = :diaSemana
              AND d.ativo = true
              AND d.id <> :idIgnorado
              AND d.horarioInicio < :fim
              AND d.horarioFim > :inicio
            """)
    boolean existeSobreposicao(@Param("usuarioId") Long usuarioId,
                               @Param("diaSemana") Integer diaSemana,
                               @Param("inicio") LocalTime inicio,
                               @Param("fim") LocalTime fim,
                               @Param("idIgnorado") Long idIgnorado);

    @Query("""
            SELECT d FROM Disponibilidade d
            WHERE (:usuarioId IS NULL OR d.usuario.id = :usuarioId)
              AND (:diaSemana IS NULL OR d.diaSemana = :diaSemana)
              AND (:ativo IS NULL OR d.ativo = :ativo)
            """)
    Page<Disponibilidade> buscarComFiltros(@Param("usuarioId") Long usuarioId,
                                           @Param("diaSemana") Integer diaSemana,
                                           @Param("ativo") Boolean ativo,
                                           Pageable pageable);
}