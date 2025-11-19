package com.distrischool.template.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO para criação de atribuição de professor a turma/disciplina
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAssignmentRequestDTO {
    
    @NotNull(message = "ID do professor é obrigatório")
    @Positive(message = "ID do professor deve ser positivo")
    private Long teacherId;
    
    @NotNull(message = "ID da disciplina é obrigatório")
    @Positive(message = "ID da disciplina deve ser positivo")
    private Long subjectId;
    
    @NotNull(message = "ID da turma é obrigatório")
    @Positive(message = "ID da turma deve ser positivo")
    private Long classGroupId;
    
    @NotNull(message = "Data de início é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    
    @NotNull(message = "Carga horária é obrigatória")
    @Positive(message = "Carga horária deve ser positiva")
    private Integer workloadHours;
    
    private String notes;
}

