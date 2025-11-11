package com.distrischool.teacher.controller;

import com.distrischool.teacher.dto.ApiResponse;
import com.distrischool.teacher.dto.TeacherDTO;
import com.distrischool.teacher.entity.Teacher;
import com.distrischool.teacher.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {
    
    private final TeacherService teacherService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeacherDTO>>> getAllTeachers() {
        log.info("GET /api/v1/teachers - Listando todos os professores");
        List<TeacherDTO> teachers = teacherService.findAll();
        return ResponseEntity.ok(ApiResponse.success(teachers, "Professores listados com sucesso"));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherDTO>> getTeacherById(@PathVariable Long id) {
        log.info("GET /api/v1/teachers/{} - Buscando professor por ID", id);
        TeacherDTO teacher = teacherService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(teacher, "Professor encontrado"));
    }
    
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<ApiResponse<TeacherDTO>> getTeacherByEmployeeId(@PathVariable String employeeId) {
        log.info("GET /api/v1/teachers/employee/{} - Buscando professor por matrícula", employeeId);
        TeacherDTO teacher = teacherService.findByEmployeeId(employeeId);
        return ResponseEntity.ok(ApiResponse.success(teacher, "Professor encontrado"));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<TeacherDTO>> createTeacher(@Valid @RequestBody TeacherDTO teacherDTO) {
        log.info("POST /api/v1/teachers - Criando novo professor");
        TeacherDTO createdTeacher = teacherService.create(teacherDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdTeacher, "Professor criado com sucesso"));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<TeacherDTO>> updateTeacher(
            @PathVariable Long id, 
            @Valid @RequestBody TeacherDTO teacherDTO) {
        log.info("PUT /api/v1/teachers/{} - Atualizando professor", id);
        TeacherDTO updatedTeacher = teacherService.update(id, teacherDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTeacher, "Professor atualizado com sucesso"));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable Long id) {
        log.info("DELETE /api/v1/teachers/{} - Excluindo professor", id);
        teacherService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Professor excluído com sucesso"));
    }
    
    @GetMapping("/subject/{subject}")
    public ResponseEntity<ApiResponse<List<TeacherDTO>>> getTeachersBySubject(@PathVariable String subject) {
        log.info("GET /api/v1/teachers/subject/{} - Buscando professores por disciplina", subject);
        List<TeacherDTO> teachers = teacherService.findBySubject(subject);
        return ResponseEntity.ok(ApiResponse.success(teachers, "Professores da disciplina listados"));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<TeacherDTO>>> getTeachersByStatus(@PathVariable Teacher.TeacherStatus status) {
        log.info("GET /api/v1/teachers/status/{} - Buscando professores por status", status);
        List<TeacherDTO> teachers = teacherService.findByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(teachers, "Professores com status listados"));
    }
    
    @GetMapping("/hired")
    public ResponseEntity<ApiResponse<List<TeacherDTO>>> getTeachersByHireDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/teachers/hired - Buscando professores contratados entre {} e {}", startDate, endDate);
        List<TeacherDTO> teachers = teacherService.findByHireDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(teachers, "Professores contratados no período listados"));
    }
}
