package br.edu.unisales.schedemy.domain.enums;

// Reflete o ENUM status_solicitacao da tabela solicitacao_edicao (RF 21/22).
public enum StatusSolicitacao {
    PENDENTE,
    ACEITA,
    RECUSADA,
    /** Quem pediu desistiu, ou a reuniao foi cancelada antes da resposta. */
    CANCELADA,
    /** Ninguem respondeu ate 24 h antes do horario original (RN 14). */
    EXPIRADA
}
