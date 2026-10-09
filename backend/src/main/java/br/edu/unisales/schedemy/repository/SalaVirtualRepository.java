package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.SalaVirtual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaVirtualRepository extends JpaRepository<SalaVirtual, Long> {
    Optional<SalaVirtual> findByAgendamentoId(Long agendamentoId);
}