package com.distrischool.teacher.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 10)
    private String code;
    
    @Column(length = 500)
    private String description;
    
    @Column(name = "workload_hours")
    private Integer workloadHours;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private SubjectLevel level;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private SubjectStatus status = SubjectStatus.ACTIVE;
    
    public enum SubjectLevel {
        BASIC, INTERMEDIATE, ADVANCED
    }
    
    public enum SubjectStatus {
        ACTIVE, INACTIVE, UNDER_REVIEW
    }
}
