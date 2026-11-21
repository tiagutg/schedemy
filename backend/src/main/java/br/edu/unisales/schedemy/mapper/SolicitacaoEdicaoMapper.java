package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.SolicitacaoEdicao;
import br.edu.unisales.schedemy.dto.response.SolicitacaoEdicaoResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class SolicitacaoEdicaoMapper {
    public SolicitacaoEdicaoResponseDTO paraResponseDTO(SolicitacaoEdicao s) {
        return new SolicitacaoEdicaoResponseDTO(
                s.getId(),
                s.getAgendamento().getId(),
                s.getSolicitante().getId(),
                s.getSolicitante().getNome(),
                s.getStatus(),
                s.getNovaData(),
                s.getNovoHorarioInicio(),
                s.getNovoHorarioFim(),
                s.getNovaDuracao().getId(),
                s.getNovaDuracao().getMinutos(),
                s.getNovoFormato(),
                s.getNovoTopico(),
                s.getNovoResumo(),
                s.getNovosConvidados().stream()
                        .sorted(Comparator.comparing(u -> u.getNome() == null ? "" : u.getNome()))
                        .map(u -> new SolicitacaoEdicaoResponseDTO.ConvidadoProposto(u.getId(), u.getNome(), u.getTipoUsuario()))
                        .toList(),
                s.getMotivo(),
                s.getCriadoEm(),
                s.getRespondidoEm()
        );
    }
}
