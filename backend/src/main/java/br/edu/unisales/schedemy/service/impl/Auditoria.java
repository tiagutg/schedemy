package br.edu.unisales.schedemy.service.impl;

import br.edu.unisales.schedemy.domain.entity.Agendamento;
import br.edu.unisales.schedemy.domain.entity.LogAuditoria;
import br.edu.unisales.schedemy.domain.entity.Usuario;
import br.edu.unisales.schedemy.domain.enums.AcaoAuditoria;
import br.edu.unisales.schedemy.repository.LogAuditoriaRepository;
import br.edu.unisales.schedemy.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.format.DateTimeFormatter;

/**
 * RNF 19 - grava a trilha de auditoria (log_auditoria): autor, acao, registro afetado, detalhes, IP e data/hora.
 *
 * Chamado pelos services depois que a operacao passou por todas as regras. Grava na MESMA transacao: se a
 * operacao falhar depois, a linha tambem e desfeita (a trilha so mostra o que de fato aconteceu). A excecao e o
 * login recusado, que precisa ficar registrado apesar do erro (registrarEmSeparado).
 *
 * O IP e o endereco que chega ao servidor (getRemoteAddr); atras de proxy, configure
 * server.forward-headers-strategy para ele vir do cabecalho X-Forwarded-For.
 */
@Component
@RequiredArgsConstructor
public class Auditoria {

    static final int MAX_DETALHES = 200;
    private static final int MAX_IP = 45;
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LogAuditoriaRepository logAuditoriaRepository;
    private final UsuarioAutenticado usuarioAutenticado;

    /** Acao de quem esta logado. */
    public void registrar(AcaoAuditoria acao, Long idRegistro, String detalhes) {
        salvar(usuarioAutenticado.obterSeAutenticado().orElse(null), acao, idRegistro, detalhes);
    }

    /** Acao automatica (RotinaDeAgenda): sem autor, aparece como "Sistema". */
    public void registrarDoSistema(AcaoAuditoria acao, Long idRegistro, String detalhes) {
        salvar(null, acao, idRegistro, detalhes);
    }

    /**
     * Login: ainda nao ha usuario no contexto de seguranca, e a tentativa recusada termina em excecao. Transacao
     * propria para a linha ficar gravada mesmo assim.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEmSeparado(Usuario autor, AcaoAuditoria acao, Long idRegistro, String detalhes) {
        salvar(autor, acao, idRegistro, detalhes);
    }

    /** Resumo da reuniao para os detalhes: "Topico - 01/12/2026 10:00 - CONFIRMADO". */
    public static String descrever(Agendamento a) {
        return "%s - %s - %s".formatted(a.getTopico(),
                a.getDataReuniao() == null || a.getHorarioInicio() == null ? "?"
                        : a.getDataReuniao().atTime(a.getHorarioInicio()).format(DATA_HORA),
                a.getStatus());
    }

    private void salvar(Usuario autor, AcaoAuditoria acao, Long idRegistro, String detalhes) {
        logAuditoriaRepository.save(LogAuditoria.builder()
                .usuario(autor)
                .acao(acao)
                .tabelaAfetada(acao.getTabela())
                .idTabelaAfetada(idRegistro)
                .detalhes(cortar(detalhes, MAX_DETALHES))
                .ipOrigem(cortar(ipDaRequisicao(), MAX_IP))
                .build());
    }

    static String cortar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.strip();
        return limpo.length() <= maximo ? limpo : limpo.substring(0, maximo - 3) + "...";
    }

    private static String ipDaRequisicao() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos
                ? atributos.getRequest().getRemoteAddr()
                : null;
    }
}
