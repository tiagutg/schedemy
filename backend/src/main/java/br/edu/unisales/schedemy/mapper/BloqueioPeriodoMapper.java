package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.BloqueioPeriodo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.BloqueioPeriodoRequestDTO;
import br.edu.unisales.schedemy.dto.response.BloqueioPeriodoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class BloqueioPeriodoMapper {
    public BloqueioPeriodo paraEntidade(BloqueioPeriodoRequestDTO dto, Usuario usuario) {
        return BloqueioPeriodo.builder()
                .usuario(usuario)
                .dataInicio(dto.dataInicio())
                .dataFim(dto.dataFim())
                .horarioInicio(dto.horarioInicio())
                .horarioFim(dto.horarioFim())
                .eRecorrente(dto.eRecorrente() == null ? Boolean.FALSE : dto.eRecorrente())
                .diaSemanaRecorrente(dto.diaSemanaRecorrente())
                .motivo(dto.motivo())
                .sincronizadoOutlook(false)
                .build();
    }

    public void atualizarEntidade(BloqueioPeriodo entidade, BloqueioPeriodoRequestDTO dto, Usuario usuario) {
        entidade.setUsuario(usuario);
        entidade.setDataInicio(dto.dataInicio());
        entidade.setDataFim(dto.dataFim());
        entidade.setHorarioInicio(dto.horarioInicio());
        entidade.setHorarioFim(dto.horarioFim());
        entidade.setERecorrente(dto.eRecorrente() == null ? Boolean.FALSE : dto.eRecorrente());
        entidade.setDiaSemanaRecorrente(dto.diaSemanaRecorrente());
        entidade.setMotivo(dto.motivo());
    }

    public BloqueioPeriodoResponseDTO paraResponseDTO(BloqueioPeriodo entidade) {
        return new BloqueioPeriodoResponseDTO(
                entidade.getId(),
                entidade.getUsuario().getId(),
                entidade.getUsuario().getNome(),
                entidade.getDataInicio(),
                entidade.getDataFim(),
                entidade.getHorarioInicio(),
                entidade.getHorarioFim(),
                entidade.getERecorrente(),
                entidade.getDiaSemanaRecorrente(),
                entidade.getMotivo(),
                entidade.getSincronizadoOutlook(),
                entidade.getCriadoEm()
        );
    }
}