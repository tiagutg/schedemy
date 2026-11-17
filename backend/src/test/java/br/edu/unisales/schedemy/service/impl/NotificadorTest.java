package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/** RF 23 / RN 07 - cada aviso vai para a tela (SISTEMA) e para a fila de e-mail (EMAIL). Roda sem banco. */
@ExtendWith(MockitoExtension.class)
class NotificadorTest {

    @Mock private NotificacaoRepository notificacaoRepository;

    @Test
    @DisplayName("Cancelamento: aviso no sistema para todos; e-mail so para quem esta ativo e tem e-mail")
    void enfileiraEmail() {
        Usuario aluno = usuario(1L, "Kevin Lins", TipoUsuario.ALUNO, "kevin.aluno@unisales.edu.br", true);
        Usuario professor = usuario(2L, "Rômulo Ferreira", TipoUsuario.PROFESSOR, "romulo.professor@unisales.edu.br", true);
        Usuario inativo = usuario(3L, "Ex Professor", TipoUsuario.PROFESSOR, "ex@unisales.edu.br", false);
        Usuario semEmail = usuario(4L, "Sem Email", TipoUsuario.ALUNO, " ", true);
        Agendamento a = reuniao(aluno, professor, inativo, semEmail);

        new Notificador(notificacaoRepository).cancelamento(a, aluno);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(n -> n.getUsuario().getId(), Notificacao::getCanalEnvio)
                .containsExactlyInAnyOrder(
                        tuple(2L, CanalEnvio.SISTEMA), tuple(2L, CanalEnvio.EMAIL),
                        tuple(3L, CanalEnvio.SISTEMA),
                        tuple(4L, CanalEnvio.SISTEMA));
        List<Notificacao> doProfessor = captor.getAllValues().stream().filter(n -> n.getUsuario() == professor).toList();
        assertThat(doProfessor).allSatisfy(n -> {
            assertThat(n.getStatusEnvio()).isEqualTo(StatusEnvio.PENDENTE);
            assertThat(n.getTipoEvento()).isEqualTo(Notificador.CANCELAMENTO);
            assertThat(n.getMensagem()).isEqualTo("Kevin cancelou \"TCC\" de 20/11 às 14:00.");
        });
    }

    private static Agendamento reuniao(Usuario organizador, Usuario... convidados) {
        Agendamento a = Agendamento.builder()
                .id(10L)
                .organizador(organizador)
                .duracao(DuracaoReuniao.builder().minutos(30).build())
                .dataReuniao(LocalDate.of(2026, 11, 20))
                .horarioInicio(LocalTime.of(14, 0))
                .formato(FormatoReuniao.PRESENCIAL)
                .topico("TCC")
                .build();
        a.getParticipantes().add(AgendamentoParticipante.builder().agendamento(a).usuario(organizador)
                .papel(PapelParticipante.ORGANIZADOR).build());
        for (Usuario c : convidados) {
            a.getParticipantes().add(AgendamentoParticipante.builder().agendamento(a).usuario(c)
                    .papel(PapelParticipante.CONVIDADO).build());
        }
        return a;
    }

    private static Usuario usuario(Long id, String nome, TipoUsuario tipo, String email, boolean ativo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setTipoUsuario(tipo);
        u.setEmail(email);
        u.setAtivo(ativo);
        return u;
    }
}
