package com.distrischool.template.integration;

import com.distrischool.template.entity.Teacher;
import com.distrischool.template.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TeacherRepository
 * Tests database operations with real JPA/Hibernate
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Teacher Repository Integration Tests")
class TeacherRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeacherRepository teacherRepository;

    private Teacher savedTeacher;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        teacherRepository.deleteAll();

        // Create and save a teacher
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
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should save teacher successfully")
    void shouldSaveTeacherSuccessfully() {
        // Given
        Teacher teacher = Teacher.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();

        // When
        Teacher result = teacherRepository.save(teacher);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertNotNull(result.getId());
        Teacher found = teacherRepository.findById(result.getId()).orElseThrow();
        assertEquals("João Santos", found.getName());
        assertEquals("PROF002", found.getEmployeeId());
    }

    @Test
    @DisplayName("Should find teacher by employee ID")
    void shouldFindTeacherByEmployeeId() {
        // When
        Optional<Teacher> result = teacherRepository.findByEmployeeId("PROF001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Maria Silva", result.get().getName());
        assertEquals("PROF001", result.get().getEmployeeId());
    }

    @Test
    @DisplayName("Should return empty when employee ID not found")
    void shouldReturnEmptyWhenEmployeeIdNotFound() {
        // When
        Optional<Teacher> result = teacherRepository.findByEmployeeId("PROF999");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find teachers by status")
    void shouldFindTeachersByStatus() {
        // Given - create inactive teacher
        Teacher inactiveTeacher = Teacher.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .status(Teacher.TeacherStatus.INACTIVE)
                .build();
        teacherRepository.save(inactiveTeacher);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Teacher> activeTeachers = teacherRepository.findByStatus(Teacher.TeacherStatus.ACTIVE);
        List<Teacher> inactiveTeachers = teacherRepository.findByStatus(Teacher.TeacherStatus.INACTIVE);

        // Then
        assertEquals(1, activeTeachers.size());
        assertEquals("Maria Silva", activeTeachers.get(0).getName());
        assertEquals(1, inactiveTeachers.size());
        assertEquals("João Santos", inactiveTeachers.get(0).getName());
    }

    @Test
    @DisplayName("Should find teachers by name containing")
    void shouldFindTeachersByNameContaining() {
        // Given - create another teacher
        Teacher teacher2 = Teacher.builder()
                .name("Maria Santos")
                .employeeId("PROF002")
                .email("maria.santos@email.com")
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();
        teacherRepository.save(teacher2);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Teacher> results = teacherRepository.findByNameContaining("Maria");

        // Then
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(t -> t.getName().equals("Maria Silva")));
        assertTrue(results.stream().anyMatch(t -> t.getName().equals("Maria Santos")));
    }

    @Test
    @DisplayName("Should find teachers by subject")
    void shouldFindTeachersBySubject() {
        // Given - create teacher with different subject
        Teacher teacher2 = Teacher.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .subjects(new ArrayList<>(Arrays.asList("Química", "Biologia")))
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();
        teacherRepository.save(teacher2);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Teacher> mathTeachers = teacherRepository.findBySubject("Matemática");

        // Then
        assertEquals(1, mathTeachers.size());
        assertEquals("Maria Silva", mathTeachers.get(0).getName());
        assertTrue(mathTeachers.get(0).getSubjects().contains("Matemática"));
    }

    @Test
    @DisplayName("Should find all active teachers")
    void shouldFindAllActiveTeachers() {
        // Given - create deleted teacher (soft delete)
        Teacher deletedTeacher = Teacher.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();
        deletedTeacher = teacherRepository.save(deletedTeacher);
        deletedTeacher.setDeletedAt(LocalDate.now().atStartOfDay());
        teacherRepository.save(deletedTeacher);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Teacher> activeTeachers = teacherRepository.findAllActive();

        // Then
        assertEquals(1, activeTeachers.size());
        assertEquals("Maria Silva", activeTeachers.get(0).getName());
    }

    @Test
    @DisplayName("Should find teachers by hire date range")
    void shouldFindTeachersByHireDateRange() {
        // Given - create teachers with different hire dates
        Teacher teacher2021 = Teacher.builder()
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .hireDate(LocalDate.of(2021, 1, 1))
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();
        teacherRepository.save(teacher2021);

        Teacher teacher2022 = Teacher.builder()
                .name("Pedro Costa")
                .employeeId("PROF003")
                .email("pedro@email.com")
                .hireDate(LocalDate.of(2022, 1, 1))
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();
        teacherRepository.save(teacher2022);
        entityManager.flush();
        entityManager.clear();

        // When
        LocalDate startDate = LocalDate.of(2021, 1, 1);
        LocalDate endDate = LocalDate.of(2021, 12, 31);
        List<Teacher> results = teacherRepository.findByHireDateBetween(startDate, endDate);

        // Then
        assertEquals(1, results.size());
        assertEquals("João Santos", results.get(0).getName());
    }

    @Test
    @DisplayName("Should update teacher successfully")
    void shouldUpdateTeacherSuccessfully() {
        // Given
        savedTeacher.setName("Maria Silva Atualizada");
        savedTeacher.setSalary(new BigDecimal("7000.00"));

        // When
        Teacher result = teacherRepository.save(savedTeacher);
        entityManager.flush();
        entityManager.clear();

        // Then
        Teacher updated = teacherRepository.findById(result.getId()).orElseThrow();
        assertEquals("Maria Silva Atualizada", updated.getName());
        assertEquals(new BigDecimal("7000.00"), updated.getSalary());
    }

    @Test
    @DisplayName("Should delete teacher successfully")
    void shouldDeleteTeacherSuccessfully() {
        // When
        teacherRepository.deleteById(savedTeacher.getId());
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<Teacher> result = teacherRepository.findById(savedTeacher.getId());
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should enforce unique employee ID constraint")
    void shouldEnforceUniqueEmployeeIdConstraint() {
        // Given
        Teacher duplicateTeacher = Teacher.builder()
                .name("Another Teacher")
                .employeeId("PROF001") // Same employee ID
                .email("another@email.com")
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();

        // When & Then
        assertThrows(Exception.class, () -> {
            teacherRepository.save(duplicateTeacher);
            entityManager.flush();
        });
    }
}

