package br.edu.unisales.schedemy.domain.entity;

import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agendamento_participante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(AgendamentoParticipanteId.class)
public class AgendamentoParticipante {
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false)
    private Agendamento agendamento;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    @Builder.Default
    private PapelParticipante papel = PapelParticipante.CONVIDADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_resposta", nullable = false, length = 20)
    @Builder.Default
    private StatusResposta statusResposta = StatusResposta.PENDENTE;

    @Column(name = "data_resposta")
    private LocalDateTime dataResposta;
}