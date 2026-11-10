package br.edu.unisales.schedemy.repository;

import br.edu.unisales.schedemy.domain.entity.AvaliacaoReuniao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvaliacaoReuniaoRepository extends JpaRepository<AvaliacaoReuniao, Long> {
    Page<AvaliacaoReuniao> findByAlunoId(Long alunoId, Pageable pageable);

    /** RNF 15 - avaliacoes do aluno apenas das reunioes de que o usuario informado participou. */
    @Query("""
            SELECT av FROM AvaliacaoReuniao av
            WHERE av.aluno.id = :alunoId
              AND EXISTS (SELECT 1 FROM AgendamentoParticipante p
                          WHERE p.agendamento = av.agendamento AND p.usuario.id = :participanteId)
            """)
    Page<AvaliacaoReuniao> buscarDoAlunoEmReunioesDoParticipante(@Param("alunoId") Long alunoId,
                                                                 @Param("participanteId") Long participanteId,
                                                                 Pageable pageable);

    /** Apoia a restricao uk_avaliacao_aluno_agendamento (uma avaliacao por aluno/reuniao). */
    boolean existsByAgendamentoIdAndAlunoId(Long agendamentoId, Long alunoId);
}
