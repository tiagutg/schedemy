package br.edu.unisales.schedemy.domain.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoParticipanteId implements Serializable {
    private Long agendamento;
    private Long usuario;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgendamentoParticipanteId that)) return false;
        return Objects.equals(agendamento, that.agendamento) && Objects.equals(usuario, that.usuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agendamento, usuario);
    }
}