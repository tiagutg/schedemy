package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipanteId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgendamentoParticipanteRepository extends JpaRepository<AgendamentoParticipante, AgendamentoParticipanteId> {
    List<AgendamentoParticipante> findByAgendamentoId(Long agendamentoId);
    List<AgendamentoParticipante> findByUsuarioId(Long usuarioId);
}