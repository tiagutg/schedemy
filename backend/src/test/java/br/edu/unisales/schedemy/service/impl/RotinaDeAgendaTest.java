package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.AgendamentoRepository;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Conclusao automatica das reunioes e lembretes (RF 23, RF 24). Roda sem banco. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RotinaDeAgendaTest {

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private NotificacaoRepository notificacaoRepository;
    private RotinaDeAgenda rotina;

    private final Usuario professor = usuario(1L, "Rômulo Ferreira", TipoUsuario.PROFESSOR);
    private final Usuario aluno = usuario(2L, "Kevin Lins", TipoUsuario.ALUNO);

    @BeforeEach
    void preparar() {
        rotina = new RotinaDeAgenda(agendamentoRepository, notificacaoRepository, new Notificador(notificacaoRepository));
    }

    @Test
    @DisplayName("Reuniao confirmada que ja terminou vira CONCLUIDO e so o aluno e convidado a avaliar")
    void concluiReuniaoQueJaTerminou() {
        Agendamento passada = confirmada(LocalDateTime.now().minusHours(2));
        confirmadasNoBanco(passada);

        rotina.executar();

        assertThat(passada.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
        List<Notificacao> avisos = avisosSalvos(1);
        assertThat(avisos.getFirst().getUsuario()).isEqualTo(aluno);
        assertThat(avisos.getFirst().getTipoEvento()).isEqualTo(Notificador.AVALIACAO_PENDENTE);
        assertThat(avisos.getFirst().getCanalEnvio()).isEqualTo(CanalEnvio.SISTEMA);
    }

    @Test
    @DisplayName("Reuniao nas proximas 24 horas gera um lembrete por participante que aceitou")
    void lembraQuemAceitou() {
        Agendamento amanha = confirmada(LocalDateTime.now().plusHours(20));
        confirmadasNoBanco(amanha);

        rotina.executar();

        assertThat(amanha.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(avisosSalvos(2)).allSatisfy(n -> {
            assertThat(n.getTipoEvento()).isEqualTo(Notificador.LEMBRETE);
            assertThat(n.getMensagem()).startsWith("Lembrete: \"Orientação do TCC\"");
        });
    }

    @Test
    @DisplayName("Lembrete ja enviado nao se repete")
    void naoRepeteLembrete() {
        confirmadasNoBanco(confirmada(LocalDateTime.now().plusHours(3)));
        when(notificacaoRepository.existsByAgendamentoIdAndUsuarioIdAndTipoEvento(anyLong(), anyLong(), anyString()))
                .thenReturn(true);

        rotina.executar();

        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Reuniao a mais de 24 horas nao recebe lembrete nem muda de status")
    void ignoraReuniaoDistante() {
        Agendamento distante = confirmada(LocalDateTime.now().plusHours(30));
        confirmadasNoBanco(distante);

        rotina.executar();

        assertThat(distante.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        verify(notificacaoRepository, never()).save(any());
    }

    private void confirmadasNoBanco(Agendamento... agendamentos) {
        when(agendamentoRepository.findByStatusAndDataReuniaoLessThanEqual(eq(StatusAgendamento.CONFIRMADO), any()))
                .thenReturn(List.of(agendamentos));
    }

    private List<Notificacao> avisosSalvos(int quantidade) {
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(quantidade)).save(captor.capture());
        return captor.getAllValues();
    }

    private Agendamento confirmada(LocalDateTime inicio) {
        Agendamento a = Agendamento.builder()
                .id(10L)
                .organizador(aluno)
                .duracao(DuracaoReuniao.builder().minutos(30).build())
                .dataReuniao(inicio.toLocalDate())
                .horarioInicio(inicio.toLocalTime().withSecond(0).withNano(0))
                .topico("Orientação do TCC")
                .status(StatusAgendamento.CONFIRMADO)
                .build();
        a.adicionarParticipante(participante(aluno, PapelParticipante.ORGANIZADOR));
        a.adicionarParticipante(participante(professor, PapelParticipante.CONVIDADO));
        return a;
    }

    private static AgendamentoParticipante participante(Usuario usuario, PapelParticipante papel) {
        return AgendamentoParticipante.builder()
                .usuario(usuario)
                .papel(papel)
                .statusResposta(StatusResposta.ACEITO)
                .build();
    }

    private static Usuario usuario(Long id, String nome, TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setTipoUsuario(tipo);
        return u;
    }
}
