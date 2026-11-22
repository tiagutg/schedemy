-- RNF 18/19 (decisao de 21/11/2026): trilha de auditoria e aceite do termo de uso/politica de privacidade.
-- - log_auditoria.id_usuario passa a aceitar nulo: as acoes automaticas da RotinaDeAgenda (reuniao cancelada sem
--   confirmacao, pedido de alteracao expirado) ficam registradas sem autor ("Sistema");
-- - nova tabela consentimento_termo: quem aceitou qual versao do termo, quando e de qual IP.
-- Nada existente e apagado. MySQL/MariaDB; rode uma vez em bancos ja criados:
--   mysql -u root schedemy_db < db/migracoes/2026-11-21_auditoria_e_consentimento_mysql.sql
-- PostgreSQL (Supabase): use 2026-11-21_auditoria_e_consentimento_postgresql.sql.

ALTER TABLE log_auditoria
    MODIFY id_usuario BIGINT UNSIGNED NULL COMMENT 'Autor; vazio quando a acao e automatica (RotinaDeAgenda)';

CREATE TABLE consentimento_termo (
    id_consentimento BIGINT UNSIGNED AUTO_INCREMENT,
    id_usuario BIGINT UNSIGNED NOT NULL,
    versao_termo VARCHAR(20) NOT NULL,
    aceito_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_origem VARCHAR(45) NULL,

    PRIMARY KEY (id_consentimento),

    CONSTRAINT fk_consentimento_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    UNIQUE KEY uk_consentimento_usuario_versao (id_usuario, versao_termo)
) COMMENT='Aceite do termo de uso e da política de privacidade, por versão (RNF 18)';
