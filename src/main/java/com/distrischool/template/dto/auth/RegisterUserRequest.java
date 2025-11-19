package com.distrischool.template.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO utilizado para registrar usuários no serviço de autenticação,
 * seguindo o mesmo contrato exposto para os clientes finais.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {
    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private String phone;
    private String documentNumber;
    private Set<String> roles;
}

