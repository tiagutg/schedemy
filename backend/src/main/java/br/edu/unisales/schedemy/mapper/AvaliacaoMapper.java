package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AvaliacaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.AvaliacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.AvaliacaoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AvaliacaoMapper {
    public AvaliacaoReuniao paraEntidade(AvaliacaoRequestDTO dto, Agendamento agendamento, Usuario aluno) {
        return AvaliacaoReuniao.builder()
                .agendamento(agendamento)
                .aluno(aluno)
                .nota(dto.nota())
                .comentario(dto.comentario())
                .build();
    }

    public void atualizarEntidade(AvaliacaoReuniao entidade, AvaliacaoRequestDTO dto) {
        entidade.setNota(dto.nota());
        entidade.setComentario(dto.comentario());
    }

    public AvaliacaoResponseDTO paraResponseDTO(AvaliacaoReuniao entidade) {
        return new AvaliacaoResponseDTO(
                entidade.getId(),
                entidade.getAgendamento().getId(),
                entidade.getAgendamento().getTopico(),
                entidade.getAluno().getId(),
                entidade.getAluno().getNome(),
                entidade.getNota(),
                entidade.getComentario(),
                entidade.getCriadoEm()
        );
    }
}
