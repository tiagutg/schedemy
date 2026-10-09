package br.edu.unisales.schedemy.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "bloqueio_periodo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueioPeriodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloqueio")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "horario_inicio")
    private LocalTime horarioInicio;

    @Column(name = "horario_fim")
    private LocalTime horarioFim;

    @Column(name = "e_recorrente", nullable = false)
    @Builder.Default
    private Boolean eRecorrente = false;

    @Column(name = "dia_semana_recorrente")
    private Integer diaSemanaRecorrente;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "sincronizado_outlook", nullable = false)
    @Builder.Default
    private Boolean sincronizadoOutlook = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
        if (this.eRecorrente == null) this.eRecorrente = false;
        if (this.sincronizadoOutlook == null) this.sincronizadoOutlook = false;
    }
}