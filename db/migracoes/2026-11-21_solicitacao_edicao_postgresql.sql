-- RF 21/22 (decisao de 21/11/2026): pedido de alteracao de reuniao confirmada.
-- Mesmo conteudo de 2026-11-21_solicitacao_edicao_mysql.sql, para o PostgreSQL (perfil supabase).
-- So acrescenta valores aceitos pelas restricoes CHECK: nada existente muda.
-- Atencao: no Supabase o banco e compartilhado - combine com a equipe antes de rodar.

ALTER TABLE registro_motivo DROP CONSTRAINT chk_motivo_tipo;
ALTER TABLE registro_motivo ADD CONSTRAINT chk_motivo_tipo CHECK (tipo_operacao IN
    ('CANCELAMENTO','SOLICITACAO_EDICAO','RECUSA_AGENDAMENTO','RECUSA_REMARCACAO','RECUSA_SOLICITACAO_EDICAO'));

ALTER TABLE solicitacao_edicao DROP CONSTRAINT chk_solicitacao_status;
ALTER TABLE solicitacao_edicao ADD CONSTRAINT chk_solicitacao_status CHECK (status_solicitacao IN
    ('PENDENTE','ACEITA','RECUSADA','CANCELADA','EXPIRADA'));
