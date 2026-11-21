-- RF 22 (decisao de 21/11/2026): grava quem decidiu o pedido de alteracao e, na recusa, o motivo.
-- solicitacao_edicao ganha:
-- - id_respondido_por (FK usuario, opcional): o docente ou o admin que aceitou ou recusou. Fica vazio quando o
--   pedido expira, quem pediu desiste ou a reuniao e cancelada/remarcada;
-- - motivo_recusa (opcional): o motivo da recusa (RF 11), que continua sendo gravado tambem em registro_motivo.
-- Pedidos ja respondidos antes desta migracao ficam sem os dois valores (o motivo segue no historico).
-- Depende de 2026-11-21_solicitacao_edicao_mysql.sql. MySQL/MariaDB; rode uma vez em bancos ja criados:
--   mysql -u root schedemy_db < db/migracoes/2026-11-21_solicitacao_respondido_por_mysql.sql
-- PostgreSQL (Supabase): use 2026-11-21_solicitacao_respondido_por_postgresql.sql.

ALTER TABLE solicitacao_edicao
    ADD COLUMN id_respondido_por BIGINT UNSIGNED NULL
        COMMENT 'Docente ou admin que aceitou ou recusou (RF 22); vazio se expirou ou foi cancelado'
        AFTER respondido_em,
    ADD COLUMN motivo_recusa VARCHAR(500) NULL
        COMMENT 'Motivo de quem recusou (RF 11); tambem fica em registro_motivo'
        AFTER id_respondido_por,
    ADD CONSTRAINT fk_solicitacao_respondido_por
        FOREIGN KEY (id_respondido_por) REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT ON UPDATE CASCADE;
