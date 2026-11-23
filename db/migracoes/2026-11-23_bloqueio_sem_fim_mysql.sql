-- RF 05 (decisao de 23/11/2026): bloqueio recorrente pode ficar sem data de fim (vale ate o docente remove-lo).
-- data_fim passa a aceitar nulo, mas so quando e_recorrente = 1 (chk_bloqueio_fim). Bloqueios existentes nao mudam.
-- MySQL/MariaDB; rode uma vez em bancos ja criados:
--   mysql -u root schedemy_db < db/migracoes/2026-11-23_bloqueio_sem_fim_mysql.sql
-- PostgreSQL (Supabase): use 2026-11-23_bloqueio_sem_fim_postgresql.sql.

ALTER TABLE bloqueio_periodo
    MODIFY COLUMN data_fim DATE NULL COMMENT 'Vazio = recorrente sem data de fim (RF 05)',
    ADD CONSTRAINT chk_bloqueio_fim CHECK (data_fim IS NOT NULL OR e_recorrente = 1);
