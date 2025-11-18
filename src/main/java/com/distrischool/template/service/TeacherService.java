package com.distrischool.template.service;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.dto.auth.ApiResponse;
import com.distrischool.template.dto.auth.UserResponse;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.exception.ResourceNotFoundException;
import com.distrischool.template.feign.AuthServiceClient;
import com.distrischool.template.kafka.DistriSchoolEvent;
import com.distrischool.template.kafka.EventProducer;
import com.distrischool.template.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeacherService {
    
    private final TeacherRepository teacherRepository;
    private final EventProducer eventProducer;
    private final AuthServiceClient authServiceClient;
    
    public List<TeacherDTO> findAll() {
        log.info("Buscando todos os professores");
        return teacherRepository.findAllActive()
                .stream()
                .map(TeacherDTO::new)
                .collect(Collectors.toList());
    }
    
    public TeacherDTO findById(Long id) {
        log.info("Buscando professor com ID: {}", id);
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + id));
        return new TeacherDTO(teacher);
    }
    
    public TeacherDTO findByEmployeeId(String employeeId) {
        log.info("Buscando professor com matrícula: {}", employeeId);
        Teacher teacher = teacherRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com matrícula: " + employeeId));
        return new TeacherDTO(teacher);
    }
    
    public TeacherDTO create(TeacherDTO teacherDTO) {
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
        
        Teacher savedTeacher = teacherRepository.save(teacher);
        
        // Publicar evento no Kafka
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("teacher", savedTeacher);
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.created", 
                "teacher-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.created", event);
        
        log.info("Professor criado com sucesso: {}", savedTeacher.getId());
        return new TeacherDTO(savedTeacher);
    }
    
    public TeacherDTO update(Long id, TeacherDTO teacherDTO) {
        log.info("Atualizando professor com ID: {}", id);
        
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + id));
        
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
        
        // Publicar evento no Kafka
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("teacher", updatedTeacher);
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.updated", 
                "teacher-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.updated", event);
        
        log.info("Professor atualizado com sucesso: {}", updatedTeacher.getId());
        return new TeacherDTO(updatedTeacher);
    }
    
    public void delete(Long id) {
        log.info("Excluindo professor com ID: {}", id);
        
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + id));
        
        teacher.setDeletedAt(java.time.LocalDateTime.now()); // Soft delete
        
        teacherRepository.save(teacher);
        
        // Publicar evento no Kafka
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("teacher", teacher);
        DistriSchoolEvent event = DistriSchoolEvent.create(
                "teacher.deleted", 
                "teacher-service", 
                eventData
        );
        eventProducer.sendEvent("teacher.deleted", event);
        
        log.info("Professor excluído com sucesso: {}", id);
    }
    
    public List<TeacherDTO> findBySubject(String subject) {
        log.info("Buscando professores da disciplina: {}", subject);
        return teacherRepository.findBySubject(subject)
                .stream()
                .map(TeacherDTO::new)
                .collect(Collectors.toList());
    }
    
    public List<TeacherDTO> findByStatus(Teacher.TeacherStatus status) {
        log.info("Buscando professores com status: {}", status);
        return teacherRepository.findByStatus(status)
                .stream()
                .map(TeacherDTO::new)
                .collect(Collectors.toList());
    }
    
    public List<TeacherDTO> findByHireDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Buscando professores contratados entre {} e {}", startDate, endDate);
        return teacherRepository.findByHireDateBetween(startDate, endDate)
                .stream()
                .map(TeacherDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca múltiplos professores por IDs (validação em lote)
     * Retorna uma lista de Maps com os dados dos professores
     */
    public List<Map<String, Object>> getTeachersByIds(List<Long> teacherIds) {
        log.debug("Buscando múltiplos professores por IDs: {}", teacherIds);
        
        if (teacherIds == null || teacherIds.isEmpty()) {
            return List.of();
        }
        
        List<Teacher> teachers = teacherRepository.findByIdsNotDeleted(teacherIds);
        
        return teachers.stream()
                .map(teacher -> {
                    TeacherDTO dto = new TeacherDTO(teacher);
                    return convertDtoToMap(dto);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Converte TeacherDTO para Map<String, Object>
     */
    private Map<String, Object> convertDtoToMap(TeacherDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", dto.getId());
        map.put("name", dto.getName());
        map.put("employeeId", dto.getEmployeeId());
        map.put("birthDate", dto.getBirthDate());
        map.put("email", dto.getEmail());
        map.put("phone", dto.getPhone());
        map.put("qualification", dto.getQualification());
        map.put("subjects", dto.getSubjects());
        map.put("status", dto.getStatus() != null ? dto.getStatus().toString() : null);
        map.put("hireDate", dto.getHireDate());
        map.put("salary", dto.getSalary());
        return map;
    }
    
    /**
     * Verifica se o usuário tem a role ADMIN
     * Usado para autorização de criação de professores
     */
    public boolean isAdmin(String auth0Id) {
        try {
            log.debug("Verificando se usuário {} tem role ADMIN", auth0Id);
            
            // Busca usuário por auth0Id
            ApiResponse<UserResponse> userResponse = 
                    authServiceClient.getUserByAuth0Id(auth0Id);
            
            if (!userResponse.getSuccess() || userResponse.getData() == null) {
                log.warn("Usuário não encontrado no auth service: {}", auth0Id);
                return false;
            }
            
            Long userId = userResponse.getData().getId();
            
            // Verifica se tem role ADMIN
            ApiResponse<Boolean> roleResponse = authServiceClient.hasRole(userId, "ADMIN");
            
            boolean isAdmin = roleResponse.getSuccess() && 
                             roleResponse.getData() != null && 
                             roleResponse.getData();
            
            log.debug("Usuário {} é admin: {}", auth0Id, isAdmin);
            return isAdmin;
            
        } catch (Exception e) {
            log.error("Erro ao verificar role do usuário {}: {}", auth0Id, e.getMessage(), e);
            return false;
        }
    }
}
