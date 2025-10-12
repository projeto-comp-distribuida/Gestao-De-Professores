-- Migração para criar tabela de disciplinas
-- V4__Create_subjects_table.sql

CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(10) UNIQUE,
    description VARCHAR(500),
    workload_hours INTEGER,
    level VARCHAR(20) DEFAULT 'BASIC',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- Campos de auditoria da BaseEntity
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Índices para performance
CREATE INDEX idx_subjects_code ON subjects(code);
CREATE INDEX idx_subjects_status ON subjects(status);
CREATE INDEX idx_subjects_level ON subjects(level);
CREATE INDEX idx_subjects_deleted_at ON subjects(deleted_at);

-- Comentários para documentação
COMMENT ON TABLE subjects IS 'Tabela de disciplinas do DistriSchool';
COMMENT ON COLUMN subjects.code IS 'Código único da disciplina';
COMMENT ON COLUMN subjects.level IS 'Nível da disciplina: BASIC, INTERMEDIATE, ADVANCED';
COMMENT ON COLUMN subjects.status IS 'Status da disciplina: ACTIVE, INACTIVE, UNDER_REVIEW';
