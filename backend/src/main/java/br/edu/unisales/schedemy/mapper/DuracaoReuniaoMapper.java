package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.dto.request.DuracaoReuniaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.DuracaoReuniaoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class DuracaoReuniaoMapper {
    public DuracaoReuniao paraEntidade(DuracaoReuniaoRequestDTO dto) {
        return DuracaoReuniao.builder()
                .minutos(dto.minutos())
                .ativo(dto.ativo() == null ? Boolean.TRUE : dto.ativo())
                .build();
    }

    public void atualizarEntidade(DuracaoReuniao entidade, DuracaoReuniaoRequestDTO dto) {
        entidade.setMinutos(dto.minutos());
        if (dto.ativo() != null) {
            entidade.setAtivo(dto.ativo());
        }
    }

    public DuracaoReuniaoResponseDTO paraResponseDTO(DuracaoReuniao entidade) {
        return new DuracaoReuniaoResponseDTO(entidade.getId(), entidade.getMinutos(), entidade.getAtivo());
    }
}