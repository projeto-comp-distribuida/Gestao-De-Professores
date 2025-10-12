package com.distrischool.template.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "performance_reports")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceReport extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id")
    private ClassGroup classGroup;
    
    @Column(name = "report_period_start", nullable = false)
    private LocalDate reportPeriodStart;
    
    @Column(name = "report_period_end", nullable = false)
    private LocalDate reportPeriodEnd;
    
    @Column(name = "total_classes")
    private Integer totalClasses;
    
    @Column(name = "classes_taught")
    private Integer classesTaught;
    
    @Column(name = "attendance_rate")
    private BigDecimal attendanceRate;
    
    @Column(name = "average_grade")
    private BigDecimal averageGrade;
    
    @Column(name = "student_satisfaction")
    private BigDecimal studentSatisfaction;
    
    @Column(name = "workload_completion")
    private BigDecimal workloadCompletion;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_rating")
    private OverallRating overallRating;
    
    @Column(length = 1000)
    private String observations;
    
    @Column(length = 1000)
    private String recommendations;
    
    @Column(name = "generated_by", length = 100)
    private String generatedBy;
    
    @Column(name = "generated_at")
    private LocalDate generatedAt;
    
    public enum OverallRating {
        EXCELLENT, GOOD, SATISFACTORY, NEEDS_IMPROVEMENT, POOR
    }
}
