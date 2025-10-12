-- Migração para criar tabela de turmas
-- V5__Create_class_groups_table.sql

CREATE TABLE class_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(10) UNIQUE,
    academic_year VARCHAR(10),
    grade_level VARCHAR(20),
    max_students INTEGER,
    current_students INTEGER DEFAULT 0,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    shift VARCHAR(20),
    
    -- Campos de auditoria da BaseEntity
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Índices para performance
CREATE INDEX idx_class_groups_code ON class_groups(code);
CREATE INDEX idx_class_groups_status ON class_groups(status);
CREATE INDEX idx_class_groups_academic_year ON class_groups(academic_year);
CREATE INDEX idx_class_groups_grade_level ON class_groups(grade_level);
CREATE INDEX idx_class_groups_shift ON class_groups(shift);
CREATE INDEX idx_class_groups_deleted_at ON class_groups(deleted_at);

-- Comentários para documentação
COMMENT ON TABLE class_groups IS 'Tabela de turmas do DistriSchool';
COMMENT ON COLUMN class_groups.code IS 'Código único da turma';
COMMENT ON COLUMN class_groups.academic_year IS 'Ano letivo da turma';
COMMENT ON COLUMN class_groups.shift IS 'Turno da turma: MORNING, AFTERNOON, EVENING, FULL_TIME';
COMMENT ON COLUMN class_groups.status IS 'Status da turma: ACTIVE, INACTIVE, COMPLETED, SUSPENDED';
