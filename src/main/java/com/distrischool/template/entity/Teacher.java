package com.distrischool.template.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "teachers")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(unique = true, nullable = false, length = 20)
    private String employeeId;
    
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Column(length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(name = "qualification", length = 200)
    private String qualification;
    
    @ElementCollection
    @CollectionTable(name = "teacher_subjects", joinColumns = @JoinColumn(name = "teacher_id"))
    @Column(name = "subject")
    private List<String> subjects;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private TeacherStatus status = TeacherStatus.ACTIVE;
    
    @Column(name = "hire_date")
    private LocalDate hireDate;
    
    @Column(name = "salary", precision = 10, scale = 2)
    private BigDecimal salary;
    
    public enum TeacherStatus {
        ACTIVE, INACTIVE, ON_LEAVE, RETIRED
    }
}
