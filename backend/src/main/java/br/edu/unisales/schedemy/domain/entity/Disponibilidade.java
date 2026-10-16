package br.edu.unisales.schedemy.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "disponibilidade")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disponibilidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidade")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "horario_inicio", nullable = false)
    private LocalTime horarioInicio;

    @Column(name = "horario_fim", nullable = false)
    private LocalTime horarioFim;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
        if (this.ativo == null) {
            this.ativo = true;
        }
    }
}