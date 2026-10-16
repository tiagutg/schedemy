package br.edu.unisales.schedemy.domain.entity;

import br.edu.unisales.schedemy.domain.enums.TipoOperacaoMotivo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_motivo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMotivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_motivo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacao", nullable = false, length = 30)
    private TipoOperacaoMotivo tipoOperacao;

    @Column(name = "justificativa", nullable = false, columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
    }
}