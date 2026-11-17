package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.FormatoReuniao;
import br.edu.unisales.schedemy.domain.enums.StatusEnvio;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RF 23 / RN 07 / RN 11 - envia por e-mail (SMTP) os avisos que o {@link Notificador} enfileira com canal EMAIL.
 *
 * A cada intervalo pega um lote de PENDENTE (mais antigos primeiro) e envia numa conexao so:
 * - aceito pelo servidor -> ENVIADO;
 * - destinatario recusado pelo servidor, e-mail invalido ou usuario inativo -> FALHA (nao adianta tentar de novo);
 * - servidor fora do ar, autenticacao ou erro temporario -> continua PENDENTE e volta na proxima rodada;
 * - PENDENTE ha mais que schedemy.email.validade -> FALHA (aviso velho nao sai mais).
 *
 * Desligado por padrao: so existe com schedemy.email.ativo=true (SCHEDEMY_EMAIL_ATIVO). Desligado, os e-mails
 * ficam na fila como PENDENTE. Com mais de uma instancia da API, o mesmo e-mail pode sair duas vezes.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "schedemy.email.ativo", havingValue = "true")
public class EnvioDeEmails {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy (EEEE)", PT_BR);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoRepository notificacaoRepository;
    private final JavaMailSender mailSender;
    private final String remetente;
    private final String urlFront;
    private final int lote;
    private final Duration validade;

    public EnvioDeEmails(NotificacaoRepository notificacaoRepository,
                         JavaMailSender mailSender,
                         @Value("${schedemy.email.remetente}") String remetente,
                         @Value("${schedemy.email.url-front}") String urlFront,
                         @Value("${schedemy.email.lote:50}") int lote,
                         @Value("${schedemy.email.validade:PT24H}") Duration validade) {
        this.notificacaoRepository = notificacaoRepository;
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.urlFront = urlFront.endsWith("/") ? urlFront.substring(0, urlFront.length() - 1) : urlFront;
        this.lote = lote;
        this.validade = validade;
    }

    @Scheduled(initialDelayString = "${schedemy.email.atraso-inicial:PT20S}",
            fixedDelayString = "${schedemy.email.intervalo:PT1M}")
    @Transactional
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        int expirados = notificacaoRepository.expirarEmailsPendentes(agora.minus(validade));
        if (expirados > 0) {
            log.warn("Envio de e-mails: {} e-mail(s) passaram de {} na fila e foram marcados como FALHA.", expirados, validade);
        }

        int falhas = 0;
        Map<MimeMessage, Notificacao> mensagens = new LinkedHashMap<>();
        for (Notificacao n : notificacaoRepository.buscarEmailsPendentes(PageRequest.of(0, lote))) {
            if (!podeReceber(n.getUsuario())) {
                n.setStatusEnvio(StatusEnvio.FALHA);
                falhas++;
                continue;
            }
            try {
                mensagens.put(montar(n), n);
            } catch (MessagingException e) {
                // Endereco que nem e um e-mail valido: nao adianta tentar de novo.
                log.warn("Envio de e-mails: notificacao {} com e-mail invalido ({}).", n.getId(), e.getMessage());
                n.setStatusEnvio(StatusEnvio.FALHA);
                falhas++;
            }
        }
        if (mensagens.isEmpty()) {
            return;
        }

        Map<Object, Exception> recusadas = Map.of();
        try {
            mailSender.send(mensagens.keySet().toArray(MimeMessage[]::new));
        } catch (MailSendException e) {
            // Conexao que caiu no meio ou destinatarios recusados: as que nao estao no mapa sairam.
            recusadas = e.getFailedMessages();
        } catch (MailException e) {
            // Autenticacao ou configuracao: nada saiu; tenta de novo na proxima rodada.
            log.warn("Envio de e-mails: servidor SMTP recusou o envio ({}). Nova tentativa na proxima rodada.", e.getMessage());
            return;
        }

