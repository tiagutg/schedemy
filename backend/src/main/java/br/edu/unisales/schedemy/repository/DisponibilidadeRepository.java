package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByUsuarioIdAndAtivoTrue(Long usuarioId);

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