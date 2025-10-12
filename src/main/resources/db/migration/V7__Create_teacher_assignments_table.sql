-- Migração para criar tabela de atribuições de professores
-- V7__Create_teacher_assignments_table.sql

CREATE TABLE teacher_assignments (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    assignment_date DATE NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    workload_hours INTEGER,
    notes VARCHAR(500),
    notification_sent BOOLEAN DEFAULT FALSE,
    
    -- Campos de auditoria da BaseEntity
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    -- Chaves estrangeiras
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE CASCADE
);

-- Índices para performance
CREATE INDEX idx_teacher_assignments_teacher_id ON teacher_assignments(teacher_id);
CREATE INDEX idx_teacher_assignments_subject_id ON teacher_assignments(subject_id);
CREATE INDEX idx_teacher_assignments_class_group_id ON teacher_assignments(class_group_id);
CREATE INDEX idx_teacher_assignments_status ON teacher_assignments(status);
CREATE INDEX idx_teacher_assignments_assignment_date ON teacher_assignments(assignment_date);
CREATE INDEX idx_teacher_assignments_notification_sent ON teacher_assignments(notification_sent);
CREATE INDEX idx_teacher_assignments_deleted_at ON teacher_assignments(deleted_at);

-- Índice único para evitar atribuições duplicadas
CREATE UNIQUE INDEX idx_teacher_assignments_unique ON teacher_assignments(teacher_id, subject_id, class_group_id, status) WHERE deleted_at IS NULL;

-- Comentários para documentação
COMMENT ON TABLE teacher_assignments IS 'Tabela de atribuições de professores a disciplinas e turmas';
COMMENT ON COLUMN teacher_assignments.status IS 'Status da atribuição: ACTIVE, INACTIVE, COMPLETED, SUSPENDED';
COMMENT ON COLUMN teacher_assignments.notification_sent IS 'Indica se a notificação de atribuição foi enviada';