        int enviados = 0;
        int adiados = 0;
        for (Map.Entry<MimeMessage, Notificacao> item : mensagens.entrySet()) {
            Notificacao n = item.getValue();
            Exception erro = recusadas.get(item.getKey());
            if (erro == null) {
                n.setStatusEnvio(StatusEnvio.ENVIADO);
                n.setEnviadoEm(agora);
                enviados++;
            } else if (destinatarioRecusado(erro)) {
                n.setStatusEnvio(StatusEnvio.FALHA);
                falhas++;
            } else {
                adiados++;
            }
        }
        if (adiados > 0) {
            log.warn("Envio de e-mails: {} e-mail(s) nao sairam e ficam para a proxima rodada ({}).",
                    adiados, recusadas.values().iterator().next().getMessage());
        }
        log.info("Envio de e-mails: {} enviado(s), {} com falha, {} adiado(s).", enviados, falhas, adiados);
    }

    private MimeMessage montar(Notificacao n) throws MessagingException {
        MimeMessage mensagem = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensagem, false, "UTF-8");
        helper.setFrom(remetente);
        helper.setTo(n.getUsuario().getEmail().trim());
        helper.setSubject(assunto(n));
        helper.setText(corpo(n, urlFront));
        // Pede aos servidores que nao respondam com ferias/ausencia (RFC 3834).
        mensagem.setHeader("Auto-Submitted", "auto-generated");
        return mensagem;
    }

    private static boolean podeReceber(Usuario usuario) {
        return !Boolean.FALSE.equals(usuario.getAtivo()) && usuario.getEmail() != null && !usuario.getEmail().isBlank();
    }

    /** Recusa definitiva do servidor (ex.: 550, caixa inexistente); erro temporario deixa o endereco em "validUnsent". */
    static boolean destinatarioRecusado(Exception erro) {
        return erro instanceof SendFailedException falha
                && falha.getInvalidAddresses() != null && falha.getInvalidAddresses().length > 0;
    }

    static String assunto(Notificacao n) {
        String prefixo = switch (n.getTipoEvento()) {
            case Notificador.CONVITE -> "Convite";
            case Notificador.REMARCACAO -> "Reunião remarcada";
            case Notificador.AGENDADO_POR_TERCEIRO -> "Reunião agendada em seu nome";
            case Notificador.ALTERACAO -> "Reunião alterada";
            case Notificador.RESPOSTA -> "Resposta ao convite";
            case Notificador.CONFIRMACAO -> "Reunião confirmada";
            case Notificador.CANCELAMENTO -> "Reunião cancelada";
            case Notificador.LEMBRETE -> "Lembrete";
            case Notificador.AVALIACAO_PENDENTE -> "Avalie a reunião";
            default -> "Aviso";
        };
        Agendamento a = n.getAgendamento();
        return a == null ? "Schedemy: " + prefixo : "Schedemy: %s - %s".formatted(prefixo, a.getTopico());
    }

    static String corpo(Notificacao n, String urlFront) {
        StringBuilder texto = new StringBuilder()
                .append("Olá, ").append(primeiroNome(n.getUsuario())).append(".\n\n")
                .append(n.getMensagem()).append("\n\n");
        Agendamento a = n.getAgendamento();
        if (a != null) {
            texto.append("Reunião: ").append(a.getTopico()).append('\n')
                    .append("Data: ").append(a.getDataReuniao().format(DATA)).append('\n')
                    .append("Horário: ").append(horario(a)).append('\n')
                    .append("Formato: ").append(formato(a.getFormato())).append('\n');
            if (a.getFormato() != null && a.getFormato() != FormatoReuniao.PRESENCIAL) {
                String link = a.getSalaVirtual() == null ? null : a.getSalaVirtual().getLinkTeams();
                texto.append("Sala virtual: ").append(link == null || link.isBlank() ? "ainda sem link" : link).append('\n');
            }
            texto.append("\nVer no Schedemy: ").append(urlFront).append("/agendamentos?abrir=").append(a.getId()).append('\n');
        } else {
            texto.append("Ver no Schedemy: ").append(urlFront).append("/notificacoes\n");
        }
        return texto.append("\n--\nMensagem automática do Schedemy (UniSales). Não responda a este e-mail.\n").toString();
    }

    private static String horario(Agendamento a) {
        LocalTime fim = a.getHorarioFim();
        String inicio = a.getHorarioInicio().format(HORA);
        return fim == null ? inicio : inicio + " às " + fim.format(HORA);
    }

    private static String formato(FormatoReuniao formato) {
        if (formato == null) {
            return "-";
        }
        return switch (formato) {
            case PRESENCIAL -> "Presencial";
            case ONLINE -> "On-line";
            case HIBRIDO -> "Híbrido";
        };
    }

    private static String primeiroNome(Usuario usuario) {
        String nome = usuario.getNome() == null ? "" : usuario.getNome().trim();
        int espaco = nome.indexOf(' ');
        return espaco > 0 ? nome.substring(0, espaco) : nome;
    }
}
