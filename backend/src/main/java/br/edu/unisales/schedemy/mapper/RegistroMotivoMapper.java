package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.RegistroMotivo;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.dto.request.RegistroMotivoRequestDTO;
import br.edu.unisales.schedemy.dto.response.RegistroMotivoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class RegistroMotivoMapper {
    public RegistroMotivo paraEntidade(RegistroMotivoRequestDTO dto, Agendamento agendamento, Usuario usuario) {
        return RegistroMotivo.builder()
                .agendamento(agendamento)
                .usuario(usuario)
                .tipoOperacao(dto.tipoOperacao())
                .justificativa(dto.justificativa())
                .build();
    }

    public RegistroMotivoResponseDTO paraResponseDTO(RegistroMotivo entidade) {
        return new RegistroMotivoResponseDTO(
                entidade.getId(),
                entidade.getAgendamento().getId(),
                entidade.getUsuario().getId(),
                entidade.getUsuario().getNome(),
                entidade.getTipoOperacao(),
                entidade.getJustificativa(),
                entidade.getCriadoEm()
        );
    }
}
