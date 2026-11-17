-- RNF 13 (decisao de 12/11/2026): a reuniao fica PENDENTE ate a decisao da docencia.
-- AGUARDANDO_RESPOSTA passa a ser reservado para solicitacao de edicao (RF 21) e sugestao de
-- remarcacao (RF 08), que ainda nao existem. Antes, a primeira resposta de qualquer convidado
-- levava a reuniao para AGUARDANDO_RESPOSTA - reunioes nesse estado voltam a PENDENTE
-- (e voltam a poder ser editadas diretamente, RN 14).
--
-- Sem mudanca de schema; vale para MySQL e PostgreSQL. Rode uma vez em bancos ja existentes:
--   mysql -u root schedemy_db < db/migracoes/2026-11-12_aguardando_resposta_para_pendente.sql
-- O dump e os dados do Supabase (supabase_dados.sql) nao tem reunioes nesse estado.
-- Atencao: no Supabase o banco e compartilhado - combine com a equipe antes de rodar.

UPDATE agendamento SET status_agend = 'PENDENTE' WHERE status_agend = 'AGUARDANDO_RESPOSTA';
