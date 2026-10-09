CREATE DATABASE IF NOT EXISTS schedemy_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE schedemy_db;

-- USUÁRIO
CREATE TABLE usuario (
    id_usuario BIGINT UNSIGNED AUTO_INCREMENT,
    id_microsoft VARCHAR(255) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefone_whatsapp VARCHAR(20),
    tipo_usuario ENUM('ALUNO', 'PROFESSOR', 'COORDENADOR', 'RECEPCIONISTA') NOT NULL,
    matricula_ra VARCHAR(30),
    num_identificacao VARCHAR(30),
    departamento VARCHAR(100),
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id_usuario),
    
    CONSTRAINT uk_usuario_id_microsoft 
		UNIQUE (id_microsoft),
    
    CONSTRAINT uk_usuario_email 
		UNIQUE (email),
    
    INDEX idx_usuario_tipo (tipo_usuario),
    INDEX idx_usuario_matricula_ra (matricula_ra)
) COMMENT='Tabela unificada para cadastro de usuários'; 

-- CONVIDADO FAVORITO
CREATE TABLE convidado_favorito (
    id_convidado_favorito BIGINT UNSIGNED AUTO_INCREMENT,
    id_aluno BIGINT UNSIGNED NOT NULL,
    id_favorito BIGINT UNSIGNED NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_convidado_favorito),
    
    CONSTRAINT fk_favorito_aluno
        FOREIGN KEY (id_aluno)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_favorito_usuario
        FOREIGN KEY (id_favorito)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
	
	CONSTRAINT uk_favorito_aluno_usuario 
		UNIQUE (id_aluno, id_favorito)
) COMMENT='Lista de professores/coordenadores favoritados por alunos';

-- DISPONIBILIDADE
CREATE TABLE disponibilidade (
    id_disponibilidade BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    dia_semana INT UNSIGNED NOT NULL,
    horario_inicio TIME NOT NULL,
    horario_fim TIME NOT NULL,
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_disponibilidade),

    CONSTRAINT fk_disponibilidade_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT chk_disponibilidade_horario
        CHECK (horario_fim > horario_inicio),

    CONSTRAINT chk_disponibilidade_dia
        CHECK (dia_semana BETWEEN 1 AND 7),

    INDEX idx_disponibilidade_busca (id_usuario, dia_semana, ativo)
) COMMENT='Grade semanal de horários de atendimento oferecidos por professores/coordenadores';

-- DURACAO REUNIÃO
CREATE TABLE duracao_reuniao (
    id_duracao BIGINT UNSIGNED AUTO_INCREMENT,
    minutos INT UNSIGNED NOT NULL,
    ativo TINYINT(1) NOT NULL DEFAULT 1,

    PRIMARY KEY (id_duracao),
    
    CONSTRAINT uk_duracao_minutos 
		UNIQUE (minutos),
    
    CONSTRAINT chk_duracao_minutos
        CHECK (minutos IN (10, 15, 20, 30, 45, 60))
) COMMENT='Durações de reunião disponibilizadas pelo sistema';

-- BLOQUEIO PERIODO
CREATE TABLE bloqueio_periodo (
    id_bloqueio BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    horario_inicio TIME,
    horario_fim TIME,
    e_recorrente TINYINT(1) NOT NULL DEFAULT 0,
    dia_semana_recorrente INT UNSIGNED,
    motivo VARCHAR(255),
    sincronizado_outlook TINYINT(1) NOT NULL DEFAULT 0,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_bloqueio),

    CONSTRAINT fk_bloqueio_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT chk_bloqueio_datas
        CHECK (data_fim >= data_inicio),
	
    CONSTRAINT chk_bloqueio_recorrencia
		CHECK (
			(e_recorrente = 0 AND dia_semana_recorrente IS NULL) 
            OR
            (e_recorrente = 1 AND dia_semana_recorrente BETWEEN 1 AND 7)
		),
    
    INDEX idx_bloqueio_consulta (id_usuario, data_inicio, data_fim)
) COMMENT='Períodos de indisponibilidade e impedimentos na agenda do docente';

-- AGENDAMENTO
CREATE TABLE agendamento (
    id_agendamento BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
	id_duracao BIGINT UNSIGNED NOT NULL,
    id_agendamento_origem BIGINT UNSIGNED, -- Olhar o que é isso
    data_reuniao DATE NOT NULL,
    horario_inicio TIME NOT NULL,
    formato ENUM('PRESENCIAL', 'ONLINE', 'HIBRIDO') NOT NULL,
    topico VARCHAR(200) NOT NULL,
    resumo VARCHAR(500),
    status_agend ENUM('PENDENTE', 'CONFIRMADO', 'EM_ANDAMENTO', 'CONCLUIDO','CANCELADO','AGUARDANDO_RESPOSTA') NOT NULL DEFAULT 'PENDENTE',
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id_agendamento),

    CONSTRAINT fk_agendamento_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
        
	CONSTRAINT fk_agendamento_duracao
        FOREIGN KEY (id_duracao)
        REFERENCES duracao_reuniao (id_duracao)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_agendamento_origem
        FOREIGN KEY (id_agendamento_origem)
        REFERENCES agendamento (id_agendamento)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
	
    INDEX idx_agendamento_data_status (data_reuniao, status_agend), 
    INDEX idx_agendamento_usuario (id_usuario), 
    INDEX idx_agendamento_status (status_agend)
) COMMENT='Tabela principal de agendamentos e solicitações de reuniões';

