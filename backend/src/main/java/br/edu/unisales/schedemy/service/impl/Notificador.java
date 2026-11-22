package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.StatusResposta;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * RF 23 / RN 07 - avisa os envolvidos em cada mudanca de um agendamento.
 *
 * Cada aviso vira duas linhas em notificacao:
 * - canal SISTEMA: aviso dentro do sistema; PENDENTE significa "nao lido" e ENVIADO, "lido";
 * - canal EMAIL: fila de e-mail; PENDENTE ate o {@link EnvioDeEmails} enviar (ENVIADO) ou desistir (FALHA).
 *   So para usuario ativo e com e-mail cadastrado.
 */
@Component
@RequiredArgsConstructor
public class Notificador {
    public static final String CONVITE = "CONVITE";
    public static final String REMARCACAO = "REMARCACAO";
    public static final String AGENDADO_POR_TERCEIRO = "AGENDADO_POR_TERCEIRO";
    public static final String ALTERACAO = "ALTERACAO";
    public static final String RESPOSTA = "RESPOSTA";
    public static final String CONFIRMACAO = "CONFIRMACAO";
    public static final String CANCELAMENTO = "CANCELAMENTO";
    /** RF 06/RF 10 - cancelada porque a agenda do docente mudou; o aviso leva ao "Agendar de novo" (RF 28). */
    public static final String CANCELAMENTO_AGENDA = "CANCELAMENTO_AGENDA";
    public static final String LEMBRETE = "LEMBRETE";
    /** RF 21/22 - pedido de alteracao de reuniao confirmada e a decisao do docente. */
    public static final String SOLICITACAO_EDICAO = "SOLICITACAO_EDICAO";
    public static final String SOLICITACAO_ACEITA = "SOLICITACAO_ACEITA";
    public static final String SOLICITACAO_RECUSADA = "SOLICITACAO_RECUSADA";
    public static final String SOLICITACAO_ENCERRADA = "SOLICITACAO_ENCERRADA";
    public static final String AVALIACAO_PENDENTE = "AVALIACAO_PENDENTE";

    private static final int TAMANHO_MAXIMO_MENSAGEM = 200;
    private static final int TAMANHO_MAXIMO_TOPICO = 60;
    /** Abaixo disso o motivo encurtado nao diz nada: fica so no registro_motivo. */
    private static final int TAMANHO_MINIMO_MOTIVO = 10;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificacaoRepository notificacaoRepository;

    /** Convite aos convidados de uma reuniao nova (ou remarcada) e aviso ao organizador quando a recepcao agendou por ele. */
    public void novoAgendamento(Agendamento agendamento, Usuario autor) {
        boolean remarcacao = agendamento.getAgendamentoOrigem() != null;
        String tipo = remarcacao ? REMARCACAO : CONVITE;
        String texto = remarcacao
                ? "%s remarcou \"%s\" para %s. Confirme se pode participar."
                : "%s convidou você para \"%s\" em %s.";
        for (Usuario convidado : convidados(agendamento)) {
            enviar(convidado, agendamento, tipo,
                    texto.formatted(primeiroNome(agendamento.getOrganizador()), topico(agendamento), quando(agendamento)));
        }
        if (!agendamento.getOrganizador().getId().equals(autor.getId())) {
            enviar(agendamento.getOrganizador(), agendamento, AGENDADO_POR_TERCEIRO,
                    "%s agendou \"%s\" em seu nome para %s."
                            .formatted(primeiroNome(autor), topico(agendamento), quando(agendamento)));
        }
    }

    /** Os convidados precisam responder de novo depois de uma edicao. */
    public void alteracao(Agendamento agendamento, Usuario autor) {
        for (Usuario convidado : convidados(agendamento)) {
            if (!convidado.getId().equals(autor.getId())) {
                enviar(convidado, agendamento, ALTERACAO,
                        "\"%s\" foi alterada: agora em %s. Responda de novo ao convite."
                                .formatted(topico(agendamento), quando(agendamento)));
            }
        }
    }

