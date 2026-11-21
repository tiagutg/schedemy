package br.edu.unisales.schedemy.domain.enums;

// Reflete o ENUM tipo_operacao da tabela registro_motivo.
public enum TipoOperacaoMotivo {
    CANCELAMENTO,
    SOLICITACAO_EDICAO,
    RECUSA_AGENDAMENTO,
    RECUSA_REMARCACAO,
    /** RF 22 - motivo de quem recusa o pedido de alteracao (RF 11). */
    RECUSA_SOLICITACAO_EDICAO
}