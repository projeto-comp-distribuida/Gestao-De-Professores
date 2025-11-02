package com.distrischool.template.integration;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.entity.TeacherAssignment;
import com.distrischool.template.entity.Subject;
import com.distrischool.template.entity.ClassGroup;
import com.distrischool.template.entity.PerformanceReport;
import com.distrischool.template.exception.ResourceNotFoundException;
import com.distrischool.template.kafka.DistriSchoolEvent;
import com.distrischool.template.kafka.EventProducer;
import com.distrischool.template.repository.TeacherRepository;
import com.distrischool.template.repository.TeacherAssignmentRepository;
import com.distrischool.template.repository.SubjectRepository;
import com.distrischool.template.repository.ClassGroupRepository;
import com.distrischool.template.repository.PerformanceReportRepository;
import com.distrischool.template.service.TeacherManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for TeacherManagementService
 * Tests service layer with real database operations
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Teacher Management Service Integration Tests")
class TeacherManagementServiceIntegrationTest {

    @Autowired
    private TeacherManagementService teacherManagementService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassGroupRepository classGroupRepository;

    @Autowired
    private TeacherAssignmentRepository assignmentRepository;

    @Autowired
    private PerformanceReportRepository performanceReportRepository;

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
                .status(Subject.SubjectStatus.ACTIVE)
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
                .status(ClassGroup.ClassStatus.ACTIVE)
                .build();
        savedClassGroup = classGroupRepository.save(savedClassGroup);
        
