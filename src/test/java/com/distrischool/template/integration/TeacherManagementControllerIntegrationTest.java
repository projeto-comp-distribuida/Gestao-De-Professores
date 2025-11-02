package com.distrischool.template.integration;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.entity.TeacherAssignment;
import com.distrischool.template.entity.Subject;
import com.distrischool.template.entity.ClassGroup;
import com.distrischool.template.feign.AuthServiceClient;
import com.distrischool.template.kafka.EventProducer;
import com.distrischool.template.repository.TeacherRepository;
import com.distrischool.template.repository.TeacherAssignmentRepository;
import com.distrischool.template.repository.SubjectRepository;
import com.distrischool.template.repository.ClassGroupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.distrischool.template.integration.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TeacherManagementController
 * Tests the full flow from HTTP request to database and back
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
@DisplayName("Teacher Management Controller Integration Tests")
class TeacherManagementControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private TeacherAssignmentRepository assignmentRepository;

    @MockBean
    private AuthServiceClient authServiceClient;

    @MockBean
    private EventProducer eventProducer;

    private Teacher savedTeacher;
    private Subject savedSubject;
    private ClassGroup savedClassGroup;

    @BeforeEach
    void setUp() {
        // Create test data
        savedTeacher = Teacher.builder()
                .name("Maria Silva")
                .employeeId("PROF001")
                .birthDate(LocalDate.of(1980, 5, 15))
                .email("maria@email.com")
                .phone("85987654321")
                .qualification("Mestrado em Matemática")
                .subjects(new ArrayList<>(Arrays.asList("Matemática", "Física")))
                .status(Teacher.TeacherStatus.ACTIVE)
                .hireDate(LocalDate.of(2020, 1, 1))
                .salary(new BigDecimal("5000.00"))
                .build();
        savedTeacher = teacherRepository.save(savedTeacher);

        savedSubject = Subject.builder()
                .name("Matemática")
                .code("MAT001")
                .description("Matemática Básica")
                .workloadHours(40)
                .build();
        savedSubject = subjectRepository.save(savedSubject);

        savedClassGroup = ClassGroup.builder()
                .name("Turma A")
                .code("TURMA-A")
                .academicYear("2024")
                .gradeLevel("1º Ano")
                .maxStudents(30)
                .currentStudents(25)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build();
        savedClassGroup = classGroupRepository.save(savedClassGroup);
        
        // Reset and configure EventProducer mock to do nothing
        reset(eventProducer);
        doNothing().when(eventProducer).sendEvent(any(String.class), any(com.distrischool.template.kafka.DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should get all teachers successfully")
    void shouldGetAllTeachers() throws Exception {
        // Given - teachers already created in setUp

        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/teachers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$.data[0].employeeId").value("PROF001"));
    }

    @Test
    @DisplayName("Should get teacher by ID successfully")
    void shouldGetTeacherById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/teachers/{id}", savedTeacher.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(savedTeacher.getId()))
                .andExpect(jsonPath("$.data.name").value("Maria Silva"))
                .andExpect(jsonPath("$.data.email").value("maria@email.com"));
    }

    @Test
    @DisplayName("Should return 404 when teacher not found")
    void shouldReturn404WhenTeacherNotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/teachers/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create teacher successfully with admin role")
    void shouldCreateTeacherWithAdminRole() throws Exception {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .birthDate(LocalDate.of(1985, 3, 20))
                .email("joao@email.com")
                .phone("85987654322")
                .qualification("Doutorado em Física")
                .subjects(new ArrayList<>(Arrays.asList("Física", "Química")))
                .status(Teacher.TeacherStatus.ACTIVE)
                .hireDate(LocalDate.of(2021, 1, 1))
                .salary(new BigDecimal("6000.00"))
                .build();

        // Mock getUserByAuth0Id to return a user with ID 1L
        com.distrischool.template.dto.auth.UserResponse userResponse = 
                com.distrischool.template.dto.auth.UserResponse.builder()
                        .id(1L)
                        .email("admin@email.com")
                        .auth0Id("1")
                        .active(true)
                        .build();
        com.distrischool.template.dto.auth.ApiResponse<com.distrischool.template.dto.auth.UserResponse> userApiResponse = 
                com.distrischool.template.dto.auth.ApiResponse.<com.distrischool.template.dto.auth.UserResponse>builder()
                        .success(true)
                        .data(userResponse)
                        .message("User found")
                        .build();
        when(authServiceClient.getUserByAuth0Id(eq("1")))
                .thenReturn(userApiResponse);

        // Mock admin check
        com.distrischool.template.dto.auth.ApiResponse<Boolean> adminResponse = 
                com.distrischool.template.dto.auth.ApiResponse.<Boolean>builder()
                        .success(true)
                        .data(true)
                        .message("User is admin")
                        .build();
        when(authServiceClient.hasRole(eq(1L), eq("ADMIN")))
                .thenReturn(adminResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/teacher-management/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(teacherDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("João Santos"))
                .andExpect(jsonPath("$.data.employeeId").value("PROF002"));

        // Verify teacher was saved in database
        Teacher createdTeacher = teacherRepository.findByEmployeeId("PROF002")
                .orElseThrow();
        assert createdTeacher.getName().equals("João Santos");
    }

    @Test
    @DisplayName("Should return 403 when creating teacher without admin role")
    void shouldReturn403WhenCreatingTeacherWithoutAdminRole() throws Exception {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .build();

        // Mock getUserByAuth0Id to return a user with ID 2L
        com.distrischool.template.dto.auth.UserResponse userResponse = 
                com.distrischool.template.dto.auth.UserResponse.builder()
                        .id(2L)
                        .email("user@email.com")
                        .auth0Id("2")
                        .active(true)
                        .build();
        com.distrischool.template.dto.auth.ApiResponse<com.distrischool.template.dto.auth.UserResponse> userApiResponse = 
                com.distrischool.template.dto.auth.ApiResponse.<com.distrischool.template.dto.auth.UserResponse>builder()
                        .success(true)
                        .data(userResponse)
                        .message("User found")
                        .build();
        when(authServiceClient.getUserByAuth0Id(eq("2")))
                .thenReturn(userApiResponse);

        // Mock non-admin user
        com.distrischool.template.dto.auth.ApiResponse<Boolean> nonAdminResponse = 
                com.distrischool.template.dto.auth.ApiResponse.<Boolean>builder()
                        .success(true)
                        .data(false)
                        .message("User is not admin")
                        .build();
        when(authServiceClient.hasRole(eq(2L), eq("ADMIN")))
                .thenReturn(nonAdminResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/teacher-management/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "2")
                        .content(objectMapper.writeValueAsString(teacherDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Apenas usuários com role ADMIN podem criar professores"));
    }

    @Test
    @DisplayName("Should return 401 when creating teacher without authentication")
    void shouldReturn401WhenCreatingTeacherWithoutAuth() throws Exception {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/teacher-management/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacherDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should update teacher successfully")
    void shouldUpdateTeacher() throws Exception {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("Maria Silva Atualizada")
                .employeeId("PROF001")
                .email("maria.nova@email.com")
                .phone("85987654399")
                .qualification("Doutorado em Matemática")
                .salary(new BigDecimal("7000.00"))
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/teacher-management/teachers/{id}", savedTeacher.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacherDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Maria Silva Atualizada"))
                .andExpect(jsonPath("$.data.salary").value(7000.00));

        // Verify teacher was updated in database
        Teacher updatedTeacher = teacherRepository.findById(savedTeacher.getId())
                .orElseThrow();
        assert updatedTeacher.getName().equals("Maria Silva Atualizada");
        assert updatedTeacher.getSalary().equals(new BigDecimal("7000.00"));
    }

    @Test
    @DisplayName("Should delete teacher successfully")
    void shouldDeleteTeacher() throws Exception {
        // Given - ensure no active assignments exist for this teacher
        // (setUp already creates a teacher without assignments)
        
        // When & Then
        mockMvc.perform(delete("/api/v1/teacher-management/teachers/{id}", savedTeacher.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Professor excluído com sucesso"));

        // Verify teacher was soft deleted (deletedAt is set)
        Teacher deletedTeacher = teacherRepository.findById(savedTeacher.getId())
                .orElseThrow();
        assert deletedTeacher.getDeletedAt() != null;
    }

    @Test
    @DisplayName("Should assign teacher to class successfully")
    void shouldAssignTeacherToClass() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        Integer workloadHours = 40;

        // When & Then
        mockMvc.perform(post("/api/v1/teacher-management/assignments")
                        .param("teacherId", String.valueOf(savedTeacher.getId()))
                        .param("subjectId", String.valueOf(savedSubject.getId()))
                        .param("classGroupId", String.valueOf(savedClassGroup.getId()))
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("workloadHours", String.valueOf(workloadHours))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teacher.id").value(savedTeacher.getId()))
                .andExpect(jsonPath("$.data.subject.id").value(savedSubject.getId()))
                .andExpect(jsonPath("$.data.classGroup.id").value(savedClassGroup.getId()))
                .andExpect(jsonPath("$.data.workloadHours").value(40));

        // Verify assignment was saved in database
        List<TeacherAssignment> assignments = assignmentRepository.findByTeacher(savedTeacher);
        assert assignments.size() == 1;
        assert assignments.get(0).getWorkloadHours().equals(workloadHours);
    }

    @Test
    @DisplayName("Should get teacher assignments successfully")
    void shouldGetTeacherAssignments() throws Exception {
        // Given - create assignment
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacher(savedTeacher)
                .subject(savedSubject)
                .classGroup(savedClassGroup)
                .assignmentDate(LocalDate.now())
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .workloadHours(40)
                .status(TeacherAssignment.AssignmentStatus.ACTIVE)
                .notificationSent(false)
                .build();
        assignmentRepository.save(assignment);

        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/assignments/teacher/{teacherId}", savedTeacher.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should get class assignments successfully")
    void shouldGetClassAssignments() throws Exception {
        // Given - create assignment
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacher(savedTeacher)
                .subject(savedSubject)
                .classGroup(savedClassGroup)
                .assignmentDate(LocalDate.now())
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .workloadHours(40)
                .status(TeacherAssignment.AssignmentStatus.ACTIVE)
                .build();
        assignmentRepository.save(assignment);

        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/assignments/class/{classGroupId}", savedClassGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should get teacher schedule successfully")
    void shouldGetTeacherSchedule() throws Exception {
        // Given - create schedule (mock data needed)
        String academicYear = "2024";

        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/schedules/teacher/{teacherId}", savedTeacher.getId())
                        .param("academicYear", academicYear)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should get class schedule successfully")
    void shouldGetClassSchedule() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/schedules/class/{classGroupId}", savedClassGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Should generate performance report successfully")
    void shouldGeneratePerformanceReport() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        // When & Then
        mockMvc.perform(post("/api/v1/teacher-management/performance-reports")
                        .param("teacherId", String.valueOf(savedTeacher.getId()))
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teacher.id").value(savedTeacher.getId()))
                .andExpect(jsonPath("$.data.reportPeriodStart").value(startDate.toString()))
                .andExpect(jsonPath("$.data.reportPeriodEnd").value(endDate.toString()));
    }

    @Test
    @DisplayName("Should get dashboard overview successfully")
    void shouldGetDashboardOverview() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/dashboard/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Should get performance summary successfully")
    void shouldGetPerformanceSummary() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/dashboard/performance-summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("Should get health check successfully")
    void shouldGetHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/teacher-management/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("Teacher Management Service"));
    }

    @Test
    @DisplayName("Should validate teacher DTO on create")
    void shouldValidateTeacherDTOOnCreate() throws Exception {
        // Given - invalid teacher DTO (missing required fields)
        // Use a unique employeeId to avoid conflicts
        TeacherDTO invalidTeacherDTO = TeacherDTO.builder()
                .name("") // Empty name should fail @NotBlank validation
                .employeeId("INVALID-TEST-001") // Unique to avoid conflicts
                .build();

        // Mock getUserByAuth0Id to return a user with ID 1L
        com.distrischool.template.dto.auth.UserResponse userResponse = 
                com.distrischool.template.dto.auth.UserResponse.builder()
                        .id(1L)
                        .email("admin@email.com")
                        .auth0Id("1")
                        .active(true)
                        .build();
        com.distrischool.template.dto.auth.ApiResponse<com.distrischool.template.dto.auth.UserResponse> userApiResponse = 
                com.distrischool.template.dto.auth.ApiResponse.<com.distrischool.template.dto.auth.UserResponse>builder()
                        .success(true)
                        .data(userResponse)
                        .message("User found")
                        .build();
        when(authServiceClient.getUserByAuth0Id(eq("1")))
                .thenReturn(userApiResponse);

        // Mock admin check
        com.distrischool.template.dto.auth.ApiResponse<Boolean> adminResponse = 
                com.distrischool.template.dto.auth.ApiResponse.<Boolean>builder()
                        .success(true)
                        .data(true)
                        .message("User is admin")
                        .build();
        when(authServiceClient.hasRole(eq(1L), eq("ADMIN")))
                .thenReturn(adminResponse);

        // When & Then - expect BadRequest due to validation failure
        mockMvc.perform(post("/api/v1/teacher-management/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(invalidTeacherDTO)))
                .andExpect(status().isBadRequest());
    }
}

