-- Migração para criar tabela de horários
-- V6__Create_schedules_table.sql

CREATE TABLE schedules (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    class_group_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(20),
    academic_year VARCHAR(10),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    notes VARCHAR(500),
    
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
CREATE INDEX idx_schedules_teacher_id ON schedules(teacher_id);
CREATE INDEX idx_schedules_subject_id ON schedules(subject_id);
CREATE INDEX idx_schedules_class_group_id ON schedules(class_group_id);
CREATE INDEX idx_schedules_day_of_week ON schedules(day_of_week);
CREATE INDEX idx_schedules_status ON schedules(status);
CREATE INDEX idx_schedules_academic_year ON schedules(academic_year);
CREATE INDEX idx_schedules_room_number ON schedules(room_number);
CREATE INDEX idx_schedules_deleted_at ON schedules(deleted_at);

-- Índice composto para busca de conflitos
CREATE INDEX idx_schedules_conflict_check ON schedules(room_number, day_of_week, start_time, end_time, status);

-- Comentários para documentação
COMMENT ON TABLE schedules IS 'Tabela de horários do DistriSchool';
COMMENT ON COLUMN schedules.day_of_week IS 'Dia da semana: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY';
COMMENT ON COLUMN schedules.status IS 'Status do horário: ACTIVE, INACTIVE, SUSPENDED, COMPLETED';