    /** RN 18 - avisa a resposta ao organizador e, se o status mudou, a todos os envolvidos. */
    public void resposta(Agendamento agendamento, Usuario quemRespondeu, boolean aceitou, StatusAgendamento statusAnterior) {
        StatusAgendamento atual = agendamento.getStatus();
        if (atual == StatusAgendamento.CONFIRMADO && statusAnterior != StatusAgendamento.CONFIRMADO) {
            for (Usuario envolvido : envolvidos(agendamento)) {
                if (!envolvido.getId().equals(quemRespondeu.getId())) {
                    enviar(envolvido, agendamento, CONFIRMACAO,
                            "\"%s\" está confirmada para %s.".formatted(topico(agendamento), quando(agendamento)));
                }
            }
            return;
        }
        if (atual == StatusAgendamento.CANCELADO && statusAnterior != StatusAgendamento.CANCELADO) {
            for (Usuario envolvido : envolvidos(agendamento)) {
                if (!envolvido.getId().equals(quemRespondeu.getId())) {
                    enviar(envolvido, agendamento, CANCELAMENTO,
                            "\"%s\" de %s foi cancelada: %s recusou o convite."
                                    .formatted(topico(agendamento), quando(agendamento), primeiroNome(quemRespondeu)));
                }
            }
            return;
        }
        Usuario organizador = agendamento.getOrganizador();
        if (!organizador.getId().equals(quemRespondeu.getId())) {
            enviar(organizador, agendamento, RESPOSTA,
                    "%s %s o convite para \"%s\"."
                            .formatted(primeiroNome(quemRespondeu), aceitou ? "aceitou" : "recusou", topico(agendamento)));
        }
    }

    public void cancelamento(Agendamento agendamento, Usuario autor) {
        for (Usuario envolvido : envolvidos(agendamento)) {
            if (!envolvido.getId().equals(autor.getId())) {
                enviar(envolvido, agendamento, CANCELAMENTO,
                        "%s cancelou \"%s\" de %s."
                                .formatted(primeiroNome(autor), topico(agendamento), quando(agendamento)));
            }
        }
    }

    /**
     * RF 06/RF 10 - a reuniao foi cancelada porque a agenda do docente mudou. Avisa todos os envolvidos menos quem
     * fez a mudanca: se foi a administracao, o proprio docente tambem recebe. O motivo e encurtado para caber no
     * aviso (o texto inteiro fica no registro_motivo).
     */
    public void canceladaPorMudancaDeAgenda(Agendamento agendamento, Usuario docente, Usuario autor, String motivo) {
        String inicio = "\"%s\" de %s foi cancelada porque a agenda de %s mudou"
                .formatted(topico(agendamento), quando(agendamento), primeiroNome(docente));
        String fim = ". Use \"Agendar de novo\" para escolher outro horário.";
        String texto = comMotivo(inicio, fim, motivo);
        for (Usuario envolvido : envolvidos(agendamento)) {
            if (!envolvido.getId().equals(autor.getId())) {
                enviar(envolvido, agendamento, CANCELAMENTO_AGENDA, texto);
            }
        }
    }

    /** RF 21 - pede aos docentes (RN 12) que decidam; o organizador e avisado se a recepcao pediu por ele. */
    public void pedidoDeAlteracao(Agendamento agendamento, Usuario solicitante, String novoQuando) {
        for (Usuario envolvido : envolvidos(agendamento)) {
            if (envolvido.getId().equals(solicitante.getId())) {
                continue;
            }
            if (CalculadoraDeAgenda.ehDocente(envolvido)) {
                enviar(envolvido, agendamento, SOLICITACAO_EDICAO,
                        "%s pediu para alterar \"%s\" de %s para %s. Aceite ou recuse o pedido."
                                .formatted(primeiroNome(solicitante), topico(agendamento), quando(agendamento), novoQuando));
            } else if (envolvido.getId().equals(agendamento.getOrganizador().getId())) {
                enviar(envolvido, agendamento, SOLICITACAO_EDICAO,
                        "%s pediu, em seu nome, para alterar \"%s\" para %s. Aguarde a resposta do docente."
                                .formatted(primeiroNome(solicitante), topico(agendamento), novoQuando));
            }
        }
    }

