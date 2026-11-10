-- Alinha registro_motivo.justificativa com a entidade (columnDefinition = TEXT) e com o Supabase.
-- Antes: VARCHAR(200) no MySQL, enquanto a API aceita motivos de ate 500 caracteres
-- (cancelamento e recusa) - motivos maiores que 200 falhavam so no MySQL.
--
-- Rode uma vez em bancos MySQL ja existentes (o db_schedemy.sql e o dump_schedemy.sql ja vem corrigidos):
--   mysql -u root schedemy_db < db/migracoes/2026-11-10_justificativa_text.sql
-- No Supabase (PostgreSQL) a coluna ja e TEXT: nada a fazer.

ALTER TABLE registro_motivo MODIFY justificativa TEXT NOT NULL;
