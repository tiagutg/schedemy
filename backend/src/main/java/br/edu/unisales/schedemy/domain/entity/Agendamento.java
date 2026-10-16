package br.edu.unisales.schedemy.domain.entity;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agendamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agendamento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario organizador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_duracao", nullable = false)
    private DuracaoReuniao duracao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agendamento_origem")
    private Agendamento agendamentoOrigem;

    @Column(name = "data_reuniao", nullable = false)
    private LocalDate dataReuniao;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false, length = 20)
    private FormatoReuniao formato;

    @Column(name = "topico", nullable = false, length = 200)
    private String topico;

    @Column(name = "resumo", length = 500)
    private String resumo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_agend", nullable = false, length = 30)
    @Builder.Default
    private StatusAgendamento status = StatusAgendamento.PENDENTE;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AgendamentoParticipante> participantes = new ArrayList<>();

    @OneToOne(mappedBy = "agendamento", cascade = CascadeType.ALL, orphanRemoval = true)
    private SalaVirtual salaVirtual;

    @PrePersist
    protected void aoPersistir() {
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        if (this.status == null) {
            this.status = StatusAgendamento.PENDENTE;
        }
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    @Transient
    public LocalTime getHorarioFim() {
        if (horarioInicio == null || duracao == null || duracao.getMinutos() == null) {
            return null;
        }
        return horarioInicio.plusMinutes(duracao.getMinutos());
    }

    public void adicionarParticipante(AgendamentoParticipante participante) {
        participantes.add(participante);
        participante.setAgendamento(this);
    }
}