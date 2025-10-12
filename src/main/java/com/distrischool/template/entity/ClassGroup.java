package com.distrischool.template.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "class_groups")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassGroup extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(length = 10)
    private String code;
    
    @Column(name = "academic_year")
    private String academicYear;
    
    @Column(name = "grade_level")
    private String gradeLevel;
    
    @Column(name = "max_students")
    private Integer maxStudents;
    
    @Column(name = "current_students")
    @Builder.Default
    private Integer currentStudents = 0;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private ClassStatus status = ClassStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "shift")
    private Shift shift;
    
    public enum ClassStatus {
        ACTIVE, INACTIVE, COMPLETED, SUSPENDED
    }
    
    public enum Shift {
        MORNING, AFTERNOON, EVENING, FULL_TIME
    }
}
