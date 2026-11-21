package br.edu.unisales.schedemy.domain.enums;

/**
 * RNF 19 - acoes gravadas na trilha de auditoria (log_auditoria.acao), com a tabela do registro afetado
 * (log_auditoria.tabela_afetada). Espelhado no front em src/utils/domain.ts (ACOES_AUDITORIA).
 */
public enum AcaoAuditoria {
    // Reunioes
    AGENDAMENTO_CRIADO("agendamento"),
    AGENDAMENTO_EDITADO("agendamento"),
    AGENDAMENTO_REMARCADO("agendamento"),
    AGENDAMENTO_RESPONDIDO("agendamento"),
    AGENDAMENTO_CANCELADO("agendamento"),
    /** RotinaDeAgenda: chegou o horario sem confirmacao (RN 08). */
    AGENDAMENTO_EXPIRADO("agendamento"),
    AGENDAMENTO_REMOVIDO("agendamento"),

    // Pedidos de alteracao (RF 21/22)
    PEDIDO_ALTERACAO_CRIADO("solicitacao_edicao"),
    PEDIDO_ALTERACAO_ACEITO("solicitacao_edicao"),
    PEDIDO_ALTERACAO_RECUSADO("solicitacao_edicao"),
    PEDIDO_ALTERACAO_DESISTIDO("solicitacao_edicao"),
    /** RotinaDeAgenda: sem resposta ate 24 h antes. */
    PEDIDO_ALTERACAO_EXPIRADO("solicitacao_edicao"),

    // Agenda do docente
    DISPONIBILIDADE_CRIADA("disponibilidade"),
    DISPONIBILIDADE_EDITADA("disponibilidade"),
    DISPONIBILIDADE_REMOVIDA("disponibilidade"),
    BLOQUEIO_CRIADO("bloqueio_periodo"),
    BLOQUEIO_EDITADO("bloqueio_periodo"),
    BLOQUEIO_REMOVIDO("bloqueio_periodo"),

    // Parametros
    DURACAO_CRIADA("duracao_reuniao"),
    DURACAO_EDITADA("duracao_reuniao"),
    DURACAO_REMOVIDA("duracao_reuniao"),

    // Cadastros e acesso
    USUARIO_CRIADO("usuario"),
    USUARIO_EDITADO("usuario"),
    USUARIO_ATIVADO("usuario"),
    USUARIO_DESATIVADO("usuario"),
    USUARIO_SENHA_ALTERADA("usuario"),
    USUARIO_REMOVIDO("usuario"),
    LOGIN("usuario"),
    /** Senha errada ou conta inativa de um e-mail cadastrado (e-mail inexistente nao tem a quem atribuir). */
    LOGIN_RECUSADO("usuario"),

    // LGPD (RNF 18)
    TERMO_ACEITO("consentimento_termo");

    private final String tabela;

    AcaoAuditoria(String tabela) {
        this.tabela = tabela;
    }

    public String getTabela() {
        return tabela;
    }
}
