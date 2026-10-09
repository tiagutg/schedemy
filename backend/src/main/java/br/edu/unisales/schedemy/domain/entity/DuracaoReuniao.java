package br.edu.unisales.schedemy.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "duracao_reuniao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuracaoReuniao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_duracao")
    private Long id;

    @Column(name = "minutos", nullable = false)
    private Integer minutos;

    @Column(name = "ativo", nullable = false)
    @Builder.Default
    private Boolean ativo = true;
}