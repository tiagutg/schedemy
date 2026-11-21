package br.edu.unisales.schedemy.domain.entity;

import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.StatusSolicitacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * RF 21/22 - pedido de alteracao de uma reuniao ja confirmada. Guarda a reuniao como ficaria (data, horario,
 * duracao, formato, topico, resumo e convidados); o organizador nao muda. O primeiro docente que responder decide.
 */
@Entity
@Table(name = "solicitacao_edicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitacaoEdicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitacao")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false)
    private Agendamento agendamento;

    /** Quem pediu: um aluno participante ou a recepcao/administracao em nome dele. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_nova_duracao", nullable = false)
    private DuracaoReuniao novaDuracao;

    @Column(name = "nova_data", nullable = false)
    private LocalDate novaData;

    @Column(name = "novo_horario_inicio", nullable = false)
    private LocalTime novoHorarioInicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "novo_formato", nullable = false, length = 20)
    private FormatoReuniao novoFormato;

    @Column(name = "novo_topico", nullable = false, length = 200)
    private String novoTopico;

    @Column(name = "novo_resumo", length = 500)
    private String novoResumo;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_solicitacao", nullable = false, length = 20)
    @Builder.Default
    private StatusSolicitacao status = StatusSolicitacao.PENDENTE;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    /** Convidados propostos (o organizador fica de fora, como em AgendamentoRequestDTO.idsConvidados). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "solicitacao_edicao_participante",
            joinColumns = @JoinColumn(name = "id_solicitacao"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario"))
    @Builder.Default
    private Set<Usuario> novosConvidados = new LinkedHashSet<>();

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusSolicitacao.PENDENTE;
        }
    }

    @Transient
    public LocalTime getNovoHorarioFim() {
        return novoHorarioInicio == null || novaDuracao == null ? null : novoHorarioInicio.plusMinutes(novaDuracao.getMinutos());
    }
}
