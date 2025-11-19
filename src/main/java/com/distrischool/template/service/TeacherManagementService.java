package com.distrischool.template.service;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.dto.auth.ApiResponse;
import com.distrischool.template.dto.auth.AuthResponse;
import com.distrischool.template.dto.auth.RegisterUserRequest;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.entity.TeacherAssignment;
import com.distrischool.template.entity.Schedule;
import com.distrischool.template.entity.PerformanceReport;
import com.distrischool.template.entity.Subject;
import com.distrischool.template.entity.ClassGroup;
import com.distrischool.template.exception.BusinessException;
import com.distrischool.template.exception.ResourceNotFoundException;
import com.distrischool.template.feign.AuthServiceClient;
import com.distrischool.template.kafka.DistriSchoolEvent;
import com.distrischool.template.kafka.EventProducer;
import com.distrischool.template.repository.TeacherRepository;
import com.distrischool.template.repository.TeacherAssignmentRepository;
import com.distrischool.template.repository.ScheduleRepository;
import com.distrischool.template.repository.PerformanceReportRepository;
import com.distrischool.template.repository.SubjectRepository;
import com.distrischool.template.repository.ClassGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeacherManagementService {
    
    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository assignmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final PerformanceReportRepository performanceReportRepository;
    private final SubjectRepository subjectRepository;
    private final ClassGroupRepository classGroupRepository;
    private final EventProducer eventProducer;
    private final AuthServiceClient authServiceClient;
    
    // ========== GESTÃO DE PROFESSORES ==========
    
    public TeacherDTO createTeacher(TeacherDTO teacherDTO, String authorizationHeader) {
        log.info("Criando novo professor: {}", teacherDTO.getName());
        
        Teacher teacher = Teacher.builder()
                .name(teacherDTO.getName())
                .employeeId(teacherDTO.getEmployeeId())
                .birthDate(teacherDTO.getBirthDate())
                .email(teacherDTO.getEmail())
                .phone(teacherDTO.getPhone())
                .qualification(teacherDTO.getQualification())
                .subjects(teacherDTO.getSubjects())
                .status(teacherDTO.getStatus())
                .hireDate(teacherDTO.getHireDate())
                .salary(teacherDTO.getSalary())
                .build();
        
        // Cria usuário no auth service antes de salvar o professor
        // auth0Id será definido após criar o usuário no serviço de auth
        log.info("Iniciando criação de usuário Auth0 antes de salvar professor no banco");
        String auth0Id = null;
        try {
            auth0Id = createAuthUserForTeacher(teacher, authorizationHeader);
            if (auth0Id == null || auth0Id.isBlank()) {
                log.error("createAuthUserForTeacher retornou auth0Id nulo ou vazio para professor: {}", teacher.getEmail());
                throw new BusinessException("Falha ao obter Auth0 ID após registro do usuário");
            }
            teacher.setAuth0Id(auth0Id);
            log.info("Auth0 ID obtido com sucesso: {} para professor: {}", auth0Id, teacher.getEmail());
        } catch (Exception e) {
            log.error("Erro ao criar usuário Auth0 para professor {}: {}", teacher.getEmail(), e.getMessage(), e);
            throw new BusinessException("Não foi possível criar o professor: falha ao registrar usuário no Auth0. " + e.getMessage());
        }
        
        Teacher savedTeacher = teacherRepository.save(teacher);
        
        // Publicar evento no Kafka - usar DTO para evitar problemas de serialização com entidade JPA
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("teacher", new TeacherDTO(savedTeacher));
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.created", 
                "teacher-management-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.created", event);
        
        // Log de auditoria
        logAuditEvent("TEACHER_CREATED", savedTeacher.getId(), "Professor criado: " + savedTeacher.getName());
        
        log.info("Professor criado com sucesso: {}", savedTeacher.getId());
        return new TeacherDTO(savedTeacher);
    }
    
    public TeacherDTO updateTeacher(Long id, TeacherDTO teacherDTO) {
        log.info("Atualizando professor com ID: {}", id);
        
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + id));
        
        String oldName = teacher.getName();
        
        teacher.setName(teacherDTO.getName());
        teacher.setEmployeeId(teacherDTO.getEmployeeId());
        teacher.setBirthDate(teacherDTO.getBirthDate());
        teacher.setEmail(teacherDTO.getEmail());
        teacher.setPhone(teacherDTO.getPhone());
        teacher.setQualification(teacherDTO.getQualification());
        teacher.setSubjects(teacherDTO.getSubjects());
        teacher.setStatus(teacherDTO.getStatus());
        teacher.setHireDate(teacherDTO.getHireDate());
        teacher.setSalary(teacherDTO.getSalary());
        
        Teacher updatedTeacher = teacherRepository.save(teacher);
        
        // Publicar evento no Kafka - usar DTO para evitar problemas de serialização com entidade JPA
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("teacher", new TeacherDTO(updatedTeacher));
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.updated", 
                "teacher-management-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.updated", event);
        
        // Log de auditoria
        logAuditEvent("TEACHER_UPDATED", updatedTeacher.getId(), 
                "Professor atualizado: " + oldName + " -> " + updatedTeacher.getName());
        
        log.info("Professor atualizado com sucesso: {}", updatedTeacher.getId());
        return new TeacherDTO(updatedTeacher);
    }
    
    public void deleteTeacher(Long id) {
        log.info("Excluindo professor com ID: {}", id);
        
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + id));
        
        // Verificar se há atribuições ativas
        List<TeacherAssignment> activeAssignments = assignmentRepository.findActiveByTeacher(teacher);
        if (!activeAssignments.isEmpty()) {
            throw new IllegalStateException("Não é possível excluir professor com atribuições ativas");
        }
        
        teacher.setDeletedAt(LocalDateTime.now());
        Teacher savedTeacher = teacherRepository.save(teacher);
        
        // Publicar evento no Kafka - usar DTO para evitar problemas de serialização com entidade JPA
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("teacher", new TeacherDTO(savedTeacher));
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.deleted", 
                "teacher-management-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.deleted", event);
        
        // Log de auditoria
        logAuditEvent("TEACHER_DELETED", teacher.getId(), "Professor excluído: " + teacher.getName());
        
        log.info("Professor excluído com sucesso: {}", id);
    }
    
    // ========== ATRIBUIÇÃO DE DISCIPLINAS/TURMAS ==========
    
    public TeacherAssignment assignTeacherToClass(Long teacherId, Long subjectId, Long classGroupId, 
                                                 LocalDate startDate, LocalDate endDate, Integer workloadHours) {
        log.info("Atribuindo professor {} à disciplina {} na turma {}", teacherId, subjectId, classGroupId);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        
        // Buscar Subject e ClassGroup
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Disciplina não encontrada"));
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada"));
        
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacher(teacher)
                .subject(subject)
                .classGroup(classGroup)
                .assignmentDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .workloadHours(workloadHours)
                .status(TeacherAssignment.AssignmentStatus.ACTIVE)
                .notificationSent(false)
                .build();
        
        TeacherAssignment savedAssignment = assignmentRepository.save(assignment);
        
        // Enviar notificação
        sendAssignmentNotification(savedAssignment);
        
        // Publicar evento no Kafka
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("assignment", savedAssignment);
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.assigned", 
                "teacher-management-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.assigned", event);
        
        // Log de auditoria
        logAuditEvent("TEACHER_ASSIGNED", savedAssignment.getId(), 
                "Professor atribuído: " + teacher.getName() + " à turma " + classGroupId);
        
        log.info("Professor atribuído com sucesso: {}", savedAssignment.getId());
        return savedAssignment;
    }
    
    // ========== VISUALIZAÇÃO DE HORÁRIOS ==========
    
    public List<Schedule> getTeacherSchedule(Long teacherId, String academicYear) {
        log.info("Buscando horários do professor {} para o ano {}", teacherId, academicYear);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        
        return scheduleRepository.findByTeacherAndAcademicYear(teacher, academicYear);
    }
    
    public List<Schedule> getClassSchedule(Long classGroupId) {
        log.info("Buscando horários da turma {}", classGroupId);
        
        ClassGroup classGroup = classGroupRepository.findById(classGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada"));
        return scheduleRepository.findByClassGroup(classGroup);
    }
    
    // ========== RELATÓRIOS DE DESEMPENHO ==========
    
    public PerformanceReport generatePerformanceReport(Long teacherId, LocalDate startDate, LocalDate endDate) {
        log.info("Gerando relatório de desempenho para professor {} no período {} - {}", 
                teacherId, startDate, endDate);
        
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        
        // Buscar dados do período
        List<TeacherAssignment> assignments = assignmentRepository.findByTeacher(teacher);
        
        // Calcular métricas
        Integer totalClasses = calculateTotalClasses(assignments);
        Integer classesTaught = calculateClassesTaught(assignments);
        BigDecimal attendanceRate = calculateAttendanceRate(classesTaught, totalClasses);
        BigDecimal averageGrade = calculateAverageGrade(assignments);
        BigDecimal studentSatisfaction = calculateStudentSatisfaction(assignments);
        BigDecimal workloadCompletion = calculateWorkloadCompletion(assignments);
        
        PerformanceReport.OverallRating overallRating = calculateOverallRating(
                attendanceRate, averageGrade, studentSatisfaction, workloadCompletion);
        
        PerformanceReport report = PerformanceReport.builder()
                .teacher(teacher)
                .reportPeriodStart(startDate)
                .reportPeriodEnd(endDate)
                .totalClasses(totalClasses)
                .classesTaught(classesTaught)
                .attendanceRate(attendanceRate)
                .averageGrade(averageGrade)
                .studentSatisfaction(studentSatisfaction)
                .workloadCompletion(workloadCompletion)
                .overallRating(overallRating)
                .generatedBy("SYSTEM")
                .generatedAt(LocalDate.now())
                .build();
        
        PerformanceReport savedReport = performanceReportRepository.save(report);
        
        // Publicar evento no Kafka
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("report", savedReport);
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "performance.report.generated", 
                "teacher-management-service", 
                eventData
        );
        eventProducer.sendEvent("performance.report.generated", event);
        
        // Log de auditoria
        logAuditEvent("PERFORMANCE_REPORT_GENERATED", savedReport.getId(), 
                "Relatório de desempenho gerado para professor: " + teacher.getName());
        
        log.info("Relatório de desempenho gerado com sucesso: {}", savedReport.getId());
        return savedReport;
    }
    
    // ========== NOTIFICAÇÕES ==========
    
    public void sendAssignmentNotification(TeacherAssignment assignment) {
        log.info("Enviando notificação de atribuição para professor {}", assignment.getTeacher().getId());
        
        // Implementar lógica de notificação (email, SMS, etc.)
        // Por enquanto, apenas marcar como enviada
        
        assignment.setNotificationSent(true);
        assignmentRepository.save(assignment);
        
        // Publicar evento no Kafka
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("assignment", assignment);
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "assignment.notification.sent", 
                "teacher-management-service", 
                eventData
        );
        eventProducer.sendEvent("assignment.notification.sent", event);
        
        // Log de auditoria
        logAuditEvent("ASSIGNMENT_NOTIFICATION_SENT", assignment.getId(), 
                "Notificação de atribuição enviada para professor: " + assignment.getTeacher().getName());
    }
    
    // ========== LOGS DE AUDITORIA ==========
    
    private void logAuditEvent(String action, Long entityId, String description) {
        log.info("AUDIT: {} | EntityId: {} | Description: {} | Timestamp: {}", 
                action, entityId, description, LocalDateTime.now());
        
        // Publicar evento de auditoria no Kafka
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("action", action);
        auditData.put("entityId", entityId);
        auditData.put("description", description);
        auditData.put("timestamp", LocalDateTime.now());
        DistriSchoolEvent auditEvent = DistriSchoolEvent.create(
                "audit.log", 
                "teacher-management-service", 
                auditData
        );
        eventProducer.sendEvent("audit.log", auditEvent);
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    /**
     * Cria um usuário no serviço de autenticação para o professor
     */
    private String createAuthUserForTeacher(Teacher teacher, String authorizationHeader) {
        log.info("Iniciando registro de usuário Auth0 para professor: {} ({})", teacher.getEmail(), teacher.getName());
        
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("Authorization header está vazio ou nulo. Tentando registrar sem header de autorização.");
        }
        
        try {
            String password = generateSecurePassword();

            RegisterUserRequest registerUserRequest = RegisterUserRequest.builder()
                    .email(teacher.getEmail())
                    .password(password)
                    .confirmPassword(password)
                    .firstName(extractFirstName(teacher.getName()))
                    .lastName(extractLastName(teacher.getName()))
                    .phone(teacher.getPhone())
                    .documentNumber(null) // Professores podem não ter CPF obrigatório
                    .roles(Set.of("TEACHER"))
                    .build();

            log.info("Chamando authServiceClient.registerUser para email: {}", teacher.getEmail());
            ApiResponse<AuthResponse> response = authServiceClient.registerUser(authorizationHeader, registerUserRequest);
            log.info("Resposta recebida do authServiceClient: success={}, message={}", 
                    response != null ? response.getSuccess() : "null", 
                    response != null ? response.getMessage() : "null");

            if (response == null) {
                throw new BusinessException("Serviço de autenticação não respondeu ao registrar o usuário do professor");
            }

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                String message = response.getMessage() != null ? response.getMessage() : "Resposta sem sucesso do serviço de autenticação";
                throw new BusinessException("Falha ao registrar usuário no Auth0: " + message);
            }

            AuthResponse data = response.getData();
            if (data == null || data.getUser() == null) {
                throw new BusinessException("Serviço de autenticação não retornou os dados do usuário registrado");
            }

            String auth0Id = data.getUser().getAuth0Id();
            if (auth0Id == null || auth0Id.isBlank()) {
                throw new BusinessException("Serviço de autenticação não retornou o Auth0 ID do usuário");
            }

            log.info("Usuário Auth0 registrado com sucesso para {} - auth0Id={}", teacher.getEmail(), auth0Id);
            return auth0Id;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Falha ao registrar usuário Auth0 para {}: {}", teacher.getEmail(), ex.getMessage(), ex);
            throw new BusinessException("Erro ao registrar usuário no Auth0: " + ex.getMessage());
        }
    }

    /**
     * Gera uma senha segura para o usuário
     */
    private String generateSecurePassword() {
        final String lower = "abcdefghijklmnopqrstuvwxyz";
        final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String digits = "0123456789";
        final String special = "@$!%*?&";
        final String allChars = lower + upper + digits + special;
        final int length = 12;

        SecureRandom random = new SecureRandom();
        ArrayList<Character> passwordChars = new ArrayList<>();

        passwordChars.add(lower.charAt(random.nextInt(lower.length())));
        passwordChars.add(upper.charAt(random.nextInt(upper.length())));
        passwordChars.add(digits.charAt(random.nextInt(digits.length())));
        passwordChars.add(special.charAt(random.nextInt(special.length())));

        while (passwordChars.size() < length) {
            passwordChars.add(allChars.charAt(random.nextInt(allChars.length())));
        }

        Collections.shuffle(passwordChars, random);

        StringBuilder password = new StringBuilder();
        for (Character c : passwordChars) {
            password.append(c);
        }
        return password.toString();
    }

    /**
     * Extrai o primeiro nome do nome completo
     */
    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Teacher";
        }
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex == -1) {
            return trimmed;
        }
        return trimmed.substring(0, spaceIndex);
    }

    /**
     * Extrai o último nome do nome completo
     */
    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "User";
        }
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex == -1) {
            return trimmed;
        }
        return trimmed.substring(spaceIndex + 1);
    }
    
    private Integer calculateTotalClasses(List<TeacherAssignment> assignments) {
        return assignments.stream()
                .mapToInt(assignment -> assignment.getWorkloadHours() != null ? assignment.getWorkloadHours() : 0)
                .sum();
    }
    
    private Integer calculateClassesTaught(List<TeacherAssignment> assignments) {
        // Implementar lógica baseada em dados reais de presença
        return (int) (calculateTotalClasses(assignments) * 0.9); // Simulação: 90% de presença
    }
    
    private BigDecimal calculateAttendanceRate(Integer classesTaught, Integer totalClasses) {
        if (totalClasses == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(classesTaught).divide(BigDecimal.valueOf(totalClasses), 2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateAverageGrade(List<TeacherAssignment> assignments) {
        // Implementar lógica baseada em notas dos alunos
        return BigDecimal.valueOf(8.5); // Simulação
    }
    
    private BigDecimal calculateStudentSatisfaction(List<TeacherAssignment> assignments) {
        // Implementar lógica baseada em pesquisas de satisfação
        return BigDecimal.valueOf(4.2); // Simulação
    }
    
    private BigDecimal calculateWorkloadCompletion(List<TeacherAssignment> assignments) {
        // Implementar lógica baseada em carga horária cumprida
        return BigDecimal.valueOf(0.95); // Simulação: 95% de cumprimento
    }
    
    private PerformanceReport.OverallRating calculateOverallRating(BigDecimal attendanceRate, 
                                                                BigDecimal averageGrade, 
                                                                BigDecimal studentSatisfaction, 
                                                                BigDecimal workloadCompletion) {
        // Lógica de cálculo do rating geral
        double score = (attendanceRate.doubleValue() + averageGrade.doubleValue() + 
                      studentSatisfaction.doubleValue() + workloadCompletion.doubleValue()) / 4;
        
        if (score >= 4.5) return PerformanceReport.OverallRating.EXCELLENT;
        if (score >= 4.0) return PerformanceReport.OverallRating.GOOD;
        if (score >= 3.5) return PerformanceReport.OverallRating.SATISFACTORY;
        if (score >= 3.0) return PerformanceReport.OverallRating.NEEDS_IMPROVEMENT;
        return PerformanceReport.OverallRating.POOR;
    }
}
