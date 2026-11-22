-- RF 21/22 (decisao de 21/11/2026): pedido de alteracao de reuniao confirmada.
-- - registro_motivo ganha RECUSA_SOLICITACAO_EDICAO (motivo de quem recusa o pedido - RF 11);
-- - solicitacao_edicao ganha CANCELADA (quem pediu desistiu ou a reuniao foi cancelada) e
--   EXPIRADA (ninguem respondeu ate 24 h antes do horario original - RN 14).
-- So acrescenta valores: nada existente muda. MySQL/MariaDB; rode uma vez em bancos ja criados:
--   mysql -u root schedemy_db < db/migracoes/2026-11-21_solicitacao_edicao_mysql.sql
-- PostgreSQL (Supabase): use 2026-11-21_solicitacao_edicao_postgresql.sql.

ALTER TABLE registro_motivo
    MODIFY tipo_operacao ENUM('CANCELAMENTO', 'SOLICITACAO_EDICAO', 'RECUSA_AGENDAMENTO', 'RECUSA_REMARCACAO',
                              'RECUSA_SOLICITACAO_EDICAO') NOT NULL;

ALTER TABLE solicitacao_edicao
    MODIFY status_solicitacao ENUM('PENDENTE', 'ACEITA', 'RECUSADA', 'CANCELADA', 'EXPIRADA') NOT NULL DEFAULT 'PENDENTE';
