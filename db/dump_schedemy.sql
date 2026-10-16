CREATE DATABASE  IF NOT EXISTS `schedemy_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
USE `schedemy_db`;
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: schedemy_db
-- ------------------------------------------------------
-- Server version	5.5.5-10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `agendamento`
--

DROP TABLE IF EXISTS `agendamento`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agendamento` (
  `id_agendamento` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `id_duracao` bigint(20) unsigned NOT NULL,
  `id_agendamento_origem` bigint(20) unsigned DEFAULT NULL,
  `data_reuniao` date NOT NULL,
  `horario_inicio` time NOT NULL,
  `formato` enum('PRESENCIAL','ONLINE','HIBRIDO') NOT NULL,
  `topico` varchar(200) NOT NULL,
  `resumo` varchar(500) DEFAULT NULL,
  `status_agend` enum('PENDENTE','CONFIRMADO','EM_ANDAMENTO','CONCLUIDO','CANCELADO','AGUARDANDO_RESPOSTA') NOT NULL DEFAULT 'PENDENTE',
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `atualizado_em` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_agendamento`),
  KEY `fk_agendamento_duracao` (`id_duracao`),
  KEY `fk_agendamento_origem` (`id_agendamento_origem`),
  KEY `idx_agendamento_data_status` (`data_reuniao`,`status_agend`),
  KEY `idx_agendamento_usuario` (`id_usuario`),
  KEY `idx_agendamento_status` (`status_agend`),
  CONSTRAINT `fk_agendamento_duracao` FOREIGN KEY (`id_duracao`) REFERENCES `duracao_reuniao` (`id_duracao`) ON UPDATE CASCADE,
  CONSTRAINT `fk_agendamento_origem` FOREIGN KEY (`id_agendamento_origem`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_agendamento_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela principal de agendamentos e solicitações de reuniões';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agendamento`
--

LOCK TABLES `agendamento` WRITE;
/*!40000 ALTER TABLE `agendamento` DISABLE KEYS */;
/*!40000 ALTER TABLE `agendamento` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agendamento_anexo`
--

DROP TABLE IF EXISTS `agendamento_anexo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agendamento_anexo` (
  `id_agendamento_anexo` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `nome_arquivo` varchar(255) NOT NULL,
  `url_arquivo` varchar(500) NOT NULL,
  PRIMARY KEY (`id_agendamento_anexo`),
  KEY `fk_anexo_agendamento` (`id_agendamento`),
  CONSTRAINT `fk_anexo_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Arquivos anexados aos agendamentos';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agendamento_anexo`
--

LOCK TABLES `agendamento_anexo` WRITE;
/*!40000 ALTER TABLE `agendamento_anexo` DISABLE KEYS */;
/*!40000 ALTER TABLE `agendamento_anexo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agendamento_participante`
--

DROP TABLE IF EXISTS `agendamento_participante`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agendamento_participante` (
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `papel` enum('ORGANIZADOR','CONVIDADO') NOT NULL DEFAULT 'CONVIDADO',
  `status_resposta` enum('PENDENTE','ACEITO','RECUSADO') NOT NULL DEFAULT 'PENDENTE',
  `data_resposta` datetime DEFAULT NULL,
  PRIMARY KEY (`id_agendamento`,`id_usuario`),
  KEY `idx_participante_usuario` (`id_usuario`),
  CONSTRAINT `fk_participante_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_participante_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Relação N:M de participantes e seus status de resposta em cada agendamento';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agendamento_participante`
--

LOCK TABLES `agendamento_participante` WRITE;
/*!40000 ALTER TABLE `agendamento_participante` DISABLE KEYS */;
/*!40000 ALTER TABLE `agendamento_participante` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `avaliacao_reuniao`
--

DROP TABLE IF EXISTS `avaliacao_reuniao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `avaliacao_reuniao` (
  `id_avaliacao` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `id_aluno` bigint(20) unsigned NOT NULL,
  `nota` int(10) unsigned NOT NULL,
  `comentario` varchar(200) DEFAULT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_avaliacao`),
  UNIQUE KEY `uk_avaliacao_aluno_agendamento` (`id_agendamento`,`id_aluno`),
  KEY `fk_avaliacao_aluno` (`id_aluno`),
  CONSTRAINT `fk_avaliacao_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_avaliacao_aluno` FOREIGN KEY (`id_aluno`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE,
  CONSTRAINT `chk_avaliacao_nota` CHECK (`nota` between 0 and 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Avaliações pós-atendimento registradas pelos alunos';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `avaliacao_reuniao`
--

LOCK TABLES `avaliacao_reuniao` WRITE;
/*!40000 ALTER TABLE `avaliacao_reuniao` DISABLE KEYS */;
/*!40000 ALTER TABLE `avaliacao_reuniao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bloqueio_periodo`
--

DROP TABLE IF EXISTS `bloqueio_periodo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bloqueio_periodo` (
  `id_bloqueio` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `data_inicio` date NOT NULL,
  `data_fim` date NOT NULL,
  `horario_inicio` time DEFAULT NULL,
  `horario_fim` time DEFAULT NULL,
  `e_recorrente` tinyint(1) NOT NULL DEFAULT 0,
  `dia_semana_recorrente` int(10) unsigned DEFAULT NULL,
  `motivo` varchar(255) DEFAULT NULL,
  `sincronizado_outlook` tinyint(1) NOT NULL DEFAULT 0,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_bloqueio`),
  KEY `idx_bloqueio_consulta` (`id_usuario`,`data_inicio`,`data_fim`),
  CONSTRAINT `fk_bloqueio_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_bloqueio_datas` CHECK (`data_fim` >= `data_inicio`),
  CONSTRAINT `chk_bloqueio_recorrencia` CHECK (`e_recorrente` = 0 and `dia_semana_recorrente` is null or `e_recorrente` = 1 and `dia_semana_recorrente` between 1 and 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Períodos de indisponibilidade e impedimentos na agenda do docente';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bloqueio_periodo`
--

LOCK TABLES `bloqueio_periodo` WRITE;
/*!40000 ALTER TABLE `bloqueio_periodo` DISABLE KEYS */;
/*!40000 ALTER TABLE `bloqueio_periodo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `convidado_favorito`
--

DROP TABLE IF EXISTS `convidado_favorito`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `convidado_favorito` (
  `id_convidado_favorito` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_aluno` bigint(20) unsigned NOT NULL,
  `id_favorito` bigint(20) unsigned NOT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_convidado_favorito`),
  UNIQUE KEY `uk_favorito_aluno_usuario` (`id_aluno`,`id_favorito`),
  KEY `fk_favorito_usuario` (`id_favorito`),
  CONSTRAINT `fk_favorito_aluno` FOREIGN KEY (`id_aluno`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_favorito_usuario` FOREIGN KEY (`id_favorito`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Lista de professores/coordenadores favoritados por alunos';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `convidado_favorito`
--

LOCK TABLES `convidado_favorito` WRITE;
/*!40000 ALTER TABLE `convidado_favorito` DISABLE KEYS */;
/*!40000 ALTER TABLE `convidado_favorito` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `disponibilidade`
--

DROP TABLE IF EXISTS `disponibilidade`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disponibilidade` (
  `id_disponibilidade` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `dia_semana` int(10) unsigned NOT NULL,
  `horario_inicio` time NOT NULL,
  `horario_fim` time NOT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT 1,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_disponibilidade`),
  KEY `idx_disponibilidade_busca` (`id_usuario`,`dia_semana`,`ativo`),
  CONSTRAINT `fk_disponibilidade_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_disponibilidade_horario` CHECK (`horario_fim` > `horario_inicio`),
  CONSTRAINT `chk_disponibilidade_dia` CHECK (`dia_semana` between 1 and 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Grade semanal de horários de atendimento oferecidos por professores/coordenadores';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `disponibilidade`
--

LOCK TABLES `disponibilidade` WRITE;
/*!40000 ALTER TABLE `disponibilidade` DISABLE KEYS */;
/*!40000 ALTER TABLE `disponibilidade` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `duracao_reuniao`
--

DROP TABLE IF EXISTS `duracao_reuniao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `duracao_reuniao` (
  `id_duracao` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `minutos` int(10) unsigned NOT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (`id_duracao`),
  UNIQUE KEY `uk_duracao_minutos` (`minutos`),
  CONSTRAINT `chk_duracao_minutos` CHECK (`minutos` in (10,15,20,30,45,60))
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Durações de reunião disponibilizadas pelo sistema';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `duracao_reuniao`
--

LOCK TABLES `duracao_reuniao` WRITE;
/*!40000 ALTER TABLE `duracao_reuniao` DISABLE KEYS */;
INSERT INTO `duracao_reuniao` VALUES (1,10,1),(2,15,1),(3,20,1),(4,30,1),(5,45,1),(6,60,1);
/*!40000 ALTER TABLE `duracao_reuniao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `evento_externo`
--

DROP TABLE IF EXISTS `evento_externo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `evento_externo` (
  `id_evento_externo` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `id_agendamento` bigint(20) unsigned DEFAULT NULL,
  `provedor` varchar(50) NOT NULL,
  `id_evento_provedor` varchar(255) NOT NULL,
  `tipo_evento` varchar(50) NOT NULL,
  `ultima_sincronizacao` datetime DEFAULT NULL,
  PRIMARY KEY (`id_evento_externo`),
  UNIQUE KEY `uk_evento_externo_outlook` (`provedor`,`id_evento_provedor`),
  KEY `idx_evento_externo_usuario` (`id_usuario`),
  KEY `idx_evento_externo_agendamento` (`id_agendamento`),
  CONSTRAINT `fk_evento_externo_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_evento_externo_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Eventos de calendários externos sincronizados com o sistema';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `evento_externo`
--

LOCK TABLES `evento_externo` WRITE;
/*!40000 ALTER TABLE `evento_externo` DISABLE KEYS */;
/*!40000 ALTER TABLE `evento_externo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_auditoria`
--

DROP TABLE IF EXISTS `log_auditoria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_auditoria` (
  `id_log` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `acao` varchar(100) NOT NULL,
  `tabela_afetada` varchar(50) NOT NULL,
  `id_tabela_afetada` bigint(20) unsigned DEFAULT NULL,
  `detalhes` varchar(200) DEFAULT NULL,
  `ip_origem` varchar(45) DEFAULT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_log`),
  KEY `idx_log_usuario_data` (`id_usuario`,`criado_em`),
  KEY `idx_log_tabela_registro` (`tabela_afetada`,`id_tabela_afetada`),
  CONSTRAINT `fk_log_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Trilha auditável de operações no sistema para conformidade com LGPD e RNF 19';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_auditoria`
--

LOCK TABLES `log_auditoria` WRITE;
/*!40000 ALTER TABLE `log_auditoria` DISABLE KEYS */;
/*!40000 ALTER TABLE `log_auditoria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notificacao`
--

DROP TABLE IF EXISTS `notificacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notificacao` (
  `id_notificacao` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `id_agendamento` bigint(20) unsigned DEFAULT NULL,
  `canal_envio` enum('EMAIL','SISTEMA','WHATSAPP') NOT NULL DEFAULT 'EMAIL',
  `tipo_evento` varchar(50) NOT NULL,
  `mensagem` varchar(200) NOT NULL,
  `status_envio` enum('PENDENTE','ENVIADO','FALHA') NOT NULL DEFAULT 'PENDENTE',
  `enviado_em` datetime DEFAULT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_notificacao`),
  KEY `fk_notificacao_agendamento` (`id_agendamento`),
  KEY `idx_notificacao_status` (`id_usuario`,`status_envio`),
  CONSTRAINT `fk_notificacao_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON UPDATE CASCADE,
  CONSTRAINT `fk_notificacao_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Fila e histórico de notificações disparadas aos envolvidos nas reuniões';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notificacao`
--

LOCK TABLES `notificacao` WRITE;
/*!40000 ALTER TABLE `notificacao` DISABLE KEYS */;
/*!40000 ALTER TABLE `notificacao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `registro_motivo`
--

DROP TABLE IF EXISTS `registro_motivo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `registro_motivo` (
  `id_registro_motivo` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `tipo_operacao` enum('CANCELAMENTO','SOLICITACAO_EDICAO','RECUSA_AGENDAMENTO','RECUSA_REMARCACAO') NOT NULL,
  `justificativa` varchar(200) NOT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_registro_motivo`),
  KEY `fk_motivo_agendamento` (`id_agendamento`),
  KEY `fk_motivo_usuario` (`id_usuario`),
  CONSTRAINT `fk_motivo_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_motivo_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Registro de justificativas obrigatórias para cancelamentos e alterações em reuniões';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `registro_motivo`
--

LOCK TABLES `registro_motivo` WRITE;
/*!40000 ALTER TABLE `registro_motivo` DISABLE KEYS */;
/*!40000 ALTER TABLE `registro_motivo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sala_virtual`
--

DROP TABLE IF EXISTS `sala_virtual`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sala_virtual` (
  `id_sala_virtual` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `link_teams` varchar(1000) NOT NULL,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id_sala_virtual`),
  UNIQUE KEY `uk_sala_agendamento` (`id_agendamento`),
  CONSTRAINT `fk_sala_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Salas virtuais do Microsoft Teams geradas automaticamente para reuniões online ou híbridas';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sala_virtual`
--

LOCK TABLES `sala_virtual` WRITE;
/*!40000 ALTER TABLE `sala_virtual` DISABLE KEYS */;
/*!40000 ALTER TABLE `sala_virtual` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solicitacao_edicao`
--

DROP TABLE IF EXISTS `solicitacao_edicao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitacao_edicao` (
  `id_solicitacao` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `id_nova_duracao` bigint(20) unsigned NOT NULL,
  `nova_data` date NOT NULL,
  `novo_horario_inicio` time NOT NULL,
  `novo_formato` enum('PRESENCIAL','ONLINE','HIBRIDO') NOT NULL,
  `novo_topico` varchar(200) NOT NULL,
  `novo_resumo` varchar(500) DEFAULT NULL,
  `motivo` varchar(200) NOT NULL,
  `status_solicitacao` enum('PENDENTE','ACEITA','RECUSADA') NOT NULL DEFAULT 'PENDENTE',
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `respondido_em` datetime DEFAULT NULL,
  PRIMARY KEY (`id_solicitacao`),
  KEY `fk_solicitacao_usuario` (`id_usuario`),
  KEY `fk_solicitacao_nova_duracao` (`id_nova_duracao`),
  KEY `idx_solicitacao_agendamento` (`id_agendamento`),
  CONSTRAINT `fk_solicitacao_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_solicitacao_nova_duracao` FOREIGN KEY (`id_nova_duracao`) REFERENCES `duracao_reuniao` (`id_duracao`) ON UPDATE CASCADE,
  CONSTRAINT `fk_solicitacao_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Solicitações de alteração de dados de agendamentos';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solicitacao_edicao`
--

LOCK TABLES `solicitacao_edicao` WRITE;
/*!40000 ALTER TABLE `solicitacao_edicao` DISABLE KEYS */;
/*!40000 ALTER TABLE `solicitacao_edicao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solicitacao_edicao_anexo`
--

DROP TABLE IF EXISTS `solicitacao_edicao_anexo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitacao_edicao_anexo` (
  `id_solicitacao_anexo` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_solicitacao` bigint(20) unsigned NOT NULL,
  `nome_arquivo` varchar(255) NOT NULL,
  `url_arquivo` varchar(500) NOT NULL,
  PRIMARY KEY (`id_solicitacao_anexo`),
  KEY `fk_solicitacao_anexo` (`id_solicitacao`),
  CONSTRAINT `fk_solicitacao_anexo` FOREIGN KEY (`id_solicitacao`) REFERENCES `solicitacao_edicao` (`id_solicitacao`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Arquivos anexados às solicitações de edição de agendamento';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solicitacao_edicao_anexo`
--

LOCK TABLES `solicitacao_edicao_anexo` WRITE;
/*!40000 ALTER TABLE `solicitacao_edicao_anexo` DISABLE KEYS */;
/*!40000 ALTER TABLE `solicitacao_edicao_anexo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `solicitacao_edicao_participante`
--

DROP TABLE IF EXISTS `solicitacao_edicao_participante`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `solicitacao_edicao_participante` (
  `id_solicitacao` bigint(20) unsigned NOT NULL,
  `id_usuario` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id_solicitacao`,`id_usuario`),
  KEY `fk_solicitacao_participante_usuario` (`id_usuario`),
  CONSTRAINT `fk_solicitacao_participante_solicitacao` FOREIGN KEY (`id_solicitacao`) REFERENCES `solicitacao_edicao` (`id_solicitacao`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_solicitacao_participante_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `solicitacao_edicao_participante`
--

LOCK TABLES `solicitacao_edicao_participante` WRITE;
/*!40000 ALTER TABLE `solicitacao_edicao_participante` DISABLE KEYS */;
/*!40000 ALTER TABLE `solicitacao_edicao_participante` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sugestao_remarcacao`
--

DROP TABLE IF EXISTS `sugestao_remarcacao`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sugestao_remarcacao` (
  `id_sugestao` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_agendamento` bigint(20) unsigned NOT NULL,
  `id_usuario` bigint(20) unsigned NOT NULL,
  `nova_data` date NOT NULL,
  `novo_horario_inicio` time NOT NULL,
  `motivo` varchar(200) NOT NULL,
  `status_remarcacao` enum('PENDENTE','ACEITO','RECUSADO') NOT NULL DEFAULT 'PENDENTE',
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `respondido_em` datetime DEFAULT NULL,
  PRIMARY KEY (`id_sugestao`),
  KEY `fk_sugestao_agendamento` (`id_agendamento`),
  KEY `fk_sugestao_usuario` (`id_usuario`),
  CONSTRAINT `fk_sugestao_agendamento` FOREIGN KEY (`id_agendamento`) REFERENCES `agendamento` (`id_agendamento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sugestao_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Propostas de reagendamento enviadas por professores quando há alterações de agenda';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sugestao_remarcacao`
--

LOCK TABLES `sugestao_remarcacao` WRITE;
/*!40000 ALTER TABLE `sugestao_remarcacao` DISABLE KEYS */;
/*!40000 ALTER TABLE `sugestao_remarcacao` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `usuario`
--

DROP TABLE IF EXISTS `usuario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `usuario` (
  `id_usuario` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `id_microsoft` varchar(255) NOT NULL,
  `nome` varchar(150) NOT NULL,
  `email` varchar(150) NOT NULL,
  `senha` varchar(255) NOT NULL,
  `telefone_whatsapp` varchar(20) DEFAULT NULL,
  `tipo_usuario` enum('ADMIN','ALUNO','PROFESSOR','COORDENADOR','RECEPCIONISTA') NOT NULL,
  `matricula_ra` varchar(30) DEFAULT NULL,
  `num_identificacao` varchar(30) DEFAULT NULL,
  `departamento` varchar(100) DEFAULT NULL,
  `ativo` tinyint(1) NOT NULL DEFAULT 1,
  `criado_em` datetime NOT NULL DEFAULT current_timestamp(),
  `atualizado_em` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uk_usuario_id_microsoft` (`id_microsoft`),
  UNIQUE KEY `uk_usuario_email` (`email`),
  KEY `idx_usuario_tipo` (`tipo_usuario`),
  KEY `idx_usuario_matricula_ra` (`matricula_ra`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tabela unificada para cadastro de usuários';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `usuario`
--

LOCK TABLES `usuario` WRITE;
/*!40000 ALTER TABLE `usuario` DISABLE KEYS */;
INSERT INTO `usuario` VALUES (1,'ms-usr-alu-001','Kevin Lins Carminati Andrade','kevin.aluno@unisales.edu.br','$2a$10$JkiHDOFmAlB9YJyMyQlXCu6WvVNT4Xj4.AZdrU3GR.FqAe28i1.My','5527999010001','ALUNO','20261001','','Análise e Desenvolvimento de Sistemas',1,'2026-10-16 16:48:35','2026-10-16 16:48:35'),(2,'ms-usr-alu-002','Pedro Lucas Sipriano Do Nascimento','pedro.aluno@unisales.edu.br','$2a$10$xkl1S1LK1PvdQMWC/LZcmutm1fEDhSKouOockgfsIP4cos9QqhKD6','5527999010002','ALUNO','20261002','','Sistema de Informação',1,'2026-10-16 16:49:28','2026-10-16 16:49:28'),(3,'ms-usr-alu-003','Tiago Lopes Alves','tiago.aluno@unisales.edu.br','$2a$10$UVDlFsh3HFaf4Oj3si7A4eut0z1iK8TW/Vv2iWUuYAlzoNYaHd.Xe','5527999010003','ALUNO','20261003','','Análise e Desenvolvimento de Sistemas',1,'2026-10-16 16:49:58','2026-10-16 16:49:58'),(4,'ms-usr-alu-004','Diogo Ventorin De Freitas','diogo.aluno@unisales.edu.br','$2a$10$hvbp98KFuUpM3AD8Rt3To.TaZeu2PBIV41W87dMiIDNlx3WWRmepK','5527999010004','ALUNO','20261004','','Análise e Desenvolvimento de Sistemas',1,'2026-10-16 16:50:30','2026-10-16 16:50:30'),(5,'ms-usr-alu-005','João Victor Wuthovsky Klein','joao.aluno@unisales.edu.br','$2a$10$ZRhirY.Ai93/u5F/3TkRiOKTfdF3IBPFMNW3W/1h8uzRgr9eaiD6u','5527999010005','ALUNO','20261005','','Análise e Desenvolvimento de Sistemas',1,'2026-10-16 16:50:56','2026-10-16 16:50:56'),(6,'ms-usr-prf-001','Rômulo Ferreira Douro','romulo.professor@unisales.edu.br','$2a$10$o5t56QBQs0riU5ctID/HKeOVn54k/g4ElDqRZSzFcVGcOi.GHCD5C','5527998020001','PROFESSOR','20261005','EMP-PRF-01','Engenharia de Software',1,'2026-10-16 16:52:36','2026-10-16 16:52:36'),(7,'ms-usr-prf-002','Éberte Valter da Silva Freitas','ebert.professor@unisales.edu.br','$2a$10$nvIj.dczFeuuXMT4vPdEIewP9PNdXESsGAto9h5C.dMT8G9Kte0TO','5527998020002','PROFESSOR','','EMP-PRF-02','Ciência da Computação',1,'2026-10-16 16:53:33','2026-10-16 16:53:33'),(8,'ms-usr-prf-003','Mykael dos Santos Deolindo','mykael.professor@unisales.edu.br','$2a$10$Ctdj3xDObYKQOenFgp11Z.BLtf26ep48KsL8eX69FN5htODdCnmDm','5527998020003','PROFESSOR','','EMP-PRF-03','Sistemas de Informação',1,'2026-10-16 16:54:03','2026-10-16 16:54:03'),(9,'ms-usr-prf-004','Lucas Daniel Barboza','lucas.professor@unisales.edu.br','$2a$10$nQeLJZ61Y63hbk2glQ1pNO3n8c8LuGwIahDY0Bh73dmj4nLfDhbXe','5527998020004','PROFESSOR','','EMP-PRF-04','Redes de Computadores',1,'2026-10-16 16:54:35','2026-10-16 16:54:35'),(10,'ms-usr-crd-001','Marcelo Albuquerque Schuster','marcelo.coord@unisales.edu.br','$2a$10$Wj8UPbNNTpQyrmZr0j0um.50axF5Ce7nwKvATFJ7OsyizNP2l.YL.','5527997030001','COORDENADOR','','EMP-CRD-01','Coordenação de Tecnologia',1,'2026-10-16 16:55:21','2026-10-16 16:55:21'),(11,'ms-usr-crd-002','Fernanda de Alguma Coisa','fernanda.coord@unisales.edu.br','$2a$10$oi6Uy//YtYvVh5w0QUV2Q.9N3gyW1MvCTtiP7gKHxk76U1jlgFZkK','5527997030002','COORDENADOR','','EMP-CRD-02','Coordenação Acadêmica',1,'2026-10-16 16:55:52','2026-10-16 16:55:52'),(12,'ms-usr-rec-001','Ana Beatriz do Ferrero Rocher','ana.recepcao@unisales.edu.br','$2a$10$z1DuXhP5KKzlb2hRa/xkHu8gSGj6pFf8pZ2v47L1oa5CuLQEel4Ye','5527996040001','RECEPCIONISTA','','EMP-REC-01','Atendimento Geral',1,'2026-10-16 16:56:33','2026-10-16 16:56:33'),(13,'ms-usr-rec-002','Mariana Naiqui da Silva','mariana.recepcao@unisales.edu.br','$2a$10$R2J9mGiEISNEbjEuKI2vlenTob8WjPg.xDGzY0FWFILgISBpShcGu','5527996040002','RECEPCIONISTA','','EMP-REC-02','Atendimento Bloco A',1,'2026-10-16 16:57:04','2026-10-16 16:57:04'),(14,'adminID','ADMIN','admin@hotmail.com','$2a$10$AmeD92ui97SdYT5Am8FDr.CUQrA8z2fQlpKH7yN6fkU7F5LAkD4H6','5527996040002','ADMIN','','','Administrativo',1,'2026-10-16 16:58:03','2026-10-16 16:58:03');
/*!40000 ALTER TABLE `usuario` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'schedemy_db'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-16 17:04:35
