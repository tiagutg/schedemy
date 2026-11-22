-- RF 22 (decisao de 21/11/2026): grava quem decidiu o pedido de alteracao e, na recusa, o motivo.
-- Mesmo conteudo de 2026-11-21_solicitacao_respondido_por_mysql.sql, para o PostgreSQL (perfil supabase).
-- Depende de 2026-11-21_solicitacao_edicao_postgresql.sql.
-- Atencao: no Supabase o banco e compartilhado - combine com a equipe antes de rodar.

ALTER TABLE solicitacao_edicao
    ADD COLUMN id_respondido_por BIGINT REFERENCES usuario (id_usuario) ON UPDATE CASCADE,
    ADD COLUMN motivo_recusa VARCHAR(500);
CREATE INDEX idx_solicitacao_respondido_por ON solicitacao_edicao (id_respondido_por);
