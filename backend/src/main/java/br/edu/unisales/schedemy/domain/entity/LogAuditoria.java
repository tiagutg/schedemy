package br.edu.unisales.schedemy.domain.entity;

import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** RNF 19 - uma linha da trilha de auditoria: quem fez o que, em qual registro, quando e de onde. */
@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long id;

    /** Autor; nulo quando a acao e automatica (RotinaDeAgenda). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "acao", nullable = false, length = 100)
    private AcaoAuditoria acao;

    @Column(name = "tabela_afetada", nullable = false, length = 50)
    private String tabelaAfetada;

    @Column(name = "id_tabela_afetada")
    private Long idTabelaAfetada;

    @Column(name = "detalhes", length = 200)
    private String detalhes;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
    }
}
