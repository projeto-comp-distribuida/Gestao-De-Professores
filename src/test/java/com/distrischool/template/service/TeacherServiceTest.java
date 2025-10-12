package com.distrischool.template.service;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.kafka.EventProducer;
import com.distrischool.template.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {
    
    @Mock
    private TeacherRepository teacherRepository;
    
    @Mock
    private EventProducer eventProducer;
    
    @InjectMocks
    private TeacherService teacherService;
    
    @Test
    void shouldCreateTeacherSuccessfully() {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("Maria Silva")
                .employeeId("PROF001")
                .birthDate(LocalDate.of(1980, 5, 15))
                .email("maria@email.com")
                .qualification("Mestrado em Matemática")
                .subjects(Arrays.asList("Matemática", "Física"))
                .hireDate(LocalDate.of(2020, 1, 1))
                .salary(new BigDecimal("5000.00"))
                .build();
        
        Teacher savedTeacher = new Teacher();
        savedTeacher.setId(1L);
        savedTeacher.setName("Maria Silva");
        savedTeacher.setEmployeeId("PROF001");
        
        when(teacherRepository.save(any(Teacher.class))).thenReturn(savedTeacher);
        
        // When
        TeacherDTO result = teacherService.create(teacherDTO);
        
        // Then
        assertNotNull(result);
        assertEquals("Maria Silva", result.getName());
        assertEquals("PROF001", result.getEmployeeId());
        verify(teacherRepository).save(any(Teacher.class));
        verify(eventProducer).sendEvent(eq("teacher.created"), any(Teacher.class));
    }
    
    @Test
    void shouldFindTeacherById() {
        // Given
        Long teacherId = 1L;
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setName("Maria Silva");
        teacher.setEmployeeId("PROF001");
        
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        
        // When
        TeacherDTO result = teacherService.findById(teacherId);
        
        // Then
        assertNotNull(result);
        assertEquals(teacherId, result.getId());
        assertEquals("Maria Silva", result.getName());
        assertEquals("PROF001", result.getEmployeeId());
    }
    
    @Test
    void shouldFindTeacherByEmployeeId() {
        // Given
        String employeeId = "PROF001";
        Teacher teacher = new Teacher();
        teacher.setId(1L);
        teacher.setName("Maria Silva");
        teacher.setEmployeeId(employeeId);
        
        when(teacherRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(teacher));
        
        // When
        TeacherDTO result = teacherService.findByEmployeeId(employeeId);
        
        // Then
        assertNotNull(result);
        assertEquals(employeeId, result.getEmployeeId());
        assertEquals("Maria Silva", result.getName());
    }
    
    @Test
    void shouldFindTeachersBySubject() {
        // Given
        String subject = "Matemática";
        Teacher teacher1 = new Teacher();
        teacher1.setId(1L);
        teacher1.setName("Maria Silva");
        teacher1.setSubjects(Arrays.asList("Matemática", "Física"));
        
        Teacher teacher2 = new Teacher();
        teacher2.setId(2L);
        teacher2.setName("João Santos");
        teacher2.setSubjects(Arrays.asList("Matemática", "Química"));
        
        List<Teacher> teachers = Arrays.asList(teacher1, teacher2);
        
        when(teacherRepository.findBySubject(subject)).thenReturn(teachers);
        
        // When
        List<TeacherDTO> result = teacherService.findBySubject(subject);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Maria Silva", result.get(0).getName());
        assertEquals("João Santos", result.get(1).getName());
    }
    
    @Test
    void shouldUpdateTeacherSuccessfully() {
        // Given
        Long teacherId = 1L;
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("Maria Silva Atualizada")
                .employeeId("PROF001")
                .email("maria.nova@email.com")
                .salary(new BigDecimal("6000.00"))
                .build();
        
        Teacher existingTeacher = new Teacher();
        existingTeacher.setId(teacherId);
        existingTeacher.setName("Maria Silva");
        existingTeacher.setEmployeeId("PROF001");
        
        Teacher updatedTeacher = new Teacher();
        updatedTeacher.setId(teacherId);
        updatedTeacher.setName("Maria Silva Atualizada");
        updatedTeacher.setEmployeeId("PROF001");
        
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(existingTeacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(updatedTeacher);
        
        // When
        TeacherDTO result = teacherService.update(teacherId, teacherDTO);
        
        // Then
        assertNotNull(result);
        assertEquals("Maria Silva Atualizada", result.getName());
        assertEquals(6000.0, result.getSalary());
        verify(teacherRepository).save(any(Teacher.class));
        verify(eventProducer).sendEvent(eq("teacher.updated"), any(Teacher.class));
    }
    
    @Test
    void shouldDeleteTeacherSuccessfully() {
        // Given
        Long teacherId = 1L;
        Teacher teacher = new Teacher();
        teacher.setId(teacherId);
        teacher.setName("Maria Silva");
        
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(teacherRepository.save(any(Teacher.class))).thenReturn(teacher);
        
        // When
        teacherService.delete(teacherId);
        
        // Then
        verify(teacherRepository).save(any(Teacher.class));
        verify(eventProducer).sendEvent(eq("teacher.deleted"), any(Teacher.class));
    }
}