    /**
     * RF 22 - pedido aceito: a reuniao ja esta com os dados novos. Quem ainda precisa responder (convidado novo)
     * recebe o pedido de confirmacao; quem saiu da reuniao, o aviso de que nao participa mais.
     */
    public void alteracaoAceita(Agendamento agendamento, Usuario docente, List<Usuario> removidos) {
        for (AgendamentoParticipante p : agendamento.getParticipantes()) {
            Usuario u = p.getUsuario();
            if (u == null || u.getId().equals(docente.getId())) {
                continue;
            }
            boolean responder = p.getStatusResposta() == StatusResposta.PENDENTE;
            enviar(u, agendamento, SOLICITACAO_ACEITA, (responder
                    ? "%s aceitou o pedido: \"%s\" agora é em %s. Confirme se pode participar."
                    : "%s aceitou o pedido: \"%s\" agora é em %s.")
                    .formatted(primeiroNome(docente), topico(agendamento), quando(agendamento)));
        }
        for (Usuario u : removidos) {
            enviar(u, agendamento, SOLICITACAO_ACEITA, "Você não participa mais de \"%s\": a reunião foi alterada."
                    .formatted(topico(agendamento)));
        }
    }

    /** RF 22 - pedido recusado: a reuniao continua como estava. */
    public void alteracaoRecusada(Agendamento agendamento, Usuario solicitante, Usuario docente, String motivo) {
        String texto = "%s recusou o pedido de alteração de \"%s\", que continua em %s"
                .formatted(primeiroNome(docente), topico(agendamento), quando(agendamento));
        texto = comMotivo(texto, ".", motivo);
        for (Usuario u : envolvidosEMais(agendamento, solicitante)) {
            if (!u.getId().equals(docente.getId())) {
                enviar(u, agendamento, SOLICITACAO_RECUSADA, texto);
            }
        }
    }

    /** RF 21 - o pedido deixou de valer: quem pediu desistiu ou o prazo (24 h antes, RN 14) passou sem resposta. */
    public void pedidoEncerrado(Agendamento agendamento, Usuario solicitante, Usuario autor, boolean expirou) {
        String texto = expirou
                ? "O pedido de alteração de \"%s\" expirou sem resposta: a reunião continua em %s."
                .formatted(topico(agendamento), quando(agendamento))
                : "%s desistiu do pedido de alteração de \"%s\": a reunião continua em %s."
                .formatted(primeiroNome(autor), topico(agendamento), quando(agendamento));
        for (Usuario u : envolvidosEMais(agendamento, solicitante)) {
            if (autor == null || !u.getId().equals(autor.getId())) {
                enviar(u, agendamento, SOLICITACAO_ENCERRADA, texto);
            }
        }
    }

    /** RN 07/RN 18 - a reuniao chegou ao horario sem as confirmacoes necessarias e foi cancelada pelo sistema. */
    public void naoConfirmada(Agendamento agendamento) {
        for (Usuario envolvido : envolvidos(agendamento)) {
            enviar(envolvido, agendamento, CANCELAMENTO,
                    "\"%s\" de %s foi cancelada: não houve confirmação até o horário marcado."
                            .formatted(topico(agendamento), quando(agendamento)));
        }
    }

    public void lembrete(Agendamento agendamento, Usuario usuario) {
        enviar(usuario, agendamento, LEMBRETE,
                "Lembrete: \"%s\" acontece em %s.".formatted(topico(agendamento), quando(agendamento)));
    }

