package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.AvaliacaoReuniao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoReuniaoRepository extends JpaRepository<AvaliacaoReuniao, Long> {
    Page<AvaliacaoReuniao> findByAlunoId(Long alunoId, Pageable pageable);

    /** Apoia a restricao uk_avaliacao_aluno_agendamento (uma avaliacao por aluno/reuniao). */
    boolean existsByAgendamentoIdAndAlunoId(Long agendamentoId, Long alunoId);
}
