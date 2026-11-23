-- RF 05 (decisao de 23/11/2026): bloqueio recorrente pode ficar sem data de fim (vale ate o docente remove-lo).
-- Mesmo conteudo de 2026-11-23_bloqueio_sem_fim_mysql.sql, para o PostgreSQL (perfil supabase).
-- Atencao: no Supabase o banco e compartilhado - combine com a equipe antes de rodar.

ALTER TABLE bloqueio_periodo
    ALTER COLUMN data_fim DROP NOT NULL,
    ADD CONSTRAINT chk_bloqueio_fim CHECK (data_fim IS NOT NULL OR e_recorrente);