    /** RF 24 - o aluno avalia a reuniao concluida. */
    public void avaliacaoPendente(Agendamento agendamento) {
        for (Usuario envolvido : envolvidos(agendamento)) {
            if (envolvido.getTipoUsuario() == TipoUsuario.ALUNO) {
                enviar(envolvido, agendamento, AVALIACAO_PENDENTE,
                        "\"%s\" foi concluída. Conte como foi: avalie a reunião.".formatted(topico(agendamento)));
            }
        }
    }

    /** Coloca o motivo entre parenteses antes do final, encurtado para caber no aviso; sem espaco, fica de fora. */
    private static String comMotivo(String inicio, String fim, String motivo) {
        int espaco = TAMANHO_MAXIMO_MENSAGEM - inicio.length() - fim.length() - " ()".length();
        String motivoLimpo = motivo == null ? "" : motivo.trim().replaceAll("\\s+", " ");
        if (!motivoLimpo.isEmpty() && espaco >= TAMANHO_MINIMO_MOTIVO) {
            return inicio + " (" + limitar(motivoLimpo, espaco) + ")" + fim;
        }
        return inicio + fim;
    }

    /** Envolvidos da reuniao e, se nao for um deles (recepcao), quem fez o pedido. */
    private static List<Usuario> envolvidosEMais(Agendamento agendamento, Usuario extra) {
        List<Usuario> lista = new java.util.ArrayList<>(envolvidos(agendamento));
        if (extra != null && lista.stream().noneMatch(u -> u.getId().equals(extra.getId()))) {
            lista.add(extra);
        }
        return lista;
    }

    private void enviar(Usuario usuario, Agendamento agendamento, String tipo, String mensagem) {
        String texto = limitar(mensagem, TAMANHO_MAXIMO_MENSAGEM);
        notificacaoRepository.save(aviso(usuario, agendamento, tipo, texto, CanalEnvio.SISTEMA));
        if (recebeEmail(usuario)) {
            notificacaoRepository.save(aviso(usuario, agendamento, tipo, texto, CanalEnvio.EMAIL));
        }
    }

    private static Notificacao aviso(Usuario usuario, Agendamento agendamento, String tipo, String mensagem,
                                     CanalEnvio canal) {
        return Notificacao.builder()
                .usuario(usuario)
                .agendamento(agendamento)
                .canalEnvio(canal)
                .tipoEvento(tipo)
                .mensagem(mensagem)
                .build();
    }

    private static boolean recebeEmail(Usuario usuario) {
        return !Boolean.FALSE.equals(usuario.getAtivo()) && usuario.getEmail() != null && !usuario.getEmail().isBlank();
    }

    private static List<Usuario> convidados(Agendamento agendamento) {
        return agendamento.getParticipantes().stream()
                .filter(p -> p.getPapel() == PapelParticipante.CONVIDADO)
                .map(AgendamentoParticipante::getUsuario)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<Usuario> envolvidos(Agendamento agendamento) {
        return agendamento.getParticipantes().stream()
                .map(AgendamentoParticipante::getUsuario)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String quando(Agendamento agendamento) {
        return quando(agendamento.getDataReuniao(), agendamento.getHorarioInicio());
    }

    /** "dd/MM às HH:mm", como nos demais avisos. */
    static String quando(java.time.LocalDate data, java.time.LocalTime horario) {
        return "%s às %s".formatted(data.format(DATA), horario.format(HORA));
    }

    private static String topico(Agendamento agendamento) {
        return limitar(agendamento.getTopico(), TAMANHO_MAXIMO_TOPICO);
    }

    private static String primeiroNome(Usuario usuario) {
        String nome = usuario.getNome() == null ? "" : usuario.getNome().trim();
        int espaco = nome.indexOf(' ');
        return espaco > 0 ? nome.substring(0, espaco) : nome;
    }

    private static String limitar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }
}
