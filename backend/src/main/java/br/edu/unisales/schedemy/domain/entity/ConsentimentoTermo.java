package br.edu.unisales.schedemy.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** RNF 18 - aceite de uma versao do termo de uso e da politica de privacidade. */
@Entity
@Table(name = "consentimento_termo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentimentoTermo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(name = "aceito_em", nullable = false, updatable = false)
    private LocalDateTime aceitoEm;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @PrePersist
    protected void aoPersistir() {
        this.aceitoEm = LocalDateTime.now();
    }
}
