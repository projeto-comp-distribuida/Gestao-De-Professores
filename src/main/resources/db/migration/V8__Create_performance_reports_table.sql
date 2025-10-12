-- Migração para criar tabela de relatórios de desempenho
-- V8__Create_performance_reports_table.sql

CREATE TABLE performance_reports (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    subject_id BIGINT,
    class_group_id BIGINT,
    report_period_start DATE NOT NULL,
    report_period_end DATE NOT NULL,
    total_classes INTEGER,
    classes_taught INTEGER,
    attendance_rate DECIMAL(5,2),
    average_grade DECIMAL(5,2),
    student_satisfaction DECIMAL(5,2),
    workload_completion DECIMAL(5,2),
    overall_rating VARCHAR(20),
    observations VARCHAR(1000),
    recommendations VARCHAR(1000),
    generated_by VARCHAR(100),
    generated_at DATE,
    
    -- Campos de auditoria da BaseEntity
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    
    -- Chaves estrangeiras
    FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL,
    FOREIGN KEY (class_group_id) REFERENCES class_groups(id) ON DELETE SET NULL
);

-- Índices para performance
CREATE INDEX idx_performance_reports_teacher_id ON performance_reports(teacher_id);
CREATE INDEX idx_performance_reports_subject_id ON performance_reports(subject_id);
CREATE INDEX idx_performance_reports_class_group_id ON performance_reports(class_group_id);
CREATE INDEX idx_performance_reports_overall_rating ON performance_reports(overall_rating);
CREATE INDEX idx_performance_reports_period_start ON performance_reports(report_period_start);
CREATE INDEX idx_performance_reports_period_end ON performance_reports(report_period_end);
CREATE INDEX idx_performance_reports_generated_at ON performance_reports(generated_at);
CREATE INDEX idx_performance_reports_deleted_at ON performance_reports(deleted_at);

-- Índice composto para busca por período
CREATE INDEX idx_performance_reports_period ON performance_reports(report_period_start, report_period_end);

-- Comentários para documentação
COMMENT ON TABLE performance_reports IS 'Tabela de relatórios de desempenho dos professores';
COMMENT ON COLUMN performance_reports.overall_rating IS 'Avaliação geral: EXCELLENT, GOOD, SATISFACTORY, NEEDS_IMPROVEMENT, POOR';
COMMENT ON COLUMN performance_reports.attendance_rate IS 'Taxa de presença (0.00 a 1.00)';
COMMENT ON COLUMN performance_reports.student_satisfaction IS 'Satisfação dos alunos (0.00 a 5.00)';
COMMENT ON COLUMN performance_reports.workload_completion IS 'Percentual de cumprimento da carga horária (0.00 a 1.00)';
