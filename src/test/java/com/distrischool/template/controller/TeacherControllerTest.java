package com.distrischool.template.controller;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.service.TeacherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeacherController.class)
class TeacherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeacherService teacherService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetAllTeachers() throws Exception {
        // Given
        TeacherDTO teacher1 = TeacherDTO.builder()
                .id(1L)
                .name("Maria Silva")
                .employeeId("PROF001")
                .email("maria@email.com")
                .build();

        TeacherDTO teacher2 = TeacherDTO.builder()
                .id(2L)
                .name("João Santos")
                .employeeId("PROF002")
                .email("joao@email.com")
                .build();

        List<TeacherDTO> teachers = Arrays.asList(teacher1, teacher2);

        when(teacherService.findAll()).thenReturn(teachers);

        // When & Then
        mockMvc.perform(get("/api/v1/teachers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Maria Silva"))
                .andExpect(jsonPath("$.data[1].name").value("João Santos"));
    }

    @Test
    void shouldGetTeacherById() throws Exception {
        // Given
        TeacherDTO teacher = TeacherDTO.builder()
                .id(1L)
                .name("Maria Silva")
                .employeeId("PROF001")
                .email("maria@email.com")
                .qualification("Mestrado em Matemática")
                .subjects(Arrays.asList("Matemática", "Física"))
                .build();

        when(teacherService.findById(1L)).thenReturn(teacher);

        // When & Then
        mockMvc.perform(get("/api/v1/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Maria Silva"))
                .andExpect(jsonPath("$.data.employeeId").value("PROF001"));
    }

    @Test
    void shouldCreateTeacher() throws Exception {
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

        TeacherDTO createdTeacher = TeacherDTO.builder()
                .id(1L)
                .name("Maria Silva")
                .employeeId("PROF001")
                .email("maria@email.com")
                .build();

        when(teacherService.create(any(TeacherDTO.class))).thenReturn(createdTeacher);

        // When & Then
        mockMvc.perform(post("/api/v1/teachers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacherDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Maria Silva"))
                .andExpect(jsonPath("$.data.employeeId").value("PROF001"));
    }

    @Test
    void shouldUpdateTeacher() throws Exception {
        // Given
        TeacherDTO teacherDTO = TeacherDTO.builder()
                .name("Maria Silva Atualizada")
                .employeeId("PROF001")
                .email("maria.nova@email.com")
                .salary(new BigDecimal("6000.00"))
                .build();

        TeacherDTO updatedTeacher = TeacherDTO.builder()
                .id(1L)
                .name("Maria Silva Atualizada")
                .employeeId("PROF001")
                .email("maria.nova@email.com")
                .salary(new BigDecimal("6000.00"))
                .build();

        when(teacherService.update(eq(1L), any(TeacherDTO.class))).thenReturn(updatedTeacher);

        // When & Then
        mockMvc.perform(put("/api/v1/teachers/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(teacherDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Maria Silva Atualizada"))
                .andExpect(jsonPath("$.data.salary").value(6000.00));
    }

    @Test
    void shouldDeleteTeacher() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/teachers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Professor excluído com sucesso"));
    }

    @Test
    void shouldGetTeachersBySubject() throws Exception {
        // Given
        String subject = "Matemática";
        TeacherDTO teacher = TeacherDTO.builder()
                .id(1L)
                .name("Maria Silva")
                .employeeId("PROF001")
                .subjects(Arrays.asList("Matemática", "Física"))
                .build();

        when(teacherService.findBySubject(subject)).thenReturn(Arrays.asList(teacher));

        // When & Then
        mockMvc.perform(get("/api/v1/teachers/subject/Matemática"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Maria Silva"));
    }

    @Test
    void shouldGetTeachersByStatus() throws Exception {
        // Given
        TeacherDTO teacher = TeacherDTO.builder()
                .id(1L)
                .name("Maria Silva")
                .employeeId("PROF001")
                .status(Teacher.TeacherStatus.ACTIVE)
                .build();

        when(teacherService.findByStatus(Teacher.TeacherStatus.ACTIVE)).thenReturn(Arrays.asList(teacher));

        // When & Then
        mockMvc.perform(get("/api/v1/teachers/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    }
}
