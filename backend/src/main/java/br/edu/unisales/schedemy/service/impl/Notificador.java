package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.AgendamentoParticipante;
import br.edu.unisales.schedemy.domain.entity.Notificacao;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.CanalEnvio;
import br.edu.unisales.schedemy.domain.enums.PapelParticipante;
import br.edu.unisales.schedemy.domain.enums.StatusAgendamento;
import br.edu.unisales.schedemy.domain.enums.TipoUsuario;
import br.edu.unisales.schedemy.repository.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * RF 23 - avisos dentro do sistema (canal SISTEMA) para os envolvidos em cada mudanca de um agendamento.
 *
 * Para o canal SISTEMA, status PENDENTE significa "nao lida" e ENVIADO significa "lida"
 * (entregue ao usuario quando ele abre o aviso).
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
    public static final String LEMBRETE = "LEMBRETE";
    public static final String AVALIACAO_PENDENTE = "AVALIACAO_PENDENTE";

    private static final int TAMANHO_MAXIMO_MENSAGEM = 200;
    private static final int TAMANHO_MAXIMO_TOPICO = 60;
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

    private void enviar(Usuario usuario, Agendamento agendamento, String tipo, String mensagem) {
        notificacaoRepository.save(Notificacao.builder()
                .usuario(usuario)
                .agendamento(agendamento)
                .canalEnvio(CanalEnvio.SISTEMA)
                .tipoEvento(tipo)
                .mensagem(limitar(mensagem, TAMANHO_MAXIMO_MENSAGEM))
                .build());
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
        return "%s às %s".formatted(agendamento.getDataReuniao().format(DATA), agendamento.getHorarioInicio().format(HORA));
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
