package br.edu.unisales.schedemy.mapper;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.dto.request.NotificacaoRequestDTO;
import br.edu.unisales.schedemy.dto.response.NotificacaoResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoMapper {
    public Notificacao paraEntidade(NotificacaoRequestDTO dto, Usuario usuario, Agendamento agendamento) {
        return Notificacao.builder()
                .usuario(usuario)
                .agendamento(agendamento)
                .canalEnvio(dto.canalEnvio() == null ? CanalEnvio.EMAIL : dto.canalEnvio())
                .tipoEvento(dto.tipoEvento())
                .mensagem(dto.mensagem())
                .build();
    }

    public NotificacaoResponseDTO paraResponseDTO(Notificacao entidade) {
        return new NotificacaoResponseDTO(
                entidade.getId(),
                entidade.getUsuario().getId(),
                entidade.getUsuario().getNome(),
                entidade.getAgendamento() == null ? null : entidade.getAgendamento().getId(),
                entidade.getCanalEnvio(),
                entidade.getTipoEvento(),
                entidade.getMensagem(),
                entidade.getStatusEnvio(),
                entidade.getEnviadoEm(),
                entidade.getCriadoEm()
        );
    }
}
