package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistroMotivoRepository extends JpaRepository<RegistroMotivo, Long> {
    List<RegistroMotivo> findByAgendamentoId(Long agendamentoId);
    Page<RegistroMotivo> findByAgendamentoId(Long agendamentoId, Pageable pageable);
}