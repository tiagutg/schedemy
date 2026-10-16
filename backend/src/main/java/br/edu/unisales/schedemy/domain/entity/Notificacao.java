package br.edu.unisales.schedemy.domain.entity;

import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** Opcional: lembretes gerais nao estao presos a um agendamento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_agendamento")
    private Agendamento agendamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_envio", nullable = false, length = 20)
    @Builder.Default
    private CanalEnvio canalEnvio = CanalEnvio.EMAIL;

    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;

    @Column(name = "mensagem", nullable = false, length = 200)
    private String mensagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_envio", nullable = false, length = 20)
    @Builder.Default
    private StatusEnvio statusEnvio = StatusEnvio.PENDENTE;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
    }
}
