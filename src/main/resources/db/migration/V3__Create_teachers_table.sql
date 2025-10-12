-- Migração para criar tabela de professores
-- V3__Create_teachers_table.sql

CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    employee_id VARCHAR(20) UNIQUE NOT NULL,
    birth_date DATE,
    email VARCHAR(100),
    phone VARCHAR(20),
    qualification VARCHAR(200),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    hire_date DATE,
    salary DECIMAL(10,2),
    
    -- Campos de auditoria da BaseEntity
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100)
);

-- Tabela para disciplinas dos professores (relacionamento many-to-many)
CREATE TABLE teacher_subjects (
    teacher_id BIGINT NOT NULL,
    subject VARCHAR(100) NOT NULL,
    PRIMARY KEY (teacher_id, subject),
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_teachers_employee_id ON teachers(employee_id);
CREATE INDEX idx_teachers_status ON teachers(status);
CREATE INDEX idx_teachers_hire_date ON teachers(hire_date);
CREATE INDEX idx_teachers_deleted_at ON teachers(deleted_at);
CREATE INDEX idx_teacher_subjects_subject ON teacher_subjects(subject);

-- Comentários para documentação
COMMENT ON TABLE teachers IS 'Tabela de professores do DistriSchool';
COMMENT ON COLUMN teachers.employee_id IS 'Matrícula única do professor';
COMMENT ON COLUMN teachers.status IS 'Status do professor: ACTIVE, INACTIVE, ON_LEAVE, RETIRED';
COMMENT ON COLUMN teachers.qualification IS 'Qualificação acadêmica do professor';
COMMENT ON TABLE teacher_subjects IS 'Tabela de relacionamento professores-disciplinas';
