package com.distrischool.template.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "teacher_assignments")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAssignment extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;
    
    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private AssignmentStatus status = AssignmentStatus.ACTIVE;
    
    @Column(name = "workload_hours")
    private Integer workloadHours;
    
    @Column(length = 500)
    private String notes;
    
    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;
    
    public enum AssignmentStatus {
        ACTIVE, INACTIVE, COMPLETED, SUSPENDED
    }
}
