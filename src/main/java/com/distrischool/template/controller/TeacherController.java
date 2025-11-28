package com.distrischool.template.controller;

import com.distrischool.template.dto.ApiResponse;
import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<ApiResponse<TeacherDTO>> createTeacher(
            @Valid @RequestBody TeacherDTO teacherDTO,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("POST /api/v1/teachers - Criando novo professor");
        TeacherDTO createdTeacher = teacherService.create(teacherDTO, authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdTeacher, "Professor criado com sucesso"));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeacherDTO>> updateTeacher(
            @PathVariable Long id, 
            @Valid @RequestBody TeacherDTO teacherDTO) {
        log.info("PUT /api/v1/teachers/{} - Atualizando professor", id);
        TeacherDTO updatedTeacher = teacherService.update(id, teacherDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTeacher, "Professor atualizado com sucesso"));
    }
    
    @DeleteMapping("/{id}")
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
    
    /**
     * Busca múltiplos professores por IDs (validação em lote)
     * POST /api/v1/teachers/batch
     * 
     * Este endpoint é usado por outros microserviços para validar a existência de professores.
     * Retorna uma lista de Maps com os dados dos professores encontrados.
     * 
     * @param teacherIds Lista de IDs dos professores a serem buscados
     * @return Lista de Maps contendo os dados dos professores encontrados
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTeachersByIds(
            @RequestBody(required = false) List<Long> teacherIds) {
        log.info("Requisição para buscar múltiplos professores por IDs: {}", teacherIds);
        
        if (teacherIds == null || teacherIds.isEmpty()) {
            log.warn("Lista de IDs vazia ou nula recebida");
            return ResponseEntity.ok(ApiResponse.success(List.of(), "Nenhum ID fornecido"));
        }
        
        List<Map<String, Object>> teachers = teacherService.getTeachersByIds(teacherIds);
        log.info("Encontrados {} professores de {} IDs solicitados", teachers.size(), teacherIds.size());
        return ResponseEntity.ok(ApiResponse.success(teachers, "Busca em lote concluída com sucesso"));
    }

    /**
     * Busca professor por Auth0 ID
     * GET /api/v1/teachers/by-auth0/{auth0Id}
     * 
     * Este endpoint é usado pelo serviço de autenticação para obter o ID do professor
     * associado a um Auth0 ID. Retorna apenas o ID do professor ou 404 se não encontrado.
     * 
     * @param auth0Id Auth0 ID do usuário
     * @return ID do professor ou 404 se não encontrado
     */
    @GetMapping("/by-auth0/{auth0Id}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getTeacherIdByAuth0Id(@PathVariable String auth0Id) {
        log.info("Requisição para buscar professor por Auth0 ID: {}", auth0Id);
        
        Long teacherId = teacherService.getTeacherIdByAuth0Id(auth0Id);
        
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Professor não encontrado para o Auth0 ID fornecido"));
        }
        
        Map<String, Long> response = Map.of("id", teacherId);
        return ResponseEntity.ok(ApiResponse.success(response, "Professor encontrado"));
    }
}
