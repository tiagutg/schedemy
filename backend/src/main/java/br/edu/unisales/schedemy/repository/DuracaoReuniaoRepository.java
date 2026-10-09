package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DuracaoReuniaoRepository extends JpaRepository<DuracaoReuniao, Long> {
    List<DuracaoReuniao> findByAtivoTrue();
    Optional<DuracaoReuniao> findByMinutos(Integer minutos);
    boolean existsByMinutos(Integer minutos);
}