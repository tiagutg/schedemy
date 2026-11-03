-- =====================================================================
-- Schedemy: dados iniciais para PostgreSQL (Supabase)
--
-- Convertido dos INSERTs do db/dump_schedemy.sql: durações de reunião e
-- os 14 usuários de teste (senhas com BCrypt; veja "Contas de acesso" no README).
-- Rode depois de db/supabase_schema.sql. Pode rodar de novo: registros que já existem são ignorados.
-- =====================================================================

INSERT INTO duracao_reuniao (id_duracao, minutos, ativo) VALUES
  (1, 10, TRUE),
  (2, 15, TRUE),
  (3, 20, TRUE),
  (4, 30, TRUE),
  (5, 45, TRUE),
  (6, 60, TRUE)
ON CONFLICT (id_duracao) DO NOTHING;

INSERT INTO usuario (id_usuario, id_microsoft, nome, email, senha, telefone_whatsapp, tipo_usuario,
                     matricula_ra, num_identificacao, departamento, ativo, criado_em, atualizado_em) VALUES
  (1, 'ms-usr-alu-001', 'Kevin Lins Carminati Andrade', 'kevin.aluno@unisales.edu.br', '$2a$10$JkiHDOFmAlB9YJyMyQlXCu6WvVNT4Xj4.AZdrU3GR.FqAe28i1.My', '5527999010001', 'ALUNO', '20261001', '', 'Análise e Desenvolvimento de Sistemas', TRUE, '2026-10-16 16:48:35', '2026-10-16 16:48:35'),
  (2, 'ms-usr-alu-002', 'Pedro Lucas Sipriano Do Nascimento', 'pedro.aluno@unisales.edu.br', '$2a$10$xkl1S1LK1PvdQMWC/LZcmutm1fEDhSKouOockgfsIP4cos9QqhKD6', '5527999010002', 'ALUNO', '20261002', '', 'Sistema de Informação', TRUE, '2026-10-16 16:49:28', '2026-10-16 16:49:28'),
  (3, 'ms-usr-alu-003', 'Tiago Lopes Alves', 'tiago.aluno@unisales.edu.br', '$2a$10$UVDlFsh3HFaf4Oj3si7A4eut0z1iK8TW/Vv2iWUuYAlzoNYaHd.Xe', '5527999010003', 'ALUNO', '20261003', '', 'Análise e Desenvolvimento de Sistemas', TRUE, '2026-10-16 16:49:58', '2026-10-16 16:49:58'),
  (4, 'ms-usr-alu-004', 'Diogo Ventorin De Freitas', 'diogo.aluno@unisales.edu.br', '$2a$10$hvbp98KFuUpM3AD8Rt3To.TaZeu2PBIV41W87dMiIDNlx3WWRmepK', '5527999010004', 'ALUNO', '20261004', '', 'Análise e Desenvolvimento de Sistemas', TRUE, '2026-10-16 16:50:30', '2026-10-16 16:50:30'),
  (5, 'ms-usr-alu-005', 'João Victor Wuthovsky Klein', 'joao.aluno@unisales.edu.br', '$2a$10$ZRhirY.Ai93/u5F/3TkRiOKTfdF3IBPFMNW3W/1h8uzRgr9eaiD6u', '5527999010005', 'ALUNO', '20261005', '', 'Análise e Desenvolvimento de Sistemas', TRUE, '2026-10-16 16:50:56', '2026-10-16 16:50:56'),
  (6, 'ms-usr-prf-001', 'Rômulo Ferreira Douro', 'romulo.professor@unisales.edu.br', '$2a$10$o5t56QBQs0riU5ctID/HKeOVn54k/g4ElDqRZSzFcVGcOi.GHCD5C', '5527998020001', 'PROFESSOR', '20261005', 'EMP-PRF-01', 'Engenharia de Software', TRUE, '2026-10-16 16:52:36', '2026-10-16 16:52:36'),
  (7, 'ms-usr-prf-002', 'Éberte Valter da Silva Freitas', 'ebert.professor@unisales.edu.br', '$2a$10$nvIj.dczFeuuXMT4vPdEIewP9PNdXESsGAto9h5C.dMT8G9Kte0TO', '5527998020002', 'PROFESSOR', '', 'EMP-PRF-02', 'Ciência da Computação', TRUE, '2026-10-16 16:53:33', '2026-10-16 16:53:33'),
  (8, 'ms-usr-prf-003', 'Mykael dos Santos Deolindo', 'mykael.professor@unisales.edu.br', '$2a$10$Ctdj3xDObYKQOenFgp11Z.BLtf26ep48KsL8eX69FN5htODdCnmDm', '5527998020003', 'PROFESSOR', '', 'EMP-PRF-03', 'Sistemas de Informação', TRUE, '2026-10-16 16:54:03', '2026-10-16 16:54:03'),
  (9, 'ms-usr-prf-004', 'Lucas Daniel Barboza', 'lucas.professor@unisales.edu.br', '$2a$10$nQeLJZ61Y63hbk2glQ1pNO3n8c8LuGwIahDY0Bh73dmj4nLfDhbXe', '5527998020004', 'PROFESSOR', '', 'EMP-PRF-04', 'Redes de Computadores', TRUE, '2026-10-16 16:54:35', '2026-10-16 16:54:35'),
  (10, 'ms-usr-crd-001', 'Marcelo Albuquerque Schuster', 'marcelo.coord@unisales.edu.br', '$2a$10$Wj8UPbNNTpQyrmZr0j0um.50axF5Ce7nwKvATFJ7OsyizNP2l.YL.', '5527997030001', 'COORDENADOR', '', 'EMP-CRD-01', 'Coordenação de Tecnologia', TRUE, '2026-10-16 16:55:21', '2026-10-16 16:55:21'),
  (11, 'ms-usr-crd-002', 'Fernanda de Alguma Coisa', 'fernanda.coord@unisales.edu.br', '$2a$10$oi6Uy//YtYvVh5w0QUV2Q.9N3gyW1MvCTtiP7gKHxk76U1jlgFZkK', '5527997030002', 'COORDENADOR', '', 'EMP-CRD-02', 'Coordenação Acadêmica', TRUE, '2026-10-16 16:55:52', '2026-10-16 16:55:52'),
  (12, 'ms-usr-rec-001', 'Ana Beatriz do Ferrero Rocher', 'ana.recepcao@unisales.edu.br', '$2a$10$z1DuXhP5KKzlb2hRa/xkHu8gSGj6pFf8pZ2v47L1oa5CuLQEel4Ye', '5527996040001', 'RECEPCIONISTA', '', 'EMP-REC-01', 'Atendimento Geral', TRUE, '2026-10-16 16:56:33', '2026-10-16 16:56:33'),
  (13, 'ms-usr-rec-002', 'Mariana Naiqui da Silva', 'mariana.recepcao@unisales.edu.br', '$2a$10$R2J9mGiEISNEbjEuKI2vlenTob8WjPg.xDGzY0FWFILgISBpShcGu', '5527996040002', 'RECEPCIONISTA', '', 'EMP-REC-02', 'Atendimento Bloco A', TRUE, '2026-10-16 16:57:04', '2026-10-16 16:57:04'),
  (14, 'adminID', 'ADMIN', 'admin@hotmail.com', '$2a$10$AmeD92ui97SdYT5Am8FDr.CUQrA8z2fQlpKH7yN6fkU7F5LAkD4H6', '5527996040002', 'ADMIN', '', '', 'Administrativo', TRUE, '2026-10-16 16:58:03', '2026-10-16 16:58:03')
ON CONFLICT (id_usuario) DO NOTHING;

-- Os ids acima foram informados à mão: avança os contadores para os próximos cadastros.
SELECT setval(pg_get_serial_sequence('duracao_reuniao', 'id_duracao'), (SELECT max(id_duracao) FROM duracao_reuniao));
SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'), (SELECT max(id_usuario) FROM usuario));
