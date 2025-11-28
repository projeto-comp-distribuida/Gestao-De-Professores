package com.distrischool.template.service;

import com.distrischool.template.dto.TeacherDTO;
import com.distrischool.template.dto.auth.ApiResponse;
import com.distrischool.template.dto.auth.AuthResponse;
import com.distrischool.template.dto.auth.RegisterUserRequest;
import com.distrischool.template.dto.auth.UserResponse;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.exception.BusinessException;
import com.distrischool.template.exception.ResourceNotFoundException;
import com.distrischool.template.feign.AuthServiceClient;
import com.distrischool.template.kafka.DistriSchoolEvent;
import com.distrischool.template.kafka.EventProducer;
import com.distrischool.template.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * Busca professor por Auth0 ID
     * Retorna apenas o ID do professor se encontrado, null caso contrário
     */
    @Transactional(readOnly = true)
    public Long getTeacherIdByAuth0Id(String auth0Id) {
        log.debug("Buscando professor por Auth0 ID: {}", auth0Id);
        return teacherRepository.findByAuth0Id(auth0Id)
                .filter(t -> t.getDeletedAt() == null)
                .map(Teacher::getId)
                .orElse(null);
    }
    
    public TeacherDTO create(TeacherDTO teacherDTO, String authorizationHeader) {
        log.info("Criando novo professor: {}", teacherDTO.getName());
        
        // Validação: email é obrigatório para criar usuário no Auth0
        if (teacherDTO.getEmail() == null || teacherDTO.getEmail().isBlank()) {
            log.error("Tentativa de criar professor sem email");
            throw new BusinessException("Email é obrigatório para criar um professor");
        }
        
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
     * Valida se o usuário existe no serviço de autenticação e tem a role TEACHER
     * 
     * @param userId ID do usuário no serviço de autenticação
     * @throws BusinessException se o usuário não existe ou não tem a role TEACHER
     */
    public void validateUserIsTeacher(Long userId) {
        log.info("Validando se usuário {} existe e tem role TEACHER", userId);
        
        try {
            // Busca o usuário no serviço de autenticação
            ApiResponse<UserResponse> userResponse = authServiceClient.getUserById(userId);
            
            if (userResponse == null || !Boolean.TRUE.equals(userResponse.getSuccess())) {
                log.warn("Usuário não encontrado no serviço de autenticação - ID: {}", userId);
                throw new BusinessException("Usuário não encontrado com ID: " + userId);
            }
            
            UserResponse user = userResponse.getData();
            if (user == null) {
                log.warn("Dados do usuário não retornados - ID: {}", userId);
                throw new BusinessException("Usuário não encontrado com ID: " + userId);
            }
            
            // Verifica se o usuário tem a role TEACHER
            ApiResponse<Boolean> roleResponse = authServiceClient.hasRole(userId, "TEACHER");
            
            if (roleResponse == null || !Boolean.TRUE.equals(roleResponse.getSuccess())) {
                log.warn("Erro ao verificar role do usuário - ID: {}", userId);
                throw new BusinessException("Erro ao verificar role do usuário. Tente novamente mais tarde.");
            }
            
            Boolean hasTeacherRole = roleResponse.getData();
            if (hasTeacherRole == null || !hasTeacherRole) {
                log.warn("Usuário {} não tem a role TEACHER", userId);
                throw new BusinessException("Usuário com ID " + userId + " não possui a role TEACHER");
            }
            
            log.info("Usuário {} validado com sucesso - possui role TEACHER", userId);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar usuário no serviço de autenticação - ID: {}, Erro: {}", userId, e.getMessage(), e);
            throw new BusinessException("Erro ao validar usuário no serviço de autenticação: " + e.getMessage());
        }
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
     * Verifica se o usuário tem a role ADMIN diretamente do token JWT
     * Usado para autorização de criação de professores
     * 
     * @param jwt O token JWT do usuário autenticado
     * @return true se o usuário tem role ADMIN, false caso contrário
     */
    public boolean isAdmin(org.springframework.security.oauth2.jwt.Jwt jwt) {
        if (jwt == null) {
            log.warn("JWT é nulo, não é possível verificar role ADMIN");
            return false;
        }
        
        try {
            log.debug("Verificando role ADMIN no token JWT para usuário: {}", jwt.getSubject());
            
            // Verifica todas as claims que contêm "role" no nome
            var allClaims = jwt.getClaims();
            for (Map.Entry<String, Object> entry : allClaims.entrySet()) {
                if (entry.getKey().contains("role")) {
                    Object roleValue = entry.getValue();
                    log.debug("Encontrado claim de role: {} = {}", entry.getKey(), roleValue);
                    
                    // Caso 1: Role como Collection (array) - ex: "https://api.distrischool.com/role": ["ADMIN"]
                    if (roleValue instanceof Collection<?> roles) {
                        for (Object r : roles) {
                            if (r != null && "ADMIN".equalsIgnoreCase(r.toString())) {
                                log.debug("Role ADMIN encontrada no token");
                                return true;
                            }
                        }
                    }
                    // Caso 2: Role como String (valor único) - ex: "https://api.distrischool.com/role": "ADMIN"
                    else if (roleValue instanceof String role) {
                        if ("ADMIN".equalsIgnoreCase(role.trim())) {
                            log.debug("Role ADMIN encontrada no token");
                            return true;
                        }
                    }
                }
            }
            
            // Também verifica as authorities do Spring Security (já processadas pelo JwtAuthenticationConverter)
            var authorities = jwt.getClaimAsStringList("authorities");
            if (authorities != null) {
                for (String authority : authorities) {
                    if (authority != null && (authority.equals("ROLE_ADMIN") || authority.equals("ADMIN"))) {
                        log.debug("Role ADMIN encontrada nas authorities");
                        return true;
                    }
                }
            }
            
            log.debug("Role ADMIN não encontrada no token para usuário: {}", jwt.getSubject());
            return false;
            
        } catch (Exception e) {
            log.error("Erro ao verificar role ADMIN no token JWT: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifica se o usuário tem a role ADMIN via chamada ao auth service
     * @deprecated Use isAdmin(Jwt jwt) para verificar diretamente do token JWT
     */
    @Deprecated
    public boolean isAdmin(String auth0Id) {
        try {
            log.debug("Verificando se usuário {} tem role ADMIN via auth service", auth0Id);
            
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
    
    /**
     * Cria um usuário no serviço de autenticação para o professor
     */
    private String createAuthUserForTeacher(Teacher teacher, String authorizationHeader) {
        log.info("Iniciando registro de usuário Auth0 para professor: {} ({})", teacher.getEmail(), teacher.getName());
        
        // Validação adicional: garantir que o email não seja null
        if (teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            throw new BusinessException("Email do professor é obrigatório para criar usuário no Auth0");
        }
        
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            log.warn("Authorization header está vazio ou nulo. Tentando registrar sem header de autorização.");
        }
        
        try {
            String password = generateSecurePassword();

            RegisterUserRequest registerUserRequest = RegisterUserRequest.builder()
                    .email(teacher.getEmail().trim()) // Garantir que não há espaços
                    .password(password)
                    .confirmPassword(password)
                    .firstName(extractFirstName(teacher.getName()))
                    .lastName(extractLastName(teacher.getName()))
                    .phone(teacher.getPhone())
                    .documentNumber(null) // Professores podem não ter CPF obrigatório
                    .roles(Set.of("TEACHER"))
                    .build();
            
            log.debug("RegisterUserRequest criado: email={}, firstName={}, lastName={}", 
                    registerUserRequest.getEmail(), 
                    registerUserRequest.getFirstName(), 
                    registerUserRequest.getLastName());

            log.info("Chamando authServiceClient.registerUser para email: {}", teacher.getEmail());
            ApiResponse<AuthResponse> response = authServiceClient.registerUser(authorizationHeader, registerUserRequest);
            log.info("Resposta recebida do authServiceClient: success={}, message={}", 
                    response != null ? response.getSuccess() : "null", 
                    response != null ? response.getMessage() : "null");

            if (response == null) {
                throw new BusinessException("Serviço de autenticação não respondeu ao registrar o usuário do professor");
            }

            // Verificar se há erro de validação mesmo quando success=true
            if (response.getMessage() != null && response.getMessage().contains("validação")) {
                String errorMessage = response.getMessage();
                if (response.getData() != null) {
                    // Tentar extrair informações de erro do data
                    errorMessage += ". Detalhes: " + response.getData().toString();
                }
                throw new BusinessException("Erro de validação ao registrar usuário no Auth0: " + errorMessage);
            }

            if (!Boolean.TRUE.equals(response.getSuccess())) {
                String message = response.getMessage() != null ? response.getMessage() : "Resposta sem sucesso do serviço de autenticação";
                throw new BusinessException("Falha ao registrar usuário no Auth0: " + message);
            }

            AuthResponse data = response.getData();
            if (data == null || data.getUser() == null) {
                // Se data não é AuthResponse, pode ser um Map com erros de validação
                if (response.getData() != null && !(response.getData() instanceof AuthResponse)) {
                    throw new BusinessException("Erro de validação ao registrar usuário no Auth0: " + response.getData().toString());
                }
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
}
