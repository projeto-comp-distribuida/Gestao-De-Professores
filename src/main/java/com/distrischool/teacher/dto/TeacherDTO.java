package com.distrischool.teacher.dto;

import com.distrischool.teacher.entity.Teacher;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherDTO {
    
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    private String name;
    
    @NotBlank(message = "Matrícula é obrigatória")
    private String employeeId;
    
    private LocalDate birthDate;
    private String email;
    private String phone;
    private String qualification;
    private List<String> subjects;
    private Teacher.TeacherStatus status;
    private LocalDate hireDate;
    private BigDecimal salary;
    
    // Construtor para conversão de Entity para DTO
    public TeacherDTO(Teacher teacher) {
        this.id = teacher.getId();
        this.name = teacher.getName();
        this.employeeId = teacher.getEmployeeId();
        this.birthDate = teacher.getBirthDate();
        this.email = teacher.getEmail();
        this.phone = teacher.getPhone();
        this.qualification = teacher.getQualification();
        this.subjects = teacher.getSubjects();
        this.status = teacher.getStatus();
        this.hireDate = teacher.getHireDate();
        this.salary = teacher.getSalary();
    }
}