-- AGENDAMENTO ANEXO
CREATE TABLE agendamento_anexo (
    id_agendamento_anexo BIGINT UNSIGNED AUTO_INCREMENT,
    id_agendamento BIGINT UNSIGNED NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    url_arquivo VARCHAR(500) NOT NULL,
    
    PRIMARY KEY (id_agendamento_anexo),
    
    CONSTRAINT fk_anexo_agendamento 
		FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE 
        ON UPDATE CASCADE
)  COMMENT='Arquivos anexados aos agendamentos';

-- AGENDAMENTO PARTICIPANTE
CREATE TABLE agendamento_participante (
    id_agendamento BIGINT UNSIGNED NOT NULL,
    id_usuario BIGINT UNSIGNED NOT NULL,
    papel ENUM('ORGANIZADOR', 'CONVIDADO') NOT NULL DEFAULT 'CONVIDADO',
    status_resposta ENUM('PENDENTE', 'ACEITO', 'RECUSADO') NOT NULL DEFAULT 'PENDENTE',
    data_resposta DATETIME NULL,

    PRIMARY KEY (id_agendamento, id_usuario),

    CONSTRAINT fk_participante_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_participante_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_participante_usuario (id_usuario)
) COMMENT='Relação N:M de participantes e seus status de resposta em cada agendamento';

-- SALA VIRTUAL
CREATE TABLE sala_virtual (
    id_sala_virtual BIGINT UNSIGNED AUTO_INCREMENT,
    id_agendamento BIGINT UNSIGNED NOT NULL,
    link_teams VARCHAR(1000) NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_sala_virtual),

    CONSTRAINT uk_sala_agendamento 
		UNIQUE (id_agendamento),

    CONSTRAINT fk_sala_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) COMMENT='Salas virtuais do Microsoft Teams geradas automaticamente para reuniões online ou híbridas';

-- SUGESTAO REMARCACAO
CREATE TABLE sugestao_remarcacao (
    id_sugestao BIGINT UNSIGNED AUTO_INCREMENT,
    id_agendamento BIGINT UNSIGNED NOT NULL,
    id_usuario BIGINT UNSIGNED NOT NULL,
    nova_data DATE NOT NULL,
    novo_horario_inicio TIME NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    status_remarcacao ENUM('PENDENTE', 'ACEITO', 'RECUSADO') NOT NULL DEFAULT 'PENDENTE',
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondido_em DATETIME NULL,

    PRIMARY KEY (id_sugestao),

    CONSTRAINT fk_sugestao_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_sugestao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) COMMENT='Propostas de reagendamento enviadas por professores quando há alterações de agenda';

-- SOLICITACAO EDICAO
CREATE TABLE solicitacao_edicao (
    id_solicitacao BIGINT UNSIGNED AUTO_INCREMENT,
    id_agendamento BIGINT UNSIGNED NOT NULL,
    id_usuario BIGINT UNSIGNED NOT NULL,
    id_nova_duracao BIGINT UNSIGNED NOT NULL,
    nova_data DATE NOT NULL,
    novo_horario_inicio TIME NOT NULL,
    novo_formato ENUM('PRESENCIAL', 'ONLINE', 'HIBRIDO') NOT NULL,
    novo_topico VARCHAR(200) NOT NULL,
    novo_resumo VARCHAR(500),
    motivo VARCHAR(200) NOT NULL,
    status_solicitacao ENUM('PENDENTE', 'ACEITA', 'RECUSADA') NOT NULL DEFAULT 'PENDENTE',
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    respondido_em DATETIME NULL,

    PRIMARY KEY (id_solicitacao),

    CONSTRAINT fk_solicitacao_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_solicitacao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_solicitacao_nova_duracao
        FOREIGN KEY (id_nova_duracao)
        REFERENCES duracao_reuniao (id_duracao)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_solicitacao_agendamento (id_agendamento)
) COMMENT='Solicitações de alteração de dados de agendamentos';

-- SOLICITACAO EDICAO ANEXO
CREATE TABLE solicitacao_edicao_anexo (
    id_solicitacao_anexo BIGINT UNSIGNED AUTO_INCREMENT,
    id_solicitacao BIGINT UNSIGNED NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    url_arquivo VARCHAR(500) NOT NULL,

    PRIMARY KEY (id_solicitacao_anexo),

    CONSTRAINT fk_solicitacao_anexo
        FOREIGN KEY (id_solicitacao)
        REFERENCES solicitacao_edicao (id_solicitacao)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) COMMENT='Arquivos anexados às solicitações de edição de agendamento';

