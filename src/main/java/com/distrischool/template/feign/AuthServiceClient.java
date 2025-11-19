package com.distrischool.template.feign;

import com.distrischool.template.dto.auth.ApiResponse;
import com.distrischool.template.dto.auth.UserResponse;
import com.distrischool.template.dto.auth.AuthResponse;
import com.distrischool.template.dto.auth.RegisterUserRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client para comunicação com o serviço de autenticação
 */
@FeignClient(name = "auth-service", url = "${microservice.auth.url:http://microservice-auth-dev:8080}")
public interface AuthServiceClient {

    /**
     * Registra um novo usuário no serviço de autenticação (mesmo fluxo dos usuários finais)
     */
    @PostMapping("/api/v1/auth/register")
    ApiResponse<AuthResponse> registerUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody RegisterUserRequest request);

    /**
     * Busca um usuário por Auth0 ID
     */
    @GetMapping("/api/v1/users/auth0/{auth0Id}")
    ApiResponse<UserResponse> getUserByAuth0Id(@PathVariable String auth0Id);

    /**
     * Verifica se o usuário tem uma role específica
     * Retorna true se o usuário tem a role especificada
     */
    @GetMapping("/api/v1/users/{userId}/has-role")
    ApiResponse<Boolean> hasRole(@PathVariable Long userId, @RequestParam String role);
}







