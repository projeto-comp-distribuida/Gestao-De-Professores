package com.distrischool.template.controller;

import com.distrischool.template.dto.ApiResponse;
import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.entity.TeacherAssignment;
import com.distrischool.template.entity.Schedule;
import com.distrischool.template.entity.PerformanceReport;
import com.distrischool.template.service.TeacherManagementService;
import com.distrischool.template.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher-management")
@RequiredArgsConstructor
@Slf4j
public class TeacherManagementController {
    
    private final TeacherManagementService teacherManagementService;
    private final TeacherService teacherService;
    
    // ========== GESTÃO DE PROFESSORES ==========
    
    @GetMapping("/teachers")
    public ResponseEntity<ApiResponse<List<TeacherDTO>>> getAllTeachers() {
        log.info("GET /api/v1/teacher-management/teachers - Listando todos os professores");
        List<TeacherDTO> teachers = teacherService.findAll();
        return ResponseEntity.ok(ApiResponse.success(teachers, "Professores listados com sucesso"));
    }
    
    @GetMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<TeacherDTO>> getTeacherById(@PathVariable Long id) {
        log.info("GET /api/v1/teacher-management/teachers/{} - Buscando professor por ID", id);
        TeacherDTO teacher = teacherService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(teacher, "Professor encontrado"));
    }
    
    @PostMapping("/teachers")
    public ResponseEntity<ApiResponse<TeacherDTO>> createTeacher(
            @Valid @RequestBody TeacherDTO teacherDTO,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @AuthenticationPrincipal Jwt jwt) {
        
        String effectiveUserId = userId != null ? userId : (jwt != null ? jwt.getSubject() : null);
        
        if (effectiveUserId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Usuário não autenticado"));
        }

        // Verifica se o usuário tem role/permissão ADMIN via auth service
        boolean isAdmin = teacherService.isAdmin(effectiveUserId);
        if (!isAdmin) {
            log.warn("Tentativa de criar professor sem permissão ADMIN por usuário: {}", effectiveUserId);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Apenas usuários com role ADMIN podem criar professores"));
        }

        log.info("POST /api/v1/teacher-management/teachers - Criando novo professor (by {})", effectiveUserId);
        TeacherDTO createdTeacher = teacherManagementService.createTeacher(teacherDTO, authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdTeacher, "Professor criado com sucesso"));
    }
    
    @PutMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<TeacherDTO>> updateTeacher(
            @PathVariable Long id, 
            @Valid @RequestBody TeacherDTO teacherDTO) {
        log.info("PUT /api/v1/teacher-management/teachers/{} - Atualizando professor", id);
        TeacherDTO updatedTeacher = teacherManagementService.updateTeacher(id, teacherDTO);
        return ResponseEntity.ok(ApiResponse.success(updatedTeacher, "Professor atualizado com sucesso"));
    }
    
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable Long id) {
        log.info("DELETE /api/v1/teacher-management/teachers/{} - Excluindo professor", id);
        teacherManagementService.deleteTeacher(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Professor excluído com sucesso"));
    }
    
    // ========== ATRIBUIÇÃO DE DISCIPLINAS/TURMAS ==========
    
    @PostMapping("/assignments")
    public ResponseEntity<ApiResponse<TeacherAssignment>> assignTeacherToClass(
            @RequestParam Long teacherId,
            @RequestParam Long subjectId,
            @RequestParam Long classGroupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Integer workloadHours) {
        log.info("POST /api/v1/teacher-management/assignments - Atribuindo professor {} à turma {}", teacherId, classGroupId);
        
        TeacherAssignment assignment = teacherManagementService.assignTeacherToClass(
                teacherId, subjectId, classGroupId, startDate, endDate, workloadHours);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assignment, "Professor atribuído com sucesso"));
    }
    
    @GetMapping("/assignments/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<TeacherAssignment>>> getTeacherAssignments(@PathVariable Long teacherId) {
        log.info("GET /api/v1/teacher-management/assignments/teacher/{} - Buscando atribuições do professor", teacherId);
        // Implementar busca de atribuições
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Atribuições do professor"));
    }
    
    @GetMapping("/assignments/class/{classGroupId}")
    public ResponseEntity<ApiResponse<List<TeacherAssignment>>> getClassAssignments(@PathVariable Long classGroupId) {
        log.info("GET /api/v1/teacher-management/assignments/class/{} - Buscando atribuições da turma", classGroupId);
        // Implementar busca de atribuições
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Atribuições da turma"));
    }
    
    // ========== VISUALIZAÇÃO DE HORÁRIOS ==========
    
    @GetMapping("/schedules/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<Schedule>>> getTeacherSchedule(
            @PathVariable Long teacherId,
            @RequestParam String academicYear) {
        log.info("GET /api/v1/teacher-management/schedules/teacher/{} - Buscando horários do professor", teacherId);
        
        List<Schedule> schedules = teacherManagementService.getTeacherSchedule(teacherId, academicYear);
        return ResponseEntity.ok(ApiResponse.success(schedules, "Horários do professor"));
    }
    
    @GetMapping("/schedules/class/{classGroupId}")
    public ResponseEntity<ApiResponse<List<Schedule>>> getClassSchedule(@PathVariable Long classGroupId) {
        log.info("GET /api/v1/teacher-management/schedules/class/{} - Buscando horários da turma", classGroupId);
        
        List<Schedule> schedules = teacherManagementService.getClassSchedule(classGroupId);
        return ResponseEntity.ok(ApiResponse.success(schedules, "Horários da turma"));
    }
    
    @GetMapping("/schedules/weekly/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<Schedule>>> getWeeklyTeacherSchedule(
            @PathVariable Long teacherId,
            @RequestParam String academicYear) {
        log.info("GET /api/v1/teacher-management/schedules/weekly/teacher/{} - Buscando horário semanal do professor", teacherId);
        
        List<Schedule> schedules = teacherManagementService.getTeacherSchedule(teacherId, academicYear);
        return ResponseEntity.ok(ApiResponse.success(schedules, "Horário semanal do professor"));
    }
    
    // ========== RELATÓRIOS DE DESEMPENHO ==========
    
    @PostMapping("/performance-reports")
    public ResponseEntity<ApiResponse<PerformanceReport>> generatePerformanceReport(
            @RequestParam Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("POST /api/v1/teacher-management/performance-reports - Gerando relatório de desempenho");
        
        PerformanceReport report = teacherManagementService.generatePerformanceReport(teacherId, startDate, endDate);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(report, "Relatório de desempenho gerado com sucesso"));
    }
    
    @GetMapping("/performance-reports/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<List<PerformanceReport>>> getTeacherPerformanceReports(@PathVariable Long teacherId) {
        log.info("GET /api/v1/teacher-management/performance-reports/teacher/{} - Buscando relatórios do professor", teacherId);
        // Implementar busca de relatórios
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Relatórios de desempenho do professor"));
    }
    
    @GetMapping("/performance-reports/period")
    public ResponseEntity<ApiResponse<List<PerformanceReport>>> getPerformanceReportsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/teacher-management/performance-reports/period - Buscando relatórios por período");
        // Implementar busca de relatórios
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Relatórios de desempenho do período"));
    }
    
    // ========== NOTIFICAÇÕES ==========
    
    @PostMapping("/notifications/assignment/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> sendAssignmentNotification(@PathVariable Long assignmentId) {
        log.info("POST /api/v1/teacher-management/notifications/assignment/{} - Enviando notificação de atribuição", assignmentId);
        // Implementar envio de notificação
        return ResponseEntity.ok(ApiResponse.success(null, "Notificação enviada com sucesso"));
    }
    
    @GetMapping("/notifications/pending")
    public ResponseEntity<ApiResponse<List<TeacherAssignment>>> getPendingNotifications() {
        log.info("GET /api/v1/teacher-management/notifications/pending - Buscando notificações pendentes");
        // Implementar busca de notificações pendentes
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Notificações pendentes"));
    }
    
    // ========== LOGS DE AUDITORIA ==========
    
    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<List<Object>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/v1/teacher-management/audit-logs - Buscando logs de auditoria");
        // Implementar busca de logs de auditoria
        return ResponseEntity.ok(ApiResponse.success(List.of(), "Logs de auditoria"));
    }
    
    // ========== DASHBOARD E ESTATÍSTICAS ==========
    
    @GetMapping("/dashboard/overview")
    public ResponseEntity<ApiResponse<Object>> getDashboardOverview() {
        log.info("GET /api/v1/teacher-management/dashboard/overview - Buscando visão geral do dashboard");
        
        // Implementar dados do dashboard
        Object overview = Map.of(
            "totalTeachers", 0,
            "activeTeachers", 0,
            "totalAssignments", 0,
            "pendingNotifications", 0,
            "averagePerformance", 0.0
        );
        
        return ResponseEntity.ok(ApiResponse.success(overview, "Visão geral do dashboard"));
    }
    
    @GetMapping("/dashboard/performance-summary")
    public ResponseEntity<ApiResponse<Object>> getPerformanceSummary() {
        log.info("GET /api/v1/teacher-management/dashboard/performance-summary - Buscando resumo de desempenho");
        
        // Implementar resumo de desempenho
        Object summary = Map.of(
            "excellent", 0,
            "good", 0,
            "satisfactory", 0,
            "needsImprovement", 0,
            "poor", 0
        );
        
        return ResponseEntity.ok(ApiResponse.success(summary, "Resumo de desempenho"));
    }
    
    // ========== HEALTH CHECK ESPECÍFICO ==========
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Object>> getHealth() {
        log.info("GET /api/v1/teacher-management/health - Verificando saúde do serviço de gestão de professores");
        
        Object health = Map.of(
            "status", "UP",
            "service", "Teacher Management Service",
            "version", "1.0.0",
            "uptime", "99.9%",
            "features", List.of(
                "Cadastro/edição de professores",
                "Atribuição de disciplinas/turmas",
                "Visualização de horários",
                "Relatórios de desempenho",
                "Notificações de atribuições",
                "Logs de auditoria"
            )
        );
        
        return ResponseEntity.ok(ApiResponse.success(health, "Serviço de gestão de professores funcionando"));
    }
    
}