        // Reset and configure EventProducer mock to do nothing
        reset(eventProducer);
        doNothing().when(eventProducer).sendEvent(any(String.class), any(DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should create teacher and publish Kafka event")
    void shouldCreateTeacherAndPublishKafkaEvent() {
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

        // When
        TeacherDTO result = teacherManagementService.createTeacher(teacherDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("João Santos", result.getName());
        assertEquals("PROF002", result.getEmployeeId());

        // Verify teacher was saved in database
        Teacher savedTeacher = teacherRepository.findById(result.getId())
                .orElseThrow();
        assertEquals("João Santos", savedTeacher.getName());

        // Verify Kafka event was published
        verify(eventProducer).sendEvent(eq("teacher.created"), any(DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should update teacher and publish Kafka event")
    void shouldUpdateTeacherAndPublishKafkaEvent() {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("Maria Silva Atualizada")
                .employeeId("PROF001")
                .email("maria.nova@email.com")
                .salary(new BigDecimal("7000.00"))
                .build();

        // When
        TeacherDTO result = teacherManagementService.updateTeacher(savedTeacher.getId(), teacherDTO);

        // Then
        assertNotNull(result);
        assertEquals("Maria Silva Atualizada", result.getName());
        assertEquals(new BigDecimal("7000.00"), result.getSalary());

        // Verify teacher was updated in database
        Teacher updatedTeacher = teacherRepository.findById(savedTeacher.getId())
                .orElseThrow();
        assertEquals("Maria Silva Atualizada", updatedTeacher.getName());
        assertEquals(new BigDecimal("7000.00"), updatedTeacher.getSalary());

        // Verify Kafka event was published
        verify(eventProducer).sendEvent(eq("teacher.updated"), any(DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent teacher")
    void shouldThrowExceptionWhenUpdatingNonExistentTeacher() {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("João Santos")
                .build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            teacherManagementService.updateTeacher(99999L, teacherDTO);
        });
    }

    @Test
    @DisplayName("Should delete teacher and publish Kafka event")
    void shouldDeleteTeacherAndPublishKafkaEvent() {
        // When
        teacherManagementService.deleteTeacher(savedTeacher.getId());

        // Then
        // Verify teacher was soft deleted
        Teacher deletedTeacher = teacherRepository.findById(savedTeacher.getId())
                .orElseThrow();
        assertNotNull(deletedTeacher.getDeletedAt());

        // Verify Kafka event was published - use any() to avoid inspecting immutable maps
        verify(eventProducer, times(1)).sendEvent(eq("teacher.deleted"), any(DistriSchoolEvent.class));
        verify(eventProducer, times(1)).sendEvent(eq("audit.log"), any(DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting teacher with active assignments")
    void shouldThrowExceptionWhenDeletingTeacherWithActiveAssignments() {
        // Given - create an active assignment
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
        assertThrows(IllegalStateException.class, () -> {
            teacherManagementService.deleteTeacher(savedTeacher.getId());
        });
    }

    @Test
    @DisplayName("Should assign teacher to class and publish Kafka event")
    void shouldAssignTeacherToClassAndPublishKafkaEvent() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);
        Integer workloadHours = 40;

        // When
        TeacherAssignment result = teacherManagementService.assignTeacherToClass(
                savedTeacher.getId(),
                savedSubject.getId(),
                savedClassGroup.getId(),
                startDate,
                endDate,
                workloadHours
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(savedTeacher.getId(), result.getTeacher().getId());
        assertEquals(savedSubject.getId(), result.getSubject().getId());
        assertEquals(savedClassGroup.getId(), result.getClassGroup().getId());
        assertEquals(workloadHours, result.getWorkloadHours());
        assertEquals(TeacherAssignment.AssignmentStatus.ACTIVE, result.getStatus());
        assertTrue(result.getNotificationSent());

        // Verify assignment was saved in database
        TeacherAssignment savedAssignment = assignmentRepository.findById(result.getId())
                .orElseThrow();
        assertEquals(workloadHours, savedAssignment.getWorkloadHours());

        // Verify Kafka events were published
        verify(eventProducer).sendEvent(eq("teacher.assigned"), any(DistriSchoolEvent.class));
        verify(eventProducer).sendEvent(eq("assignment.notification.sent"), any(DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when assigning non-existent teacher")
    void shouldThrowExceptionWhenAssigningNonExistentTeacher() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            teacherManagementService.assignTeacherToClass(
                    99999L,
                    savedSubject.getId(),
                    savedClassGroup.getId(),
                    startDate,
                    endDate,
                    40
            );
        });
    }

    @Test
    @DisplayName("Should generate performance report and publish Kafka event")
    void shouldGeneratePerformanceReportAndPublishKafkaEvent() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        // Create assignments for the teacher
        TeacherAssignment assignment = TeacherAssignment.builder()
                .teacher(savedTeacher)
                .subject(savedSubject)
                .classGroup(savedClassGroup)
                .assignmentDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .workloadHours(40)
                .status(TeacherAssignment.AssignmentStatus.ACTIVE)
                .build();
        assignmentRepository.save(assignment);

        // When
        PerformanceReport result = teacherManagementService.generatePerformanceReport(
                savedTeacher.getId(),
                startDate,
                endDate
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(savedTeacher.getId(), result.getTeacher().getId());
        assertEquals(startDate, result.getReportPeriodStart());
        assertEquals(endDate, result.getReportPeriodEnd());
        assertNotNull(result.getOverallRating());
        assertNotNull(result.getAttendanceRate());
        assertNotNull(result.getAverageGrade());

        // Verify report was saved in database
        PerformanceReport savedReport = performanceReportRepository.findById(result.getId())
                .orElseThrow();
        assertEquals(savedTeacher.getId(), savedReport.getTeacher().getId());

        // Verify Kafka event was published
        verify(eventProducer).sendEvent(eq("performance.report.generated"), any(DistriSchoolEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when generating report for non-existent teacher")
    void shouldThrowExceptionWhenGeneratingReportForNonExistentTeacher() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            teacherManagementService.generatePerformanceReport(99999L, startDate, endDate);
        });
    }

    @Test
    @DisplayName("Should get teacher schedule successfully")
    void shouldGetTeacherSchedule() {
        // Given
        String academicYear = "2024";

        // When
        List<com.distrischool.template.entity.Schedule> schedules = teacherManagementService.getTeacherSchedule(
                savedTeacher.getId(),
                academicYear
        );

        // Then
        assertNotNull(schedules);
        // Can be empty if no schedules exist
    }

    @Test
    @DisplayName("Should get class schedule successfully")
    void shouldGetClassSchedule() {
        // When
        List<com.distrischool.template.entity.Schedule> schedules = teacherManagementService.getClassSchedule(
                savedClassGroup.getId()
        );

        // Then
        assertNotNull(schedules);
        // Can be empty if no schedules exist
    }

    @Test
    @DisplayName("Should handle multiple assignments for same teacher")
    void shouldHandleMultipleAssignmentsForSameTeacher() {
        // Given - create second subject and class group
        Subject subject2 = Subject.builder()
                .name("Física")
                .code("FIS001")
                .workloadHours(30)
                .build();
        subject2 = subjectRepository.save(subject2);

        ClassGroup classGroup2 = ClassGroup.builder()
                .name("Turma B")
                .code("TURMA-B")
                .academicYear("2024")
                .build();
        classGroup2 = classGroupRepository.save(classGroup2);

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        // When - create multiple assignments
        TeacherAssignment assignment1 = teacherManagementService.assignTeacherToClass(
                savedTeacher.getId(),
                savedSubject.getId(),
                savedClassGroup.getId(),
                startDate,
                endDate,
                40
        );

        TeacherAssignment assignment2 = teacherManagementService.assignTeacherToClass(
                savedTeacher.getId(),
                subject2.getId(),
                classGroup2.getId(),
                startDate,
                endDate,
                30
        );

        // Then
        assertNotNull(assignment1);
        assertNotNull(assignment2);

        List<TeacherAssignment> assignments = assignmentRepository.findByTeacher(savedTeacher);
        assertEquals(2, assignments.size());
    }
}