-- SOLICITACAO EDICAO PARTICIPANTE
CREATE TABLE solicitacao_edicao_participante (
    id_solicitacao BIGINT UNSIGNED NOT NULL,
    id_usuario BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (id_solicitacao, id_usuario),

    CONSTRAINT fk_solicitacao_participante_solicitacao
        FOREIGN KEY (id_solicitacao)
        REFERENCES solicitacao_edicao (id_solicitacao)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_solicitacao_participante_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- REGISTRO MOTIVO
CREATE TABLE registro_motivo (
    id_registro_motivo BIGINT UNSIGNED AUTO_INCREMENT,
    id_agendamento BIGINT UNSIGNED NOT NULL,
    id_usuario BIGINT UNSIGNED NOT NULL,
    tipo_operacao ENUM('CANCELAMENTO', 'SOLICITACAO_EDICAO', 'RECUSA_AGENDAMENTO', 'RECUSA_REMARCACAO') NOT NULL,
    justificativa VARCHAR(200) NOT NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_registro_motivo),

    CONSTRAINT fk_motivo_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_motivo_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) COMMENT='Registro de justificativas obrigatórias para cancelamentos e alterações em reuniões';

-- AVALIAÇÃO REUNIÃO
CREATE TABLE avaliacao_reuniao (
    id_avaliacao BIGINT UNSIGNED AUTO_INCREMENT,
    id_agendamento BIGINT UNSIGNED NOT NULL,
    id_aluno BIGINT UNSIGNED NOT NULL,
    nota INT UNSIGNED NOT NULL,
    comentario VARCHAR(200) NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_avaliacao),

    CONSTRAINT fk_avaliacao_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_avaliacao_aluno
        FOREIGN KEY (id_aluno)
        REFERENCES usuario (id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT chk_avaliacao_nota
        CHECK (nota BETWEEN 0 AND 5),
        
	CONSTRAINT uk_avaliacao_aluno_agendamento
		UNIQUE (id_agendamento, id_aluno)
) COMMENT='Avaliações pós-atendimento registradas pelos alunos';

-- NOTIFICAÇÃO
CREATE TABLE notificacao (
    id_notificacao BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    id_agendamento BIGINT UNSIGNED,
    canal_envio ENUM('EMAIL', 'SISTEMA', 'WHATSAPP') NOT NULL DEFAULT 'EMAIL',
    tipo_evento VARCHAR(50) NOT NULL,
    mensagem VARCHAR(200) NOT NULL,
    status_envio ENUM('PENDENTE', 'ENVIADO', 'FALHA') NOT NULL DEFAULT 'PENDENTE',
    enviado_em DATETIME,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_notificacao),

    CONSTRAINT fk_notificacao_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_notificacao_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON UPDATE CASCADE,

    INDEX idx_notificacao_status (id_usuario, status_envio)
) COMMENT='Fila e histórico de notificações disparadas aos envolvidos nas reuniões';

-- LOG AUDITORIA
CREATE TABLE log_auditoria (
    id_log BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    acao VARCHAR(100) NOT NULL,
    tabela_afetada VARCHAR(50) NOT NULL,
    id_tabela_afetada BIGINT UNSIGNED,
    detalhes VARCHAR(200),
    ip_origem VARCHAR(45) NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id_log),

    CONSTRAINT fk_log_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON UPDATE CASCADE,

    INDEX idx_log_usuario_data (id_usuario, criado_em),
    INDEX idx_log_tabela_registro (tabela_afetada, id_tabela_afetada)
) COMMENT='Trilha auditável de operações no sistema para conformidade com LGPD e RNF 19';

-- EVENTO EXTERNO
CREATE TABLE evento_externo (
    id_evento_externo BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    id_agendamento BIGINT UNSIGNED,
    provedor VARCHAR(50) NOT NULL,
    id_evento_provedor VARCHAR(255) NOT NULL,
    tipo_evento VARCHAR(50) NOT NULL,
    ultima_sincronizacao DATETIME,

    PRIMARY KEY (id_evento_externo),

    CONSTRAINT fk_evento_externo_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_evento_externo_agendamento
        FOREIGN KEY (id_agendamento)
        REFERENCES agendamento (id_agendamento)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT uk_evento_externo_outlook
        UNIQUE (provedor, id_evento_provedor),

    INDEX idx_evento_externo_usuario (id_usuario),
    INDEX idx_evento_externo_agendamento (id_agendamento)
) COMMENT='Eventos de calendários externos sincronizados com o sistema';

INSERT INTO duracao_reuniao (minutos, ativo) VALUES
    (10, 1),
    (15, 1),
    (20, 1),
    (30, 1),
    (45, 1),
    (60, 1);