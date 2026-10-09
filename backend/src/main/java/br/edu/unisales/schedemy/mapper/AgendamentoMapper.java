package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.SalaVirtual;
import br.edu.unisales.schedemy.dto.response.AgendamentoResponseDTO;
import br.edu.unisales.schedemy.dto.response.ParticipanteResponseDTO;
import br.edu.unisales.schedemy.dto.response.SalaVirtualResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgendamentoMapper {
    public AgendamentoResponseDTO paraResponseDTO(Agendamento agendamento) {
        List<ParticipanteResponseDTO> participantes = agendamento.getParticipantes() == null
                ? List.of()
                : agendamento.getParticipantes().stream().map(this::paraParticipanteResponseDTO).toList();

        SalaVirtualResponseDTO salaDTO = agendamento.getSalaVirtual() == null
                ? null
                : paraSalaResponseDTO(agendamento.getSalaVirtual());

        return new AgendamentoResponseDTO(
                agendamento.getId(),
                agendamento.getOrganizador().getId(),
                agendamento.getOrganizador().getNome(),
                agendamento.getDuracao().getMinutos(),
                agendamento.getAgendamentoOrigem() == null ? null : agendamento.getAgendamentoOrigem().getId(),
                agendamento.getDataReuniao(),
                agendamento.getHorarioInicio(),
                agendamento.getHorarioFim(),
                agendamento.getFormato(),
                agendamento.getTopico(),
                agendamento.getResumo(),
                agendamento.getStatus(),
                participantes,
                salaDTO,
                agendamento.getCriadoEm(),
                agendamento.getAtualizadoEm()
        );
    }

    public ParticipanteResponseDTO paraParticipanteResponseDTO(AgendamentoParticipante participante) {
        return new ParticipanteResponseDTO(
                participante.getUsuario().getId(),
                participante.getUsuario().getNome(),
                participante.getUsuario().getEmail(),
                participante.getPapel(),
                participante.getStatusResposta(),
                participante.getDataResposta()
        );
    }

    public SalaVirtualResponseDTO paraSalaResponseDTO(SalaVirtual sala) {
        return new SalaVirtualResponseDTO(sala.getId(), sala.getLinkTeams(), sala.getCriadoEm());
    }
}