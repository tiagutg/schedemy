package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.DuracaoReuniao;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.SalaVirtual;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** RF 23 - fila de e-mail (canal EMAIL da tabela notificacao). Roda sem banco e sem SMTP. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnvioDeEmailsTest {

    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private JavaMailSender mailSender;
    private EnvioDeEmails envio;

    private final Usuario aluno = usuario(2L, "Kevin Lins", "kevin.aluno@unisales.edu.br");
    private final Usuario professor = usuario(1L, "Rômulo Ferreira", "romulo.professor@unisales.edu.br");

    @BeforeEach
    void preparar() {
        envio = new EnvioDeEmails(notificacaoRepository, mailSender,
                "Schedemy <nao-responda@unisales.edu.br>", "http://localhost:5173/", 50, Duration.ofHours(24));
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    @DisplayName("Envia os pendentes numa chamada so e marca ENVIADO com a data do envio")
    void enviaPendentes() throws Exception {
        Notificacao convite = email(aluno, Notificador.CONVITE, "Rômulo convidou você para \"TCC\" em 20/11 às 14:00.");
        Notificacao lembrete = email(professor, Notificador.LEMBRETE, "Lembrete: \"TCC\" acontece em 20/11 às 14:00.");
        naFila(convite, lembrete);

        envio.executar();

        MimeMessage[] enviadas = enviadas();
        assertThat(enviadas).hasSize(2);
        MimeMessage primeira = enviadas[0];
        assertThat(primeira.getAllRecipients()).extracting(Address::toString).containsExactly("kevin.aluno@unisales.edu.br");
        assertThat(primeira.getFrom()[0].toString()).contains("nao-responda@unisales.edu.br");
        assertThat(primeira.getSubject()).isEqualTo("Schedemy: Convite - TCC");
        assertThat(primeira.getHeader("Auto-Submitted")).containsExactly("auto-generated");
        String corpo = primeira.getContent().toString();
        assertThat(corpo).startsWith("Olá, Kevin.")
                .contains("Rômulo convidou você", "Data: 20/11/2026 (sexta-feira)", "Horário: 14:00 às 14:30",
                        "Formato: On-line", "Sala virtual: https://teams.exemplo/sala",
                        "http://localhost:5173/agendamentos?abrir=10");
        assertThat(List.of(convite, lembrete)).allSatisfy(n -> {
            assertThat(n.getStatusEnvio()).isEqualTo(StatusEnvio.ENVIADO);
            assertThat(n.getEnviadoEm()).isNotNull();
        });
    }

    @Test
    @DisplayName("Servidor fora do ar: nada sai e tudo continua PENDENTE para a proxima rodada")
    void servidorForaDoAr() {
        Notificacao convite = email(aluno, Notificador.CONVITE, "Convite");
        Notificacao lembrete = email(professor, Notificador.LEMBRETE, "Lembrete");
        naFila(convite, lembrete);
        doThrow(new MailAuthenticationException("535 credenciais invalidas")).when(mailSender).send(any(MimeMessage[].class));

        envio.executar();

        assertThat(List.of(convite, lembrete)).allSatisfy(n -> assertThat(n.getStatusEnvio()).isEqualTo(StatusEnvio.PENDENTE));
    }

    @Test
    @DisplayName("Conexao caiu no meio: o que saiu vira ENVIADO, o resto continua PENDENTE")
    void conexaoCaiuNoMeio() {
        Notificacao primeira = email(aluno, Notificador.CONVITE, "Convite");
        Notificacao segunda = email(professor, Notificador.CONVITE, "Convite");
        naFila(primeira, segunda);
        falhaNoEnvio(1, new MessagingException("Connection reset"));

        envio.executar();

        assertThat(primeira.getStatusEnvio()).isEqualTo(StatusEnvio.ENVIADO);
        assertThat(segunda.getStatusEnvio()).isEqualTo(StatusEnvio.PENDENTE);
    }

    @Test
    @DisplayName("Destinatario recusado pelo servidor (550) vira FALHA e nao e tentado de novo")
    void destinatarioRecusado() throws Exception {
        Notificacao recusada = email(aluno, Notificador.CONVITE, "Convite");
        Notificacao boa = email(professor, Notificador.CONVITE, "Convite");
        naFila(recusada, boa);
        Address[] invalidos = {new InternetAddress("kevin.aluno@unisales.edu.br")};
        falhaNoEnvio(0, new SendFailedException("550 caixa inexistente", null, new Address[0], new Address[0], invalidos));

        envio.executar();

        assertThat(recusada.getStatusEnvio()).isEqualTo(StatusEnvio.FALHA);
        assertThat(boa.getStatusEnvio()).isEqualTo(StatusEnvio.ENVIADO);
    }

    @Test
    @DisplayName("Usuario inativo, sem e-mail ou com e-mail invalido: FALHA sem chamar o servidor")
    void destinatarioSemCondicao() {
        Usuario inativo = usuario(3L, "Ex Aluno", "ex@unisales.edu.br");
        inativo.setAtivo(false);
        Notificacao paraInativo = email(inativo, Notificador.CONVITE, "Convite");
        Notificacao invalido = email(usuario(4L, "Fulano", "nao e e-mail@@"), Notificador.CONVITE, "Convite");
        naFila(paraInativo, invalido);

        envio.executar();

        verify(mailSender, never()).send(any(MimeMessage[].class));
        assertThat(List.of(paraInativo, invalido)).allSatisfy(n -> assertThat(n.getStatusEnvio()).isEqualTo(StatusEnvio.FALHA));
    }

    @Test
    @DisplayName("E-mails parados na fila alem da validade viram FALHA")
    void expiraPendentesAntigos() {
        naFila();
        LocalDateTime antes = LocalDateTime.now();

        envio.executar();

        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(notificacaoRepository).expirarEmailsPendentes(limite.capture());
        assertThat(limite.getValue()).isBetween(antes.minusHours(24).minusSeconds(1), LocalDateTime.now().minusHours(24));
        verify(mailSender, never()).send(any(MimeMessage[].class));
    }

    @Test
    @DisplayName("Aviso sem agendamento e reuniao presencial: sem linha de sala e link para os avisos")
    void corpoSemAgendamento() throws IOException, MessagingException {
        Notificacao avulsa = email(aluno, "OUTRO", "Aviso geral.");
        avulsa.setAgendamento(null);
        assertThat(EnvioDeEmails.assunto(avulsa)).isEqualTo("Schedemy: Aviso");
        assertThat(EnvioDeEmails.corpo(avulsa, "http://front")).contains("Aviso geral.", "http://front/notificacoes")
                .doesNotContain("Sala virtual");

        Notificacao presencial = email(aluno, Notificador.CANCELAMENTO, "Cancelada.");
        presencial.getAgendamento().setFormato(FormatoReuniao.PRESENCIAL);
        assertThat(EnvioDeEmails.assunto(presencial)).isEqualTo("Schedemy: Reunião cancelada - TCC");
        assertThat(EnvioDeEmails.corpo(presencial, "http://front")).contains("Formato: Presencial").doesNotContain("Sala virtual");
    }

    private void naFila(Notificacao... notificacoes) {
        when(notificacaoRepository.buscarEmailsPendentes(any())).thenReturn(List.of(notificacoes));
    }

    /** Simula o JavaMailSenderImpl: a mensagem na posicao indicada falha com o erro dado. */
    private void falhaNoEnvio(int posicao, Exception erro) {
        doAnswer(chamada -> {
            // send(MimeMessage...) e varargs: o Mockito entrega cada mensagem como um argumento.
            Map<Object, Exception> falharam = new LinkedHashMap<>();
            falharam.put(chamada.getArguments()[posicao], erro);
            throw new MailSendException(falharam);
        }).when(mailSender).send(any(MimeMessage[].class));
    }

    private MimeMessage[] enviadas() {
        ArgumentCaptor<MimeMessage[]> captor = ArgumentCaptor.forClass(MimeMessage[].class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private Notificacao email(Usuario usuario, String tipo, String mensagem) {
        Agendamento a = Agendamento.builder()
                .id(10L)
                .organizador(professor)
                .duracao(DuracaoReuniao.builder().minutos(30).build())
                .dataReuniao(LocalDate.of(2026, 11, 20))
                .horarioInicio(LocalTime.of(14, 0))
                .formato(FormatoReuniao.ONLINE)
                .topico("TCC")
                .build();
        a.setSalaVirtual(SalaVirtual.builder().agendamento(a).linkTeams("https://teams.exemplo/sala").build());
        return Notificacao.builder()
                .usuario(usuario)
                .agendamento(a)
                .canalEnvio(CanalEnvio.EMAIL)
                .tipoEvento(tipo)
                .mensagem(mensagem)
                .build();
    }

    private static Usuario usuario(Long id, String nome, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setTipoUsuario(TipoUsuario.ALUNO);
        u.setAtivo(true);
        return u;
    }
}
