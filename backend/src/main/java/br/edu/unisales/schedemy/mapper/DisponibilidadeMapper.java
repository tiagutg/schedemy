package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.Disponibilidade;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.DisponibilidadeRequestDTO;
import br.edu.unisales.schedemy.dto.response.DisponibilidadeResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class DisponibilidadeMapper {
    public Disponibilidade paraEntidade(DisponibilidadeRequestDTO dto, Usuario usuario) {
        return Disponibilidade.builder()
                .usuario(usuario)
                .diaSemana(dto.diaSemana())
                .horarioInicio(dto.horarioInicio())
                .horarioFim(dto.horarioFim())
                .ativo(dto.ativo() == null ? Boolean.TRUE : dto.ativo())
                .build();
    }

    public void atualizarEntidade(Disponibilidade entidade, DisponibilidadeRequestDTO dto, Usuario usuario) {
        entidade.setUsuario(usuario);
        entidade.setDiaSemana(dto.diaSemana());
        entidade.setHorarioInicio(dto.horarioInicio());
        entidade.setHorarioFim(dto.horarioFim());
        if (dto.ativo() != null) {
            entidade.setAtivo(dto.ativo());
        }
    }

    public DisponibilidadeResponseDTO paraResponseDTO(Disponibilidade entidade) {
        return new DisponibilidadeResponseDTO(
                entidade.getId(),
                entidade.getUsuario().getId(),
                entidade.getUsuario().getNome(),
                entidade.getDiaSemana(),
                entidade.getHorarioInicio(),
                entidade.getHorarioFim(),
                entidade.getAtivo(),
                entidade.getCriadoEm()
        );
    }
}