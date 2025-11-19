-- Migração para adicionar coluna auth0_id na tabela teachers
-- V9__Add_auth0_id_to_teachers.sql

-- Adicionar coluna auth0_id
ALTER TABLE teachers
ADD COLUMN auth0_id VARCHAR(255) UNIQUE;

-- Criar índice para melhorar performance nas consultas por auth0_id
CREATE INDEX idx_teachers_auth0_id ON teachers(auth0_id);

-- Comentário para documentação
COMMENT ON COLUMN teachers.auth0_id IS 'ID do usuário no Auth0 (vínculo com o serviço de autenticação)';

